package ch.sebpiller.babyphone.service.ia;

import ch.sebpiller.babyphone.service.rtsp.ScheduledFetchRtspImage;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

@SpringBootTest
class RecognitionServiceTest {

    @MockitoBean
    private ScheduledFetchRtspImage scheduledFetchRtspImage;

    @Autowired
    private RecognitionService recognitionService;


    @SneakyThrows
    public static Stream<Arguments> listImages() {
        return Files
                .list(Path.of("src/test/resources/images"))
                .sorted()
                .map(x -> Arguments.of(x.toString()));
    }

    @ParameterizedTest
    @MethodSource("listImages")
    void test_detect_on_image_and_write(String source) {
        var path = Objects.requireNonNull(getClass().getResource(source)).getPath();
        var src = Imgcodecs.imread(path);

        var res = recognitionService.annotateImage(src);
        Assertions.assertThat(Imgcodecs.imwrite(path.replace("/images/", "/"), res)).isTrue();
    }
}