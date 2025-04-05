package ch.sebpiller.babyphone.ui;

import ch.sebpiller.babyphone.ui.config.BabyphoneConfiguration;
import ch.sebpiller.babyphone.ui.swing.MainController;
import ch.sebpiller.babyphone.ui.swing.MainFrame;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import javax.swing.*;


@Import(value = {BabyphoneConfiguration.class, MainController.class})
@ComponentScan(basePackageClasses = BabyPhoneSwingUI.class)
public class BabyPhoneSwingUI {

    static {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                     UnsupportedLookAndFeelException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    public static void main(String[] args) {
        var context = new SpringApplicationBuilder(BabyPhoneSwingUI.class).headless(false).run(args);
        var appFrame = context.getBean(MainFrame.class);
        var ctrl = context.getBean(MainController.class);

        ctrl.setDetecteds(appFrame::reannotateImage);
        ctrl.setFps(appFrame::setFps);
        SwingUtilities.invokeLater(() -> appFrame.setVisible(true));
    }

}