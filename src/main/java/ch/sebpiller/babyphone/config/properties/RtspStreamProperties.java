package ch.sebpiller.babyphone.config.properties;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "baby-phone.rtsp-stream")
public class RtspStreamProperties {
    @NotEmpty
    private String host;

    @NotEmpty
    private String path;

    @NotEmpty
    private String user;

    @NotEmpty
    private String pass;

    public String toRtspUrl() {
        return "rtsp://" + user + ":" + pass + "@" + host + path;
    }

}
