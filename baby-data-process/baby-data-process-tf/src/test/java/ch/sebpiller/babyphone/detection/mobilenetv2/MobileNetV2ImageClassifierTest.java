package ch.sebpiller.babyphone.detection.mobilenetv2;

import ch.sebpiller.testing.dataset.TestDataProvider;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.image.BufferedImage;


@Slf4j
class MobileNetV2ImageClassifierTest {

    private final MobileNetV2ImageClassifier testing = new MobileNetV2ImageClassifier();

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource(TestDataProvider.ALL_IMAGES)
    @DisplayName(value = "run detection against available images")
    void test_detect_objects_on(String name, BufferedImage source) {
        log.info("{}: {}", name, testing.detectObjectsOn(source, x -> true));
    }


}