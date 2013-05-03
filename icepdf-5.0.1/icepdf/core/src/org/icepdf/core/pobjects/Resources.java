/*
 * Copyright 2006-2013 ICEsoft Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.icepdf.core.pobjects;

import org.icepdf.core.pobjects.fonts.FontFactory;
import org.icepdf.core.pobjects.graphics.*;
import org.icepdf.core.util.Library;

import java.awt.*;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A resource is a dictionary type as defined by the PDF specification.  It
 * can contain fonts, xobjects, colorspaces, patterns, shading and
 * external graphic states.
 *
 * @since 1.0
 */
public class Resources extends Dictionary {

    public static final Name COLORSPACE_KEY = new Name("ColorSpace");
    public static final Name FONT_KEY = new Name("Font");
    public static final Name XOBJECT_KEY = new Name("XObject");
    public static final Name PATTERN_KEY = new Name("Pattern");
    public static final Name SHADING_KEY = new Name("Shading");
    public static final Name EXTGSTATE_KEY = new Name("ExtGState");
    public static final Name PROPERTIES_KEY = new Name("Properties");

    // shared resource counter. 
    private static int uniqueCounter = 0;

    private static synchronized int getUniqueId() {
        return uniqueCounter++;
    }

    private static final Logger logger =
            Logger.getLogger(Resources.class.toString());

    HashMap fonts;
    HashMap xobjects;
    HashMap colorspaces;
    HashMap patterns;
    HashMap shading;
    HashMap extGStates;
    HashMap properties;

    /**
     * @param l
     * @param h
     */
    public Resources(Library l, HashMap h) {
        super(l, h);
        colorspaces = library.getDictionary(entries, COLORSPACE_KEY);
        fonts = library.getDictionary(entries, FONT_KEY);
        xobjects = library.getDictionary(entries, XOBJECT_KEY);
        patterns = library.getDictionary(entries, PATTERN_KEY);
        shading = library.getDictionary(entries, SHADING_KEY);
        extGStates = library.getDictionary(entries, EXTGSTATE_KEY);
        properties = library.getDictionary(entries, PROPERTIES_KEY);
    }


    /**
     * @param o
     * @return
     */
    public PColorSpace getColorSpace(Object o) {

        if (o == null) {
            return null;
        }

        Object tmp;
        // every resource has a color space entry and o can be tmp in it.
        if (colorspaces != null && colorspaces.get(o) != null) {
            tmp = colorspaces.get(o);
            PColorSpace cs = PColorSpace.getColorSpace(library, tmp);
            if (cs != null) {
                cs.init();
            }
            return cs;
        }
        // look for our name in the pattern dictionary
        if (patterns != null && patterns.get(o) != null) {
            tmp = patterns.get(o);
            PColorSpace cs = PColorSpace.getColorSpace(library, tmp);
            if (cs != null) {
                cs.init();
            }
            return cs;
        }

        // if its not in color spaces or pattern then its a plain old
        // named colour space.  
        PColorSpace cs = PColorSpace.getColorSpace(library, o);
        if (cs != null) {
            cs.init();
        }
        return cs;

    }

    /**
     * @param s
     * @return
     */
    public org.icepdf.core.pobjects.fonts.Font getFont(Name s) {
        org.icepdf.core.pobjects.fonts.Font font = null;
        if (fonts != null) {
            Object ob = fonts.get(s);
            // check to make sure the library contains a font
            if (ob instanceof org.icepdf.core.pobjects.fonts.Font) {
                font = (org.icepdf.core.pobjects.fonts.Font) ob;
            }
            // the default value is most likely Reference
            else if (ob instanceof Reference) {
                Reference ref = (Reference) ob;
                ob = library.getObject((Reference) ob);
                if (ob instanceof org.icepdf.core.pobjects.fonts.Font) {
                    font = (org.icepdf.core.pobjects.fonts.Font) ob;
                } else {
                    font = FontFactory.getInstance().getFont(library, (HashMap) ob);
                }
                // cache the font for later use.
                library.addObject(font, ref);
                font.setPObjectReference(ref);
            }
        }
        if (font != null) {
            font.setParentResource(this);
            font.init();
        }
        return font;
    }

    /**
     * @param s
     * @param fill
     * @return
     */
    public Image getImage(Name s, Color fill) {

        // check xobjects for stream
        ImageStream st = (ImageStream) library.getObject(xobjects, s);
        if (st == null) {
            return null;
        }
        // return null if the xobject is not an image
        if (!st.isImageSubtype()) {
            return null;
        }
        // lastly return the images.
        Image image = null;
        try {
            image = st.getImage(fill, this);
        } catch (Exception e) {
            logger.log(Level.FINE, "Error getting image by name: " + s, e);
        }
        return image;
    }

    public ImageStream getImageStream(Name s) {
        // check xobjects for stream
        Object st = library.getObject(xobjects, s);
        if (st instanceof ImageStream) {
            return (ImageStream) st;
        }
        return null;
    }

    /**
     * @param s
     * @return
     */
    public boolean isForm(Name s) {
        Object o = library.getObject(xobjects, s);
        return o instanceof Form;
    }

    /**
     * Gets the Form XObject specified by the named reference.
     *
     * @param nameReference name of resourse to retreive.
     * @return if the named reference is found return it, otherwise return null;
     */
    public Form getForm(Name nameReference) {
        Form formXObject = null;
        Object tempForm = library.getObject(xobjects, nameReference);
        if (tempForm instanceof Form) {
            formXObject = (Form) tempForm;
        }
        return formXObject;
    }

    /**
     * Retrieves a Pattern object given the named resource.  This can be
     * call for a fill, text fill or do image mask.
     *
     * @param name of object to find.
     * @return tiling or shading type pattern object.  If not constructor is
     *         found, then null is returned.
     */
    public Pattern getPattern(Name name) {
        if (patterns != null) {

            Object attribute = library.getObject(patterns, name);
            // An instance of TilingPattern will always have a stream
            if (attribute != null && attribute instanceof TilingPattern) {
                return (TilingPattern) attribute;
            } else if (attribute != null && attribute instanceof Stream) {
                return new TilingPattern((Stream) attribute);
            }
            // ShaddingPatterns will not have a stream but still need to parsed
            else if (attribute != null && attribute instanceof HashMap) {
                return ShadingPattern.getShadingPattern(library,
                        (HashMap) attribute);
            }
        }
        return null;
    }

    /**
     * Gets the shadding pattern based on a shading dictionary name,  similar
     * to getPattern but is only called for the 'sh' token.
     *
     * @param name name of shading dictionary
     * @return associated shading pattern if any.
     */
    public ShadingPattern getShading(Name name) {
        // look for pattern name in the shading dictionary, used by 'sh' tokens
        if (shading != null) {
            Object shadingDictionary = library.getObject(shading, name);
            if (shadingDictionary != null && shadingDictionary instanceof HashMap) {
                return ShadingPattern.getShadingPattern(library, entries,
                        (HashMap) shadingDictionary);
            }
//            else if (shadingDictionary != null && shadingDictionary instanceof Stream) {
//                System.out.println("Found Type 6 shading pattern.... returning empty pattern data. ");
            // todo: alter parser to take into account stream shading types...
//                return new ShadingType6Pattern(library, null);
//                return null;
//            }
        }
        return null;
    }

    /**
     * Returns the ExtGState object which has the specified reference name.
     *
     * @param namedReference name of ExtGState object to try and find.
     * @return ExtGState which contains the named references ExtGState attrbutes,
     *         if the namedReference could not be found null is returned.
     */
    public ExtGState getExtGState(Name namedReference) {
        ExtGState gsState = null;
        if (extGStates != null) {
            Object attribute = library.getObject(extGStates, namedReference);
            if (attribute instanceof HashMap) {
                gsState = new ExtGState(library, (HashMap) attribute);
            } else if (attribute instanceof Reference) {
                gsState = new ExtGState(library,
                        (HashMap) library.getObject(
                                (Reference) attribute));
            }
        }
        return gsState;

    }

    /**
     * Looks for the specified key in the Properties dictionary.  If the dictionary
     * and corresponding value is found the object is returned otherwise null.
     *
     * @param key key to find a value of in the Properties dictionary.
     * @return key value if found, null otherwise.
     */
    public OptionalContents getPropertyEntry(Name key) {
        if (properties != null) {
            return (OptionalContents) library.getObject(properties.get(key));
        }
        return null;
    }
}
