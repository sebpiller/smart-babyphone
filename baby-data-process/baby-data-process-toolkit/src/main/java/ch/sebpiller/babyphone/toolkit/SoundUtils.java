package ch.sebpiller.babyphone.toolkit;

import lombok.experimental.UtilityClass;

import javax.sound.sampled.AudioFormat;

@UtilityClass
public class SoundUtils {

    public static AudioFormat getAudioFormat() {
        float sampleRate = 22_050;
        var sampleSizeInBits = 16;
        var channels = 1;
        var signed = true;
        var bigEndian = true;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }
}
