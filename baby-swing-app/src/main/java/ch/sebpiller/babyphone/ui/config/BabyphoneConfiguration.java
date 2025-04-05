package ch.sebpiller.babyphone.ui.config;

import ch.sebpiller.babyphone.data.process.opencv.OpenCvObjectRecognizer;
import ch.sebpiller.babyphone.detection.ObjectRecognizer;
import ch.sebpiller.babyphone.fetch.rtsp.RtspImageProvider;
import ch.sebpiller.babyphone.fetch.rtsp.properties.RtspStreamProperties;
import ch.sebpiller.babyphone.process.piaihat.PiaihatObjectRecognizer;
import ch.sebpiller.babyphone.ui.config.properties.BabyPhoneProperties;
import ch.sebpiller.impl.FasterRcnnObjectRecognizer;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({BabyPhoneProperties.class, RtspStreamProperties.class})
//@Import(AopConfig.class)
public class BabyphoneConfiguration {
    @Bean
    RtspImageProvider rtspStreamReader(BabyPhoneProperties p) {
        return new RtspImageProvider(p.getRtspStream());
    }

    @Bean
    ObjectRecognizer piaihatRecognizer() {
        return new PiaihatObjectRecognizer();
    }

    @Bean
    ObjectRecognizer fasterRcnnRecognizer() {
        return new FasterRcnnObjectRecognizer();
    }

    @Bean
    ObjectRecognizer openCvRecognizer(BabyPhoneProperties p) {
        return new OpenCvObjectRecognizer(
                Arrays.stream(p.getDetectors())
                        .collect(Collectors.toMap(
                                detector -> detector,
                                detector -> new CascadeClassifier(detector.getFile().toFile().getAbsolutePath())
                        ))
        );
    }
}
