package ch.sebpiller.testing.dataset;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.junit.jupiter.params.provider.Arguments;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Stream;

@UtilityClass
public class TestDataProvider {
    private static final String FQCN = "ch.sebpiller.testing.dataset.TestDataProvider";
    // ----
    public static final String ALL_IMAGES = FQCN + "#findAvailableImages";
    public static final String JPEG = FQCN + "#findJpegs";

    // ----
    private static final Predicate<Path> JPEGS = withExts("jpg", "jpeg");
    private static final Predicate<Path> BMP = withExts("bmp");
    private static final Predicate<Path> PNG = withExts("png");
    private static final Predicate<Path> GIF = withExts("gif");
    private static final Predicate<Path> IMAGES = JPEGS.or(BMP).or(PNG).or(GIF);
    // ----

    public static final String ALL_SOUNDS = FQCN + "#findAvailableAudios";

    @SuppressWarnings("unused")
    @SneakyThrows
    public static Stream<Arguments> findAvailableImages() {
        return listClasspathFiles("/samples", true, IMAGES)
                .map(x -> {
                    try {
                        return Arguments.of(x.getFileName().toString(), ImageIO.read(x.toFile()));
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                });
    }

    @SuppressWarnings("unused")
    @SneakyThrows
    public static Stream<Arguments> findJpegs() {
        return listClasspathFiles("/samples", true, IMAGES)
                .map(x -> {
                    try {
                        return Arguments.of(x.getFileName().toString(), ImageIO.read(x.toFile()));
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                });
    }

    @SuppressWarnings("unused")
    @SneakyThrows
    public static Stream<Arguments> findAvailableAudios() {
        return listClasspathFiles("/samples/sounds", false, x -> true)
                .map(x -> {
                    try {
                        return Arguments.of(x.getFileName().toString(), new AudioInputStream(x.toUri().toURL().openStream(), new AudioFormat(44100, 16, 1, true, true), -1));
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                });
    }

    private static Stream<Path> listClasspathFiles(String classpathFolder, boolean recursive, Predicate<Path> predicate) {
        var resourceUrl = TestDataProvider.class.getResource(classpathFolder);
        if (resourceUrl == null) {
            throw new IllegalStateException("Path '" + classpathFolder + "' not found in the classpath.");
        }

        try {
            var result = Stream.<Path>empty();
            Path root = Path.of(resourceUrl.toURI());

            var subdir = Files.list(root)
                    .filter(p -> p.toFile().isDirectory() && recursive);


            for (var folder : Stream.concat(Stream.of(root), subdir).toArray(Path[]::new)) {
                result = Stream.concat(result, Files.list(folder)
                        .filter(p -> p.toFile().isFile())
                        .sorted(Comparator.comparing((Path o) -> o.toAbsolutePath().toString().toLowerCase()))
                        .filter(predicate));
            }

            return result;
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Predicate<Path> withExts(String... exts) {
        return x -> {
            for (var ext : exts) {
                if (x.getFileName().toString().toLowerCase().endsWith("." + ext)) {
                    return true;
                }
            }

            return false;
        };
    }


}
