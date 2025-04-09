package ch.sebpiller.babyphone.ui.config;

import ch.sebpiller.babyphone.data.process.opencv.OpenCvImageAnalyzer;
import ch.sebpiller.babyphone.detection.ImageAnalyzer;
import ch.sebpiller.babyphone.detection.SoundAnalyzer;
import ch.sebpiller.babyphone.detection.sound.YamnetSoundAnalyzer;
import ch.sebpiller.babyphone.fetch.image.ImageSource;
import ch.sebpiller.babyphone.fetch.rtsp.properties.RtspStreamProperties;
import ch.sebpiller.babyphone.fetch.sound.LineInSoundSource;
import ch.sebpiller.babyphone.fetch.sound.SoundSource;
import ch.sebpiller.babyphone.process.piaihat.PiaihatImageAnalyzer;
import ch.sebpiller.babyphone.ui.config.properties.BabyPhoneProperties;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({BabyPhoneProperties.class, RtspStreamProperties.class})
public class BabyphoneConfiguration {
    @Bean
    SoundSource soundSource() {
        //return new RtspSoundSource();
        return new LineInSoundSource();
//
//        return (d, dsfx) -> {
//            var x = new File("smart-babyphone/baby-samples-data/src/main/resources/samples/sounds/music_samples").listFiles();
//            assert x != null;
//            var xx = Arrays.stream(x).collect(Collectors.toCollection(ArrayList::new));
//            Collections.shuffle(xx);
//            try (var fis = new FileInputStream(xx.getFirst())) {
//                return fis.readAllBytes();
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        };
    }

    @Bean
    SoundAnalyzer soundAnalyzer() {
        return new YamnetSoundAnalyzer();
        // return new ResNetV2AudioClassifier();
        //return new Cifar10AudioClassifier();
    }

    @Bean
    ImageSource rtspStreamReader(BabyPhoneProperties p) {
        //  return new RtspImageSource(p.getRtspStream());

        return () -> new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    }

    @Bean
    ImageAnalyzer piaihatRecognizer() {
        return new PiaihatImageAnalyzer();
    }

    @Bean
    ImageAnalyzer fasterRcnnRecognizer() {
        //  return new FasterRcnnImageAnalyzer();
        return new PiaihatImageAnalyzer();
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
