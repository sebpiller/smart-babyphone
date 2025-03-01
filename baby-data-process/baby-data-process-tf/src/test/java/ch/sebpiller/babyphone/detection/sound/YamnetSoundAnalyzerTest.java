package ch.sebpiller.babyphone.detection.sound;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest(classes = YamnetSoundAnalyzer.class)
class YamnetSoundAnalyzerTest {

    @Autowired
    private YamnetSoundAnalyzer yamnetSoundAnalyzer;

    @Test
    void test_detect_objects_on() throws IOException {
        var result = yamnetSoundAnalyzer.detectObjectsOn(getClass().getResourceAsStream("/samples/sounds/miaow_16k.wav").readAllBytes());
        assertNotNull(result);
    }
}