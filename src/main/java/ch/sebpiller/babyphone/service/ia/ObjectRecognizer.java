package ch.sebpiller.babyphone.service.ia;

import ch.sebpiller.babyphone.service.ia.impl.FasterRcnnObjectRecognizer;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface ObjectRecognizer {
    DetectionResult detectAndWrite(String imagePath, String outputPath, Predicate<FasterRcnnObjectRecognizer.DetectedObject> p);

    @Data
    @Builder
    class DetectionResult {
        private List<Detected> detected;

        public boolean isEmpty() {
            return detected == null || detected.isEmpty();
        }

        Stream<Detected> forType(String type) {
            return getDetected()
                    .stream()
                    .filter(x -> x.getType().equals(type));
        }

        @Data
        @Builder
        public static class Detected {
            private String type;
            private int x;
            private int y;
            private int width;
            private int height;
        }
    }
}
