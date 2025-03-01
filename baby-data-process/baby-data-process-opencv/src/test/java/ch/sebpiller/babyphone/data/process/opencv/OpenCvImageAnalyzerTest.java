package ch.sebpiller.babyphone.data.process.opencv;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.core.io.Resource;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@Disabled
class OpenCvImageAnalyzerTest {

    private static OpenCvImageAnalyzer openCvObjectRecognizer;

    @BeforeAll
    static void setup() {
        openCvObjectRecognizer = new OpenCvImageAnalyzer(Map.of(new Detector(), new CascadeClassifier(Objects.requireNonNull(OpenCvImageAnalyzerTest.class.getResource("/haarcascades/haarcascade_frontalface_alt.xml")).getPath())));

    }


    @SneakyThrows
    public static Stream<Arguments> listImages() {
        var resolver = new org.springframework.core.io.support.PathMatchingResourcePatternResolver();
        return Stream.of(resolver.getResources("classpath:/samples/images/*"))
                .map(Arguments::of)
                ;
    }

    @ParameterizedTest
    @MethodSource("listImages")
    void test_detect_on_image_and_write(Resource source) throws IOException {
        var src = Imgcodecs.imread(source.getFile().getAbsolutePath());

        var bi = ImageIO.read(source.getFile());
//        var res = recognitionService.annotateImage(src);
        var out = openCvObjectRecognizer.detectObjectsOn(bi, x -> true);

        log.info("Detected {} objects.", out.getDetected().size());

    }
}