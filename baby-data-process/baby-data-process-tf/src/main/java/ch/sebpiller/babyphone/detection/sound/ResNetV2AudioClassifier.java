package ch.sebpiller.babyphone.detection.sound;

import ch.sebpiller.babyphone.detection.Detected;
import ch.sebpiller.babyphone.detection.DetectionResult;
import ch.sebpiller.babyphone.detection.SoundAnalyzer;

import ch.sebpiller.babyphone.toolkit.ImageUtils;
import ch.sebpiller.babyphone.toolkit.sound.MelSpectrogram;
import ch.sebpiller.babyphone.toolkit.tensorflow.BaseTensorFlowRunnerFacade;
import ch.sebpiller.babyphone.toolkit.tensorflow.TensorUtils;
import ch.sebpiller.spi.toolkit.aop.AutoLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.tensorflow.ndarray.buffer.DataBuffers;
import org.tensorflow.types.TFloat32;

import javax.sound.sampled.AudioFormat;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Predicate;

@Lazy
@Slf4j
@Service
@AutoLog
public class ResNetV2AudioClassifier extends BaseTensorFlowRunnerFacade implements SoundAnalyzer {

    public static final String MODELS = "/home/seb/models";
    public static final String HIGH_RES = MODELS + "/resnetv2/resnet-v2.pb";
    public static final String MODEL_PATH = HIGH_RES;

    public static final int FRAME_WIDTH = 1366;
    public static final int FRAME_HEIGHT = 96;
    public static final String[] LABELS = new String[]{
            "blues",
            "classical",
            "country",
            "disco",
            "hiphop",
            "jazz",
            "metal",
            "pop",
            "reggae",
            "rock",
    };

    public ResNetV2AudioClassifier() {
        super(Path.of(MODEL_PATH));
    }

    @Override
    public DetectionResult detectObjectsOn(byte[] sound, AudioFormat format, Predicate<Detected> includeInResult) {
        var image = MelSpectrogram.extractSpectrogramFromAudioFile(sound, format);

        if (image == null) {
            throw new IllegalStateException("Failed to extract mel spectrogram from audio file");
        }

        return analyzeHistogram(image);
    }

    public DetectionResult analyzeHistogram(BufferedImage histogram) {
        return analyzeHistogram(histogram, FRAME_WIDTH, FRAME_HEIGHT);
    }

    DetectionResult analyzeHistogram(BufferedImage histogram, int imgWidth, int imgHeight) {
        var dr = DetectionResult.builder().image(histogram);
        var predicted = runModelOnImage(histogram, imgWidth, imgHeight);

        if (predicted != null) {
            var d = new ArrayList<Detected>();

            for (var x = 0; x < predicted.length; x++) {
                d.add(new Detected(LABELS[x], predicted[x], 0, 0, 1, 1));
            }

            dr.detected(d);
        }

        return dr.build();
    }

    public float[] runModelOnImage(BufferedImage bi, int imgWidth, int imgHeight) {
        var image = ImageUtils.resizeImage(bi, imgWidth, imgHeight);
        var imageTensor = TensorUtils.getImageTensor(image, imgWidth, imgHeight);

        try (var result = session.runner().feed("input_1:0", imageTensor)
                //.feed("dropout_1/keras_learning_phase:0", Tensor.create(false))
                .fetch("output_node0:0").run()) {

            var res = (TFloat32) result.get(0);
            var rshape = res.shape();
            if (rshape.numDimensions() != 2 || rshape.asArray()[0] != 1) {
                throw new IllegalStateException("Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape " + rshape);
            }

            var dst = DataBuffers.ofFloats(res.size());
            res.copyTo(dst);

            var x = new float[(int) dst.size()];
            dst.read(x);
            return x;
        }
    }
}
