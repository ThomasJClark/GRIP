package edu.wpi.grip.ui;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Guice;
import com.google.inject.Injector;
import edu.wpi.grip.core.GRIPCoreModule;
import edu.wpi.grip.core.events.FatalErrorEvent;
import edu.wpi.grip.ui.util.DPIUtility;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
//    private final EventBus eventBus = new EventBus((exception, context) -> {
//        this.onFatalErrorEvent(new FatalErrorEvent(exception));
//    });

    private final Injector injector = Guice.createInjector(new GRIPCoreModule());

    private final Object dialogLock = new Object();
    private Parent root;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
//        this.eventBus.register(this);
//        this.root = new MainWindowController(eventBus);

        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MainWindow.fxml"));
        fxmlLoader.setControllerFactory(injector::getInstance);

        try {
            this.root = fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        /**
         * Any exceptions thrown by the UI will be caught here and an exception dialog will be displayed
         */
//        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
//            this.eventBus.post(new FatalErrorEvent(throwable));
//        });

        root.setStyle("-fx-font-size: " + DPIUtility.FONT_SIZE + "px");

        stage.setTitle("GRIP Computer Vision Engine");
        stage.getIcons().add(new Image("/edu/wpi/grip/ui/icons/grip.png"));
        stage.setScene(new Scene(root));
        stage.show();
    }

    @Subscribe
    public final void onFatalErrorEvent(FatalErrorEvent error) {
        // Print throwable before showing the exception so that errors are in order in the console
        error.getThrowable().printStackTrace();
        Platform.runLater(() -> {
            synchronized (this.dialogLock) {
                try {
                    // Don't create more than one exception dialog at the same time
                    final ExceptionAlert exceptionAlert = new ExceptionAlert(root, error.getThrowable(), getHostServices());
                    exceptionAlert.showAndWait();
                } catch (RuntimeException e) {
                    // Well in this case something has gone very, very wrong
                    // We don't want to create a feedback loop either.
                    e.printStackTrace();
                    assert false : "Could not rethrow exception.";
                    Platform.exit();
                }
            }
        });
    }
}
