package ch.sebpiller.babyphone.service.ia;

import ch.sebpiller.babyphone.config.properties.BabyPhoneProperties;
import ch.sebpiller.babyphone.config.properties.Detector;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.HOGDescriptor;
import org.opencv.objdetect.Objdetect;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

//@Service
@Slf4j
@RequiredArgsConstructor
@Deprecated
// OpenCV implem
public class RecognitionService implements InitializingBean {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
    public static final Scalar BORDER = new Scalar(100, 255, 100);
    public static final Scalar BLACK = new Scalar(0, 0, 0);
    private final BabyPhoneProperties babyPhoneProperties;

    private Map<Detector, CascadeClassifier> classifiers;

    @Override
    public void afterPropertiesSet() {
        var x = new HashMap<Detector, CascadeClassifier>();
        for (var detector : babyPhoneProperties.getDetectors()) {
            x.put(detector, new CascadeClassifier(Objects.requireNonNull(getClass().getResource(detector.getFile())).getPath()));
        }
        classifiers = Collections.unmodifiableMap(x);
    }

    @SneakyThrows
    public Mat annotateImage(Mat frame) {
// remove background ?
//        var filter = Video.createBackgroundSubtractorMOG2(1, 30, true);
//        var noBackgroundFrame = new Mat();
//        filter.apply(frame, noBackgroundFrame);
//        Imgcodecs.imwrite("no-background.jpg", noBackgroundFrame);

        var frameCopy = frame;
        //  Imgproc.resize(frame, frameCopy, new Size(640, 480));

        log.debug("Converting frame to grayscale.");
        var grayFrame = new Mat();
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);

        log.debug("Equalizing histogram of the grayscale frame.");
        Imgproc.equalizeHist(grayFrame, grayFrame);

        //detectWithHOG(grayFrame, frameCopy);

        for (var e : classifiers.entrySet()) {
            var k = e.getKey();
            var v = e.getValue();
            log.debug("Detecting object using {}", k.getName());

            var found = new MatOfRect();
            v.detectMultiScale(grayFrame,
                    found,
                    1.1, // ?
                    5, // ?
                    Objdetect.CASCADE_SCALE_IMAGE, //| Objdetect.CASCADE_DO_CANNY_PRUNING ,//| Objdetect.CASCADE_FIND_BIGGEST_OBJECT, // ?
                    new Size(k.minSizeAdaptedFor(grayFrame.cols()), k.minSizeAdaptedFor(grayFrame.rows())),
                    grayFrame.size());

            var f = found.toArray();
            log.debug("Number of object detected: {}", f.length);
            var color = new Scalar(Math.random() * 255, Math.random() * 255, Math.random() * 255);

            for (var z : f) {
                log.debug("  >> Detected {} at [Top-Left: ({}, {}), Bottom-Right: ({}, {})]", k.getName(), z.tl().x, z.tl().y, z.br().x, z.br().y);
                Imgproc.rectangle(frameCopy, z.tl(), z.br(), color, 2);
                Imgproc.putText(frameCopy, k.getName(), z.tl(), Imgproc.FONT_HERSHEY_PLAIN, 2, BLACK);
            }
        }

        return frameCopy;
    }

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

}
