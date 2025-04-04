package ch.sebpiller.babyphone.fetch.rtsp;


import ch.sebpiller.babyphone.fetch.image.ImageCapturer;
import ch.sebpiller.babyphone.fetch.rtsp.properties.RtspStreamProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.Closeable;
import java.io.IOException;

import static org.opencv.videoio.Videoio.CAP_PROP_FPS;

@Slf4j
@Service
@RequiredArgsConstructor
public class RtspStreamReader implements ImageCapturer, Closeable, AutoCloseable {

    private final RtspStreamProperties streamProperties;
    private Mat frame = new Mat();
    private VideoCapture videoCapture;
    private double videoFps;
    private long lastGetTime = 0;

    public void reload() {
        if (this.videoCapture != null) {
            this.videoCapture.release();
        }

        this.videoCapture = newVideoCapture();
    }

    private VideoCapture newVideoCapture() {
        VideoCapture videoCapture = new VideoCapture();
        videoCapture.setExceptionMode(true);

        log.info("Initializing RTSP stream reader...");
        var rtspUrl = streamProperties.toRtspUrl();

        log.debug("Attempting to open RTSP stream with URL: {}", rtspUrl);
        videoCapture.open(rtspUrl);
        if (!videoCapture.isOpened()) {
            log.error("Failed to open the RTSP stream: {}", rtspUrl);
            throw new IllegalStateException("Error: unable to open the RTSP stream.");
        }

        log.info("RTSP stream opened successfully.");

        frame = new Mat();

        videoFps = videoCapture.get(CAP_PROP_FPS);

        return videoCapture;
    }

    @PostConstruct
    private void init() {
        reload();
    }

    @Override
    public BufferedImage get() {
        log.debug("Attempting to capture a frame from the RTSP stream.");

        if (lastGetTime != 0) {
            var skipFrame = (System.currentTimeMillis() - lastGetTime) / 1_000d * videoFps;
            var m = new Mat();
            var i = 0;
            try {
                while (++i < skipFrame && videoCapture.grab() && videoCapture.read(m))
                    m.release();
            } catch (Exception e) {
                log.error("An error occurred while skipping frames: ", e);
            }
        }

        if (!videoCapture.read(frame)) {
            log.error("Failed to read a frame from the RTSP stream.");
            throw new IllegalStateException("Error: unable to capture a frame.");
        }
        lastGetTime = System.currentTimeMillis();

        if (frame.empty()) {
            log.error("Captured frame is empty.");
            throw new IllegalStateException("Error: the captured frame is empty.");
        }

        log.debug("Frame captured successfully. Dimensions: {}x{}, Channels: {}", frame.cols(), frame.rows(), frame.channels());
        var type = frame.channels() > 1 ? BufferedImage.TYPE_3BYTE_BGR : BufferedImage.TYPE_BYTE_GRAY;
        var image = new BufferedImage(frame.cols(), frame.rows(), type);
        var data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        frame.get(0, 0, data);
        log.debug("Frame successfully converted to BufferedImage.");

        return image;

    }

    public void release() {
        log.info("Releasing video resources...");
        if (videoCapture.isOpened()) {
            videoCapture.release();
            log.debug("Video resources released successfully.");
        } else {
            log.debug("No video resources to release.");
        }
    }

    @Override
    public void close() throws IOException {
        log.info("Closing RTSP stream reader.");
        release();
    }
}