package ch.sebpiller.babyphone.service.ia.impl;

import ch.sebpiller.babyphone.service.rtsp.ScheduledFetchRtspImage;
import lombok.SneakyThrows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;


@SpringBootTest
class FasterRcnnObjectRecognizerTest {

    @MockitoBean
    private ScheduledFetchRtspImage scheduledFetchRtspImage;

    @Autowired
    private FasterRcnnObjectRecognizer fasterRcnnObjectRecognizer;

    @SneakyThrows
    public static Stream<Arguments> listImages() {
        return Files
                .list(Path.of("src/test/resources/images"))
                .sorted(Comparator.comparing((Path o) -> o.toAbsolutePath().toString().toLowerCase()))
                .map(x -> Arguments.of(x.toString()));
    }

    @ParameterizedTest
    @MethodSource("listImages")
    void test_detect_on_image_and_write(String source) {
        fasterRcnnObjectRecognizer.detectAndWrite(
                 source,
                "target/" + source,
                x -> {
                    var type = x.getType();
                    var score = x.getScore();
                  //  return type.equals("person") && score > 0.75;
                    return score > 0.7;
                }
        );
    }

}