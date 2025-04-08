package ch.sebpiller.babyphone.detection.sound.torefacto;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.util.PitchConverter;
import be.tarsos.dsp.util.fft.FFT;
import ch.sebpiller.babyphone.detection.sound.torefacto.consts.MelSpectrogramDimension;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

// The MelSpectrogram class processes audio files into mel spectrogram images using TarsosDSP utilities.
@Getter
@Setter
@Slf4j
public class MelSpectrogram implements PitchDetectionHandler {

    String currentPitch = ""; // Holds the current detected pitch (e.g., for display purposes).
    int position = 0; // Tracks rendering position for spectrogram visualization.
    private boolean log2Console = false; // Controls whether logarithmic binning messages are logged.
    private boolean showMarkers = false; // Flag to enable/disable visual frequency markers on the spectrogram.
    private boolean showPitch = false; // Flag to enable/disable displaying the detected pitch on the image.
    private float sampleRate = 44100; // Standard sample rate for audio processing, defining processing frequency.
    private int bufferSize = 1024 * 4; // Defines the chunk size of audio data for FFT processing.
    private int overlap = 768 * 4; // Overlap size between audio chunks for smoother frequency continuity.
    private double pitch; // Stores the current pitch frequency result from pitch detection processing.
    private int outputFrameWidth = 640 * 4; // Configurable width of the generated spectrogram image.
    private int outputFrameHeight = 480 * 4; // Configurable height of the generated spectrogram image.
    BufferedImage bufferedImage = new BufferedImage(outputFrameWidth, outputFrameHeight, BufferedImage.TYPE_INT_RGB); // In-memory representation of the generated spectrogram.
    private boolean wrapEnabled = false; // Flag to control whether drawing overflows back to the start of the image.

    // AudioProcessor implementation to compute FFT on audio data and generate the spectrogram visualization.
    AudioProcessor fftProcessor = new AudioProcessor() {

        FFT fft = new FFT(bufferSize); // Instantiates FFT with the defined buffer size.
        float[] amplitudes = new float[bufferSize]; // Array to store the computed amplitudes for each frequency bin.

        public boolean process(AudioEvent audioEvent) {
            var audioFloatBuffer = audioEvent.getFloatBuffer(); // Gets the audio float buffer for processing.
            var transformBuffer = new float[bufferSize * 2]; // Buffer to hold transformed data (real and imaginary parts).
            System.arraycopy(audioFloatBuffer, 0, transformBuffer, 0, audioFloatBuffer.length); // Copies audio data to the buffer.
            fft.forwardTransform(transformBuffer); // Executes the forward FFT transformation on the buffer.
            fft.modulus(transformBuffer, amplitudes); // Computes the modulus (magnitude) of each frequency bin.
            drawFFT(pitch, amplitudes, fft, bufferedImage); // Calls to render the frequency data as an image.
            return true; // Signals the dispatcher to continue processing.
        }

        public void processingFinished() {
            // No additional post-processing is needed.
        }
    };

    // Static utility method to generate a mel spectrogram image from an audio file.
    public static BufferedImage extractSpectrogramFromAudioFile(File f) {
        var melGram = new MelSpectrogram(); // Create a new instance of MelSpectrogram.
        melGram.setOutputFrameWidth(MelSpectrogramDimension.Width); // Sets the standard mel spectrogram width.
        melGram.setOutputFrameHeight(MelSpectrogramDimension.Height); // Sets the standard mel spectrogram height.

        try {
            return melGram.convertAudio(f); // Processes the audio file and generates the spectrogram.
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // Processes an audio file to create a spectrogram and return it as a BufferedImage.
    public BufferedImage convertAudio(File audioFile) throws IOException, UnsupportedAudioFileException {
        var dispatcher = AudioDispatcherFactory.fromFile(audioFile, bufferSize, overlap); // Creates a dispatcher for file-based audio sources.
        bufferedImage = new BufferedImage(outputFrameWidth, outputFrameHeight, BufferedImage.TYPE_INT_RGB); // Initializes the output image.
        dispatcher.addAudioProcessor(fftProcessor); // Adds the FFT-based processor to the dispatcher pipeline.

        position = 0; // Resets rendering state.
        currentPitch = ""; // Clears previous pitch state.
        dispatcher.run(); // Starts processing audio with the dispatcher.

        return bufferedImage; // Returns the generated image.
    }


    // Draws the FFT data onto the buffered image, creating a visual representation (spectrogram).
    private void drawFFT(double pitch, float[] amplitudes, FFT fft, BufferedImage bufferedImage) {
        if (position >= outputFrameWidth && !wrapEnabled) {
            return; // Stops drawing if the edge of the image has been reached and wrapping is disabled.
        }

        var bufferedGraphics = bufferedImage.createGraphics(); // Gets the graphics object for drawing on the image.

        double maxAmplitude = 0; // Tracks the highest amplitude value for normalization.

        var pixelAmplitudes = new float[outputFrameHeight]; // Store amplitudes mapped to spectrogram pixel bins.

        for (var i = amplitudes.length / 800; i < amplitudes.length; i++) { // Processes frequency bins above a threshold for better resolution.
            var pixelY = frequencyToBin((i * 44_100d) / (amplitudes.length * 8f)); // Maps frequency indices to pixel rows.
            pixelAmplitudes[pixelY] += amplitudes[i]; // Accumulates amplitude data for the respective frequency bin.
            maxAmplitude = Math.max(pixelAmplitudes[pixelY], maxAmplitude); // Updates the maximum amplitude found if needed.
        }

        for (var i = 0; i < pixelAmplitudes.length; i++) { // Loops over pixel bins to draw the corresponding intensities.
            var color = Color.black; // Default color for zero amplitude (black background).
            if (maxAmplitude != 0) { // Avoids divide-by-zero errors when normalizing amplitude.
                final var greyValue = (int) (Math.log1p(pixelAmplitudes[i] / maxAmplitude) / Math.log1p(1.0000001) * 255); // Maps amplitude to grayscale intensity.
                color = new Color(greyValue, greyValue, greyValue); // Creates a shade of gray based on amplitude.
            }
            bufferedGraphics.setColor(color); // Sets the pixel color based on the calculated intensity.
            bufferedGraphics.fillRect(position, i, 3, 1); // Draws the pixel as a rectangle on the image.
        }

        if (showPitch && pitch != -1) { // Draws the detected pitch on the spectrogram.
            var pitchIndex = frequencyToBin(pitch); // Converts pitch frequency to spectrogram row index.
            bufferedGraphics.setColor(Color.RED); // Uses red to indicate the pitch.
            bufferedGraphics.fillRect(position, pitchIndex, 1, 1); // Marks the detected pitch on the spectrogram.
            currentPitch = "Current frequency: " + (int) pitch + "Hz"; // Updates the pitch text for display.
        }

        if (showMarkers) { // Adds visual markers for specific frequencies if enabled.
            bufferedGraphics.clearRect(0, 0, 190, 30); // Draws a background for markers.
            bufferedGraphics.setColor(Color.WHITE); // Sets the marker text color.
            bufferedGraphics.drawString(currentPitch, 20, 20); // Displays the current pitch information.

            for (var i = 100; i < 500; i += 100) { // Draws markers for lower frequency bins.
                var bin = frequencyToBin(i);
                bufferedGraphics.drawLine(0, bin, 5, bin); // Marks the bin on the image with a short line.
            }

            for (var i = 500; i <= 20_000; i += 500) { // Draws markers for higher frequency bins.
                var bin = frequencyToBin(i);
                bufferedGraphics.drawLine(0, bin, 5, bin);
            }

            for (var i = 100; i <= 20_000; i *= 10) { // Draws labels for logarithmic frequency bins.
                var bin = frequencyToBin(i);
                bufferedGraphics.drawString(String.valueOf(i), 10, bin); // Writes the frequency value beside the marker.
            }
        }

        position += 3; // Advances the drawing position for the next FFT frame.
        position = position % outputFrameWidth; // Wraps around if wrapping is enabled.
    }

    // Maps a frequency value to a spectrogram image row index.
    private int frequencyToBin(final double frequency) {
        final double minFrequency = 50; // The minimum frequency represented on the spectrogram.
        final double maxFrequency = 11000; // The maximum frequency represented on the spectrogram.
        var bin = 0; // Default bin value for frequencies outside the range.
        final var logaritmic = true; // Flag for logarithmic scale processing.
        if (frequency > minFrequency && frequency < maxFrequency) { // Processes in-range frequencies only.
            double binEstimate = 0; // Holds the row index estimate.
            if (logaritmic) { // Calculates row index based on a logarithmic scale.
                var minCent = PitchConverter.hertzToAbsoluteCent(minFrequency); // Converts min frequency to cent scale.
                var maxCent = PitchConverter.hertzToAbsoluteCent(maxFrequency); // Converts max frequency to cent scale.
                var absCent = PitchConverter.hertzToAbsoluteCent(frequency * 2); // Converts the target frequency to cent scale.
                binEstimate = (absCent - minCent) / maxCent * outputFrameHeight; // Normalizes the cent value to pixel height.
            } else { // Linearly maps frequency to the row index.
                binEstimate = (frequency - minFrequency) / maxFrequency * outputFrameHeight;
            }
            if (binEstimate > 700) {
                log.info("bin estimate: {}", binEstimate);
            }
            bin = outputFrameHeight - 1 - (int) binEstimate; // Subtracts the estimate from height for correct orientation.
        }
        return bin;
    }

    // Handles pitch detection results and updates the class's state accordingly.
    public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
        if (pitchDetectionResult.isPitched()) { // Checks if the pitch detection algorithm detected a pitch.
            pitch = pitchDetectionResult.getPitch(); // Updates the pitch value for rendering and display.
        } else {
            pitch = -1; // Sets an invalid pitch value if none was detected.
        }
    }
}
