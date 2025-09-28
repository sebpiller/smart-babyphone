package ch.sebpiller.babyphone.ui.config;

import ch.sebpiller.babyphone.detection.DetectionResult;
import ch.sebpiller.babyphone.detection.SoundAnalyzer;
import ch.sebpiller.babyphone.fetch.rtsp.RtspSoundSource;
import ch.sebpiller.babyphone.fetch.rtsp.properties.RtspStreamProperties;
import ch.sebpiller.babyphone.fetch.sound.SoundSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SoundConfiguration {


    @Bean
    @ConditionalOnBean(RtspStreamProperties.class)
    SoundSource defaultSoundSource(RtspStreamProperties p) {
        return new RtspSoundSource(p);
    }


//    @Bean
//    @ConditionalOnProperty(name = "babyphone.sound-analyzer", havingValue = "cifar")
//    SoundAnalyzer cifarSoundAnalyzer() {
//        log.info("Creating cifar sound analyzer");
//        return new Cifar10AudioClassifier();
//    }
//
//    @Bean
//    @ConditionalOnProperty(name = "babyphone.sound-analyzer", havingValue = "yamnet")
//    SoundAnalyzer yamnetSoundAnalyzer() {
//        log.info("Creating yamnet sound analyzer");
//        return new YamnetSoundAnalyzer();
//    }
//
//    @Bean
//    @ConditionalOnProperty(name = "babyphone.sound-analyzer", havingValue = "resnet")
//    SoundAnalyzer resNetV2AudioClassifier() {
//        log.info("Creating resnet sound analyzer");
//        return new ResNetV2AudioClassifier();
//    }


//
//    @Bean
//    SoundSource lineInSoundSource() {
//        log.info("Creating line in sound source");
//        return new LineInSoundSource();
//    }

    @Bean
    @ConditionalOnMissingBean(SoundSource.class)
    SoundSource noopSoundSource() {
        return (duration, format) -> new byte[0];
    }

    @Bean
    @ConditionalOnMissingBean(SoundAnalyzer.class)
    SoundAnalyzer noopSoundAnalyzer() {
        return (sound, format, includeInResult) -> DetectionResult.builder().build();
    }

}
