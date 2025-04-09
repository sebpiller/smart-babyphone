package ch.sebpiller.babyphone.ui.swing;

import ch.sebpiller.babyphone.detection.DetectionResult;
import ch.sebpiller.babyphone.detection.ImageAnalyzer;
import ch.sebpiller.babyphone.detection.SoundAnalyzer;
import ch.sebpiller.spi.toolkit.aop.AutoLog;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationShutdownHandlers;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioFormat;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

@Slf4j
@RequiredArgsConstructor
@Component
@Lazy
@AutoLog
public class MainController {
    private final ConfigurableApplicationContext context;

    private final ThreadPoolTaskScheduler taskScheduler;

    @Getter
    @NotEmpty
    private final List<ImageAnalyzer> availableAlgorithms;
    @Getter
    @NotEmpty
    private final List<SoundAnalyzer> soundAnalyzers;
    private final SoundAnalyzer soundAnalyzer;
    @Getter
    @Setter
    private ImageAnalyzer iaProcessingAlgorithm;
    @Getter
    @Setter
    private LongConsumer fps = x -> log.debug("fps: {}", x);

    private BufferedImage image;

    @Getter
    @Setter
    private float confidencyThreshold = 0.8f;

    @Getter
    @Setter
    private Consumer<DetectionResult> detecteds = x -> {
    };
    @Getter
    @Setter
    private Consumer<DetectionResult> detectedSounds = x -> {
    };

    @Getter
    private DetectionResult detectionResult = DetectionResult.builder().build();

    @SneakyThrows
    public void receiveRawImage(BufferedImage raw) {
        var start = System.currentTimeMillis();
        if (this.image != null)
            this.image.flush();

        this.image = raw;

        log.info("Received raw image for processing. Dimensions: {}x{}", raw.getWidth(), raw.getHeight());

        if (iaProcessingAlgorithm == null) {
            detectionResult = DetectionResult.builder().image(raw).build();
            log.debug("IA processing is disabled. Proceeding with raw image.");
        } else {
            detectionResult = iaProcessingAlgorithm.detectObjectsOn(raw, xxx -> xxx.score() >= this.confidencyThreshold);
            log.info("Detected: {}", detectionResult);
        }

        detecteds.accept(detectionResult);
        var stop = System.currentTimeMillis();
        log.debug("Image processing took {}ms", stop - start);
        fps.accept(stop - start);
    }

    public void receiveRawSound(String name, byte[] raw, AudioFormat format) {
        var start = System.currentTimeMillis();

        var x = soundAnalyzer.detectObjectsOn(raw, format, xx -> true);
        log.info("Detected sound infos: {} is {}", name, x.matched().findFirst().orElse(null));

        detectedSounds.accept(x);

        var stop = System.currentTimeMillis();
        log.debug("Sound processing took {}ms", stop - start);
    }

    public void requestApplicationTermination() {
        log.info("Stopping scheduled tasks...");
        taskScheduler.initiateShutdown();
        taskScheduler.shutdown();

        log.info("Application exiting with status code 0.");
        context.stop();
        SpringApplication.exit(context, () -> 0);
        System.exit(0);
    }
}
