package com.gurukrupa.customUI;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Elegant JavaFX TextField with autocomplete suggestion popup.
 * Suggestions are passed via the constructor or setter.
 */
public class AutoCompleteTextField {

    @Getter
    private final TextField textField;
    private final ContextMenu suggestionsPopup;
    private List<String> suggestions = new ArrayList<>();
    private List<String> filteredSuggestions = new ArrayList<>();
    private int selectedIndex = 0;
    private boolean isSelectingSuggestion = false;

    // Reference to text listener for removal/re-adding
    private ChangeListener<String> textListener;

    public AutoCompleteTextField(TextField textField, List<String> suggestions) {
        this.textField = textField;
        this.suggestionsPopup = new ContextMenu();
        this.suggestionsPopup.setAutoHide(true);
        this.suggestions = new ArrayList<>(suggestions);
        styleTextField();
        attachListeners();
        stylePopup();
    }

    public AutoCompleteTextField(TextField textField) {
        this(textField, new ArrayList<>());
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = new ArrayList<>(suggestions);
    }

    private void attachListeners() {
        textListener = (obs, oldText, newText) -> {
            if (isSelectingSuggestion) {
                return; // Skip processing during suggestion selection
            }

            if (newText == null || newText.isEmpty()) {
                suggestionsPopup.hide();
                selectedIndex = 0; // Reset selected index
            } else {
                filteredSuggestions = suggestions.stream()
                        .filter(item -> item.toLowerCase().startsWith(newText.toLowerCase()))
                        .collect(Collectors.toList());

                if (filteredSuggestions.isEmpty()) {
                    suggestionsPopup.hide();
                    selectedIndex = 0; // Reset selected index
                } else {
                    selectedIndex = 0; // Always start with first suggestion
                    populatePopup();
                    if (!suggestionsPopup.isShowing()) {
                        suggestionsPopup.show(textField,
                                textField.localToScreen(textField.getBoundsInLocal()).getMinX(),
                                textField.localToScreen(textField.getBoundsInLocal()).getMaxY());
                    }
                }
            }
        };

        textField.textProperty().addListener(textListener);

        textField.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ESCAPE -> {
                    suggestionsPopup.hide();
                    selectedIndex = 0;
                }
                case ENTER -> {
                    System.out.println("Enter button pressed");
                    if (!filteredSuggestions.isEmpty() && selectedIndex >= 0 && selectedIndex < filteredSuggestions.size()) {
                        selectSuggestion(selectedIndex);
                        event.consume(); // Consume the event to prevent other handlers
                    }
                }
                case DOWN -> {
                    if (suggestionsPopup.isShowing() && selectedIndex < filteredSuggestions.size() - 1) {
                        selectedIndex++;
                        updatePopupHighlight();
                    }
                    event.consume();
                }
                case UP -> {
                    if (suggestionsPopup.isShowing() && selectedIndex > 0) {
                        selectedIndex--;
                        updatePopupHighlight();
                    }
                    event.consume();
                }
                default -> {
                    // Do nothing for other keys
                }
            }
        });

        // Add focus listener to hide popup when focus is lost
        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                suggestionsPopup.hide();
                selectedIndex = 0;
            }
        });
    }

    private void selectSuggestion(int index) {
        if (index < 0 || index >= filteredSuggestions.size()) {
            return; // Safety check
        }

        isSelectingSuggestion = true; // Set flag to prevent listener interference

        String suggestion = filteredSuggestions.get(index);
        textField.setText(suggestion);
        textField.positionCaret(suggestion.length());

        suggestionsPopup.hide();
        selectedIndex = 0;

        isSelectingSuggestion = false; // Reset flag

        System.out.println("Selected suggestion: " + suggestion);
    }

    private void populatePopup() {
        List<CustomMenuItem> menuItems = new ArrayList<>();
        for (int i = 0; i < filteredSuggestions.size(); i++) {
            String suggestion = filteredSuggestions.get(i);
            Label entryLabel = createSuggestionLabel(suggestion, i == selectedIndex);
            int index = i;
            CustomMenuItem item = new CustomMenuItem(entryLabel, true);
            item.setOnAction(evt -> selectSuggestion(index));
            menuItems.add(item);
        }
        suggestionsPopup.getItems().clear();
        suggestionsPopup.getItems().addAll(menuItems);

        suggestionsPopup.setPrefWidth(Region.USE_COMPUTED_SIZE);
        suggestionsPopup.setAutoFix(true);
    }

    private void updatePopupHighlight() {
        for (int i = 0; i < suggestionsPopup.getItems().size(); i++) {
            CustomMenuItem item = (CustomMenuItem) suggestionsPopup.getItems().get(i);
            Label label = (Label) item.getContent();
            if (i == selectedIndex) {
                label.setStyle(HIGHLIGHT_LABEL_STYLE);
                label.setTextFill(Color.WHITE);
            } else {
                label.setStyle(NORMAL_LABEL_STYLE);
                label.setTextFill(Color.web("#374151"));
            }
        }
    }

    private Label createSuggestionLabel(String text, boolean highlighted) {
        Label label = new Label(text);
        label.setPrefWidth(300);
        label.setPadding(new Insets(8, 12, 8, 12));
        label.setFont(Font.font("Inter", 16));
        label.setStyle(highlighted ? HIGHLIGHT_LABEL_STYLE : NORMAL_LABEL_STYLE);
        label.setWrapText(false);
        label.setTextFill(highlighted ? Color.WHITE : Color.web("#374151"));
        return label;
    }

    private void styleTextField() {
        textField.setFont(Font.font("Inter", 16));
        textField.setStyle(
                "-fx-background-radius: 12px; " +
                        "-fx-border-radius: 12px; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-padding: 8px 12px 8px 12px;" +
                        "-fx-background-color: #ffffff;"
        );
    }

    private void stylePopup() {
        suggestionsPopup.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 12px; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 4); " +
                        "-fx-padding: 4px;"
        );
    }

    private static final String HIGHLIGHT_LABEL_STYLE = "-fx-background-color: #2563eb; -fx-font-weight: 600; -fx-background-radius: 8px;";
    private static final String NORMAL_LABEL_STYLE = "-fx-background-color: transparent;";
}
