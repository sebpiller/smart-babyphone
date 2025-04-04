package ch.sebpiller.babyphone.ui.swing;

import ch.sebpiller.babyphone.fetch.rtsp.RtspStreamReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class ScheduledFetchRtspImage {

    private final RtspStreamReader streamReader;

    private final MainController mainController;

    @Scheduled(fixedRate = 80, timeUnit = TimeUnit.MILLISECONDS)
    public void captureAndAnalyzeCurrentImage() {
        log.debug("Fetching new image from RTSP stream.");
        mainController.receiveRawImage(streamReader.get());
    }

}

