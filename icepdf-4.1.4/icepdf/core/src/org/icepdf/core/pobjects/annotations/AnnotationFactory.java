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
 * The Original Code is ICEpdf 4.1 open source software code, released
 * May 1st, 2009. The Initial Developer of the Original Code is ICEsoft
 * Technologies Canada, Corp. Portions created by ICEsoft are Copyright (C)
 * 2004-2010 ICEsoft Technologies Canada, Corp. All Rights Reserved.
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
package org.icepdf.core.pobjects.annotations;

import org.icepdf.core.pobjects.Dictionary;
import org.icepdf.core.pobjects.Name;
import org.icepdf.core.pobjects.PRectangle;
import org.icepdf.core.pobjects.StateManager;
import org.icepdf.core.util.Library;

import java.awt.*;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 * Factory for build annotations.
 * <p/>
 * Note: Currently only Link annotations are supported.
 *
 * @since 4.0
 */
public class AnnotationFactory {

    private static final Logger logger =
            Logger.getLogger(AnnotationFactory.class.toString());

    public static final int LINK_ANNOTATION = 1;

    /**
     * Creates a new Annotation object using properties from the annotationState
     * paramater.  If no annotaitonState is provided a LinkAnnotation is returned
     * with with a black border.  The rect specifies where the annotation should
     * be located in user space.
     * <p/>
     * This call adds the new Annotation object to the document library as well
     * as the document StateManager.
     *
     * @param library         library to register annotation with
     * @param type            type of annotation to create
     * @param rect            bounds of new annotation specified in user space.
     * @param annotationState annotation state to copy state rom.
     * @return new annotation object with the same properties as the one
     *         specified in annotaiton state.
     */
    public static Annotation buildAnnotation(Library library,
                                             int type,
                                             Rectangle rect,
                                             AnnotationState annotationState) {
        // state manager 
        StateManager stateManager = library.getStateManager();

        // create a new entries to hold the annotation properties
        Hashtable<Name, Object> entries = new Hashtable<Name, Object>();
        // set default link annotation values. 
        entries.put(Dictionary.TYPE_KEY, Annotation.TYPE_VALUE);
        entries.put(Dictionary.SUBTYPE_KEY, Annotation.SUBTYPE_LINK);
        // coordinates
        if (rect != null) {
            entries.put(Annotation.RECTANGLE_KEY,
                    PRectangle.getPRectangleVector(rect));
        } else {
            entries.put(Annotation.RECTANGLE_KEY, new Rectangle(10, 10, 50, 100));
        }
        // build up a link annotation
        if (type == LINK_ANNOTATION) {
            // we only support one type of annotation creation for now
            LinkAnnotation linkAnnotation = new LinkAnnotation(library, entries);
            linkAnnotation.setPObjectReference(stateManager.getNewReferencNumber());
            linkAnnotation.setNew(true);

            // apply state
            if (annotationState != null) {
                annotationState.restore(linkAnnotation);
            }
            // some defaults just for display purposes.
            else {
                annotationState = new AnnotationState(
                        Annotation.VISIBLE_RECTANGLE,
                        LinkAnnotation.HIGHLIGHT_INVERT, 1f,
                        BorderStyle.BORDER_STYLE_SOLID, Color.RED);
                annotationState.restore(linkAnnotation);
            }
            return linkAnnotation;
        } else {
            logger.warning("Unsupported Annotation type. ");
            return null;
        }

    }
}
