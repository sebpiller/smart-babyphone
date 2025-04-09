package ch.sebpiller.babyphone.ui.swing;

import ch.sebpiller.babyphone.detection.DetectionResult;
import ch.sebpiller.babyphone.detection.ImageAnalyzer;
import ch.sebpiller.babyphone.detection.SoundAnalyzer;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioFormat;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

@Slf4j
@RequiredArgsConstructor
@Component
public class MainController {

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
}
