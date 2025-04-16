package ch.sebpiller.babyphone.ui.config;

import ch.sebpiller.babyphone.fetch.rtsp.properties.RtspStreamProperties;
import ch.sebpiller.babyphone.ui.config.properties.BabyPhoneProperties;
import ch.sebpiller.spi.toolkit.CopyMdcTaskDecorator;
import ch.sebpiller.spi.toolkit.aop.AopConfig;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.TaskUtils;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({
        BabyPhoneProperties.class,
        RtspStreamProperties.class,
})
@ImportAutoConfiguration(BabyphoneAutoConfiguration.class)
@Import({
        AopConfig.class,
})
public class BabyphoneConfiguration {

    @Bean
    ThreadPoolTaskScheduler taskScheduler() {
        var s = new ThreadPoolTaskScheduler();
        s.setDaemon(true);
        s.setPoolSize(4);
        s.setAwaitTerminationSeconds(5);
        s.setThreadNamePrefix("baby-schedule-");
        s.setWaitForTasksToCompleteOnShutdown(false);
        s.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        s.setRemoveOnCancelPolicy(true);
        s.setErrorHandler(TaskUtils.getDefaultErrorHandler(false));
        s.setTaskDecorator(new CopyMdcTaskDecorator());
        return s;
    }

}
