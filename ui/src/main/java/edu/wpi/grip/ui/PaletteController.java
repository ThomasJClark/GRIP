package edu.wpi.grip.ui;

import com.google.common.eventbus.EventBus;
import edu.wpi.grip.core.Operation;
import edu.wpi.grip.core.Palette;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Controller for a list of the available operations that the user may select from
 */
@Singleton
public class PaletteController {

    @FXML private VBox root;
    @FXML private CustomTextField operationSearch;
    @FXML private Tab allOperations;
    @FXML private Tab imgprocOperations;
    @FXML private Tab featureOperations;
    @FXML private Tab networkOperations;
    @FXML private Tab opencvOperations;
    @FXML private Tab miscellaneousOperations;

    @Inject private EventBus eventBus;
    @Inject private Palette palette;

    @FXML
    public void initialize() {
        // Make the search box have a "clear" button. This is the only way to do this unfortunately.
        // https://bitbucket.org/controlsfx/controlsfx/issues/330/making-textfieldssetupclearbuttonfield
        try {
            final Method m = TextFields.class.getDeclaredMethod("setupClearButtonField", TextField.class, ObjectProperty.class);
            m.setAccessible(true);
            m.invoke(null, operationSearch, operationSearch.rightProperty());
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        imgprocOperations.setUserData(Operation.Category.IMAGE_PROCESSING);
        featureOperations.setUserData(Operation.Category.FEATURE_DETECTION);
        networkOperations.setUserData(Operation.Category.NETWORK);
        opencvOperations.setUserData(Operation.Category.OPENCV);
        miscellaneousOperations.setUserData(Operation.Category.MISCELLANEOUS);

        // Bind the filterText of all of the individual tabs to the search field
        operationSearch.textProperty().addListener(observable -> {
            allOperations.getProperties().put("filterText", operationSearch.getText());
            imgprocOperations.getProperties().put("filterText", operationSearch.getText());
            featureOperations.getProperties().put("filterText", operationSearch.getText());
            networkOperations.getProperties().put("filterText", operationSearch.getText());
            opencvOperations.getProperties().put("filterText", operationSearch.getText());
            miscellaneousOperations.getProperties().put("filterText", operationSearch.getText());
        });

        // The palette should have a lower priority for resizing than other elements
        root.getProperties().put("resizable-with-parent", false);
    }
}
