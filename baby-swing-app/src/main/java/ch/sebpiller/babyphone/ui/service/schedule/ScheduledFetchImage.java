package ch.sebpiller.babyphone.ui.service.schedule;

import ch.sebpiller.babyphone.fetch.image.ImageSource;
import ch.sebpiller.babyphone.ui.swing.MainController;
import ch.sebpiller.spi.toolkit.aop.AutoLog;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
@AutoLog
public class ScheduledFetchImage {

    private final ImageSource imageSource;
    private final MainController mainController;

    @Scheduled(initialDelay = 5_000, fixedRate = 5000, timeUnit = TimeUnit.MILLISECONDS, scheduler = "taskScheduler")
    public void captureNextImage() {
        mainController.receiveRawImage(imageSource.get());
    }

}

