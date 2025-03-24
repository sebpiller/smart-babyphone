package ch.sebpiller.babyphone.service.ia;

import ch.sebpiller.babyphone.service.ia.impl.FasterRcnnObjectRecognizer;
import lombok.Builder;
import lombok.Data;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.op.image.EncodeJpeg;
import org.tensorflow.proto.Summary;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface ObjectRecognizer {
    DetectionResult detectAndWrite(String imagePath, String outputPath, Predicate<FasterRcnnObjectRecognizer.DetectedObject> p);

    @Data
    @Builder
    class DetectionResult {
        private List<Detected> detected;
        private EncodeJpeg image;

        public boolean isEmpty() {
            return detected == null || detected.isEmpty();
        }

        Stream<Detected> forType(String type) {
            return getDetected()
                    .stream()
                    .filter(x -> x.getType().equalsIgnoreCase(type));
        }

        Mat asOpenCvMat() {
            if (image == null ) {
                return new Mat();
            }
            
            
            return Imgcodecs.imdecode(new MatOfByte(), Imgcodecs.IMREAD_UNCHANGED);
        }

        @Data
        @Builder
        public static class Detected {
            private String type;
            private float score;
            private int x;
            private int y;
            private int width;
            private int height;
        }
    }
}
