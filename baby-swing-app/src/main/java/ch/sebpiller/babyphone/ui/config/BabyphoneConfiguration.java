package ch.sebpiller.babyphone.ui.config;

import ch.sebpiller.babyphone.data.process.opencv.OpenCvImageAnalyzer;
import ch.sebpiller.babyphone.detection.ImageAnalyzer;
import ch.sebpiller.babyphone.detection.SoundAnalyzer;
import ch.sebpiller.babyphone.detection.sound.ResNetV2AudioClassifier;
import ch.sebpiller.babyphone.fetch.image.ImageSource;
import ch.sebpiller.babyphone.fetch.rtsp.properties.RtspStreamProperties;
import ch.sebpiller.babyphone.fetch.sound.SoundSource;
import ch.sebpiller.babyphone.process.piaihat.PiaihatImageAnalyzer;
import ch.sebpiller.babyphone.ui.config.properties.BabyPhoneProperties;
import ch.sebpiller.spi.toolkit.aop.AopConfig;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.TaskUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({BabyPhoneProperties.class, RtspStreamProperties.class})
@Import(AopConfig.class)
public class BabyphoneConfiguration {

    @Bean
    ThreadPoolTaskScheduler taskScheduler() {
        var s = new ThreadPoolTaskScheduler();
        s.setDaemon(true);
        s.setPoolSize(4);
        s.setAwaitTerminationSeconds(5);
        s.setThreadNamePrefix("baby-schedule-");
        s.setWaitForTasksToCompleteOnShutdown(false);
        s.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        s.setRemoveOnCancelPolicy(true);
        s.setErrorHandler(TaskUtils.getDefaultErrorHandler(false));
        return s;
    }

    @Bean
    SoundSource soundSource() {
        //return new RtspSoundSource();
        //return new LineInSoundSource();

        return (d, dsfx) -> {
            var x = new File("smart-babyphone/baby-samples-data/src/main/resources/samples/sounds/music_samples").listFiles();
            assert x != null;
            var xx = new ArrayList<>(List.of(x));
            Collections.shuffle(xx);

            try (var fis = new FileInputStream(xx.getFirst())) {
                return fis.readAllBytes();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Bean
    SoundAnalyzer soundAnalyzer() {
        // return new YamnetSoundAnalyzer();
        return new ResNetV2AudioClassifier();
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
