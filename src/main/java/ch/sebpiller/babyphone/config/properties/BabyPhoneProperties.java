package ch.sebpiller.babyphone.config.properties;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
@ConfigurationProperties(prefix = "baby-phone")
public class BabyPhoneProperties {

    @NotNull
    private @Valid RtspStreamProperties rtspStream;

    @NotEmpty
    private @Valid Detector[] detectors;
}
