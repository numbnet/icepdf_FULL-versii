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
package org.icepdf.ri.common.views.annotations;

import org.icepdf.core.pobjects.PDate;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.pobjects.Reference;
import org.icepdf.core.pobjects.annotations.Annotation;
import org.icepdf.core.pobjects.annotations.MarkupAnnotation;
import org.icepdf.core.pobjects.annotations.PopupAnnotation;
import org.icepdf.ri.common.tools.TextAnnotationHandler;
import org.icepdf.ri.common.views.*;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

/**
 * MarkupAnnotationComponent class encapsulates the component functionality
 * needed to display an MarkupAnnotations PopupAnnnotaion component. When
 * a MarkupAnnotationComponent is double clicked is child PopupAnnotation component
 * will be displayed.
 *
 * @see CircleAnnotationComponent
 * @see FreeTextAnnotationComponent
 * @see InkAnnotationComponent
 * @see LineAnnotationComponent
 * @see LinkAnnotationComponent
 * @see PolygonAnnotationComponent
 * @see PolyLineAnnotationComponent
 * @see SquareAnnotationComponent
 * @see TextAnnotationComponent
 * @see TextMarkupAnnotationComponent
 * @since 5.0
 */
public abstract class MarkupAnnotationComponent extends AbstractAnnotationComponent {

    private static final Logger logger =
            Logger.getLogger(TextAnnotationComponent.class.toString());

    protected MarkupAnnotation markupAnnotation;

    public MarkupAnnotationComponent(Annotation annotation,
                                     DocumentViewController documentViewController,
                                     AbstractPageViewComponent pageViewComponent,
                                     DocumentViewModel documentViewModel) {
        super(annotation, documentViewController, pageViewComponent, documentViewModel);

        if (annotation instanceof MarkupAnnotation) {
            markupAnnotation = (MarkupAnnotation) annotation;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
        // on double click toggle the visibility of the popup component.
        if (e.getClickCount() == 2) {
            // we have an annotation so toggle it's visibility
            if (markupAnnotation != null) {
                PopupAnnotation popup = markupAnnotation.getPopupAnnotation();
                if (popup != null) {
                    // toggle the visibility of the popup
                    popup.setOpen(!popup.isOpen());
                    // find the popup component
                    ArrayList<AnnotationComponent> annotationComponents =
                            pageViewComponent.getAnnotationComponents();
                    Reference compReference;
                    Reference popupReference = popup.getPObjectReference();
                    for (AnnotationComponent annotationComponent : annotationComponents) {
                        compReference = annotationComponent.getAnnotation().getPObjectReference();
                        // find the component and toggle it's visibility.
                        if (compReference.equals(popupReference)) {
                            if (annotationComponent instanceof AbstractAnnotationComponent) {
                                ((AbstractAnnotationComponent) annotationComponent).setVisible(popup.isOpen());
                            }
                            break;
                        }
                    }
                }

                // no markupAnnotation so we need to create one and display for
                // the addition comments.
                else {
                    // convert bbox and start and end line points.
                    Rectangle bounds = this.getBounds();
                    Rectangle bBox = new Rectangle(bounds.x, bounds.y, 215, 150);

                    Rectangle tBbox = convertToPageSpace(bBox).getBounds();

                    // apply creation date and title for the markup annotation
                    // so the popup has some content
                    if (markupAnnotation != null) {
                        markupAnnotation.setCreationDate(PDate.createDate(new Date()));
                        markupAnnotation.setTitleText(System.getProperty("user.name"));
                        markupAnnotation.setContents("");
                    }

                    PopupAnnotation annotation =
                            TextAnnotationHandler.createPopupAnnotation(
                                    documentViewModel.getDocument().getPageTree().getLibrary(),
                                    tBbox, markupAnnotation);

                    // create the annotation object.
                    AbstractAnnotationComponent comp =
                            AnnotationComponentFactory.buildAnnotationComponent(
                                    annotation,
                                    documentViewController,
                                    pageViewComponent, documentViewModel);
                    // set the bounds and refresh the userSpace rectangle
                    comp.setBounds(bBox);
                    // resets user space rectangle to match bbox converted to page space
                    comp.refreshAnnotationRect();

                    // add them to the container, using absolute positioning.
                    if (documentViewController.getAnnotationCallback() != null) {
                        AnnotationCallback annotationCallback =
                                documentViewController.getAnnotationCallback();
                        annotationCallback.newAnnotation(pageViewComponent, comp);
                    }
                    pageViewComponent.revalidate();
                }
            }
        }
    }

    /**
     * Convert the shapes that make up the annotation to page space so that
     * they will scale correctly at different zooms.
     *
     * @return transformed bbox.
     */
    protected Shape convertToPageSpace(Shape shape) {
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

        shape = at.createTransformedShape(shape);

        return shape;

    }

}
