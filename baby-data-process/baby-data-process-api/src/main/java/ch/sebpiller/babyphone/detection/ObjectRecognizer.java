package ch.sebpiller.babyphone.detection;

import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.function.Predicate;

public interface ObjectRecognizer {
    DetectionResult detectAndWrite(BufferedImage image, Optional<String> outputPath, Predicate<Detected> p);

}
