package ch.sebpiller.babyphone.lampf.notifier;


import ch.sebpiller.babyphone.fetch.rtsp.properties.RtspStreamProperties;
import ch.sebpiller.iot.lamp.SmartLampFacade;
import ch.sebpiller.iot.lamp.luke.roberts.LampFBle;
import ch.sebpiller.iot.lamp.luke.roberts.LukeRoberts;
import ch.sebpiller.iot.lamp.sequencer.SmartLampSequence;
import ch.sebpiller.spi.toolkit.aop.AutoLog;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
@Slf4j
@AutoLog
@RequiredArgsConstructor
public class RtspSoundMonitor {

    private final RtspStreamProperties rtspStreamProperties;

    private  int threshold = 50;
    public static final int MIN_BLINKING_GAP = 5_000;
    private static final int BUFFERIZE_FRAMES = 8;
    private Consumer<Double> c;
    private boolean over;
    private boolean notifyLamp = true;


    public void addListener(Consumer<Double> c) {
        this.c = c;
    }

    private long grabStart;

    static {
        // Conseillé pour flux RTSP à faible latence
        avutil.av_log_set_level(avutil.AV_LOG_WARNING);
        //avutil.av_log_set_level(avutil.AV_LOG_INFO);
        avformat.avformat_network_init();
    }

    public void stop() {
        over = true;
    }

    @SneakyThrows
    @PostConstruct
    public void init() {
        var lamp = initBluetoothLamp();

        var rtspUrl = rtspStreamProperties.toRtspUrl();

        runThread(rtspUrl, lamp);
    }

    private void runThread(String rtspUrl, SmartLampFacade lamp) {

        var t = new Thread(() -> {

            try (var grabber = initGrabber(rtspUrl)) {

                var lastBlink = 0L;

                while (!over) {

                    var now = System.currentTimeMillis();
                    var sumRms = 0d;
                    var countFrame = 0;
                    long firstFrameTime = 0L;
                    long lastFrameTime = 0L;


                    do {
                        var grabbedFrame = grabber.grabFrame();
                        if (grabbedFrame == null) {
                            continue;
                        }

                        if (grabbedFrame.samples != null) {
                            if (firstFrameTime == 0L) {
                                firstFrameTime = grabbedFrame.timestamp;
                            }
                            lastFrameTime = grabbedFrame.timestamp;
                            sumRms += computeRms(grabbedFrame);
                            countFrame++;
                        }
                        grabbedFrame.close();
                    } while (countFrame < BUFFERIZE_FRAMES);

                    if (countFrame == 0) {
                        log.debug("no frame was captured, continuing");
                        continue;
                    }

                    if (log.isDebugEnabled()) {
                        var from = Instant.ofEpochMilli(grabStart + TimeUnit.MICROSECONDS.toMillis(firstFrameTime))
                                .atZone(ZoneId.systemDefault());
                        var to = Instant.ofEpochMilli(grabStart + TimeUnit.MICROSECONDS.toMillis(lastFrameTime))
                                .atZone(ZoneId.systemDefault());

                        log.debug("analyzing {} frames between {} to {} (duration {}ms)",
                                countFrame,
                                from.format(DateTimeFormatter.ISO_LOCAL_TIME),
                                to.format(DateTimeFormatter.ISO_LOCAL_TIME),
                                TimeUnit.MICROSECONDS.toMillis(lastFrameTime - firstFrameTime)
                        );
                    }

                    var avgRms = sumRms / countFrame;
                    c.accept(avgRms);

                    if (avgRms < threshold) {
                        log.debug("  > average noise: {}", avgRms);
                    } else {
                        log.info("*** detected noise *** avg: {}", avgRms);

                        if (notifyLamp) {
                            if (now - lastBlink <= MIN_BLINKING_GAP) {
                                log.debug("not blinking because last blink was done recently");
                            } else {
                                log.debug("blinking!");

                                SmartLampSequence.begin().flash(3, (byte) ((avgRms / 100d) * 100d)).end().play(lamp);
                                lastBlink = System.currentTimeMillis();
                            }
                        }
                    }
                }
            } catch (FrameGrabber.Exception e) {
                throw new RuntimeException(e);
            } finally {
                avformat.avformat_network_deinit();
            }

        });
        t.setDaemon(true);
        t.start();
    }

    @SneakyThrows
    private FFmpegFrameGrabber initGrabber(String rtspUrl) {
        var grabber = getFFmpegFrameGrabber(rtspUrl);

        log.info("Audio: codec={} ch={} rate={} fmt={}", grabber.getAudioCodecName(), grabber.getAudioChannels(), grabber.getSampleRate(), grabber.getSampleFormat());

        // Si nécessaire, forcer un format PCM S16 pour calculer facilement le RMS
        // Attention: cela déclenche un resample par FFmpeg si format source différent
        var forceS16 = true;
        if (forceS16) {
            grabber.setSampleFormat(avutil.AV_SAMPLE_FMT_S16);
            grabber.setAudioBitrate(16);
        }

        return grabber;
    }

    private FFmpegFrameGrabber getFFmpegFrameGrabber(String rtspUrl) throws FFmpegFrameGrabber.Exception {
        var grabber = new FFmpegFrameGrabber(rtspUrl);
        grabber.setOption("rtsp_transport", "tcp");           // ou "udp" selon votre caméra/réseau
        grabber.setOption("stimeout", String.valueOf(TimeUnit.SECONDS.toMicros(5))); // timeout socket
        grabber.setOption("reorder_queue_size", "0");
        grabber.setOption("fflags", "nobuffer");
        grabber.setOption("flags", "low_delay");
        grabber.setOption("max_delay", "100");
        grabber.setOption("probesize", String.valueOf((64 * 1024)));
        grabber.setOption("analyzeduration", "0");

        grabStart = System.currentTimeMillis();
        grabber.start();
        return grabber;
    }

    private static SmartLampFacade initBluetoothLamp() {
        var config = LukeRoberts.LampF.Config.getDefaultConfig();

        if (System.getProperty("lamp.f.mac") != null) {
            config.setMac(System.getProperty("lamp.f.mac"));
        }

        var lamp = new LampFBle(config);
        lamp.setBrightness((byte) 0);      //  lamp.setScene(LukeRoberts.LampF.Scene.INDIRECT_SCENE.getId());

        return lamp;
    }

    private static double computeRms(Frame frame) {
        // Gestion des formats audio courants: S16 interleaved et FLT/FLTP
        Object[] samples = frame.samples;
        if (samples == null || samples.length == 0) {
            return 0D;
        }

        // Cas float (planar ou interleaved selon la source)
        if (samples[0] instanceof float[] data) {
            return rmsFloatInterleaved(data);
        }

        if (samples[0] instanceof FloatBufferWrapper fbw) {
            // Certaines versions exposent un ByteBuffer; ici par sécurité on tente d'extraire les floats
            var arr = new float[fbw.remaining()];
            fbw.get(arr);
            return rmsFloatInterleaved(arr);
        }

        if (samples[0] instanceof short[] data) {
            // Interleaved 16-bit PCM
            return rmsShortInterleaved(data);
        }

        if (samples[0] instanceof ByteBuffer bb) {
            // Fallback: essayer de lire en PCM16
            var len = bb.remaining() / 2;
            var sum = 0D;
            for (var i = 0; i < len; i++) {
                var lo = bb.get() & 0xFF;
                var hi = bb.get();
                var s = (short) ((hi << 8) | lo);
                sum += s * (double) s;
            }
            var mean = sum / Math.max(1, len);
            return Math.sqrt(mean);
        }

        if (samples[0] instanceof java.nio.Buffer buf) {
            // Planar: plusieurs buffers, un par canal (ex: FLTP)
            // On calcule la moyenne sur tous les canaux
            var sum = 0D;
            var count = 0L;
            for (var o : samples) {
                if (o instanceof FloatBuffer fb) {
                    while (fb.hasRemaining()) {
                        var v = fb.get();
                        sum += v * v;
                        count++;
                    }
                    fb.rewind();
                } else if (o instanceof java.nio.ShortBuffer sb) {
                    while (sb.hasRemaining()) {
                        var v = sb.get();
                        sum += v * (double) v;
                        count++;
                    }
                    sb.rewind();
                }
            }
            if (count == 0) {
                return 0D;
            }
            return Math.sqrt(sum / count);
        }

        return 0.0;
    }

    private static double rmsShortInterleaved(short[] data) {
        var sum = 0L;
        for (var s : data) {
            sum += (long) s * s;
        }
        var mean = sum / Math.max(1D, data.length);
        return Math.sqrt(mean);
    }

    private static double rmsFloatInterleaved(float[] data) {
        var sum = 0d;
        for (var v : data) {
            sum += v * v;
        }
        var mean = sum / Math.max(1.0, data.length);
        // data en float est souvent [-1,1]; RMS déjà « normalisé »
        return Math.sqrt(mean);
    }

    // Méthode fournie par vous (pour PCM16 little-endian) si vous travaillez avec byte[]
    public static double calculateRMS(byte[] audioBuffer) {
        var sum = 0L;
        for (var i = 0; i < audioBuffer.length; i += 2) {
            var sample = (audioBuffer[i + 1] << 8) | (audioBuffer[i] & 0xff);
            sum += sample * (long) sample;
        }
        var mean = sum / (audioBuffer.length / 2.0);
        return Math.sqrt(mean);
    }

    public void setNotifyLamp(boolean selected) {
        this.notifyLamp = selected;
    }

    public void setThreshold(int value) {
        this.threshold = value;
    }

    private record FloatBufferWrapper(FloatBuffer buf) {
        int remaining() {
            return buf.remaining();
        }

        void get(float[] dst) {
            buf.get(dst);
        }
    }
}
