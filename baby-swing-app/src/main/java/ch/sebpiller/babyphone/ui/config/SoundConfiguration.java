package ch.sebpiller.babyphone.ui.config;

import ch.sebpiller.babyphone.detection.SoundAnalyzer;
import ch.sebpiller.babyphone.detection.sound.Cifar10AudioClassifier;
import ch.sebpiller.babyphone.detection.sound.ResNetV2AudioClassifier;
import ch.sebpiller.babyphone.detection.sound.YamnetSoundAnalyzer;
import ch.sebpiller.babyphone.fetch.sound.LineInSoundSource;
import ch.sebpiller.babyphone.fetch.sound.SoundSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SoundConfiguration {


    @Bean
    SoundSource lineInSoundSource() {
        log.info("Creating line in sound source");
        return new LineInSoundSource();
    }

//    @Bean
//    @ConditionalOnBean(RtspStreamProperties.class)
//    SoundSource defaultSoundSource(RtspStreamProperties p) {
//        return new RtspSoundSource();
//    }

    @Bean
    @ConditionalOnProperty(name = "babyphone.sound-analyzer", havingValue = "cifar")
    SoundAnalyzer cifarSoundAnalyzer() {
        log.info("Creating cifar sound analyzer");
        return new Cifar10AudioClassifier();
    }

    @Bean
    @ConditionalOnProperty(name = "babyphone.sound-analyzer", havingValue = "yamnet")
    SoundAnalyzer yamnetSoundAnalyzer() {
        log.info("Creating yamnet sound analyzer");
        return new YamnetSoundAnalyzer();
    }

    @Bean
    @ConditionalOnProperty(name = "babyphone.sound-analyzer", havingValue = "resnet")
    SoundAnalyzer resNetV2AudioClassifier() {
        log.info("Creating resnet sound analyzer");
        return new ResNetV2AudioClassifier();
    }

}
