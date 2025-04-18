/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2021 Neil C Smith - Codelerity Ltd.
 *
 * Copying and distribution of this file, with or without modification,
 * are permitted in any medium without royalty provided the copyright
 * notice and this notice are preserved. This file is offered as-is,
 * without any warranty.
 *
 */
package ch.sebpiller.babyphone.ui;

import lombok.extern.slf4j.Slf4j;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.AppSink;
import org.freedesktop.gstreamer.elements.PlayBin;
import org.freedesktop.gstreamer.event.SeekFlags;
import org.freedesktop.gstreamer.message.Message;
import org.freedesktop.gstreamer.swing.GstVideoComponent;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

/**
 * A simple PlayBin-based video player with Swing UI for file selection, video
 * control, seek and volume meters.
 *
 * @author Neil C Smith ( https://www.codelerity.com )
 */
@Slf4j
public class SwingPlayer {

    /**
     * Always store the top-level pipeline (in this case PlayBin) reference to
     * stop it being garbage collected.
     */
    private static PlayBin playbin;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        /**
         * Set up paths to native GStreamer libraries - see adjacent file.
         */
        Utils.configurePaths();

        /**
         * Initialize GStreamer. Always pass the lowest version you require -
         * Version.BASELINE is GStreamer 1.8. Use Version.of() for higher.
         * Features requiring later versions of GStreamer than passed here will
         * throw an exception in the bindings even if the actual native library
         * is a higher version.
         */
        Gst.init(Version.BASELINE, "SwingPlayer", args);

        EventQueue.invokeLater(() -> {

            // It's 2021! Using Apache-licensed FlatLaf.
            boolean useNativeFileDialog = true;

            /**
             * GstVideoComponent from gst1-java-swing is a Swing component that
             * wraps a GStreamer AppSink to display video in a Swing UI.
             */
            GstVideoComponent vc = new GstVideoComponent();

            /**
             * Create a PlayBin element and set the AppSink from the Swing
             * component as the video sink.
             */
            playbin = new PlayBin("playbin");
            AppSink element = new AppSink("xxx");
            element.set("emit-signals", true);
            element.connect((AppSink.NEW_SAMPLE) elem -> {
                log.info("New sample");
                var s = elem.pullSample();

                var buf = s.getBuffer().map(false);

                try {
                    BufferedImage bi = ImageIO.read(new InputStream() {

                                                        public int read() throws IOException {
                                                            if (!buf.hasRemaining()) {
                                                                return -1;
                                                            }
                                                            return buf.get() & 0xFF;
                                                        }

                                                        @Override
                                                        public int read(byte[] bytes, int off, int len)
                                                                throws IOException {
                                                            if (!buf.hasRemaining()) {
                                                                return -1;
                                                            }

                                                            len = Math.min(len, buf.remaining());
                                                            buf.get(bytes, off, len);
                                                            return len;
                                                        }
                                                    }
                    );


                    try {
                        File outputFile = new File("snapshot_" + System.currentTimeMillis() + ".png");
                        ImageIO.write(bi, "png", outputFile);
                        log.info("Snapshot saved to: {}", outputFile.getAbsolutePath());
                    } catch (IOException ex) {
                        log.error("Failed to save snapshot", ex);
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                return FlowReturn.OK;
            });

            playbin.setVideoSink(element);


            /**
             * Create a level component and set it as the audio-filter property
             * on the playbin - this will post audio level messages to the bus -
             * see below how to display them.
             */
            Element level = ElementFactory.make("level", "level");
            playbin.set("audio-filter", level);

            /**
             * A basic Swing UI.
             */
            JFrame window = new JFrame("Video Player");
            window.add(vc);

            vc.setPreferredSize(new Dimension(800, 600));
            JToolBar buttons = new JToolBar();

            JButton fileButton = new JButton("File...");
            fileButton.addActionListener(e -> {
                File file = null;
                if (useNativeFileDialog) {
                    FileDialog fileDialog = new FileDialog(window);
                    fileDialog.setVisible(true);
                    File[] files = fileDialog.getFiles();
                    if (files.length > 0) {
                        file = files[0];
                    }
                } else {
                    JFileChooser fileChooser = new JFileChooser();
                    if (fileChooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
                        file = fileChooser.getSelectedFile();
                    }
                }
                if (file != null) {
                    playbin.stop();
                    playbin.setURI(file.toURI());
                    playbin.play();
                }

            });


            playbin.stop();
            playbin.setURI(URI.create(
                    "rtsp://admin:adminadmin1234@192.168.1.247:554/h265Preview_01_main"
            ));

            playbin.play();


            // playback controls
            JButton playButton = new JButton("Play");
            playButton.addActionListener(e -> playbin.play());
            JButton pauseButton = new JButton("Pause");
            pauseButton.addActionListener(e -> playbin.pause());
            JToggleButton loopButton = new JToggleButton("Loop", true);

            // position slider
            JSlider position = new JSlider(0, 1000, 0);
            position.addChangeListener(e -> {
                if (position.getValueIsAdjusting()) {
                    long dur = playbin.queryDuration(Format.TIME);
                    if (dur > 0) {
                        double relPos = position.getValue() / 1000.0;
                        playbin.seekSimple(Format.TIME,
                                EnumSet.of(SeekFlags.FLUSH),
                                (long) (relPos * dur));
                    }
                }
            });
            // sync slider position to video when not dragging
            new Timer(50, e -> {
                if (!position.getValueIsAdjusting()) {
                    long dur = playbin.queryDuration(Format.TIME);
                    long pos = playbin.queryPosition(Format.TIME);
                    if (dur > 0) {
                        double relPos = (double) pos / dur;
                        position.setValue((int) (relPos * 1000));
                    }
                }
            }).start();

            // quick and dirty level display using JProgressBar
            Box levels = Box.createVerticalBox();
            JProgressBar leftLevel = new JProgressBar();
            leftLevel.setMaximumSize(new Dimension(200, 20));
            JProgressBar rightLevel = new JProgressBar();
            rightLevel.setMaximumSize(new Dimension(200, 20));
            levels.add(leftLevel);
            levels.add(rightLevel);

            buttons.add(fileButton);
            buttons.addSeparator();
            buttons.add(playButton);
            buttons.add(pauseButton);
            buttons.addSeparator();
            buttons.add(position);
            buttons.add(loopButton);
            buttons.addSeparator();
            buttons.add(levels);
            window.add(buttons, BorderLayout.SOUTH);

            window.pack();
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            playbin.getBus().connect((Bus.EOS) source -> {
                // handle on Swing thread!
                EventQueue.invokeLater(() -> {
                    if (loopButton.isSelected()) {
                        playbin.seekSimple(Format.TIME,
                                EnumSet.of(SeekFlags.FLUSH),
                                0);
                    } else {
                        playbin.stop();
                        position.setValue(0);
                    }
                });
            });

            // listen for level messages on the bus
            playbin.getBus().connect("element", new Bus.MESSAGE() {

                @Override
                public void busMessage(Bus arg0, Message message) {
                    if (message.getSource() == level) {
                        Structure struct = message.getStructure();
                        // We can get either rms or peak
                        double[] levels = struct.getDoubles("peak");
                        // Calculate the time offset required to get the level
                        // information in sync with the video display
                        long timeDelay = getTimeOffset(struct);
                        Gst.getExecutor().schedule(
                                () -> EventQueue.invokeLater(() -> updateLevelDisplay(levels)),
                                timeDelay, TimeUnit.NANOSECONDS);
                    }
                }

                private long getTimeOffset(Structure struct) {
                    long actualTime = playbin.getClock().getTime()
                            - playbin.getBaseTime();
                    long runningTime = (long) struct.getValue("running-time");
                    long duration = (long) struct.getValue("duration");
                    long messageTime = runningTime + (duration / 2);
                    return messageTime - actualTime;
                }

                private void updateLevelDisplay(double[] levels) {
                    if (playbin.isPlaying() && levels.length > 0) {
                        // convert levels for display
                        for (int i = 0; i < levels.length; i++) {
                            levels[i] = Math.pow(10, levels[i] / 20);
                        }
                        if (levels.length >= 2) {
                            leftLevel.setValue((int) Math.max(0, Math.min(levels[0] * 100, 100)));
                            rightLevel.setValue((int) Math.max(0, Math.min(levels[1] * 100, 100)));
                        } else {
                            leftLevel.setValue((int) Math.max(0, Math.min(levels[0] * 100, 100)));
                            rightLevel.setValue((int) Math.max(0, Math.min(levels[0] * 100, 100)));
                        }
                    } else {
                        leftLevel.setValue(0);
                        rightLevel.setValue(0);
                    }
                }
            });

            window.setVisible(true);
        });
    }
}
