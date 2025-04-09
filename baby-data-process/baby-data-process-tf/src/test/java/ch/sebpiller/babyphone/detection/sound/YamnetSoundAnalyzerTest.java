package ch.sebpiller.babyphone.detection.sound;

import ch.sebpiller.babyphone.toolkit.SoundUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


@Slf4j
class YamnetSoundAnalyzerTest {
    @Test
    void test() throws IOException {
        var classifier = new YamnetSoundAnalyzer();

        var x = new File("../../baby-samples-data/src/main/resources/samples/sounds/music_samples").listFiles();

        assert x != null;
        for (var xxx : x) {
            var xxxx = classifier.detectObjectsOn(Files.readAllBytes(xxx.toPath()), SoundUtils.getAudioFormat(),
                    xxxxx ->
                            !xxxxx.type().equals("514,/m/0chx_,White noise") &&
                                    !xxxxx.type().equals("507,/m/096m7z,Noise") &&
                                    !xxxxx.type().equals("132,/m/04rlf,Music") &&
                                    xxxxx.score() >= 0.05);

            log.info("{} is {}", xxx.getName(), xxxx.matched().limit(5).toArray());
        }

    }

}