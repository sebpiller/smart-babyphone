package ch.sebpiller.babyphone.ui.config;

import ch.sebpiller.babyphone.fetch.image.ImageSource;
import ch.sebpiller.babyphone.fetch.sound.SoundSource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@AutoConfiguration
public class BabyphoneAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean(ImageSource.class)
    ImageSource randomSamplesImage() {
        log.info("Creating random samples image source");

        return () -> {
            var x = new File("smart-babyphone/baby-samples-data/src/main/resources/samples/images/").listFiles();
            assert x != null;
            var xx = new ArrayList<>(List.of(x));
            Collections.shuffle(xx);

            try {
                File first = xx.getFirst();
                MDC.put("source_image", first.getName());
                return ImageIO.read(first);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(SoundSource.class)
    SoundSource randomSamplesSoundSource() {
        log.info("Creating random samples sound source");

        return (d, af) -> {
            var x = new File("smart-babyphone/baby-samples-data/src/main/resources/samples/sounds/").listFiles();
            assert x != null;
            var xx = new ArrayList<>(List.of(x));
            Collections.shuffle(xx);

            try {
                return Files.readAllBytes(xx.getFirst().toPath());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        };
    }
}
