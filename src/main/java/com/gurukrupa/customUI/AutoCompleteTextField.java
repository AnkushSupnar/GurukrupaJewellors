package com.gurukrupa.customUI;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Callback;
import javafx.util.StringConverter;
import lombok.Getter;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enhanced JavaFX TextField with autocomplete suggestion popup.
 * Supports both string suggestions and complex object suggestions with custom display.
 * @param <T> The type of items in the autocomplete suggestions
 */
public class AutoCompleteTextField<T> {

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
    
    // Styling
    private String textFieldStyle = 
            "-fx-background-radius: 8px; " +
            "-fx-border-radius: 8px; " +
            "-fx-border-color: #E0E0E0; " +
            "-fx-padding: 8px 12px 8px 12px;" +
            "-fx-background-color: #F8F9FA; " +
            "-fx-font-family: 'Segoe UI'; " +
            "-fx-font-size: 13px;";
    
    private String focusedStyle = 
            "-fx-background-radius: 8px; " +
            "-fx-border-radius: 8px; " +
            "-fx-border-color: #2196F3; " +
            "-fx-border-width: 2px; " +
            "-fx-padding: 7px 11px 7px 11px;" +
            "-fx-background-color: #FFFFFF; " +
            "-fx-font-family: 'Segoe UI'; " +
            "-fx-font-size: 13px;";

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
        container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        container.setSpacing(5);
        container.setStyle("-fx-background-color: transparent;");
        
        // Search icon in a separate container
        HBox iconContainer = new HBox();
        iconContainer.setAlignment(javafx.geometry.Pos.CENTER);
        iconContainer.setPrefWidth(30);
        iconContainer.setMinWidth(30);
        iconContainer.setMaxWidth(30);
        
        searchIcon = new FontAwesomeIcon();
        searchIcon.setGlyphName("SEARCH");
        searchIcon.setSize("14");
        searchIcon.setFill(Color.web("#757575"));
        iconContainer.getChildren().add(searchIcon);
        
        // Clear button with icon
        clearIcon = new FontAwesomeIcon();
        clearIcon.setGlyphName("TIMES_CIRCLE");
        clearIcon.setSize("16");
        clearIcon.setFill(Color.web("#757575"));
        
        clearButton = new Button();
        clearButton.setGraphic(clearIcon);
        clearButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0; -fx-min-width: 30; -fx-pref-width: 30; -fx-max-width: 30;");
        clearButton.setVisible(false);
        clearButton.setOnAction(e -> clear());
        
        // Setup text field with proper padding
        HBox.setHgrow(textField, Priority.ALWAYS);
        textField.setMaxWidth(Double.MAX_VALUE);
        
        // Add hover effect to clear button
        clearButton.setOnMouseEntered(e -> clearIcon.setFill(Color.web("#424242")));
        clearButton.setOnMouseExited(e -> clearIcon.setFill(Color.web("#757575")));
        
        // Store reference to icon container for later use
        this.iconContainer = iconContainer;
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
            HBox wrapper = new HBox();
            wrapper.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            wrapper.setStyle(
                "-fx-background-color: #F8F9FA; " +
                "-fx-background-radius: 8px; " +
                "-fx-border-radius: 8px; " +
                "-fx-border-color: #E0E0E0; " +
                "-fx-border-width: 1px; " +
                "-fx-padding: 0;"
            );
            
            // Make wrapper expand to fill available space
            wrapper.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(wrapper, Priority.ALWAYS);
            
            // Update text field style to be transparent
            textField.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-border-color: transparent; " +
                "-fx-padding: 8px 0px 8px 0px; " +
                "-fx-font-family: 'Segoe UI'; " +
                "-fx-font-size: 13px;"
            );
            
            // Make container expand to fill wrapper
            container.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(container, Priority.ALWAYS);
            
            container.getChildren().addAll(iconContainer, textField, clearButton);
            wrapper.getChildren().add(container);
            
            // Add focus listener to wrapper for border color change
            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (isNowFocused) {
                    wrapper.setStyle(
                        "-fx-background-color: #FFFFFF; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-border-color: #2196F3; " +
                        "-fx-border-width: 2px; " +
                        "-fx-padding: -1;"
                    );
                } else {
                    wrapper.setStyle(
                        "-fx-background-color: #F8F9FA; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1px; " +
                        "-fx-padding: 0;"
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
                                textField.localToScreen(textField.getBoundsInLocal()).getMaxY() + 2);
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
                            textField.localToScreen(textField.getBoundsInLocal()).getMaxY() + 2);
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

        // Focus handling
        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                searchIcon.setFill(Color.web("#2196F3"));
                
                // Show suggestions if text exists
                if (!textField.getText().isEmpty()) {
                    textListener.changed(null, "", textField.getText());
                }
            } else {
                searchIcon.setFill(Color.web("#757575"));
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
        for (int i = 0; i < Math.min(filteredSuggestions.size(), 8); i++) { // Limit to 8 suggestions
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

        // Adjust popup width to match text field
        suggestionsPopup.setMinWidth(textField.getWidth());
        suggestionsPopup.setPrefWidth(textField.getWidth());
        suggestionsPopup.setMaxWidth(textField.getWidth());
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
        label.setStyle("-fx-background-color: #2196F3; -fx-background-radius: 6px; -fx-padding: 8 12 8 12;");
        label.setTextFill(Color.WHITE);
    }
    
    private void applyNormalStyle(Label label) {
        label.setStyle("-fx-background-color: transparent; -fx-padding: 8 12 8 12;");
        label.setTextFill(Color.web("#424242"));
    }

    private Label createDefaultLabel(T item, boolean highlighted) {
        Label label = new Label(displayFunction.apply(item));
        label.setPrefWidth(Region.USE_COMPUTED_SIZE);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setFont(Font.font("Segoe UI", 13));
        label.setWrapText(false);
        return label;
    }

    private void styleComponents() {
        // Text field style is now set in getNode() method
        
        suggestionsPopup.setStyle(
                "-fx-background-color: white; " +
                "-fx-background-radius: 8px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 2); " +
                "-fx-padding: 4px;"
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
}