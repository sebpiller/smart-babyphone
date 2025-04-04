package ch.sebpiller.babyphone.ui.swing;

import ch.sebpiller.babyphone.detection.ObjectRecognizer;
import ch.sebpiller.babyphone.fetch.rtsp.RtspStreamReader;
import ch.sebpiller.babyphone.fetch.rtsp.properties.RtspStreamProperties;
import ch.sebpiller.babyphone.ui.swing.properties.BabyPhoneProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({BabyPhoneProperties.class, RtspStreamProperties.class  })
@ComponentScan(basePackageClasses = {BabyPhoneSwingUI.class,})
//@Import(AopConfig.class)
public class BabyphoneConfiguration {
    @Bean
    RtspStreamReader rtspStreamReader(RtspStreamProperties p) {
        return new RtspStreamReader(p);
    }
    @Bean
    ObjectRecognizer objectRecognizer() {
        return new ch.sebpiller.impl.FasterRcnnObjectRecognizer();
    }
}
