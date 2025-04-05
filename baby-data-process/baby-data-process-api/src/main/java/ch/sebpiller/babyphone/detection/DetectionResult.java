package ch.sebpiller.babyphone.detection;

import lombok.Builder;
import lombok.Data;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Data
@Builder
public class DetectionResult {
    @Builder.Default
    private List<Detected> detected = Collections.emptyList();

    private BufferedImage image;

    Stream<Detected> forType(String type) {
        return getDetected()
                .stream()
                .filter(x -> x.type().equalsIgnoreCase(type));
    }
}