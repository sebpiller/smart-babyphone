package ch.sebpiller.babyphone.config;


import ch.sebpiller.babyphone.config.properties.BabyPhoneProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(BabyPhoneProperties.class)
public class BabyphoneConfiguration {
}
