package ch.sebpiller.babyphone.service.ia;

import ch.sebpiller.babyphone.service.rtsp.ScheduledFetchRtspImage;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Objects;

@SpringBootTest
class RecognitionServiceTest {

    @MockitoBean
    private ScheduledFetchRtspImage scheduledFetchRtspImage;

    @Autowired
    private RecognitionService recognitionService;

    @ParameterizedTest
    @ValueSource(strings = {
            "/images/cat1.jpg",
            "/images/cat2.jpg",
            "/images/cat3.jpg",
            "/images/cat4.jpg",
            "/images/human1.jpg",
            "/images/human2.jpg",
            "/images/human3.jpg",
            "/images/human4.webp",
            "/images/human5.jpg",
            "/images/baby1.jpg",
            "/images/baby2.webp",
            "/images/baby3.webp",
    })
    void test_detect_on_image_and_write(String source) {
        var path = Objects.requireNonNull(getClass().getResource(source)).getPath();
        var src = Imgcodecs.imread(path);

        var res = recognitionService.annotateImage(src);
        Assertions.assertThat(Imgcodecs.imwrite(path.replace("/images/", "/"), res)).isTrue();
    }
}