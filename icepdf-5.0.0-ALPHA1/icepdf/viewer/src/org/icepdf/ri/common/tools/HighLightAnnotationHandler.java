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
package org.icepdf.ri.common.tools;

import org.icepdf.core.pobjects.Name;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.pobjects.annotations.Annotation;
import org.icepdf.core.pobjects.annotations.AnnotationFactory;
import org.icepdf.core.pobjects.annotations.TextMarkupAnnotation;
import org.icepdf.core.pobjects.graphics.text.GlyphText;
import org.icepdf.core.pobjects.graphics.text.LineText;
import org.icepdf.core.pobjects.graphics.text.PageText;
import org.icepdf.core.pobjects.graphics.text.WordText;
import org.icepdf.ri.common.views.AbstractPageViewComponent;
import org.icepdf.ri.common.views.AnnotationCallback;
import org.icepdf.ri.common.views.DocumentViewController;
import org.icepdf.ri.common.views.DocumentViewModel;
import org.icepdf.ri.common.views.annotations.AbstractAnnotationComponent;
import org.icepdf.ri.common.views.annotations.AnnotationComponentFactory;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;

/**
 * HighLightAnnotationHandler tool extends TextSelectionPageHandler which
 * takes care visually selected text as the mouse is dragged across text on the
 * current page.
 * <p/>
 * Once the mouseReleased event is fired this handler will create new
 * HighLightAnnotation and respective AnnotationComponent.  The addition of the
 * Annotation object to the page is handled by the annotation callback. Once
 * create the handler will deselect the text and the newly created annotation
 * will be displayed.
 *
 * @since 5.0
 */
public class HighLightAnnotationHandler extends TextSelectionPageHandler {

    protected static Name highLightType;

    public HighLightAnnotationHandler(DocumentViewController documentViewController,
                                      AbstractPageViewComponent pageViewComponent,
                                      DocumentViewModel documentViewModel) {
        super(documentViewController, pageViewComponent, documentViewModel);
        // default type
        highLightType = Annotation.SUBTYPE_HIGHLIGHT;
    }

    /**
     * Override the base functionality as we don't want to support double and
     * triple click work selection for highlights.
     *
     * @param e mouse event
     */
    public void mouseClicked(MouseEvent e) {

    }

    /**
     * Invoked when a mouse button has been released on a component.
     */
    public void mouseReleased(MouseEvent e) {

        // update selection rectangle
        updateSelectionSize(e, pageViewComponent);

        // clear selected rectangle
        clearRectangle(pageViewComponent);

        // get the geometric path of the selected text
        ArrayList<Shape> highlightBounds = getSelectedTextBounds();

        // clear the selected text
        clearSelection();

        if (highlightBounds != null) {

            // bound of the selected text
            GeneralPath highlightPath = new GeneralPath();
            for (Shape bounds : highlightBounds) {
                highlightPath.append(bounds, false);
            }
            // get the bounds before convert to page space
            Rectangle bounds = highlightPath.getBounds();

            Rectangle tBbox = convertToPageSpace(highlightBounds, highlightPath);

            // create annotations types that that are rectangle based;
            // which is actually just link annotations
            TextMarkupAnnotation annotation = (TextMarkupAnnotation)
                    AnnotationFactory.buildAnnotation(
                            documentViewModel.getDocument().getPageTree().getLibrary(),
                            highLightType,
                            tBbox);

            // pass outline shapes and bounds to create the highlight shapes
            annotation.setAppearanceStream(tBbox, highlightBounds, highlightPath);

            // create new annotation given the general path
            AbstractAnnotationComponent comp =
                    AnnotationComponentFactory.buildAnnotationComponent(
                            annotation,
                            documentViewController,
                            pageViewComponent, documentViewModel);

            // convert to user rect to page space along with the bounds.
            comp.setBounds(bounds);
            comp.refreshAnnotationRect();

            // create component and add it to the page.
            // add them to the container, using absolute positioning.
            if (documentViewController.getAnnotationCallback() != null) {
                AnnotationCallback annotationCallback =
                        documentViewController.getAnnotationCallback();
                annotationCallback.newAnnotation(pageViewComponent, comp);
            }
        }

        // set the annotation tool to he select tool
//        documentViewController.getParentController().setDocumentToolMode(
//                DocumentViewModel.DISPLAY_TOOL_SELECTION);

        pageViewComponent.repaint();
    }

    public void paintTool(Graphics g) {
        paintSelectedText(g);
        paintSelectionBox(g);
    }

    private ArrayList<Shape> getSelectedTextBounds() {
        Page currentPage = pageViewComponent.getPage();
        ArrayList<Shape> highlightBounds = null;
        if (currentPage != null && currentPage.isInitiated()) {
            PageText pageText = currentPage.getViewText();
            if (pageText != null) {
                // get page transformation
                AffineTransform pageTransform = currentPage.getPageTransform(
                        documentViewModel.getPageBoundary(),
                        documentViewModel.getViewRotation(),
                        documentViewModel.getViewZoom());
                // paint the sprites
                GeneralPath textPath;
                for (LineText lineText : pageText.getPageLines()) {
                    for (WordText wordText : lineText.getWords()) {
                        // paint whole word
                        if (wordText.isSelected() || wordText.isHighlighted()) {
                            textPath = new GeneralPath(wordText.getBounds());
                            textPath.transform(pageTransform);
                            // paint highlight over any selected
                            if (wordText.isSelected()) {
                                if (highlightBounds == null) {
                                    highlightBounds = new ArrayList<Shape>();
                                }
                                highlightBounds.add(textPath.getBounds2D());
                            }

                        }
                        // check children
                        else {
                            for (GlyphText glyph : wordText.getGlyphs()) {
                                if (glyph.isSelected()) {
                                    textPath = new GeneralPath(glyph.getBounds());
                                    textPath.transform(pageTransform);
                                    if (highlightBounds == null) {
                                        highlightBounds = new ArrayList<Shape>();
                                    }
                                    highlightBounds.add(textPath.getBounds2D());
                                }
                            }
                        }
                    }
                }
            }
        }
        return highlightBounds;
    }

    /**
     * Convert the shapes that make up the annotation to page space so that
     * they will scale correctly at different zooms.
     *
     * @return transformed bbox.
     */
    protected Rectangle convertToPageSpace(ArrayList<Shape> bounds,
                                           GeneralPath path) {
        Page currentPage = pageViewComponent.getPage();
        AffineTransform at = currentPage.getPageTransform(
                documentViewModel.getPageBoundary(),
                documentViewModel.getViewRotation(),
                documentViewModel.getViewZoom());
        try {
            at = at.createInverse();
        } catch (NoninvertibleTransformException e1) {
            e1.printStackTrace();
        }
        // convert the two points as well as the bbox.
        Rectangle tBbox = at.createTransformedShape(path).getBounds();

        // convert the points
        Shape bound;
        for (int i = 0; i < bounds.size(); i++) {
            bound = bounds.get(i);
            bound = at.createTransformedShape(bound);
            bounds.set(i, bound);
//            bound.setRect(tBound.getX(), tBound.getY(),
//                    tBound.getWidth(), tBound.getHeight());
        }

        path.transform(at);

        return tBbox;

    }

}
