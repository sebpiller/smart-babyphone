package ch.sebpiller.babyphone.fetch.sound;

import java.io.File;
import java.time.Duration;

public interface SoundSource {

    File captureClip(Duration duration);

}
