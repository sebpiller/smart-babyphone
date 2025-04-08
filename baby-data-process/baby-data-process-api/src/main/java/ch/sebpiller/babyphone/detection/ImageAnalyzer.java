package ch.sebpiller.babyphone.detection;

import java.awt.image.BufferedImage;
import java.util.function.Predicate;

public interface ImageAnalyzer {
    DetectionResult detectObjectsOn(BufferedImage image, Predicate<Detected> includeInResult);

}
