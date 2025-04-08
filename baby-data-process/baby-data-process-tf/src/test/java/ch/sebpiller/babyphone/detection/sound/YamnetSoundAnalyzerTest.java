package ch.sebpiller.babyphone.detection.sound;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;


@SpringBootTest(classes = YamnetSoundAnalyzer.class)
class YamnetSoundAnalyzerTest {

    @Autowired
    private YamnetSoundAnalyzer yamnetSoundAnalyzer;

    @Test
    void test_detect_objects_on() throws IOException {
    }
}