package ch.sebpiller.babyphone.config.properties;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
public class Detector {
    @NotEmpty
    private String name;

    @NotEmpty
    private String file;

    @Pattern(regexp = "\\d+%")
    private String objectMinSize = "15%";

    public int minSizeAdaptedFor(int v) {
        return Math.max(0, Math.min((int) (v * Float.parseFloat(objectMinSize.replace("%", "")) / 100), Integer.MAX_VALUE));
    }
}