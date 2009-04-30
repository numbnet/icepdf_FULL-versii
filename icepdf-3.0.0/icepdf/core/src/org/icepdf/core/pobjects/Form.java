/*
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * "The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is ICEpdf 3.0 open source software code, released
 * May 1st, 2009. The Initial Developer of the Original Code is ICEsoft
 * Technologies Canada, Corp. Portions created by ICEsoft are Copyright (C)
 * 2004-2009 ICEsoft Technologies Canada, Corp. All Rights Reserved.
 *
 * Contributor(s): _____________________.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"
 * License), in which case the provisions of the LGPL License are
 * applicable instead of those above. If you wish to allow use of your
 * version of this file only under the terms of the LGPL License and not to
 * allow others to use your version of this file under the MPL, indicate
 * your decision by deleting the provisions above and replace them with
 * the notice and other provisions required by the LGPL License. If you do
 * not delete the provisions above, a recipient may use your version of
 * this file under either the MPL or the LGPL License."
 *
 */
package org.icepdf.core.pobjects;

import org.icepdf.core.io.SeekableInputConstrainedWrapper;
import org.icepdf.core.pobjects.graphics.GraphicsState;
import org.icepdf.core.pobjects.graphics.Shapes;
import org.icepdf.core.util.ContentParser;
import org.icepdf.core.util.Library;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Form XObject class. Not currently part of the public api.
 *
 * @since 1.0
 */
public class Form extends Stream {

    private static final Logger logger =
            Logger.getLogger(Form.class.toString());

    private AffineTransform matrix = new AffineTransform();
    private Rectangle2D bbox;
    private Shapes shapes;
    // Graphics state object to be used by content parser
    private GraphicsState graphicsState;
    private Resources resources;
    private Resources parentResource;
    private boolean inited = false;

    /**
     * @param l
     * @param h
     * @param streamInputWrapper
     */
    public Form(Library l, Hashtable h, SeekableInputConstrainedWrapper streamInputWrapper) {
        super(l, h, streamInputWrapper);
    }

    public void dispose(boolean cache) {
        if (shapes != null)
            shapes.dispose();
        if (resources != null) {
            resources.dispose(cache);
        }
        if (parentResource != null) {
            // remove parent reference to parent
            parentResource = null;
        }
        inited = false;
        graphicsState = null;
    }

    /**
     * Sets the GraphicsState which should be used by the content parser when
     * parsing the Forms content stream.  The GraphicsState should be set
     * before init() is called, or it will have not effect on the rendered
     * content.
     *
     * @param graphicsState current graphic state
     */
    public void setGraphicsState(GraphicsState graphicsState) {
        if (graphicsState != null) {
            this.graphicsState = graphicsState;
        }
    }

    /**
     * Utility method for parsing a vector of affinetranform values to an
     * affine transform.
     *
     * @param v vectory containing affine transform values.
     * @return affine tansform based on v
     */
    private static AffineTransform getAffineTransform(Vector v) {
        float f[] = new float[6];
        for (int i = 0; i < 6; i++) {
            f[i] = ((Number) v.elementAt(i)).floatValue();
        }
        return new AffineTransform(f);
    }

    /**
     * As of the PDF 1.2 specification, a resouce entry is not required for
     * a XObject and thus it needs to point to the parent resource enable
     * to correctly load the content stream.
     *
     * @param parentResource parent objects resourse when available.
     */
    public void setParentResources(Resources parentResource) {
        this.parentResource = parentResource;
    }

    /**
     *
     */
    public void init() {
        if (inited) {
            return;
        }
        Vector v = (Vector) library.getObject(entries, "Matrix");
        if (v != null) {
            matrix = getAffineTransform(v);
        }
        bbox = library.getRectangle(entries, "BBox");
        // try and find the form's resources dictionary.
        Resources leafResources = library.getResources(entries, "Resources");
        // apply parent resource, if the current resources is null
        if (leafResources != null) {
            resources = leafResources;
        } else {
            leafResources = parentResource;
        }
        // Build a new content parser for the content streams and apply the
        // content stream of the calling content stream. 
        ContentParser cp = new ContentParser(library, leafResources);
        cp.setGraphicsState(graphicsState);
        InputStream in = getInputStreamForDecodedStreamBytes();
        if (in != null) {
            try {
                shapes = cp.parse(in);
            }
            catch (Throwable e) {
                // reset shapes vector, we don't want to mess up the paint stack
                shapes = new Shapes();
                logger.log(Level.SEVERE, "Error parsing Form content stream.", e);
            }
            finally {
                try {
                    in.close();
                }
                catch (IOException e) {
                }
            }
        }
        inited = true;
    }

    /**
     * @return
     */
    public Shapes getShapes() {
        return shapes;
    }

    /**
     * @return
     */
    public Rectangle2D getBBox() {
        return bbox;
    }

    /**
     * @return
     */
    public AffineTransform getMatrix() {
        return matrix;
    }
}
