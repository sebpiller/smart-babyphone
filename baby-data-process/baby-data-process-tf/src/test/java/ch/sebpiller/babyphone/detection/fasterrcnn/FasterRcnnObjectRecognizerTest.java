package ch.sebpiller.babyphone.detection.fasterrcnn;


import lombok.SneakyThrows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;


@SpringBootTest(classes = FasterRcnnImageAnalyzer.class)
class FasterRcnnObjectRecognizerTest {

    @Autowired
    private FasterRcnnImageAnalyzer fasterRcnnImageAnalyzer;

    @SneakyThrows
    public static Stream<Arguments> listImages() {
        return Files
                .list(Path.of("/samples/images/"))
                .sorted(Comparator.comparing((Path o) -> o.toAbsolutePath().toString().toLowerCase()))
                .map(x -> Arguments.of(x.toString()));
    }

    @ParameterizedTest
    @MethodSource("listImages")
    void test_detect_objects_on(String source) throws IOException {
        fasterRcnnImageAnalyzer.detectObjectsOn(
                ImageIO.read(new File(source)),
                x -> {
                    var type = x.type();
                    var score = x.score();
                    //  return type.equals("person") && score > 0.75;
                    return score > 0.7;
                }
        );
    }

}