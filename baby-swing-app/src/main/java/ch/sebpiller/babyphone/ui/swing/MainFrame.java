package ch.sebpiller.babyphone.ui.swing;

import ch.sebpiller.babyphone.detection.Detected;
import ch.sebpiller.babyphone.detection.DetectionResult;
import ch.sebpiller.babyphone.detection.ImageAnalyzer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static ch.sebpiller.babyphone.detection.sound.ResNetV2AudioClassifier.labels;

@Slf4j
@RequiredArgsConstructor
@Component
public class MainFrame extends JFrame {

    private final transient MainController controller;
    private final transient AtomicReference<Image> image = new AtomicReference<>();
    private final JPanel display = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            var i = image.get();
            if (i != null) {
                g.drawImage(i, 0, 0, getWidth(), getHeight(), this);
                log.debug("Image displayed with dimensions: {}x{}", getWidth(), getHeight());
            } else {
                log.debug("No image to display.");
            }
        }
    };
    private final JLabel[] detecteds = new JLabel[10];
    private JLabel fps;
    private transient Detected highlight;
    private BufferedImage soundImage;
    private JPanel soundImagePanel = new JPanel() {
        @Override
        public void paint(Graphics g) {
            super.paint(g);

            if (soundImage != null) {
                g.drawImage(soundImage, 0, 0, getWidth(), getHeight(), this);
                log.debug("Sound image displayed with dimensions: {}x{}", getWidth(), getHeight());
            } else {
                log.debug("No sound image to display.");
            }

        }
    };

    @SneakyThrows
    @PostConstruct
    private void init() {
        log.info("Initializing MainFrame...");
        SwingUtilities.invokeAndWait(() -> {
            for (var i = 0; i < detecteds.length; i++) {
                detecteds[i] = new JLabel("---");
                detecteds[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                var defaultColor = detecteds[i].getForeground();
                var finalI = i;
                detecteds[i].addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        ((JLabel) e.getSource()).setForeground(Color.RED);
                        e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        var detectionResult = controller.getDetectionResult();
                        var detected = detectionResult.getDetected();
                        highlight = finalI < detected.size() ? detected.get(finalI) : null;
                        log.debug("Mouse entered on detection label: {}", highlight);
                        reannotateImage(detectionResult);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        ((JLabel) e.getSource()).setForeground(defaultColor);
                        log.debug("Mouse exited from detection label.");
                    }
                });
            }

            display.setMinimumSize(new Dimension(640, 480));
            display.setPreferredSize(new Dimension(640, 480));
            setTitle("Smart Babyphone");
            var contentPane = getContentPane();
            contentPane.setLayout(new BorderLayout(10, 10));
            contentPane.add(display, BorderLayout.CENTER);
            var eastPane = new JPanel();
            eastPane.setLayout(new BoxLayout(eastPane, BoxLayout.Y_AXIS));

            fps = new JLabel("Latency: ?");
            eastPane.add(wrap(fps));
            log.debug("FPS label initialized.");

            var sel = new JComboBox<>();

            var xxx = new ArrayList<>(controller.getAvailableAlgorithms());
            xxx.addFirst(null);
            sel.setModel(new DefaultComboBoxModel<>(xxx.toArray()));
            sel.setSelectedItem(controller.getIaProcessingAlgorithm());
            sel.addActionListener(e -> {
                var selectedAlgorithm = (ImageAnalyzer) ((JComboBox<?>) (e.getSource())).getSelectedItem();
                controller.setIaProcessingAlgorithm(selectedAlgorithm);
                log.info("Algorithm changed to: {}", selectedAlgorithm);
            });

            eastPane.add(wrap(new JLabel("Algorithm in use:"), sel));

            var slider = new JSlider(0, 100, 0);
            slider.setValue((int) (controller.getConfidencyThreshold() * 100));
            slider.addChangeListener(e -> {
                var newThreshold = (float) (((JSlider) e.getSource()).getValue() / 100d);
                controller.setConfidencyThreshold(newThreshold);
                log.info("Confidence threshold adjusted to: {}", newThreshold);
            });

            eastPane.add(wrap(new JLabel("Confidence threshold:"), slider));

            var comp = new JPanel();
            comp.setLayout(new BoxLayout(comp, BoxLayout.Y_AXIS));
            comp.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    highlight = null;
                    log.debug("Highlight cleared on mouse exit from detection area.");
                    reannotateImage(controller.getDetectionResult());
                    repaint();
                }
            });
            eastPane.add(comp);

            var i = 0;
            for (var d : detecteds) {
                comp.add(wrap(new JLabel("Detected #" + ++i), d));
                log.debug("Detection label added: Detected #{}", i);
            }

            contentPane.add(eastPane, BorderLayout.EAST);
            contentPane.add(getSouthPane(), BorderLayout.SOUTH);
            log.debug("Layout components initialized.");

            setDefaultCloseOperation(EXIT_ON_CLOSE);

            pack();
            setLocationRelativeTo(null);
            log.info("MainFrame initialization complete.");
        });
    }

    public void reannotateImage(DetectionResult dr) {
        log.debug("Reannotating image...");
        SwingUtilities.invokeLater(() -> {
            var annotatedImage = annotateBufferedWithDetection(dr);
            image.set(annotatedImage);
            if (annotatedImage != null) {
                log.debug("Annotated image set with detections: {}", dr.getDetected());
            } else {
                log.debug("Annotated image is null, no detections found.");
            }

            var i = new AtomicInteger();
            dr.matched()
                    .limit(detecteds.length)
                    .forEach(x -> {
                        detecteds[i.getAndIncrement()].setText(x.type() + " " + ((int) (x.score() * 100)) + "%");
                        log.debug("Label updated for detection: {}", x);
                    });

            for (var ii = i.get(); ii < detecteds.length; ii++) {
                detecteds[ii].setText("---");
            }

            repaint();
            log.debug("Image reannotation complete.");
        });
    }

    private JComponent wrap(JComponent... sel) {
        var jPanel = new JPanel();
        Arrays.stream(sel).forEach(jPanel::add);
        return jPanel;
    }

    private JPanel getSouthPane() {
        log.debug("Initializing south pane...");
        var p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.add(Box.createHorizontalGlue());

        for (var l : labels) {
            var pp = new JPanel();
            pp.setLayout(new BorderLayout());
            pp.add(new JLabel(l), BorderLayout.SOUTH);
            JProgressBar comp = new JProgressBar(SwingConstants.VERTICAL);
            comp.setName(l);
            comp.setPreferredSize(new Dimension(10, 100));
            comp.setStringPainted(true);
            comp.setMaximum(100);
            pp.add(comp, BorderLayout.CENTER);

            p.add(pp);
        }

        soundImagePanel.setPreferredSize(new Dimension(320, 240));
        p.add(soundImagePanel);

        log.debug("South pane initialization complete.");
        return p;
    }

    private BufferedImage annotateBufferedWithDetection(DetectionResult x) {
        var img = x.getImage();
        if (img == null) return null;
        if (x.getDetected().isEmpty()) return img;

        var graphics = (Graphics2D) img.getGraphics();
        graphics.setFont(Font.decode("Arial-24"));
        x.matched().forEach(d -> {
            graphics.setColor(highlight == d ? Color.RED : Color.GREEN);
            graphics.setStroke(new BasicStroke((float) (10 * Math.pow(d.score(), 2))));
            graphics.drawString(d.type() + " " + d.score(),
                    Math.clamp(d.x(), 10, img.getWidth() - 100),
                    Math.clamp(d.y(), 10, img.getHeight() - 100));
            graphics.drawRect(d.x(), d.y(), d.width(), d.height());
        });

        graphics.dispose();
        return img;
    }

    private <T extends JComponent> T getNamedChild(Container c, String name, Class<T> clazz) {
        if (c == null) c = this.getContentPane();

        var xxx = Arrays.stream(c.getComponents())
                .filter(x -> clazz.isAssignableFrom(x.getClass()))
                .filter(x -> x.getName().equals(name))
                .findFirst();
        if (xxx.isPresent()) return (T) xxx.get();

        for (var x : c.getComponents()) {
            if (x instanceof Container xx) {
                var y = getNamedChild(xx, name, clazz);
                if (y != null) return y;
            }
        }

        return null;
    }

    public void setFps(Long x) {
        SwingUtilities.invokeLater(() -> fps.setText("Latency: " + x + "ms"));
    }

    public void receiveSound(DetectionResult x) {
        x.matched()
                .forEach(xx ->
                        Objects.requireNonNull(getNamedChild(this.getContentPane(), xx.type(), JProgressBar.class))
                                .setValue((int) (xx.score() * 100))
                );

        soundImage = x.getImage();
        soundImagePanel.repaint();
    }
}
