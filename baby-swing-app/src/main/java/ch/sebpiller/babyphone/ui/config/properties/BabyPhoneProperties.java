package ch.sebpiller.babyphone.ui.config.properties;


import ch.sebpiller.babyphone.data.process.opencv.Detector;
import ch.sebpiller.babyphone.fetch.rtsp.properties.RtspStreamProperties;
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
