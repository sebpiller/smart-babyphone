package ch.sebpiller.babyphone.ui.config;

import ch.sebpiller.babyphone.fetch.image.ImageSource;
import ch.sebpiller.babyphone.fetch.rtsp.RtspImageSource;
import ch.sebpiller.babyphone.fetch.rtsp.properties.RtspStreamProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ImageConfiguration {
    @Bean
    @ConditionalOnBean(RtspStreamProperties.class)
    ImageSource rtspImageSource(RtspStreamProperties p) {
        log.info("Creating rtsp image source");
        return new RtspImageSource(p);
    }
//
//    @Bean
//    ImageAnalyzer piaihatRecognizer() {
//        log.info("Creating piaihat image analyzer");
//        return new PiaihatImageAnalyzer();
//    }
//
//    @Bean
//    ImageAnalyzer fasterRcnnRecognizer() {
//        log.info("Creating faster rcnn image analyzer");
//        return new FasterRcnnImageAnalyzer();
//    }
//
//    @Bean
//    ImageAnalyzer openCvRecognizer(BabyPhoneProperties p) {
//        log.info("Creating opencv image analyzer");
//        return new OpenCvImageAnalyzer(
//                Arrays.stream(p.getDetectors())
//                        .collect(Collectors.toMap(
//                                detector -> detector,
//                                detector -> new CascadeClassifier(detector.getFile().toFile().getAbsolutePath())
//                        ))
//        );
//    }

}
