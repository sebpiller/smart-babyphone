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
class Cifar10AudioClassifierTest {

    @Test
    void test() throws IOException {
        var classifier = new Cifar10AudioClassifier();
        classifier.loadModel();


        var x = new File("../../baby-samples-data/src/main/resources/samples/sounds/music_samples").listFiles();
        assert x != null;
        var xx = Arrays.stream(x).collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(xx);


        for (var path : xx) {
            var detected = classifier.detectObjectsOn(path, xxxxx -> true);

            detected.matched()
                    .forEach(xxxxx -> log.info("{} at {}%", xxxxx.type(), (int) (xxxxx.score() * 100)));

        }
    }


}