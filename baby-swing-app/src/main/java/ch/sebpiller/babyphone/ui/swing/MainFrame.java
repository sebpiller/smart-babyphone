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
import java.util.concurrent.atomic.AtomicReference;

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
            }
        }
    };
    private final JLabel[] detecteds = new JLabel[10];
    private JLabel fps;
    private transient Detected highlight;

    @SneakyThrows
    @PostConstruct
    private void init() {
        SwingUtilities.invokeAndWait(() -> {
            for (var i = 0; i < detecteds.length; i++) {
                detecteds[i] = new JLabel("");
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
                        reannotateImage(detectionResult);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        ((JLabel) e.getSource()).setForeground(defaultColor);
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

            var sel = new JComboBox<>();

            var xxx = new ArrayList<>(controller.getAvailableAlgorithms());
            xxx.addFirst(null);
            sel.setModel(new DefaultComboBoxModel<>(xxx.toArray()));
            sel.setSelectedItem(controller.getIaProcessingAlgorithm());
            sel.addActionListener(e -> controller.setIaProcessingAlgorithm((ImageAnalyzer) ((JComboBox<?>) (e.getSource())).getSelectedItem()));

            eastPane.add(wrap(new JLabel("Algorithm in use:"), sel));

            var slider = new JSlider(0, 100, 0);
            slider.setValue((int) (controller.getConfidencyThreshold() * 100));
            slider.addChangeListener(e -> controller.setConfidencyThreshold((float) (((JSlider) e.getSource()).getValue() / 100d)));

            eastPane.add(wrap(new JLabel("Confidence threshold:"), slider));

            var comp = new JPanel();
            comp.setLayout(new BoxLayout(comp, BoxLayout.Y_AXIS));
            comp.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    highlight = null;
                    reannotateImage(controller.getDetectionResult());
                    repaint();
                }
            });
            eastPane.add(comp);

            var i = 0;
            for (var d : detecteds) {
                comp.add(wrap(new JLabel("Detected #" + ++i), d));
            }

            contentPane.add(eastPane, BorderLayout.EAST);
            setDefaultCloseOperation(EXIT_ON_CLOSE);

            pack();
            setLocationRelativeTo(null);
            log.debug("MainFrame initialization complete");
        });
    }

    public void reannotateImage(DetectionResult dr) {
        image.set(annotateBufferedWithDetection(dr));
        SwingUtilities.invokeLater(() -> {
            for (var i = 0; i < detecteds.length; i++) {
                var x = i < dr.getDetected().size() ? dr.getDetected().get(i) : null;
                detecteds[i].setText(x != null ? x.type() + " " + ((int) (x.score() * 100)) + "%" : " ");
            }
            repaint();
        });
    }

    private JComponent wrap(JComponent... sel) {
        var jPanel = new JPanel();
        Arrays.stream(sel).forEach(jPanel::add);
        return jPanel;
    }

    private BufferedImage annotateBufferedWithDetection(DetectionResult x) {
        var img = x.getImage();
        if (img == null) return null;
        if (x.getDetected().isEmpty()) return img;

        var graphics = (Graphics2D) img.getGraphics();
        graphics.setFont(Font.decode("Arial-24"));
        for (var d : x.getDetected()) {
            graphics.setColor(highlight == d ? Color.RED : Color.GREEN);
            graphics.setStroke(new BasicStroke((float) (10 * Math.pow(d.score(), 2))));
            graphics.drawString(d.type() + " " + d.score(),
                    Math.clamp(d.x(), 10, img.getWidth() - 100),
                    Math.clamp(d.y(), 10, img.getHeight() - 100));
            graphics.drawRect(d.x(), d.y(), d.width(), d.height());
        }
        graphics.dispose();
        return img;
    }

    public void setFps(Long x) {
        SwingUtilities.invokeLater(() -> fps.setText("Latency: " + x + "ms"));
    }
}
