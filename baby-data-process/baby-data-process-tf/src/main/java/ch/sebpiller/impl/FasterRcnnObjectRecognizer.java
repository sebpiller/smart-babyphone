package ch.sebpiller.impl;

import ch.sebpiller.babyphone.detection.Detected;
import ch.sebpiller.babyphone.detection.DetectionResult;
import ch.sebpiller.babyphone.detection.ObjectRecognizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.tensorflow.*;
import org.tensorflow.ndarray.FloatNdArray;
import org.tensorflow.ndarray.NdArrays;
import org.tensorflow.op.Ops;
import org.tensorflow.op.core.Constant;
import org.tensorflow.op.core.Placeholder;
import org.tensorflow.op.core.Reshape;
import org.tensorflow.op.image.DecodeJpeg;
import org.tensorflow.op.image.EncodeJpeg;
import org.tensorflow.op.image.ResizeBilinear;
import org.tensorflow.proto.ConfigProto;
import org.tensorflow.types.TFloat32;
import org.tensorflow.types.TString;
import org.tensorflow.types.TUint8;
import org.tensorflow.types.family.TNumber;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

/**
 * @see <a href="https://www.kaggle.com/models/tensorflow/faster-rcnn-inception-resnet-v2/tensorFlow2/1024x1024/1">Faster RCNN Resnet Model</a>
 */
//@AutoLog(printArgs = true)
@Slf4j
@Service
public class FasterRcnnObjectRecognizer implements ObjectRecognizer, InitializingBean {

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
    private final SavedModelBundle model = SavedModelBundle.load(Objects.requireNonNull(Path.of(MODEL_PATH)).toString(), "serve");
    private final Graph graph = new Graph();
    private Ops tf;
    private TreeMap<Float, String> cocoTreeMap;
    private Session session;

    @Override
    public DetectionResult detectAndWrite(BufferedImage image, Optional<String> outputPath, Predicate<Detected> p) {
        var decodeImage = toDecodeJpeg(image);

        var detected = new ArrayList<Detected>();
        var result = DetectionResult.builder().detected(detected);


        var shapeResult = session.runner().fetch(decodeImage).run();
        var imageShape = shapeResult.get(0).shape();
        log.debug("Image shape: {}", imageShape);

        var imageShapeArray = imageShape.asArray();
        var reshape = tf.reshape(decodeImage,
                tf.array(1,
                        imageShapeArray[0],
                        imageShapeArray[1],
                        imageShapeArray[2]
                )
        );

        var reshapeResult = session.runner().fetch(reshape).run();
        var reshapeTensor = (TUint8) reshapeResult.get(0);
        var feedDict = new HashMap<String, Tensor>();
        feedDict.put("input_tensor", reshapeTensor);

        var outputTensorMap = invokeIaModel(feedDict);
        var numDetections = (TFloat32) outputTensorMap.get("num_detections").orElseThrow();
        var numDetects = (int) numDetections.getFloat(0);
        log.info("Number of detections found: {}", numDetects);

        if (numDetects <= 0) {
            log.warn("No detections were found.");
        } else {
            var detectionBoxes = (TFloat32) outputTensorMap.get("detection_boxes").orElseThrow();
            var detectionScores = (TFloat32) outputTensorMap.get("detection_scores").orElseThrow();
            var detectionClasses = (TFloat32) outputTensorMap.get("detection_classes").orElseThrow();
            var boxArray = new ArrayList<FloatNdArray>();

            for (var n = 0; n < numDetects; n++) {
                var detectionScore = detectionScores.getFloat(0, n);
                var detectionClass = detectionClasses.getFloat(0, n);
                var d = cocoTreeMap.get(detectionClass - 1);
                var e = detectionBoxes.get(0, n);

                int x = (int) (e.getFloat(1) * image.getWidth());
                int y = (int) (e.getFloat(0) * image.getHeight());
                int width = (int) (e.getFloat(3) * image.getWidth()) - x;
                int height = (int) (e.getFloat(2) * image.getHeight()) - y;


                var t = new Detected(d, detectionScore, x, y, width, height);
                if (p.test(t)) {
                    detected.add(t);
                }
            }
//
//            Operand<TFloat32> colors = tf.constant(new float[][]{
//                    {0.9f, 0.3f, 0.3f, 0.0f},
//                    {0.3f, 0.3f, 0.9f, 0.0f},
//                    {0.3f, 0.9f, 0.3f, 0.0f}
//            });
//
//            var boxesShape = Shape.of(1, boxArray.size(), 4);
//            try (var boxes = TFloat32.tensorOf(boxesShape)) {
//                if (!boxArray.isEmpty()) {
//                    boxes.setFloat(1, 0, 0, 0);
//                    var boxCount = 0;
//                    for (var floatNdArray : boxArray) {
//                        boxes.set(floatNdArray, 0, boxCount);
//                        boxCount++;
//                    }
//                }
//
//                log.info("Drawing bounding boxes on detected objects...");
//                var scaledImage = tf.math.div(
//                        tf.dtypes.cast(tf.constant(reshapeTensor), TFloat32.class),
//                        tf.constant(255.0f)
//                );
//
//                var boxesPlaceHolder = tf.placeholder(TFloat32.class, Placeholder.shape(boxesShape));
//                var boundingBoxOverlay = tf.image.drawBoundingBoxes(scaledImage, boxesPlaceHolder, colors);
//
//                var rescaledOverlay = tf.math.mul(boundingBoxOverlay, tf.constant(255.0f));
//                var reshapedOverlay = tf.reshape(
//                        rescaledOverlay,
//                        tf.array(
//                                imageShapeArray[0],
//                                imageShapeArray[1],
//                                imageShapeArray[2]
//                        )
//                );
//
//                if (outputPath.isPresent()) {
//                    encodeAndWrite(outputPath.get(), tf, reshapedOverlay, boxesPlaceHolder, boxes);
//
//                }
//
//                result.image(image);
//            }
        }

        return result.build();
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
        Constant<TString> constant = tf.constant(tString);

        var options = DecodeJpeg.channels(3L).ratio(8L);
        var decodeImage = tf.image.decodeJpeg(constant, options);
        return decodeImage;
    }

    private EncodeJpeg encodeAndWrite(String outputPath, Ops tf, Reshape<TFloat32> reshapedOverlay, Placeholder<TFloat32> boxesPlaceHolder, TFloat32 boxes) {
        var outImagePathPlaceholder = tf.placeholder(TString.class);
        var jpgOptions = EncodeJpeg.quality(100L);
        var encodedImage = tf.image.encodeJpeg(tf.dtypes.cast(reshapedOverlay, TUint8.class), jpgOptions);
        writeF(outputPath, tf, boxesPlaceHolder, boxes, outImagePathPlaceholder, encodedImage);
        return encodedImage;
    }

    private void writeF(String outputPath, Ops tf, Placeholder<TFloat32> boxesPlaceHolder, TFloat32 boxes, Placeholder<TString> outImagePathPlaceholder, EncodeJpeg encodedImage) {
        var writeFile = tf.io.writeFile(outImagePathPlaceholder, encodedImage);
        log.info("Writing output image to: {}", outputPath);
        session.runner().feed(outImagePathPlaceholder, TString.scalarOf(outputPath))
                .feed(boxesPlaceHolder, boxes)
                .addTarget(writeFile).run();
    }

    private Result invokeIaModel(Map<String, Tensor> input) {
        var startTime = System.currentTimeMillis();
        var result = model.function("serving_default").call(input);
        var endTime = System.currentTimeMillis();
        log.info("Execution time for model invocation: {} ms", (endTime - startTime));
        return result;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        cocoTreeMap = new TreeMap<>();
        float cocoCount = 0;
        for (var cocoLabel : cocoLabels) {
            cocoTreeMap.put(cocoCount, cocoLabel);
            cocoCount++;
        }

        session = new Session(graph, ConfigProto.newBuilder()
                // .addDeviceFilters("/device:GPU:0")
                .setLogDevicePlacement(true)
                .setAllowSoftPlacement(true)
//                .setGraphOptions(GraphOptions.newBuilder()
//                        .setOptimizerOptions(OptimizerOptions.newBuilder()
//                            //    .setGlobalJitLevel(OptimizerOptions.GlobalJitLevel.ON_2)
//                                .build())
//                        .build())
//                .setGpuOptions(GPUOptions.newBuilder()
//                        .setForceGpuCompatible(true)
//                                .setAllowGrowth(true)
//                        //.setExperimental(GPUOptions.Experimental.newBuilder().build())
//                        .build())
                .build());


        tf = Ops.create(graph);
    }

    public ResizeBilinear resizeImage(Operand<TNumber> imageTensor) {
        long[] shape = imageTensor.shape().asArray();
        float height = shape[shape.length - 2];
        float width = shape[shape.length - 1];
        float minSize = Math.min(height, width);
        float maxSize = Math.max(height, width);

        // Determine the target size based on training mode
        float size = 320;

        // Calculate scaling factor
        float scaleFactor = size / minSize;


        return tf.image.resizeBilinear(imageTensor, tf.constant(new int[]{
                (int) Math.round(height * scaleFactor),
                (int) Math.round(width * scaleFactor)
        }));

    }

}
