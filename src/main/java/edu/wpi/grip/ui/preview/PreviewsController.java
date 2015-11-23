package edu.wpi.grip.ui.preview;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import edu.wpi.grip.core.OutputSocket;
import edu.wpi.grip.core.events.SocketPreviewChangedEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple JavaFX container that automatically shows previews of all sockets marked as "previewed".
 *
 * @see OutputSocket#isPreviewed()
 */
public class PreviewsController {

    @FXML
    private HBox previewBox;

    @Inject
    private EventBus eventBus;

    private final List<OutputSocket<?>> previewedSockets = new ArrayList<>();

    public void initialize() {
        this.eventBus.register(this);
    }

    @Subscribe
    public synchronized void onSocketPreviewChanged(SocketPreviewChangedEvent event) {
        Platform.runLater(() -> {
            final OutputSocket<?> socket = event.getSocket();

            if (socket.isPreviewed()) {
                // If the socket was just set as previewed, add it to the list of previewed sockets and add a new view for it.
                if (!this.previewedSockets.contains(socket)) {
                    this.previewedSockets.add(socket);
                    this.previewBox.getChildren().add(SocketPreviewViewFactory.createPreviewView(this.eventBus, socket));
                }
            } else {
                // If the socket was just set as not previewed, remove both it and the corresponding control
                int index = this.previewedSockets.indexOf(socket);
                if (index != -1) {
                    this.previewedSockets.remove(index);
                    this.eventBus.unregister(this.previewBox.getChildren().remove(index));
                }
            }
        });
    }
}
