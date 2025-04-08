package ch.sebpiller.babyphone.detection.sound;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

@Slf4j
class ResNetV2AudioClassifierTest {
    @Test
    void test() throws IOException {
        var classifier = new ResNetV2AudioClassifier();
        classifier.loadModel();


        var x = new File("../../baby-samples-data/src/main/resources/samples/sounds/music_samples").listFiles();
        assert x != null;
        var xx = Arrays.stream(x).collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(xx);

        for (var xxx : x) {
            var xxxx = classifier.detectObjectsOn(xxx, xxxxx -> true);

            log.info("{} is {}", xxx.getName(), xxxx.matched().findFirst().orElse(null));
        }

    }

}