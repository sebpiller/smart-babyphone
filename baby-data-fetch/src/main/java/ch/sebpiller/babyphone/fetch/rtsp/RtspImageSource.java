package ch.sebpiller.babyphone.fetch.rtsp;


import ch.sebpiller.babyphone.fetch.image.ImageSource;
import ch.sebpiller.babyphone.fetch.rtsp.properties.RtspStreamProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.Closeable;

import static org.opencv.videoio.Videoio.CAP_PROP_FPS;


@Lazy
@Slf4j
@Service
@RequiredArgsConstructor
public class RtspImageSource implements ImageSource, Closeable, AutoCloseable {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private final RtspStreamProperties streamProperties;
    private final Mat frame = new Mat();
    private VideoCapture videoCapture;
    private double videoFps;
    private long lastGetTime = 0;

    public void reload() {
        if (this.videoCapture != null) {
            this.videoCapture.release();
            this.videoCapture = null;
        }

        this.videoCapture = newVideoCapture();
    }

    private VideoCapture newVideoCapture() {
        var videoCapture = new VideoCapture();
        videoCapture.setExceptionMode(true);

        var rtspUrl = streamProperties.toRtspUrl();
        videoCapture.open(rtspUrl);
        if (!videoCapture.isOpened()) {
            throw new IllegalStateException("Error: unable to open the RTSP stream.");
        }

        videoFps = videoCapture.get(CAP_PROP_FPS);
        return videoCapture;
    }

    @Override
    public BufferedImage get() {
        skipFramesIfNeeded();
        if (videoCapture == null) {
            videoCapture = newVideoCapture();
        }

        var b = false;
        try {
            b = videoCapture.read(frame);
            lastGetTime = System.currentTimeMillis();
        } catch (CvException e) {
            // crashes sometimes for a (yet) unknown reason
            log.warn("Failed to read a frame from the RTSP stream. Reloading the stream...");
            close();
            videoCapture = newVideoCapture();
            b = videoCapture.read(frame);
        }

        if (!b) {
            throw new IllegalStateException("Error: unable to capture a frame.");
        }

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

    private void skipFramesIfNeeded() {
        if (lastGetTime != 0) {
            var skipFrame = (System.currentTimeMillis() - lastGetTime) / 1_000d * videoFps;
            var m = new Mat();
            var i = 0;
            try {
                while (++i < skipFrame && videoCapture.grab() && videoCapture.read(m))
                    m.release();
            } catch (Exception e) {
                log.warn("An error occurred while skipping frames: {}", String.valueOf(e));
            }
        }
    }

    @Override
    public void close() {
        log.info("Releasing video resources...");
        if (videoCapture.isOpened()) {
            videoCapture.release();
            videoCapture = null;
        }
    }
}