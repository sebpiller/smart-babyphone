package ch.sebpiller.babyphone.detection;

import lombok.Builder;
import lombok.Data;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Data
@Builder
public class DetectionResult {
    @Builder.Default
    private List<Detected> detected = new ArrayList<>();
    private BufferedImage image;

    public DetectionResult addDetected(Detected detected) {
        this.detected.add(detected);
        return this;
    }

    Stream<Detected> forType(String type) {
        return matched().filter(x -> x.type().equalsIgnoreCase(type));
    }

    /* iterate from best match to lowest */
    public Stream<Detected> matched() {
        return detected.stream().sorted(Comparator.comparingDouble(Detected::score).reversed());
    }
}