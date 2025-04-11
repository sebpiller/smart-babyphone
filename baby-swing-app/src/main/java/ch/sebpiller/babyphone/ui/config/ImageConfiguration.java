package ch.sebpiller.babyphone.ui.config;

import ch.sebpiller.babyphone.data.process.opencv.OpenCvImageAnalyzer;
import ch.sebpiller.babyphone.detection.ImageAnalyzer;
import ch.sebpiller.babyphone.detection.images.fasterrcnn.FasterRcnnImageAnalyzer;
import ch.sebpiller.babyphone.fetch.image.ImageSource;
import ch.sebpiller.babyphone.fetch.rtsp.RtspImageSource;
import ch.sebpiller.babyphone.fetch.rtsp.properties.RtspStreamProperties;
import ch.sebpiller.babyphone.process.piaihat.PiaihatImageAnalyzer;
import ch.sebpiller.babyphone.ui.config.properties.BabyPhoneProperties;
import lombok.extern.slf4j.Slf4j;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Configuration
public class ImageConfiguration {
 //   @Bean
    @ConditionalOnBean(RtspStreamProperties.class)
    ImageSource rtspImageSource(RtspStreamProperties p) {
        log.info("Creating rtsp image source");
        return new RtspImageSource(p);
    }

    @Bean
    ImageAnalyzer piaihatRecognizer() {
        log.info("Creating piaihat image analyzer");
        return new PiaihatImageAnalyzer();
    }

    @Bean
    ImageAnalyzer fasterRcnnRecognizer() {
        log.info("Creating faster rcnn image analyzer");
        return new FasterRcnnImageAnalyzer();
    }

    @Bean
    ImageAnalyzer openCvRecognizer(BabyPhoneProperties p) {
        log.info("Creating opencv image analyzer");
        return new OpenCvImageAnalyzer(
                Arrays.stream(p.getDetectors())
                        .collect(Collectors.toMap(
                                detector -> detector,
                                detector -> new CascadeClassifier(detector.getFile().toFile().getAbsolutePath())
                        ))
        );
    }
}
