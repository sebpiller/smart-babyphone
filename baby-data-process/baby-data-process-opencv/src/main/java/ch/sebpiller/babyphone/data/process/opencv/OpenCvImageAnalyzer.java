package ch.sebpiller.babyphone.data.process.opencv;

import ch.sebpiller.babyphone.detection.Detected;
import ch.sebpiller.babyphone.detection.DetectionResult;
import ch.sebpiller.babyphone.detection.ImageAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.HOGDescriptor;
import org.opencv.objdetect.Objdetect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@ToString(exclude = "classifiers")
@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnMissingBean
public class OpenCvImageAnalyzer implements ImageAnalyzer {
    public static final Scalar BORDER = new Scalar(100, 255, 100);
    public static final Scalar BLACK = new Scalar(0, 0, 0);

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private final Map<Detector, CascadeClassifier> classifiers;

    private void detectWithHOG(Mat grayFrame, Mat frameCopy) {
        var found2 = new MatOfRect();
        var found3 = new MatOfDouble();
        var hog = new HOGDescriptor();
        hog.setSVMDetector(HOGDescriptor.getDefaultPeopleDetector());
        hog.detectMultiScale(grayFrame, found2, found3);
        var ff = found2.toArray();
        log.debug("Number of object detected: {}", ff.length);

        for (var z : ff) {
            log.debug("  >> Detected {} at [Top-Left: ({}, {}), Bottom-Right: ({}, {})]", "HOG", z.tl().x, z.tl().y, z.br().x, z.br().y);
            Imgproc.rectangle(frameCopy, z.tl(), z.br(), BORDER, 2);
            Imgproc.putText(frameCopy, "HOG", z.tl(), Imgproc.FONT_HERSHEY_PLAIN, 2, BLACK);
        }
    }

    @Override
    public DetectionResult detectObjectsOn(BufferedImage image, Predicate<Detected> acceptanceTest) {
        var matImage = convertBufferedImageToMat(image);

        return DetectionResult.builder()
                .image(image)
                .detected(findDetected(matImage).stream().sorted().toList())
                .build();
    }

    private Mat convertBufferedImageToMat(BufferedImage image) {
        var type = image.getType() == BufferedImage.TYPE_BYTE_GRAY ? org.opencv.core.CvType.CV_8UC1 : org.opencv.core.CvType.CV_8UC3;
        var mat = new Mat(image.getHeight(), image.getWidth(), type);
        var data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, data);
        return mat;
    }

    @SneakyThrows
    public List<Detected> findDetected(Mat frame) {
        var l = new ArrayList<Detected>();

        var factory = 5d;

        Imgproc.resize(frame, frame, new Size(), 1d / factory, 1d / factory, Imgproc.INTER_AREA);

        var grayFrame = new Mat();
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(grayFrame, grayFrame);

        for (var e : classifiers.entrySet()) {
            var k = e.getKey();
            var v = e.getValue();
            log.debug("Detecting object using {}", k.getName());

            var found = new MatOfRect();
            v.detectMultiScale(grayFrame,
                    found,
                    1.1, // ?
                    5, // ?
                    Objdetect.CASCADE_SCALE_IMAGE | Objdetect.CASCADE_DO_CANNY_PRUNING | Objdetect.CASCADE_FIND_BIGGEST_OBJECT, // ?
                    new Size(k.minSizeAdaptedFor(grayFrame.cols()), k.minSizeAdaptedFor(grayFrame.rows())),
                    grayFrame.size());

            for (var z : found.toArray()) {
                l.add(new Detected(k.getName(), 1,
                        (int) (z.x * factory),
                        (int) (z.y * factory),
                        (int) (z.width * factory),
                        (int) (z.height * factory)
                ));
            }
        }

        return l;
    }
}
