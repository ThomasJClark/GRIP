package edu.wpi.grip.ui.pipeline;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import edu.wpi.grip.core.*;
import edu.wpi.grip.core.events.*;
import edu.wpi.grip.ui.pipeline.input.InputSocketView;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import javax.inject.Inject;

/**
 * A JavaFX controller for the pipeline.  The view renders a list of sources, steps, and connections.
 */
public class PipelineController {

    @FXML
    private StackPane root;

    @FXML
    private VBox sources;

    @FXML
    private HBox steps;

    @FXML
    private Group connections;

    @Inject
    private EventBus eventBus;

    @Inject
    private Pipeline pipeline;

    /**
     * Add an initial view for any source or step already in the pipeline
     */
    public void initialize() {
        this.pipeline.getSources().forEach(source ->
                this.sources.getChildren().addAll(new SourceView(this.eventBus, source)));

        this.pipeline.getSteps().forEach(step ->
                this.steps.getChildren().addAll(new StepView(this.eventBus, step)));

        connections.getChildren().add(new Rectangle(0, 0, 1, 1));

        this.eventBus.register(this);
    }

    public Pipeline getPipeline() {
        return this.pipeline;
    }

    /**
     * @return An unmodifiable list of {@link SourceView} corresponding to all of the sources in the pipeline
     */
    @SuppressWarnings("unchecked")
    public ObservableList<SourceView> getSources() {
        return (ObservableList) this.sources.getChildrenUnmodifiable();
    }

    /**
     * @return An unmodifiable list of {@link StepView}s corresponding to all of the steps in the pipeline
     */
    @SuppressWarnings("unchecked")
    public ObservableList<StepView> getSteps() {
        return (ObservableList) this.steps.getChildrenUnmodifiable();
    }

    /**
     * @return An unmodifiable list of the {@link ConnectionView}s corresponding to all of the connections in the
     * pipeline
     */
    @SuppressWarnings("unchecked")
    public ObservableList<ConnectionView> getConnections() {
        return (ObservableList) this.connections.getChildrenUnmodifiable().filtered(node -> node instanceof ConnectionView);
    }

    /**
     * @return The {@link InputSocketView} that corresponds with the given socket
     */
    private InputSocketView findInputSocketView(InputSocket socket) {
        for (StepView stepView : this.getSteps()) {
            for (InputSocketView socketView : stepView.getInputSockets()) {
                if (socketView.getSocket() == socket) {
                    return socketView;
                }
            }
        }

        throw new IllegalArgumentException("input socket view does not exist in pipeline: " + socket);
    }

    /**
     * @return The {@link OutputSocketView} that corresponds with the given socket, either from one of the steps in
     * the pipeline or from a source
     */
    private OutputSocketView findOutputSocketView(OutputSocket socket) {
        for (StepView stepView : this.getSteps()) {
            for (OutputSocketView socketView : stepView.getOutputSockets()) {
                if (socketView.getSocket() == socket) {
                    return socketView;
                }
            }
        }

        for (SourceView sourceView : this.getSources()) {
            for (OutputSocketView socketView : sourceView.getOutputSockets()) {
                if (socketView.getSocket() == socket) {
                    return socketView;
                }
            }
        }

        throw new IllegalArgumentException("output socket view does not exist in pipeline: " + socket);
    }

    /**
     * @return The {@link SourceView} that corresponds with the given source
     */

    private SourceView findSourceView(Source source) {
        for (SourceView sourceView : this.getSources()) {
            if (sourceView.getSource() == source) {
                return sourceView;
            }
        }

        throw new IllegalArgumentException("source view does not exist in pipeline: " + source);
    }

    /**
     * @return The {@link StepView} that corresponds with the given step
     */
    private StepView findStepView(Step step) {
        for (StepView stepView : this.getSteps()) {
            if (stepView.getStep() == step) {
                return stepView;
            }
        }

        throw new IllegalArgumentException("step view does not exist in pipeline: " + step);
    }

    /**
     * @return The {@link ConnectionView} that corresponds with the given connection
     */
    private ConnectionView findConnectionView(Connection connection) {
        for (ConnectionView connectionView : this.getConnections()) {
            if (connectionView.getConnection() == connection) {
                return connectionView;
            }
        }

        throw new IllegalArgumentException("connection view does not exist in pipeline: " + connection);
    }

    /**
     * Add a view for the given connection to the pipeline view.  This method figures out the positioning and other
     * details of adding the connection.
     */
    private void addConnectionView(Connection connection) {
        Platform.runLater(() -> {
            // Before adding a connection control, we have to look up the controls for both sockets in the connection so
            // we know where to position it.
            final OutputSocketView outputSocketView = findOutputSocketView(connection.getOutputSocket());
            final InputSocketView inputSocketView = findInputSocketView(connection.getInputSocket());

            final ConnectionView connectionView = new ConnectionView(this.eventBus, connection);

            // Re-position the start and end points of the connection whenever one of the handles is moved.  This
            // happens, for example, when a step in the pipeline is deleted, so all of the steps after it shift left.
            final InvalidationListener handleListener = observable -> {
                synchronized (this) {
                    final Node outputHandle = outputSocketView.getHandle();
                    final Node inputHandle = inputSocketView.getHandle();
                    final Bounds outputSocketBounds = this.root.sceneToLocal(outputHandle.localToScene(
                            outputHandle.getLayoutBounds()));
                    final Bounds inputSocketBounds = this.root.sceneToLocal(inputHandle.localToScene(
                            inputHandle.getLayoutBounds()));
                    final double x1 = outputSocketBounds.getMinX() + outputSocketBounds.getWidth() / 2.0;
                    final double y1 = outputSocketBounds.getMinY() + outputSocketBounds.getHeight() / 2.0;
                    final double x2 = inputSocketBounds.getMinX() + inputSocketBounds.getWidth() / 2.0;
                    final double y2 = inputSocketBounds.getMinY() + inputSocketBounds.getHeight() / 2.0;

                    Platform.runLater(() -> {
                        connectionView.inputHandleProperty().setValue(new Point2D(x1, y1));
                        connectionView.outputHandleProperty().setValue(new Point2D(x2, y2));
                        ((ReadOnlyObjectProperty) observable).get();
                    });
                }
            };

            inputSocketView.localToSceneTransformProperty().addListener(handleListener);
            outputSocketView.localToSceneTransformProperty().addListener(handleListener);
            handleListener.invalidated(inputSocketView.localToSceneTransformProperty());

            this.connections.getChildren().add(connectionView);
        });
    }

    @Subscribe
    public void onSourceAdded(SourceAddedEvent event) {
        Platform.runLater(() -> this.sources.getChildren().add(new SourceView(this.eventBus, event.getSource())));
    }

    @Subscribe
    public void onSourceRemoved(SourceRemovedEvent event) {
        Platform.runLater(() -> this.sources.getChildren().remove(findSourceView(event.getSource())));
    }

    @Subscribe
    public void onStepAdded(StepAddedEvent event) {
        // Add a new control to the pipelineview for the step that was added
        Platform.runLater(() -> {
            int index = event.getIndex().or(this.steps.getChildren().size());
            this.steps.getChildren().add(index, new StepView(this.eventBus, event.getStep()));
        });
    }

    @Subscribe
    public void onStepRemoved(StepRemovedEvent event) {
        // Remove the control that corresponds with the step that was removed
        Platform.runLater(() -> {
            final StepView stepView = findStepView(event.getStep());
            this.steps.getChildren().remove(stepView);
            this.eventBus.unregister(stepView);
        });
    }

    @Subscribe
    public void onStepMoved(StepMovedEvent event) {
        Platform.runLater(() -> {
            final StepView stepView = findStepView(event.getStep());

            final int oldIndex = this.getSteps().indexOf(stepView);
            final int newIndex = Math.min(Math.max(oldIndex + event.getDistance(), 0), this.getSteps().size() - 1);

            if (newIndex != oldIndex) {
                this.steps.getChildren().remove(oldIndex);
                this.steps.getChildren().add(newIndex, stepView);
            }
        });
    }

    @Subscribe
    public void onConnectionAdded(ConnectionAddedEvent event) {
        // Add the new connection view
        Platform.runLater(() -> this.addConnectionView(event.getConnection()));
    }

    @Subscribe
    public void onConnectionRemoved(ConnectionRemovedEvent event) {
        // Remove the control that corresponds with the connection that was removed
        Platform.runLater(() -> {
            final ConnectionView connectionView = findConnectionView(event.getConnection());
            this.connections.getChildren().remove(connectionView);
            this.eventBus.unregister(connectionView);
        });
    }
}

