package ch.sebpiller.babyphone.detection;

import javax.sound.sampled.AudioFormat;
import java.util.function.Predicate;

public interface SoundAnalyzer {

    DetectionResult detectObjectsOn(byte[] sound, AudioFormat format, Predicate<Detected> includeInResult);
}
