package ch.sebpiller.babyphone.detection.images.fasterrcnn;


import ch.sebpiller.testing.dataset.TestDataProvider;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.image.BufferedImage;


@Slf4j
class FasterRcnnImageAnalyzerTest {

    private static final FasterRcnnImageAnalyzer testing = new FasterRcnnImageAnalyzer();

    @BeforeAll
    static void setup() {
        System.setProperty("org.bytedeco.javacpp.logger.debug", "true");
    }

    @AfterAll
    static void teardown() {
        System.clearProperty("org.bytedeco.javacpp.logger.debug");
        testing.close();
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource(TestDataProvider.ALL_IMAGES)
    @DisplayName(value = "run detection against available images")
    void test_detect_objects_on(String name, BufferedImage source) {
        log.info("{}: {}", name, testing.detectObjectsOn(source, x -> true));
    }

}