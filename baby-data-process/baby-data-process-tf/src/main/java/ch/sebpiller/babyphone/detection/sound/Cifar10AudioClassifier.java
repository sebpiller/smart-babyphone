package ch.sebpiller.babyphone.detection.sound;

import ch.sebpiller.babyphone.detection.Detected;
import ch.sebpiller.babyphone.detection.DetectionResult;
import ch.sebpiller.babyphone.detection.SoundAnalyzer;
import ch.sebpiller.babyphone.sound.toolkit.MelSpectrogram;
import ch.sebpiller.babyphone.tensorflow.BaseTensorFlowRunnerFacade;
import ch.sebpiller.babyphone.tensorflow.TensorUtils;
import ch.sebpiller.babyphone.toolkit.ImageUtils;
import ch.sebpiller.spi.toolkit.aop.AutoLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.tensorflow.Session;
import org.tensorflow.types.TFloat32;

import javax.sound.sampled.AudioFormat;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.function.Predicate;

@Lazy
@Slf4j
@Service
@AutoLog
public class Cifar10AudioClassifier extends BaseTensorFlowRunnerFacade implements Closeable, SoundAnalyzer {


    public static final String MODELS = "/home/seb/models";
    public static final String HIGH_RES = MODELS + "/cifar10";
    public static final String MODEL_PATH = HIGH_RES;

    public static final int Width = 1366;
    public static final int Height = 96;

    public static final String[] labels = new String[]{
            "blues", "classical", "country", "disco", "hiphop", "jazz", "metal", "pop", "reggae", "rock"

    };

    public Cifar10AudioClassifier() {
        super(MODEL_PATH);
    }

    @Override
    public DetectionResult detectObjectsOn(byte[] sound, AudioFormat format, Predicate<Detected> includeInResult) {
        var image = MelSpectrogram.extractSpectrogramFromAudioFile(sound, format);

        if (image != null) {
            return analyzeHistogram(image);
        }

        return null;
    }

    DetectionResult analyzeHistogram(BufferedImage image) {
        var dr = DetectionResult.builder();
        var predicted = runModelOnImage(image, Width, Height);

        var d = new ArrayList<Detected>();
        for (var x = 0; x < predicted.length; x++) {
            d.add(new Detected(labels[x], predicted[x], 0, 0, 1, 1));
        }

        dr.detected(d);
        return dr.build();
    }

    private float[] runModelOnImage(BufferedImage image, int imgWidth, int imgHeight) {
        image = ImageUtils.resizeImage(image, imgWidth, imgHeight);
        var imageTensor = TensorUtils.getImageTensor(image, imgWidth, imgHeight);

        try (var sess = new Session(graph); var result = sess.runner()
                .feed("conv2d_1_input:0", imageTensor)
                //.feed("dropout_1/keras_learning_phase:0", Tensor.create(false))
                .fetch("output_node0:0").run();) {

            var res = (TFloat32) result.get(0);
            var rshape = res.shape();
            if (rshape.numDimensions() != 2 || rshape.asArray()[0] != 1) {
                throw new IllegalStateException("Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape " + rshape);
            }

            var array = result.get(0).shape().asArray();
            var ress = new float[array.length];
            var i = 0;

            for (var l : array) {
                ress[i++] = l;
            }

            return ress;
        }
    }
}
