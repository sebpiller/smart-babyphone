package ch.sebpiller.babyphone.detection.images.fasterrcnn;

import ch.sebpiller.babyphone.detection.Detected;
import ch.sebpiller.babyphone.detection.DetectionResult;
import ch.sebpiller.babyphone.detection.ImageAnalyzer;
import ch.sebpiller.babyphone.tensorflow.BaseTensorFlowRunnerFacade;
import ch.sebpiller.spi.toolkit.aop.AutoLog;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.tensorflow.Result;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import org.tensorflow.ndarray.NdArrays;
import org.tensorflow.op.core.Constant;
import org.tensorflow.op.image.DecodeJpeg;
import org.tensorflow.types.TFloat32;
import org.tensorflow.types.TString;
import org.tensorflow.types.TUint8;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * @see <a href="https://www.kaggle.com/models/tensorflow/faster-rcnn-inception-resnet-v2/tensorFlow2/1024x1024/1">Faster RCNN Resnet Model</a>
 */
//@AutoLog(printArgs = true)
@Lazy
@Slf4j
@Service
@AutoLog
public class FasterRcnnImageAnalyzer extends BaseTensorFlowRunnerFacade implements ImageAnalyzer {


    public static final String MODELS = "/home/seb/models";
    public static final String HIGH_RES = MODELS + "/faster_rcnn_inception_resnet_v2_1024x1024";
    public static final String MODEL_PATH = HIGH_RES;
    public static final String LOW_RES = MODELS + "/faster-rcnn-inception-resnet-v2-tensorflow2-640x640-v1";
    private static final String[] cocoLabels = new String[]{
            "person",
            "bicycle",
            "car",
            "motorcycle",
            "airplane",
            "bus",
            "train",
            "truck",
            "boat",
            "traffic light",
            "fire hydrant",
            "street sign",
            "stop sign",
            "parking meter",
            "bench",
            "bird",
            "cat",
            "dog",
            "horse",
            "sheep",
            "cow",
            "elephant",
            "bear",
            "zebra",
            "giraffe",
            "hat",
            "backpack",
            "umbrella",
            "shoe",
            "eye glasses",
            "handbag",
            "tie",
            "suitcase",
            "frisbee",
            "skis",
            "snowboard",
            "sports ball",
            "kite",
            "baseball bat",
            "baseball glove",
            "skateboard",
            "surfboard",
            "tennis racket",
            "bottle",
            "plate",
            "wine glass",
            "cup",
            "fork",
            "knife",
            "spoon",
            "bowl",
            "banana",
            "apple",
            "sandwich",
            "orange",
            "broccoli",
            "carrot",
            "hot dog",
            "pizza",
            "donut",
            "cake",
            "chair",
            "couch",
            "potted plant",
            "bed",
            "mirror",
            "dining table",
            "window",
            "desk",
            "toilet",
            "door",
            "tv",
            "laptop",
            "mouse",
            "remote",
            "keyboard",
            "cell phone",
            "microwave",
            "oven",
            "toaster",
            "sink",
            "refrigerator",
            "blender",
            "book",
            "clock",
            "vase",
            "scissors",
            "teddy bear",
            "hair drier",
            "toothbrush",
            "hair brush",
    };
    private TreeMap<Float, String> cocoTreeMap;
    private SavedModelBundle model;

    public FasterRcnnImageAnalyzer() {
        super(MODEL_PATH + "/saved_model.pb");
    }

    @Override
    public DetectionResult detectObjectsOn(BufferedImage image, Predicate<Detected> includeInResult) {
        var decodeJpeg = toDecodeJpeg(image);

        var detected = new ArrayList<Detected>();
        var result = DetectionResult.builder().detected(detected);

        try (var shapeResult = session.runner().fetch(decodeJpeg).run()) {
            var imageShape = shapeResult.get(0).shape();
            var imageShapeArray = imageShape.asArray();
            var reshape = ops.reshape(decodeJpeg,
                    ops.array(1,
                            imageShapeArray[0],
                            imageShapeArray[1],
                            imageShapeArray[2]
                    )
            );

            try (var reshapeResult = session.runner().fetch(reshape).run();
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
                                var d = cocoTreeMap.get(detectionClass - 1);
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", baos);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to convert BufferedImage to byte array", e);
        }
        byte[] imageData = baos.toByteArray();

        TString tString = TString.tensorOfBytes(NdArrays.scalarOfObject(imageData));
        Constant<TString> constant = ops.constant(tString);

        var options = DecodeJpeg.channels(3L)
                //.ratio(8L)
                ;
        return ops.image.decodeJpeg(constant, options);
    }

    private Result invokeIaModel(Map<String, Tensor> input) {
        var startTime = System.currentTimeMillis();
        var result = model.function("serving_default").call(input);
        var endTime = System.currentTimeMillis();
        log.info("Execution time for model invocation: {} ms", (endTime - startTime));
        return result;
    }

    @PostConstruct
    private void postConstruct() {
        cocoTreeMap = new TreeMap<>();
        float cocoCount = 0;
        for (var cocoLabel : cocoLabels) {
            cocoTreeMap.put(cocoCount, cocoLabel);
            cocoCount++;
        }

        model = SavedModelBundle.load(MODEL_PATH);
    }

}
