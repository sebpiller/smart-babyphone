package ch.sebpiller.babyphone.ui.swing;

import ch.sebpiller.babyphone.detection.DetectionResult;
import ch.sebpiller.babyphone.detection.ObjectRecognizer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

@Slf4j
@RequiredArgsConstructor
@Component
public class MainController {

    @Getter
    private final List<ObjectRecognizer> availableAlgorithms;

    @Getter
    @Setter
    private ObjectRecognizer iaProcessingAlgorithm;

//    @PostConstruct
//    private void init() {
//        if (objectRecognizers.isEmpty())
//            throw new IllegalStateException("need at least one object recognizer");
//        objectRecognizer = objectRecognizers.getFirst();
//    }

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
    private DetectionResult detectionResult = DetectionResult.builder().build();

    @SneakyThrows
    public void receiveRawImage(BufferedImage x) {
        long start = System.currentTimeMillis();
        if (this.image != null)
            this.image.flush();

        this.image = x;

        log.info("Received raw image for processing. Dimensions: {}x{}", x.getWidth(), x.getHeight());

        var or = iaProcessingAlgorithm;
        if (or != null) {
            detectionResult = or.detectAndWrite(x, Optional.empty(), xxx -> xxx.score() >= this.confidencyThreshold);
            log.info("Detected: {}", detectionResult);
        } else {
            detectionResult = DetectionResult.builder().image(x).build();
            log.debug("IA processing is disabled. Proceeding with raw image.");
        }

        detecteds.accept(detectionResult);
        long stop = System.currentTimeMillis();
        log.debug("Image processing took {}ms", stop - start);
        fps.accept(stop - start);
    }

}
