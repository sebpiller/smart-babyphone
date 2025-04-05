package ch.sebpiller.babyphone.process.piaihat;

import ch.sebpiller.babyphone.detection.Detected;
import ch.sebpiller.babyphone.detection.DetectionResult;
import ch.sebpiller.babyphone.detection.ObjectRecognizer;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.function.Predicate;


@ToString
@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnMissingBean
public class PiaihatObjectRecognizer implements ObjectRecognizer {
    @Override
    public DetectionResult detectAndWrite(BufferedImage image, Optional<String> outputPath, Predicate<Detected> p) {
        return DetectionResult.builder()
                .image(image)
                .build();
    }
}
