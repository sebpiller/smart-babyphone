package ch.sebpiller.babyphone.ui.config;

import ch.sebpiller.babyphone.data.process.opencv.OpenCvImageAnalyzer;
import ch.sebpiller.babyphone.detection.ImageAnalyzer;
import ch.sebpiller.babyphone.detection.SoundAnalyzer;
import ch.sebpiller.babyphone.detection.fasterrcnn.FasterRcnnImageAnalyzer;
import ch.sebpiller.babyphone.detection.sound.YamnetSoundAnalyzer;
import ch.sebpiller.babyphone.fetch.rtsp.RtspImageSource;
import ch.sebpiller.babyphone.fetch.rtsp.RtspSoundSource;
import ch.sebpiller.babyphone.fetch.rtsp.properties.RtspStreamProperties;
import ch.sebpiller.babyphone.process.piaihat.PiaihatImageAnalyzer;
import ch.sebpiller.babyphone.ui.config.properties.BabyPhoneProperties;
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
    RtspSoundSource soundSource() {
        return new RtspSoundSource();
    }

    @Bean
    SoundAnalyzer soundAnalyzer() {
        return new YamnetSoundAnalyzer();
    }

    @Bean
    RtspImageSource rtspStreamReader(BabyPhoneProperties p) {
        return new RtspImageSource(p.getRtspStream());
    }

    @Bean
    ImageAnalyzer piaihatRecognizer() {
        return new PiaihatImageAnalyzer();
    }

    @Bean
    ImageAnalyzer fasterRcnnRecognizer() {
        return new FasterRcnnImageAnalyzer();
    }

    @Bean
    ImageAnalyzer openCvRecognizer(BabyPhoneProperties p) {
        return new OpenCvImageAnalyzer(
                Arrays.stream(p.getDetectors())
                        .collect(Collectors.toMap(
                                detector -> detector,
                                detector -> new CascadeClassifier(detector.getFile().toFile().getAbsolutePath())
                        ))
        );
    }
}
