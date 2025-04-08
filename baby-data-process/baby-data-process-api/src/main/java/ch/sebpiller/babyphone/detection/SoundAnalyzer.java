package ch.sebpiller.babyphone.detection;

import java.io.File;
import java.util.function.Predicate;

public interface SoundAnalyzer {

    DetectionResult detectObjectsOn(File sound, Predicate<Detected> includeInResult);

}
