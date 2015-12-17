package edu.wpi.grip.ui.preview;

import com.google.common.eventbus.Subscribe;
import edu.wpi.grip.core.OutputSocket;
import edu.wpi.grip.core.events.SocketChangedEvent;
import edu.wpi.grip.ui.util.GRIPPlatform;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.bytedeco.javacpp.IntPointer;

import static org.bytedeco.javacpp.opencv_core.Point;
import static org.bytedeco.javacpp.opencv_core.Size;

/**
 * A {@link SocketPreviewView} for OpenCV points and sizes
 */
public class PointSizeSocketPreviewView extends SocketPreviewView<IntPointer> {

    private final TextField x, y;

    /**
     * @param socket   An output socket to preview
     */
    PointSizeSocketPreviewView(OutputSocket<IntPointer> socket) {
        super(socket);

        x = new TextField();
        x.setEditable(false);
        y = new TextField();
        y.setEditable(false);

        final GridPane gridPane = new GridPane();
        gridPane.add(x, 1, 0);
        gridPane.add(y, 1, 1);

        // The only difference between point and size previews is the labels
        if (socket.getSocketHint().getType().equals(Point.class)) {
            gridPane.add(new Label("x: "), 0, 0);
            gridPane.add(new Label("y: "), 0, 1);
        } else if (socket.getSocketHint().getType().equals(Size.class)) {
            gridPane.add(new Label("width: "), 0, 0);
            gridPane.add(new Label("height: "), 0, 1);
        }
        this.setContent(gridPane);

        assert Platform.isFxApplicationThread() : "Must be in FX Thread to create this or you will be exposing constructor to another thread!";
        updateTextFields();
    }

    @Subscribe
    public void onSocketChanged(SocketChangedEvent event) {
        if (event.getSocket() == this.getSocket()) {
            this.updateTextFields();
        }
    }

    private void updateTextFields() {
        GRIPPlatform.runAndWait(() -> {
            this.x.setText(Integer.toString(this.getSocket().getValue().get().get(0)));
            this.y.setText(Integer.toString(this.getSocket().getValue().get().get(1)));
        });
    }
}
