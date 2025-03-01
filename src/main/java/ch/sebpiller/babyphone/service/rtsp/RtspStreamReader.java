package ch.sebpiller.babyphone.service.rtsp;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

@Slf4j
public class RtspStreamReader {

    private final VideoCapture videoCapture = new VideoCapture();

    public RtspStreamReader(String rtspUrl) {
        videoCapture.open(rtspUrl);

        if (!videoCapture.isOpened()) {
            throw new IllegalStateException("Error: unable to open the RTSP stream.");
        }
    }

    public Mat getFrame() {
        var frame = new Mat();
        if (!videoCapture.read(frame)) {
            throw new IllegalStateException("Error: unable to capture a frame.");
        }

        return frame;
    }

    public void release() {
        if (videoCapture.isOpened()) {
            videoCapture.release();
            log.debug("Video resources released successfully.");
        }
    }
}