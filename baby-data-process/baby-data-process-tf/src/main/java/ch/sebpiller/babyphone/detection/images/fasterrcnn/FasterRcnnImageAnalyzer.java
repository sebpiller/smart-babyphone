package ch.sebpiller.babyphone.detection.images.fasterrcnn;

import ch.sebpiller.babyphone.detection.Detected;
import ch.sebpiller.babyphone.detection.DetectionResult;
import ch.sebpiller.babyphone.detection.ImageAnalyzer;
import ch.sebpiller.babyphone.toolkit.tensorflow.BaseTensorFlowRunnerFacade;
import ch.sebpiller.spi.toolkit.aop.AutoLog;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.tensorflow.Result;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.ndarray.NdArrays;
import org.tensorflow.op.Ops;
import org.tensorflow.op.image.DecodeJpeg;
import org.tensorflow.proto.ConfigProto;
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

    public static final String MODELS = "/home/seb/models";
    public static final String HIGH_RES = MODELS + "/faster_rcnn_inception_resnet_v2_1024x1024";
    public static final String MODEL_PATH = HIGH_RES;
    public static final String LOW_RES = MODELS + "/faster-rcnn-inception-resnet-v2-tensorflow2-640x640-v1";
    private static final String[] COCO_LABELS = IOUtils
            .readLines(Objects.requireNonNull(FasterRcnnImageAnalyzer.class.getResourceAsStream("/coco_labels.csv")), StandardCharsets.UTF_8)
            .toArray(String[]::new);
    private final SavedModelBundle model;
    private final Ops o;
    private final Session s;

    @Autowired
    public FasterRcnnImageAnalyzer() {
        super(null);
        log.info("creating {}. Loading model from {}", this, MODEL_PATH);
        model = SavedModelBundle.load(MODEL_PATH);
        o = Ops.create(model.graph());
        s = new Session(model.graph(), ConfigProto.newBuilder()
                .setAllowSoftPlacement(true)
                .build());
    }

    @Override
    public DetectionResult detectObjectsOn(BufferedImage image, Predicate<Detected> includeInResult) {
        var decodeJpeg = toDecodeJpeg(image);
        var detected = new ArrayList<Detected>();
        var result = DetectionResult.builder().detected(detected);

        try (var shapeResult = model.session().runner().fetch(decodeJpeg).run()) {
            var imageShapeArray = shapeResult.get(0).shape().asArray();
            var reshape = o.reshape(decodeJpeg,
                    o.array(1,
                            imageShapeArray[0],
                            imageShapeArray[1],
                            imageShapeArray[2]
                    )
            );

            try (var reshapeResult = model.session().runner().fetch(reshape).run();
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
                        try (var detectionBoxes = (TFloat32) outputTensorMap.get("detection_boxes").orElseThrow();
                             var detectionScores = (TFloat32) outputTensorMap.get("detection_scores").orElseThrow();
                             var detectionClasses = (TFloat32) outputTensorMap.get("detection_classes").orElseThrow()) {

                            for (var n = 0; n < numDetects; n++) {
                                var detectionScore = detectionScores.getFloat(0, n);
                                var detectionClass = detectionClasses.getFloat(0, n);
                                var d = COCO_LABELS[(int) (detectionClass - 1)];
                                var e = detectionBoxes.get(0, n);

                                var x = (int) (e.getFloat(1) * image.getWidth());
                                var y = (int) (e.getFloat(0) * image.getHeight());
                                var width = (int) (e.getFloat(3) * image.getWidth()) - x;
                                var height = (int) (e.getFloat(2) * image.getHeight()) - y;

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
                //.ratio(8L)
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
            model.graph().close();
            model.close();
        } finally {
            super.close();
        }
    }
}
