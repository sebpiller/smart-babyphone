package  ch.sebpiller.babyphone.rest.config;

import ch.sebpiller.babyphone.rest.config.properties.BabyPhoneProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(BabyPhoneProperties.class)
//@Import(AopConfig.class)
public class BabyphoneConfiguration {

}
