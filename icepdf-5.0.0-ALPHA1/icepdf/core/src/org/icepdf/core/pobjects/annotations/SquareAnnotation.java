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
package org.icepdf.core.pobjects.annotations;

import org.icepdf.core.pobjects.Dictionary;
import org.icepdf.core.pobjects.Name;
import org.icepdf.core.pobjects.PRectangle;
import org.icepdf.core.pobjects.StateManager;
import org.icepdf.core.pobjects.graphics.Shapes;
import org.icepdf.core.pobjects.graphics.commands.*;
import org.icepdf.core.util.Library;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Square annotations (PDF 1.3) shall display, respectively, a
 * rectangle or an ellipse on the page. When opened, they shall display a
 * pop-up window containing the text of the associated note. The rectangle or
 * ellipse shall be inscribed within the annotation rectangle defined by the
 * annotation dictionary’s Rect entry (see Table 168).
 * <p/>
 * Figure 63 shows two annotations, each with a border width of 18 points. Despite
 * the names square and circle, the width and height of the annotation rectangle
 * need not be equal. Table 177 shows the annotation dictionary entries specific
 * to these types of annotations.
 *
 * @since 5.0
 */
public class SquareAnnotation extends MarkupAnnotation {

    private static final Logger logger =
            Logger.getLogger(SquareAnnotation.class.toString());

    /**
     * (Optional; PDF 1.4) An array of numbers in the range 0.0 to 1.0 specifying
     * the interior color that shall be used to fill the annotation’s line endings
     * (see Table 176). The number of array elements shall determine the colour
     * space in which the colour is defined:
     * 0 - No colour; transparent
     * 1 - DeviceGray
     * 3 - DeviceRGB
     * 4 - DeviceCMYK
     */
    public static final Name IC_KEY = new Name("IC");

    private Color fillColor;
    private boolean isFillColor;
    private Rectangle rectangle;

    public SquareAnnotation(Library l, HashMap h) {
        super(l, h);

        // line border style
        HashMap BS = (HashMap) getObject(BORDER_STYLE_KEY);
        if (BS != null) {
            borderStyle = new BorderStyle(library, BS);
        } else {
            borderStyle = new BorderStyle(library, new HashMap());
        }

        // parse out interior colour, specific to link annotations.
        fillColor = Color.WHITE; // we default to black but probably should be null
        java.util.List C = (java.util.List) getObject(IC_KEY);
        // parse thought rgb colour.
        if (C != null && C.size() >= 3) {
            float red = ((Number) C.get(0)).floatValue();
            float green = ((Number) C.get(1)).floatValue();
            float blue = ((Number) C.get(2)).floatValue();
            red = Math.max(0.0f, Math.min(1.0f, red));
            green = Math.max(0.0f, Math.min(1.0f, green));
            blue = Math.max(0.0f, Math.min(1.0f, blue));
            fillColor = new Color(red, green, blue);
            isFillColor = true;
        }
    }

    /**
     * Gets an instance of a SquareAnnotation that has valid Object Reference.
     *
     * @param library document library
     * @param rect    bounding rectangle in user space
     * @return new SquareAnnotation Instance.
     */
    public static SquareAnnotation getInstance(Library library,
                                               Rectangle rect) {
        // state manager
        StateManager stateManager = library.getStateManager();

        // create a new entries to hold the annotation properties
        HashMap<Name, Object> entries = new HashMap<Name, Object>();
        // set default link annotation values.
        entries.put(Dictionary.TYPE_KEY, Annotation.TYPE_VALUE);
        entries.put(Dictionary.SUBTYPE_KEY, Annotation.SUBTYPE_SQUARE);
        // coordinates
        if (rect != null) {
            entries.put(Annotation.RECTANGLE_KEY,
                    PRectangle.getPRectangleVector(rect));
        } else {
            entries.put(Annotation.RECTANGLE_KEY, new Rectangle(10, 10, 50, 100));
        }

        // create the new instance
        SquareAnnotation squareAnnotation = new SquareAnnotation(library, entries);
        squareAnnotation.setPObjectReference(stateManager.getNewReferencNumber());
        squareAnnotation.setNew(true);

        return squareAnnotation;
    }

    /**
     * Resets the annotations appearance stream.
     */
    public void resetAppearanceStream() {

        setAppearanceStream(bbox.getBounds());
    }

    /**
     * Sets the shapes that make up the appearance stream that match the
     * current state of the annotation.
     *
     * @param bbox bounding box bounds.
     */
    public void setAppearanceStream(Rectangle bbox) {
        matrix = new AffineTransform();
        this.bbox = bbox;
        shapes = new Shapes();

        BasicStroke stroke;
        if (borderStyle.isStyleDashed()) {
            stroke = new BasicStroke(
                    borderStyle.getStrokeWidth(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, borderStyle.getDashArray(), 0.0f);
        } else {
            stroke = new BasicStroke(borderStyle.getStrokeWidth());
        }

        if (rectangle == null) {
            rectangle = new Rectangle(bbox.x + 5, bbox.y + 5,
                    bbox.width - 10, bbox.height - 10);
        }

        // setup the space for the AP content stream.
        AffineTransform af = new AffineTransform();
        af.translate(-this.bbox.getMinX(), -this.bbox.getMinY());

        shapes.add(new TransformDrawCmd(af));
        shapes.add(new StrokeDrawCmd(stroke));
        shapes.add(new ShapeDrawCmd(rectangle));
        if (isFillColor) {
            shapes.add(new ColorDrawCmd(fillColor));
            shapes.add(new FillDrawCmd());
        }
        if (borderStyle.getStrokeWidth() > 0) {
            shapes.add(new ColorDrawCmd(color));
            shapes.add(new DrawDrawCmd());
        }

    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public Rectangle getRectangle() {
        return rectangle;
    }

    public void setRectangle(Rectangle rectangle) {
        this.rectangle = rectangle;
    }

    public boolean isFillColor() {
        return isFillColor;
    }

    public void setFillColor(boolean fillColor) {
        isFillColor = fillColor;
    }
}
