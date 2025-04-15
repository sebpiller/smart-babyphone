package ch.sebpiller.babyphone.detection.images.fasterrcnn;


import ch.sebpiller.babyphone.detection.DetectionResult;
import ch.sebpiller.testing.dataset.TestDataProvider;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


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

    @Test
    @DisplayName("Detect objects on null image should throw NullPointerException")
    void test_detect_objects_on_null_image() {
        assertThatThrownBy(() -> testing.detectObjectsOn(null, x -> true))
                .isInstanceOf(NullPointerException.class);
    }


    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource(TestDataProvider.ALL_IMAGES)
    @DisplayName("Detect objects and filter results based on a predicate")
    void test_detect_objects_with_filtering(String name, BufferedImage source) {
        var detectionResult = testing.detectObjectsOn(source, detected -> detected.score() > 0.5);

        assertThat(detectionResult).isNotNull();
        assertThat(detectionResult.matched()).allMatch(detected -> detected.score() > 0.5);
    }

    @Test
    @DisplayName("Detect objects but handle no detections scenario gracefully")
    void test_detect_objects_with_no_detections() {
        DetectionResult detectionResult = testing.detectObjectsOn(new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB), x -> true);

        assertThat(detectionResult).isNotNull();
        assertThat(detectionResult.matched().findAny()).isEmpty();
    }
}