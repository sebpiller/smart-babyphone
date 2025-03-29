package ch.sebpiller.babyphone.service.rtsp;

import ch.sebpiller.babyphone.config.properties.RtspStreamProperties;
import ch.sebpiller.babyphone.service.ia.RecognitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class ScheduledFetchRtspImage {

    private final RtspStreamProperties rtspStreamProperties;
    private final RecognitionService recognitionService;

    private Mat lastFrame;

    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.SECONDS)
    public void captureAndAnalyzeCurrentImage() {
        long v = System.currentTimeMillis();
        var streamReader = new RtspStreamReader(rtspStreamProperties.toRtspUrl());

        var frame = streamReader.getFrame();
        Imgcodecs.imwrite("captured_" + v + ".jpg", frame);

        streamReader.release();
        lastFrame = frame;

        var annotatedImage = recognitionService.annotateImage(frame);
        Imgcodecs.imwrite("annotated_" + v + ".jpg", annotatedImage);
    }

}

