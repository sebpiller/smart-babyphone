package ch.sebpiller.babyphone.ui.service.schedule;

import ch.sebpiller.babyphone.fetch.rtsp.RtspImageProvider;
import ch.sebpiller.babyphone.ui.swing.MainController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class ScheduledFetchRtspImage {

    private final RtspImageProvider streamReader;

    private final MainController mainController;

    @Scheduled(fixedRate = 80, timeUnit = TimeUnit.MILLISECONDS)
    public void captureNextImage() {
        log.debug("Fetching new image from RTSP stream.");
        mainController.receiveRawImage(streamReader.get());
    }

}

