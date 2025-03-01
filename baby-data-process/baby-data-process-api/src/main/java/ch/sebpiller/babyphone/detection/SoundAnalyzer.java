package ch.sebpiller.babyphone.detection;

public interface SoundAnalyzer {

    DetectionResult detectObjectsOn(byte[] sound);

}
