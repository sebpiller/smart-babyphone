package ch.sebpiller.babyphone.config;


import ch.sebpiller.babyphone.config.properties.BabyPhoneProperties;
import ch.sebpiller.spi.toolkit.aop.AopConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(BabyPhoneProperties.class)
@Import(AopConfig.class)
public class BabyphoneConfiguration {
}
