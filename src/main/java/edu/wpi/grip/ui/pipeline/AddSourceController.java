package edu.wpi.grip.ui.pipeline;

import com.google.common.eventbus.EventBus;
import edu.wpi.grip.core.events.FatalErrorEvent;
import edu.wpi.grip.core.events.SourceAddedEvent;
import edu.wpi.grip.core.sources.CameraSource;
import edu.wpi.grip.core.sources.ImageFileSource;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.function.Predicate;

/**
 * A box of buttons that let the user add different kinds of {@link edu.wpi.grip.core.Source Source}s.  Depending on
 * which button is pressed, a different dialog is presented for the user to construct that source.  As an example,
 * the image file source results in a file picker that the user can use to browse for an image.
 */
public class AddSourceController {

    @FXML
    private HBox root;

    @Inject
    private EventBus eventBus;

    /**
     * Show a file picker so the user can open one or more images from disk
     */
    @FXML
    public void addImage() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open an image");

        final List<File> imageFiles = fileChooser.showOpenMultipleDialog(this.root.getScene().getWindow());

        if (imageFiles == null) return;

        // Add a new source for each image .
        imageFiles.forEach(file -> {
            try {
                eventBus.post(new SourceAddedEvent(new ImageFileSource(eventBus, file)));
            } catch (IOException e) {
                e.printStackTrace();
                eventBus.post(new FatalErrorEvent(e));
            }
        });
    }

    /**
     * Show a dialog for the user to pick a device number, then add a new source for the webcam with that number
     */
    @FXML
    public void addWebcam() {
        final Parent root = this.root.getScene().getRoot();

        final Dialog<ButtonType> dialog = new Dialog<>();
        final Spinner<Integer> cameraIndex = new Spinner<Integer>(0, Integer.MAX_VALUE, 0);

        dialog.setTitle("Add Webcam");
        dialog.setHeaderText("Choose a camera");
        dialog.setContentText("index");
        dialog.getDialogPane().setContent(cameraIndex);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.getDialogPane().setStyle(root.getStyle());
        dialog.getDialogPane().getStylesheets().addAll(root.getStylesheets());

        // If the user clicks OK, add a new camera source
        dialog.showAndWait().filter(Predicate.isEqual(ButtonType.OK)).ifPresent(result -> {
            try {
                final CameraSource source = new CameraSource(eventBus, cameraIndex.getValue());
                eventBus.post(new SourceAddedEvent(source));
            } catch (IOException e) {
                eventBus.post(new FatalErrorEvent(e));
                e.printStackTrace();
            }
        });
    }

    /**
     * Show a dialog that promps for a URL, then open a new source for a network stream at that URL
     */
    @FXML
    public void addIPCamera() {
        final Parent root = this.root.getScene().getRoot();

        // Show a dialog for the user to pick a camera URL
        final Dialog<ButtonType> dialog = new Dialog<>();

        final TextField cameraAddress = new TextField();
        cameraAddress.setPromptText("Ex: http://10.1.90.11/mjpg/video.mjpg");
        cameraAddress.textProperty().addListener(observable -> {
            boolean validURL = true;

            try {
                new URL(cameraAddress.getText()).toURI();
            } catch (MalformedURLException | URISyntaxException e) {
                validURL = false;
            }

            // Enable the "OK" button only if the user has entered a valid URL
            dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(!validURL);
        });

        dialog.setTitle("Add IP Camera");
        dialog.setHeaderText("Enter the IP camera URL");
        dialog.setContentText("URL");
        dialog.getDialogPane().setContent(cameraAddress);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
        dialog.getDialogPane().setStyle(root.getStyle());
        dialog.getDialogPane().getStylesheets().addAll(root.getStylesheets());

        // If the user clicks OK, add a new camera source
        dialog.showAndWait().filter(Predicate.isEqual(ButtonType.OK)).ifPresent(result -> {
            try {
                final CameraSource source = new CameraSource(eventBus, cameraAddress.getText());
                eventBus.post(new SourceAddedEvent(source));
            } catch (IOException e) {
                eventBus.post(new FatalErrorEvent(e));
                e.printStackTrace();
            }
        });
    }
}
