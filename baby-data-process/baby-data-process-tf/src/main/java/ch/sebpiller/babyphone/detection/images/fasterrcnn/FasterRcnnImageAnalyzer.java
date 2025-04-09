package ch.sebpiller.babyphone.detection.images.fasterrcnn;

import ch.sebpiller.babyphone.detection.Detected;
import ch.sebpiller.babyphone.detection.DetectionResult;
import ch.sebpiller.babyphone.detection.ImageAnalyzer;
import ch.sebpiller.babyphone.toolkit.tensorflow.BaseTensorFlowRunnerFacade;
import ch.sebpiller.spi.toolkit.aop.AutoLog;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.tensorflow.*;
import org.tensorflow.ndarray.NdArrays;
import org.tensorflow.op.Ops;
import org.tensorflow.op.image.DecodeJpeg;
import org.tensorflow.proto.ConfigProto;
import org.tensorflow.proto.GraphOptions;
import org.tensorflow.proto.OptimizerOptions;
import org.tensorflow.types.TFloat32;
import org.tensorflow.types.TString;
import org.tensorflow.types.TUint8;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * @see <a href="https://www.kaggle.com/models/tensorflow/faster-rcnn-inception-resnet-v2/tensorFlow2/1024x1024/1">Faster RCNN Resnet Model</a>
 */
@Lazy
@Slf4j
@Service
@AutoLog
@ToString
public class FasterRcnnImageAnalyzer extends BaseTensorFlowRunnerFacade implements ImageAnalyzer {

    private static final String[] COCO_LABELS = IOUtils.readLines(Objects.requireNonNull(FasterRcnnImageAnalyzer.class.getResourceAsStream("/coco_labels.csv")), StandardCharsets.UTF_8).toArray(String[]::new);
    private final Configuration configuration;

    private final SavedModelBundle model;
    private final Session s;
    private final Graph g;
    private final Ops o;

    public FasterRcnnImageAnalyzer() {
        this(null);
    }

    @Autowired
    public FasterRcnnImageAnalyzer(Configuration conf) {
        super(null);
        this.configuration = conf == null ? Configuration.builder().build() : conf;

        log.info("creating {} with configuration {}", this, this.configuration);
        model = SavedModelBundle.load(this.configuration.getModelPath(), SavedModelBundle.DEFAULT_TAG);
        g = model.graph();
        //s = model.session();
        o = Ops.create(g);

        s = new Session(g, ConfigProto.getDefaultInstance().toBuilder()
                .setAllowSoftPlacement(true)
                .setLogDevicePlacement(true)
                .setUsePerSessionThreads(true)
                .setGraphOptions(GraphOptions.newBuilder()
                        .setOptimizerOptions(OptimizerOptions.getDefaultInstance().toBuilder()
                                .setDoFunctionInlining(true)
                                .setDoCommonSubexpressionElimination(true)
                                .setOptLevel(OptimizerOptions.Level.L1)
                                .setGlobalJitLevel(OptimizerOptions.GlobalJitLevel.ON_2)
                                .build())
                        .setPlacePrunedGraph(true)
                        .setEnableBfloat16Sendrecv(true)
                        .setInferShapes(true)
                        .setEnableRecvScheduling(true)
                        .setInferShapes(true)
                )
                .build());
    }

    @Override
    public DetectionResult detectObjectsOn(BufferedImage image, Predicate<Detected> includeInResult) {

        //var i = ImageUtils.resizeImage(image, 32, 24);
        var i = image;

        var decodeJpeg = toDecodeJpeg(i);
        var detected = new ArrayList<Detected>();
        var result = DetectionResult.builder().detected(detected);

        try (var shapeResult = s.runner().fetch(decodeJpeg).run()) {
            var imageShapeArray = shapeResult.get(0).shape().asArray();
            var reshape = o.reshape(decodeJpeg, o.array(1, imageShapeArray[0], imageShapeArray[1], imageShapeArray[2]));

            try (var reshapeResult = s.runner().fetch(reshape).run();
                 var reshapeTensor = (TUint8) reshapeResult.get(0)) {
                var feedDict = new HashMap<String, Tensor>();
                feedDict.put("input_tensor", reshapeTensor);

                try (var outputTensorMap = invokeIaModel(feedDict);
                     var numDetections = (TFloat32) outputTensorMap.get("num_detections").orElseThrow()) {
                    var numDetects = (int) numDetections.getFloat(0);
                    log.info("Number of detections found: {}", numDetects);

                    if (numDetects <= 0) {
                        log.warn("No detections were found.");
                    } else {
                        try (var detectionBoxes = (TFloat32) outputTensorMap.get("detection_boxes").orElseThrow(); var detectionScores = (TFloat32) outputTensorMap.get("detection_scores").orElseThrow(); var detectionClasses = (TFloat32) outputTensorMap.get("detection_classes").orElseThrow()) {

                            for (var n = 0; n < numDetects; n++) {
                                var detectionScore = detectionScores.getFloat(0, n);
                                var detectionClass = detectionClasses.getFloat(0, n);
                                var d = COCO_LABELS[(int) (detectionClass - 1)];
                                var e = detectionBoxes.get(0, n);

                                var x = (int) (e.getFloat(1) * i.getWidth());
                                var y = (int) (e.getFloat(0) * i.getHeight());
                                var width = (int) (e.getFloat(3) * i.getWidth()) - x;
                                var height = (int) (e.getFloat(2) * i.getHeight()) - y;

                                var t = new Detected(d, detectionScore, x, y, width, height);
                                if (includeInResult.test(t)) {
                                    detected.add(t);
                                }
                            }
                        }
                    }
                }
            }

            return result.image(image).build();
        }
    }

    private DecodeJpeg toDecodeJpeg(BufferedImage image) {
        var baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", baos);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to convert BufferedImage to byte array", e);
        }
        var imageData = baos.toByteArray();

        var tString = TString.tensorOfBytes(NdArrays.scalarOfObject(imageData));
        var constant = o.constant(tString);

        var options = DecodeJpeg

                .channels(3L)
                //  .ratio(128L)
                ;
        return o.image.decodeJpeg(constant, options);
    }

    private Result invokeIaModel(Map<String, Tensor> input) {
        var startTime = System.currentTimeMillis();
        var result = model.function("serving_default").call(input);
        var endTime = System.currentTimeMillis();
        log.info("Execution time for model invocation: {} ms", (endTime - startTime));
        return result;
    }

    @Override
    public void close() {
        try {
            s.close();
            g.close();
            model.close();
        } finally {
            super.close();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Configuration {

        public static final String MODELS = "/home/seb/models";
        public static final String HIGH_RES = MODELS + "/faster_rcnn_inception_resnet_v2_1024x1024";
        public static final String LOW_RES = MODELS + "/faster-rcnn-inception-resnet-v2-tensorflow2-640x640-v1";
        public static final String MODEL_PATH = HIGH_RES;
        //public static final String MODEL_PATH = LOW_RES;
        @Builder.Default
        private boolean useGpu = false;
        @Builder.Default
        private int gpuMemoryFraction = 1;
        @Builder.Default
        private int gpuMemoryLimitMB = 1024;
        @Builder.Default
        private int numThreads = 4;
        @Builder.Default
        private String modelPath = MODEL_PATH;
    }
}
