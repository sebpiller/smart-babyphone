package ch.sebpiller.babyphone.ui.service.schedule;

import ch.sebpiller.babyphone.fetch.image.ImageSource;
import ch.sebpiller.babyphone.ui.swing.MainController;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Lazy
@RequiredArgsConstructor
@Service
public class ScheduledFetchImage {

    private final ImageSource imageSource;
    private final MainController mainController;

    @Scheduled(fixedRate = 80, timeUnit = TimeUnit.MILLISECONDS)
    public void captureNextImage() {
        mainController.receiveRawImage(imageSource.get());
    }

}

