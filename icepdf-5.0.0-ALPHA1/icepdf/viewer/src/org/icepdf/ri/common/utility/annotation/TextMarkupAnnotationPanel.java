/*
 * Copyright 2006-2012 ICEsoft Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.icepdf.ri.common.utility.annotation;

import org.icepdf.core.pobjects.Name;
import org.icepdf.core.pobjects.annotations.TextMarkupAnnotation;
import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.common.views.AbstractDocumentViewModel;
import org.icepdf.ri.common.views.AnnotationComponent;
import org.icepdf.ri.common.views.annotations.AnnotationState;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

/**
 * TextAnnotationPanel is a configuration panel for changing the properties
 * of a TextAnnotationComponent and the underlying annotation component.
 *
 * @since 5.0
 */
public class TextMarkupAnnotationPanel extends AnnotationPanelAdapter implements ItemListener,
        ActionListener {

    // default list values.
    private static final int DEFAULT_TEXT_MARKUP_TYPE = 0;
    private static final Color DEFAULT_BORDER_COLOR = Color.BLACK;

    // text markup sub types.
    private final ValueLabelItem[] TEXT_MARKUP_TYPE_LIST = new ValueLabelItem[]{
            new ValueLabelItem(TextMarkupAnnotation.SUBTYPE_HIGHLIGHT, "Highlight"),
            new ValueLabelItem(TextMarkupAnnotation.SUBTYPE_STRIKE_OUT, "Strikeout"),
            new ValueLabelItem(TextMarkupAnnotation.SUBTYPE_UNDERLINE, "Underline")};

    private SwingController controller;
    private ResourceBundle messageBundle;

    // action instance that is being edited
    private AnnotationComponent currentAnnotationComponent;

    // text markup appearance properties.
    private JComboBox textMarkupTypes;
    private JButton colorButton;

    private TextMarkupAnnotation annotation;

    public TextMarkupAnnotationPanel(SwingController controller) {
        super(new GridLayout(2, 2, 5, 2), true);

        this.controller = controller;
        this.messageBundle = this.controller.getMessageBundle();

        // Setup the basics of the panel
        setFocusable(true);
//        setBorder(new EmptyBorder(10, 5, 1, 5));

        // Add the tabbed pane to the overall panel
        createGUI();

        // Start the panel disabled until an action is clicked
        setEnabled(false);

        revalidate();
    }

    /**
     * Method that should be called when a new AnnotationComponent is selected by the user
     * The associated object will be stored locally as currentAnnotation
     * Then all of it's properties will be applied to the UI pane
     * For example if the border was red, the color of the background button will
     * be changed to red
     *
     * @param newAnnotation to set and apply to this UI
     */
    public void setAnnotationComponent(AnnotationComponent newAnnotation) {

        if (newAnnotation == null || newAnnotation.getAnnotation() == null) {
            setEnabled(false);
            return;
        }
        // assign the new action instance.
        this.currentAnnotationComponent = newAnnotation;

        // For convenience grab the Annotation object wrapped by the component
        annotation = (TextMarkupAnnotation)
                currentAnnotationComponent.getAnnotation();

        applySelectedValue(textMarkupTypes, annotation.getSubType());
        colorButton.setBackground(annotation.getTextMarkupColor());

        // disable appearance input if we have a invisible rectangle
        safeEnable(textMarkupTypes, true);
        safeEnable(colorButton, true);
    }

    public void itemStateChanged(ItemEvent e) {
        ValueLabelItem item = (ValueLabelItem) e.getItem();
        if (e.getStateChange() == ItemEvent.SELECTED) {
            if (e.getSource() == textMarkupTypes) {
                annotation.setSubtype((Name) item.getValue());
            }
            // save the action state back to the document structure.
            updateAnnotationState();
            currentAnnotationComponent.resetAppearanceShapes();
            currentAnnotationComponent.repaint();
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == colorButton) {
            Color chosenColor =
                    JColorChooser.showDialog(colorButton,
                            messageBundle.getString(
                                    "viewer.utilityPane.annotation.textMarkup.colorChooserTitle"),
                            colorButton.getBackground());
            if (chosenColor != null) {
                // change the colour of the button background
                colorButton.setBackground(chosenColor);
                annotation.setTextMarkupColor(chosenColor);

                // save the action state back to the document structure.
                updateAnnotationState();
                currentAnnotationComponent.resetAppearanceShapes();
                currentAnnotationComponent.repaint();
            }
        }
    }

    /**
     * Method to create link annotation GUI.
     */
    private void createGUI() {

        // Create and setup an Appearance panel
        setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED),
                messageBundle.getString("viewer.utilityPane.annotation.textMarkup.appearance.title"),
                TitledBorder.LEFT,
                TitledBorder.DEFAULT_POSITION));
        // Text markup type
        textMarkupTypes = new JComboBox(TEXT_MARKUP_TYPE_LIST);
        textMarkupTypes.setSelectedIndex(DEFAULT_TEXT_MARKUP_TYPE);
        textMarkupTypes.addItemListener(this);
        add(new JLabel(
                messageBundle.getString("viewer.utilityPane.annotation.textMarkup.highlightType")));
        add(textMarkupTypes);
        // border colour
        colorButton = new JButton();
        colorButton.addActionListener(this);
        colorButton.setOpaque(true);
        colorButton.setBackground(DEFAULT_BORDER_COLOR);
        add(new JLabel(
                messageBundle.getString("viewer.utilityPane.annotation.textMarkup.colorLabel")));
        add(colorButton);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        safeEnable(textMarkupTypes, enabled);
        safeEnable(colorButton, enabled);
    }

    private void updateAnnotationState() {
        // store old state
        AnnotationState oldState = new AnnotationState(currentAnnotationComponent);
        // store new state from panel
        AnnotationState newState = new AnnotationState(currentAnnotationComponent);
        // todo: update how state is stored as we have a lot of annotations...
//        AnnotationState changes = new AnnotationState(
//                linkType, null, 0, textMarkupType, color);
        // apply new properties to the action and the component
//        newState.apply(changes);
        // temporary apply new state info
        TextMarkupAnnotation textMarkupAnnotation = (TextMarkupAnnotation)
                currentAnnotationComponent.getAnnotation();

        // Add our states to the undo caretaker
        ((AbstractDocumentViewModel) controller.getDocumentViewController().
                getDocumentViewModel()).getAnnotationCareTaker()
                .addState(oldState, newState);

        // Check with the controller whether we can enable the undo/redo menu items
        controller.reflectUndoCommands();
    }


    /**
     * Convenience method to ensure a component is safe to toggle the enabled state on
     *
     * @param comp    to toggle
     * @param enabled the status to use
     * @return true on success
     */
    protected boolean safeEnable(JComponent comp, boolean enabled) {
        if (comp != null) {
            comp.setEnabled(enabled);
            return true;
        }
        return false;
    }

    private void applySelectedValue(JComboBox comboBox, Object value) {
        comboBox.removeItemListener(this);
        ValueLabelItem currentItem;
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            currentItem = (ValueLabelItem) comboBox.getItemAt(i);
            if (currentItem.getValue().equals(value)) {
                comboBox.setSelectedIndex(i);
                break;
            }
        }
        comboBox.addItemListener(this);
    }

}