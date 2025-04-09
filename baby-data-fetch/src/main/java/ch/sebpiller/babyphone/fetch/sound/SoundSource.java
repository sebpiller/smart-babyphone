package ch.sebpiller.babyphone.fetch.sound;

import lombok.SneakyThrows;

import javax.sound.sampled.AudioFormat;
import java.time.Duration;

public interface SoundSource {

    @SneakyThrows
    byte[] captureClip(Duration duration, AudioFormat format);
}
