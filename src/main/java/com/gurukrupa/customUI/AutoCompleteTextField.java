package com.gurukrupa.customUI;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import javafx.util.StringConverter;
import lombok.Getter;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Professional Enhanced JavaFX TextField with autocomplete suggestion popup.
 * Supports both string suggestions and complex object suggestions with custom display.
 * Features Material Design color scheme for a professional look and feel.
 *
 * @param <T> The type of items in the autocomplete suggestions
 */
public class AutoCompleteTextField<T> {

    // ========== Material Design Professional Color Palette ==========
    // Primary Colors
    private static final String PRIMARY_COLOR = "#1976D2";        // Material Blue 700
    private static final String PRIMARY_LIGHT = "#42A5F5";        // Material Blue 400
    private static final String PRIMARY_DARK = "#0D47A1";         // Material Blue 900
    private static final String ACCENT_COLOR = "#1E88E5";         // Material Blue 600

    // Background Colors
    private static final String BACKGROUND_DEFAULT = "#FAFAFA";   // Material Grey 50
    private static final String BACKGROUND_FOCUS = "#FFFFFF";     // White
    private static final String BACKGROUND_HOVER = "#E3F2FD";     // Material Blue 50
    private static final String BACKGROUND_HOVER_LIGHT = "#F5F5F5"; // Material Grey 100
    private static final String BACKGROUND_SELECTED = "#1976D2";  // Material Blue 700

    // Border Colors
    private static final String BORDER_DEFAULT = "#BDBDBD";       // Material Grey 400
    private static final String BORDER_FOCUS = "#1976D2";         // Material Blue 700
    private static final String BORDER_HOVER = "#90CAF9";         // Material Blue 200
    private static final String BORDER_ERROR = "#D32F2F";         // Material Red 700

    // Text Colors
    private static final String TEXT_PRIMARY = "#212121";         // Material Grey 900
    private static final String TEXT_SECONDARY = "#757575";       // Material Grey 600
    private static final String TEXT_HINT = "#9E9E9E";            // Material Grey 500
    private static final String TEXT_ON_PRIMARY = "#FFFFFF";      // White
    private static final String TEXT_DISABLED = "#BDBDBD";        // Material Grey 400

    // Icon Colors
    private static final String ICON_DEFAULT = "#757575";         // Material Grey 600
    private static final String ICON_HOVER = "#424242";           // Material Grey 800
    private static final String ICON_ACTIVE = "#1976D2";          // Material Blue 700

    // Shadow Effects
    private static final String SHADOW_DEFAULT = "rgba(0, 0, 0, 0.12)";
    private static final String SHADOW_FOCUS = "rgba(25, 118, 210, 0.2)";
    private static final String SHADOW_POPUP = "rgba(0, 0, 0, 0.16)";

    @Getter
    private final TextField textField;
    private final ContextMenu suggestionsPopup;
    private List<T> suggestions = new ArrayList<>();
    private List<T> filteredSuggestions = new ArrayList<>();
    private int selectedIndex = 0;
    private boolean isSelectingSuggestion = false;

    // Properties for complex object handling
    private StringConverter<T> converter;
    private Function<T, String> displayFunction;
    private Function<String, List<T>> filterFunction;
    private Callback<T, Label> cellFactory;

    // Selected value property
    private final ObjectProperty<T> selectedItem = new SimpleObjectProperty<>();

    // Visual enhancements
    private FontAwesomeIcon searchIcon;
    private FontAwesomeIcon clearIcon;
    private Button clearButton;
    private HBox container;
    private HBox iconContainer;
    private HBox wrapper;

    // Styling
    private String textFieldStyle =
            "-fx-background-radius: 10px; " +
            "-fx-border-radius: 10px; " +
            "-fx-border-color: " + BORDER_DEFAULT + "; " +
            "-fx-padding: 10px 14px 10px 14px; " +
            "-fx-background-color: " + BACKGROUND_DEFAULT + "; " +
            "-fx-font-family: 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif; " +
            "-fx-font-size: 14px; " +
            "-fx-text-fill: " + TEXT_PRIMARY + "; " +
            "-fx-prompt-text-fill: " + TEXT_HINT + ";";

    private String focusedStyle =
            "-fx-background-radius: 10px; " +
            "-fx-border-radius: 10px; " +
            "-fx-border-color: " + BORDER_FOCUS + "; " +
            "-fx-border-width: 2px; " +
            "-fx-padding: 9px 13px 9px 13px; " +
            "-fx-background-color: " + BACKGROUND_FOCUS + "; " +
            "-fx-font-family: 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif; " +
            "-fx-font-size: 14px; " +
            "-fx-text-fill: " + TEXT_PRIMARY + "; " +
            "-fx-prompt-text-fill: " + TEXT_HINT + ";";

    // Reference to text listener for removal/re-adding
    private ChangeListener<String> textListener;

    /**
     * Constructor for string-based suggestions
     */
    public AutoCompleteTextField(List<String> suggestions) {
        this.textField = new TextField();
        this.suggestionsPopup = new ContextMenu();
        this.suggestionsPopup.setAutoHide(true);
        this.suggestions = new ArrayList<>((List<T>) suggestions);
        this.converter = new StringConverter<T>() {
            @Override
            public String toString(T object) {
                return object != null ? object.toString() : "";
            }
            @Override
            public T fromString(String string) {
                return (T) string;
            }
        };
        this.displayFunction = obj -> obj != null ? obj.toString() : "";
        this.filterFunction = searchText -> this.suggestions.stream()
                .filter(item -> displayFunction.apply(item).toLowerCase().contains(searchText.toLowerCase()))
                .collect(Collectors.toList());

        setupUI();
        styleComponents();
        attachListeners();
    }

    /**
     * Constructor for complex object suggestions
     */
    public AutoCompleteTextField(List<T> suggestions, StringConverter<T> converter,
                                Function<String, List<T>> filterFunction) {
        this.textField = new TextField();
        this.suggestionsPopup = new ContextMenu();
        this.suggestionsPopup.setAutoHide(true);
        this.suggestions = new ArrayList<>(suggestions);
        this.converter = converter;
        this.displayFunction = obj -> converter.toString(obj);
        this.filterFunction = filterFunction != null ? filterFunction :
            searchText -> this.suggestions.stream()
                .filter(item -> displayFunction.apply(item).toLowerCase().contains(searchText.toLowerCase()))
                .collect(Collectors.toList());

        setupUI();
        styleComponents();
        attachListeners();
    }

    /**
     * Default constructor
     */
    public AutoCompleteTextField() {
        this(new ArrayList<String>());
    }

    private void setupUI() {
        // Create container
        container = new HBox();
        container.setAlignment(Pos.CENTER_LEFT);
        container.setSpacing(8);
        container.setStyle("-fx-background-color: transparent;");

        // Search icon in a separate container
        iconContainer = new HBox();
        iconContainer.setAlignment(Pos.CENTER);
        iconContainer.setPrefWidth(32);
        iconContainer.setMinWidth(32);
        iconContainer.setMaxWidth(32);

        searchIcon = new FontAwesomeIcon();
        searchIcon.setGlyphName("SEARCH");
        searchIcon.setSize("15");
        searchIcon.setFill(Color.web(ICON_DEFAULT));
        iconContainer.getChildren().add(searchIcon);

        // Clear button with icon
        clearIcon = new FontAwesomeIcon();
        clearIcon.setGlyphName("TIMES_CIRCLE");
        clearIcon.setSize("16");
        clearIcon.setFill(Color.web(ICON_DEFAULT));

        clearButton = new Button();
        clearButton.setGraphic(clearIcon);
        clearButton.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 0; " +
            "-fx-min-width: 32; " +
            "-fx-pref-width: 32; " +
            "-fx-max-width: 32; " +
            "-fx-background-radius: 16;"
        );
        clearButton.setVisible(false);
        clearButton.setOnAction(e -> clear());

        // Setup text field with proper padding
        HBox.setHgrow(textField, Priority.ALWAYS);
        textField.setMaxWidth(Double.MAX_VALUE);

        // Add hover effect to clear button with smooth transition
        clearButton.setOnMouseEntered(e -> {
            clearIcon.setFill(Color.web(ICON_HOVER));
            clearButton.setStyle(
                clearButton.getStyle() +
                "-fx-background-color: " + BACKGROUND_HOVER_LIGHT + ";"
            );
        });
        clearButton.setOnMouseExited(e -> {
            clearIcon.setFill(Color.web(ICON_DEFAULT));
            clearButton.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0; " +
                "-fx-min-width: 32; " +
                "-fx-pref-width: 32; " +
                "-fx-max-width: 32; " +
                "-fx-background-radius: 16;"
            );
        });
    }

    public void setSuggestions(List<T> suggestions) {
        this.suggestions = new ArrayList<>(suggestions);
    }

    public void setPromptText(String promptText) {
        textField.setPromptText(promptText);
    }

    public void setCellFactory(Callback<T, Label> cellFactory) {
        this.cellFactory = cellFactory;
    }

    public ObjectProperty<T> selectedItemProperty() {
        return selectedItem;
    }

    public T getSelectedItem() {
        return selectedItem.get();
    }

    public void setSelectedItem(T item) {
        selectedItem.set(item);
        if (item != null) {
            textField.setText(displayFunction.apply(item));
        } else {
            textField.clear();
        }
    }

    public void clear() {
        textField.clear();
        selectedItem.set(null);
        clearButton.setVisible(false);
        textField.requestFocus();
    }

    /**
     * Get the container that includes the text field and icons
     */
    public HBox getNode() {
        if (container.getChildren().isEmpty()) {
            // Create wrapper for visual consistency
            wrapper = new HBox();
            wrapper.setAlignment(Pos.CENTER_LEFT);
            wrapper.setStyle(
                "-fx-background-color: " + BACKGROUND_DEFAULT + "; " +
                "-fx-background-radius: 10px; " +
                "-fx-border-radius: 10px; " +
                "-fx-border-color: " + BORDER_DEFAULT + "; " +
                "-fx-border-width: 1px; " +
                "-fx-padding: 2px 8px 2px 8px; " +
                "-fx-effect: dropshadow(gaussian, " + SHADOW_DEFAULT + ", 2, 0.0, 0, 1);"
            );

            // Make wrapper expand to fill available space
            wrapper.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(wrapper, Priority.ALWAYS);

            // Update text field style to be transparent
            textField.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-border-color: transparent; " +
                "-fx-padding: 10px 0px 10px 0px; " +
                "-fx-font-family: 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif; " +
                "-fx-font-size: 14px; " +
                "-fx-text-fill: " + TEXT_PRIMARY + "; " +
                "-fx-prompt-text-fill: " + TEXT_HINT + ";"
            );

            // Make container expand to fill wrapper
            container.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(container, Priority.ALWAYS);

            container.getChildren().addAll(iconContainer, textField, clearButton);
            wrapper.getChildren().add(container);

            // Add focus listener to wrapper for border color change with smooth transitions
            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (isNowFocused) {
                    wrapper.setStyle(
                        "-fx-background-color: " + BACKGROUND_FOCUS + "; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-border-radius: 10px; " +
                        "-fx-border-color: " + BORDER_FOCUS + "; " +
                        "-fx-border-width: 2px; " +
                        "-fx-padding: 1px 7px 1px 7px; " +
                        "-fx-effect: dropshadow(gaussian, " + SHADOW_FOCUS + ", 8, 0.0, 0, 2);"
                    );
                } else {
                    wrapper.setStyle(
                        "-fx-background-color: " + BACKGROUND_DEFAULT + "; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-border-radius: 10px; " +
                        "-fx-border-color: " + BORDER_DEFAULT + "; " +
                        "-fx-border-width: 1px; " +
                        "-fx-padding: 2px 8px 2px 8px; " +
                        "-fx-effect: dropshadow(gaussian, " + SHADOW_DEFAULT + ", 2, 0.0, 0, 1);"
                    );
                }
            });

            return wrapper;
        }
        return container;
    }

    private void attachListeners() {
        textListener = (obs, oldText, newText) -> {
            if (isSelectingSuggestion) {
                return;
            }

            // Show/hide clear button
            clearButton.setVisible(newText != null && !newText.isEmpty());

            // Clear selected item if text is manually changed
            if (selectedItem.get() != null && !displayFunction.apply(selectedItem.get()).equals(newText)) {
                selectedItem.set(null);
            }

            if (newText == null || newText.isEmpty()) {
                suggestionsPopup.hide();
                selectedIndex = 0;
            } else {
                filteredSuggestions = filterFunction.apply(newText);

                if (filteredSuggestions.isEmpty()) {
                    suggestionsPopup.hide();
                    selectedIndex = 0;
                } else {
                    selectedIndex = 0;
                    populatePopup();
                    if (!suggestionsPopup.isShowing()) {
                        suggestionsPopup.show(textField,
                                textField.localToScreen(textField.getBoundsInLocal()).getMinX(),
                                textField.localToScreen(textField.getBoundsInLocal()).getMaxY() + 4);
                    }
                }
            }
        };

        textField.textProperty().addListener(textListener);

        textField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                suggestionsPopup.hide();
                selectedIndex = 0;
            } else if (event.getCode() == KeyCode.ENTER) {
                if (!filteredSuggestions.isEmpty() && selectedIndex >= 0 && selectedIndex < filteredSuggestions.size()) {
                    selectSuggestion(selectedIndex);
                    event.consume();
                }
            } else if (event.getCode() == KeyCode.DOWN) {
                if (!suggestionsPopup.isShowing() && !filteredSuggestions.isEmpty()) {
                    // Show popup if not showing
                    populatePopup();
                    suggestionsPopup.show(textField,
                            textField.localToScreen(textField.getBoundsInLocal()).getMinX(),
                            textField.localToScreen(textField.getBoundsInLocal()).getMaxY() + 4);
                } else if (suggestionsPopup.isShowing() && selectedIndex < filteredSuggestions.size() - 1) {
                    selectedIndex++;
                    updatePopupHighlight();
                }
                event.consume();
            } else if (event.getCode() == KeyCode.UP) {
                if (suggestionsPopup.isShowing() && selectedIndex > 0) {
                    selectedIndex--;
                    updatePopupHighlight();
                }
                event.consume();
            } else if (event.getCode() == KeyCode.TAB) {
                if (!filteredSuggestions.isEmpty() && selectedIndex >= 0 && selectedIndex < filteredSuggestions.size()) {
                    selectSuggestion(selectedIndex);
                }
            }
        });

        // Focus handling with icon color changes
        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                searchIcon.setFill(Color.web(ICON_ACTIVE));

                // Show suggestions if text exists
                if (!textField.getText().isEmpty()) {
                    textListener.changed(null, "", textField.getText());
                }
            } else {
                searchIcon.setFill(Color.web(ICON_DEFAULT));
                // Hide popup when focus is lost
                javafx.application.Platform.runLater(() -> suggestionsPopup.hide());
            }
        });
    }

    private void selectSuggestion(int index) {
        if (index < 0 || index >= filteredSuggestions.size()) {
            return;
        }

        isSelectingSuggestion = true;

        T suggestion = filteredSuggestions.get(index);
        selectedItem.set(suggestion);
        textField.setText(displayFunction.apply(suggestion));
        textField.positionCaret(textField.getText().length());

        suggestionsPopup.hide();
        selectedIndex = 0;

        isSelectingSuggestion = false;

        // Move focus to next control
        textField.getParent().requestFocus();
    }

    private void populatePopup() {
        List<CustomMenuItem> menuItems = new ArrayList<>();
        for (int i = 0; i < Math.min(filteredSuggestions.size(), 10); i++) { // Limit to 10 suggestions
            T suggestion = filteredSuggestions.get(i);
            Label entryLabel;

            if (cellFactory != null) {
                entryLabel = cellFactory.call(suggestion);
            } else {
                entryLabel = createDefaultLabel(suggestion, i == selectedIndex);
            }

            // Ensure proper styling for selection
            if (i == selectedIndex) {
                applyHighlightStyle(entryLabel);
            } else {
                applyNormalStyle(entryLabel);
            }

            int index = i;
            CustomMenuItem item = new CustomMenuItem(entryLabel, true);
            item.setOnAction(evt -> selectSuggestion(index));

            // Add hover effect
            entryLabel.setOnMouseEntered(e -> {
                selectedIndex = index;
                updatePopupHighlight();
            });

            menuItems.add(item);
        }

        suggestionsPopup.getItems().clear();
        suggestionsPopup.getItems().addAll(menuItems);

        // Adjust popup width to match text field (use wrapper width for consistency)
        double popupWidth = wrapper != null ? wrapper.getWidth() : textField.getWidth();
        suggestionsPopup.setMinWidth(popupWidth);
        suggestionsPopup.setPrefWidth(popupWidth);
        suggestionsPopup.setMaxWidth(popupWidth);
    }

    private void updatePopupHighlight() {
        for (int i = 0; i < suggestionsPopup.getItems().size(); i++) {
            CustomMenuItem item = (CustomMenuItem) suggestionsPopup.getItems().get(i);
            Label label = (Label) item.getContent();
            if (i == selectedIndex) {
                applyHighlightStyle(label);
            } else {
                applyNormalStyle(label);
            }
        }
    }

    private void applyHighlightStyle(Label label) {
        // Professional solid color highlight with perfect readability
        label.setStyle(
            "-fx-background-color: " + BACKGROUND_SELECTED + "; " +
            "-fx-background-radius: 8px; " +
            "-fx-padding: 14px 18px 14px 18px; " +
            "-fx-text-fill: " + TEXT_ON_PRIMARY + "; " +
            "-fx-font-weight: 600; " +
            "-fx-effect: dropshadow(gaussian, " + SHADOW_FOCUS + ", 4, 0.0, 0, 2);"
        );
        label.setTextFill(Color.web(TEXT_ON_PRIMARY));
        label.setTextAlignment(TextAlignment.LEFT);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setWrapText(true); // Enable text wrapping for long text
        label.setEllipsisString("..."); // Add ellipsis for very long text

        // Also update nested labels (for custom cell factories with VBox/HBox)
        if (label.getGraphic() != null && label.getGraphic() instanceof javafx.scene.layout.Pane) {
            updateNestedLabelsColor((javafx.scene.layout.Pane) label.getGraphic(), Color.web(TEXT_ON_PRIMARY));
        }
    }

    private void applyNormalStyle(Label label) {
        // Clean professional style with excellent readability
        label.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-background-radius: 8px; " +
            "-fx-padding: 14px 18px 14px 18px; " +
            "-fx-text-fill: " + TEXT_PRIMARY + "; " +
            "-fx-font-weight: 500;"
        );
        label.setTextFill(Color.web(TEXT_PRIMARY));
        label.setTextAlignment(TextAlignment.LEFT);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setWrapText(true); // Enable text wrapping for long text
        label.setEllipsisString("..."); // Add ellipsis for very long text

        // Update nested labels
        if (label.getGraphic() != null && label.getGraphic() instanceof javafx.scene.layout.Pane) {
            updateNestedLabelsColorWithSecondary((javafx.scene.layout.Pane) label.getGraphic());
        }

        // Add smooth hover effect
        label.setOnMouseEntered(e -> {
            if (!label.getStyle().contains(BACKGROUND_SELECTED)) {
                label.setStyle(
                    "-fx-background-color: " + BACKGROUND_HOVER + "; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-padding: 14px 18px 14px 18px; " +
                    "-fx-text-fill: " + PRIMARY_DARK + "; " +
                    "-fx-font-weight: 600;"
                );
                label.setTextFill(Color.web(PRIMARY_DARK));

                // Update nested labels on hover
                if (label.getGraphic() != null && label.getGraphic() instanceof javafx.scene.layout.Pane) {
                    updateNestedLabelsColor((javafx.scene.layout.Pane) label.getGraphic(), Color.web(PRIMARY_DARK));
                }
            }
        });

        label.setOnMouseExited(e -> {
            if (!label.getStyle().contains(BACKGROUND_SELECTED)) {
                label.setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-padding: 14px 18px 14px 18px; " +
                    "-fx-text-fill: " + TEXT_PRIMARY + "; " +
                    "-fx-font-weight: 500;"
                );
                label.setTextFill(Color.web(TEXT_PRIMARY));

                // Reset nested labels when hover ends
                if (label.getGraphic() != null && label.getGraphic() instanceof javafx.scene.layout.Pane) {
                    updateNestedLabelsColorWithSecondary((javafx.scene.layout.Pane) label.getGraphic());
                }
            }
        });
    }

    /**
     * Update all nested labels to a single color (for selected/hover states)
     */
    private void updateNestedLabelsColor(javafx.scene.layout.Pane pane, Color color) {
        for (javafx.scene.Node node : pane.getChildren()) {
            if (node instanceof Label) {
                ((Label) node).setTextFill(color);
            } else if (node instanceof javafx.scene.layout.Pane) {
                updateNestedLabelsColor((javafx.scene.layout.Pane) node, color);
            }
        }
    }

    /**
     * Update nested labels with primary and secondary colors (for normal state)
     * First label gets primary color, others get secondary color
     */
    private void updateNestedLabelsColorWithSecondary(javafx.scene.layout.Pane pane) {
        boolean isFirst = true;
        for (javafx.scene.Node node : pane.getChildren()) {
            if (node instanceof Label) {
                if (isFirst) {
                    ((Label) node).setTextFill(Color.web(TEXT_PRIMARY)); // Primary text
                    isFirst = false;
                } else {
                    ((Label) node).setTextFill(Color.web(TEXT_SECONDARY)); // Secondary text
                }
            } else if (node instanceof javafx.scene.layout.Pane) {
                updateNestedLabelsColorWithSecondary((javafx.scene.layout.Pane) node);
            }
        }
    }

    private Label createDefaultLabel(T item, boolean highlighted) {
        Label label = new Label(displayFunction.apply(item));
        label.setPrefWidth(Region.USE_COMPUTED_SIZE);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setFont(Font.font("Segoe UI", FontWeight.MEDIUM, 14));
        label.setWrapText(true); // Enable text wrapping
        label.setEllipsisString("..."); // Add ellipsis for truncated text
        label.setTextAlignment(TextAlignment.LEFT);
        label.setStyle("-fx-cursor: hand; -fx-text-fill: " + TEXT_PRIMARY + ";");
        return label;
    }

    private void styleComponents() {
        // Professional popup styling with modern Material Design shadow
        suggestionsPopup.setStyle(
                "-fx-background-color: " + BACKGROUND_FOCUS + "; " +
                "-fx-background-radius: 12px; " +
                "-fx-border-color: " + BORDER_DEFAULT + "; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 12px; " +
                "-fx-effect: dropshadow(gaussian, " + SHADOW_POPUP + ", 20, 0.0, 0, 6); " +
                "-fx-padding: 8px;"
        );
    }

    public void setStyle(String style) {
        this.textFieldStyle = style;
        textField.setStyle(style);
    }

    public void setFocusedStyle(String style) {
        this.focusedStyle = style;
    }

    /**
     * Request focus on the text field
     */
    public void requestFocus() {
        textField.requestFocus();
    }

    /**
     * Set whether the field is editable
     */
    public void setEditable(boolean editable) {
        textField.setEditable(editable);
    }

    /**
     * Get the current text value
     */
    public String getText() {
        return textField.getText();
    }

    /**
     * Set the text value
     */
    public void setText(String text) {
        textField.setText(text);
    }

    /**
     * Set maximum width for the component
     */
    public void setMaxWidth(double width) {
        if (wrapper != null) {
            wrapper.setMaxWidth(width);
        }
        textField.setMaxWidth(width);
    }

    /**
     * Set preferred width for the component
     */
    public void setPrefWidth(double width) {
        if (wrapper != null) {
            wrapper.setPrefWidth(width);
        }
        textField.setPrefWidth(width);
    }
}
