package ch.sebpiller.impl;


import ch.sebpiller.babyphone.detection.Detected;
import ch.sebpiller.babyphone.detection.DetectionResult;
import ch.sebpiller.babyphone.detection.ObjectRecognizer;

import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.function.Predicate;

public class HailoRecognizer implements ObjectRecognizer {

    @Override
    public DetectionResult detectAndWrite(BufferedImage image, Optional<String> outputPath, Predicate<Detected> p) {
        return null;
    }
}
