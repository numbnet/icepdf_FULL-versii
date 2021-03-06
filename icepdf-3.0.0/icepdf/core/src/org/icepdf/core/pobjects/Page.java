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

import org.icepdf.core.events.PaintPageEvent;
import org.icepdf.core.events.PaintPageListener;
import org.icepdf.core.io.SequenceInputStream;
import org.icepdf.core.pobjects.annotations.Annotation;
import org.icepdf.core.pobjects.graphics.Shapes;
import org.icepdf.core.util.ContentParser;
import org.icepdf.core.util.GraphicsRenderingHints;
import org.icepdf.core.util.Library;
import org.icepdf.core.util.MemoryManageable;
import org.icepdf.core.views.swing.PageViewComponentImpl;

import java.awt.*;
import java.awt.geom.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>This class represents the leaves of a <code>PageTree</code> object known
 * as <code>Page</code> class. The page dictionary specifies attributes
 * of the single page of the document.  Many of the page's attributes are
 * inherited from the page tree dictionary if not specified in the page
 * dictionary.</p>
 * <p/>
 * <p>The page object also provides a method which will extract a page's content,
 * such as text and images.  The <code>paint</code> method is the core of
 * the ICEpdf renderer, allowing page content to be painted to any Java graphics
 * context. </p>
 * <p/>
 * <p>Page objects in a PDF document have different boundaries defined which
 * govern various aspects of the pre-press process, such as cropping, bleed,
 * and trimming. Facilities for including printer's marks, such a registration
 * targets, gray ramps color bars, and cut marks which assist in the production
 * process.  When getting a page's size, the default boundary used is the crop
 * box which is what most viewer applications should use.  However, if your application
 * requires the use of different page boundaries, they can be specified when
 * using the getSize or paint methods.  If in doubt, always use the crop box
 * constant.</p>
 *
 * @see org.icepdf.core.pobjects.PageTree
 * @since 1.0
 */
public class Page extends Dictionary implements MemoryManageable {

    private static final Logger logger =
            Logger.getLogger(Page.class.toString());

    /**
     * Defines the boundaries of the physical medium on which the page is
     * intended to be displayed or printed.
     */
    public static final int BOUNDARY_MEDIABOX = 1;

    /**
     * Defines the visible region of the default user space. When the page
     * is displayed or printed, its contents are to be clipped to this
     * rectangle and then imposed on the output medium in some implementation
     * defined manner.
     */
    public static final int BOUNDARY_CROPBOX = 2;

    /**
     * Defines the region to which the contents of the page should be clipped
     * when output in a production environment (Mainly commercial printing).
     */
    public static final int BOUNDARY_BLEEDBOX = 3;

    /**
     * Defines the intended dimensions of the finished page after trimming.
     */
    public static final int BOUNDARY_TRIMBOX = 4;

    /**
     * Defines the extent of the page's meaningful content as intended by the
     * page's creator.
     */
    public static final int BOUNDARY_ARTBOX = 5;

    // Flag for call to init method, very simple cache
    private boolean isInited = false;
    private final Object isInitedLock = new Object();

    // resources for page's parent pages, default fonts, etc.
    private Resources resources;

    // Vector of annotations
    private Vector<Annotation> annotation;

    // Contents
    private Vector<Stream> contents;
    // Container for all shapes stored on page
    private Shapes shapes = null;

    // extracted text from page
    private Vector<StringBuffer> extractedText;

    // the collection of objects listening for page paint events
    private Vector<PaintPageListener> paintPageListeners = new Vector<PaintPageListener>();

    // Defines the boundaries of the physical medium on which the page is
    // intended to be displayed on.
    private PRectangle mediaBox;
    // Defining the visible region of default user space.
    private PRectangle cropBox;
    // Defines the region to which the contents of the page should be clipped
    // when output in a production environment.
    private PRectangle bleedBox;
    // Defines the intended dimension of the finished page after trimming.
    private PRectangle trimBox;
    // Defines the extent of the pages meaningful content as intended by the
    // pages creator.
    private PRectangle artBox;

    // page has default rotation value
    private float pageRotation = 0;

    /**
     * Create a new Page object.  A page object represents a PDF object that
     * has the name page associated with it.  It also conceptually represents
     * a page entity and all of it child elements that are associated with it.
     *
     * @param l pointer to default library containing all document objects
     * @param h hashtable containing all of the dictionary entries
     */
    public Page(Library l, Hashtable h) {
        super(l, h);
    }

    /**
     * Dispose the Page.
     *
     * @param cache if true, cached files are removed; otherwise, objects are freed
     *              but object caches are left intact.
     */
    protected void dispose(boolean cache) {
        // Do not null out Library library reference here, without taking
        //   into account that MemoryManager.releaseAllByLibrary(Library)
        //   requires Page to still have Library library in getLibrary()
        // dispose only if the pages has been initiated
        synchronized (isInitedLock) {
            if (isInited) {
                // un-init a page to free up memory
                isInited = false;
                // null data collections for page content
                if (annotation != null) {
                    annotation.clear();
                }
                // work through contents and null any stream that have images in them
                if (contents != null) {
                    Enumeration pageContent = contents.elements();
                    //System.out.println("   Content size " + contents.size());
                    while (pageContent.hasMoreElements()) {
                        Object tmp = pageContent.nextElement();
                        if (tmp instanceof Stream) {
                            //System.out.println("  -------------> Found stream");
                            Stream stream = (Stream) tmp;
                            stream.dispose(cache);
                        }
                    }
                    contents.clear();
                }

                // work through contents and null any stream that have images in them
                if (shapes != null) {
                    shapes.dispose();
                    shapes = null;
                }

                // clear extracted text
                if (extractedText != null) {
                    extractedText.clear();
                    extractedText = null;
                }

                // work through resources and null any images in the image hash
                if (resources != null) {
                    resources.dispose(cache);
                    resources = null;
                }
            }
            // clear vector of listeners
            if (paintPageListeners != null) {
                paintPageListeners.clear();
            }
        }
    }

    public boolean isInitiated() {
        return isInited;
    }

    private void initPageContents() {
        Object pageContent = library.getObject(entries, "Contents");

        // if a stream process it as needed
        if (pageContent instanceof Stream) {
            contents = new Vector<Stream>();
            Stream tmpStream = (Stream) pageContent;
            tmpStream.setPObjectReference(
                    library.getObjectReference(entries, "Contents"));
            contents.addElement(tmpStream);
        }
        // if a vector, process it as needed
        else if (pageContent instanceof Vector) {
            Vector conts = (Vector) pageContent;
            contents = new Vector<Stream>();
            // pull all of the page content references from the library
            for (int i = 0; i < conts.size(); i++) {
                Stream tmpStream = (Stream) library.getObject((Reference) conts.elementAt(i));
                tmpStream.setPObjectReference((Reference) conts.elementAt(i));
                contents.addElement(tmpStream);
            }
        }
    }

    private void initPageResources() {
        Resources res = library.getResources(entries, "Resources");
        if (res == null) {
            PageTree pt = getParent();
            while (pt != null) {
                Resources parentResources = pt.getResources();
                if (parentResources != null) {
                    res = parentResources;
                    break;
                }
                pt = pt.getParent();
            }
        }
        resources = res;
    }

    private void initPageAnnotations() {
        // find annotation in main library for our pages dictionary
        Object annots = library.getObject(entries, "Annots");
        if (annots != null && annots instanceof Vector) {
            Vector v = (Vector) annots;
            annotation = new Vector<Annotation>(v.size() + 1);
            // add annotation
            Object annotObj;
            org.icepdf.core.pobjects.annotations.Annotation a = null;
            for (int i = 0; i < v.size(); i++) {

                annotObj = v.elementAt(i);

                // we might have a reference
                if (annotObj instanceof Reference) {
                    Reference ref = (Reference) v.elementAt(i);
                    annotObj = library.getObject(ref);
                }

                // but most likely its an annotation base class
                if (annotObj instanceof Annotation)
                    a = (Annotation) annotObj;
                    // or build annotation from dictionary.
                else if (annotObj instanceof Hashtable) // Hashtable lacks "Type"->"Annot" entry
                    a = Annotation.buildAnnotation(library, (Hashtable) annotObj);

                // add any found annotations to the vector.
                annotation.addElement(a);
            }
        }
    }

    /**
     * Initialize the Page object.  This method triggers the parsing of a page's
     * child elements.  Once a page has been initialized, it can be painted.
     */
    public synchronized void init() {
        // make sure we are not revisiting this method
        if (isInited) {
            return;
        }
//try { throw new RuntimeException("Page.init() ****"); } catch(Exception e) { e.printStackTrace(); }

        synchronized (isInitedLock) {
            // get pages resources
            initPageResources();

            // annotations
            initPageAnnotations();

            // Get the value of the page's content entry
            initPageContents();

            /**
             * Finally iterate through the contents vector and concat all of the
             * the resourse streams together so that the content parser can
             * go to town and build all of the page's shapes.
             */

            if (contents != null) {
                Vector<InputStream> inputStreamsVec = new Vector<InputStream>(contents.size());
                for (int st = 0, max = contents.size(); st < max; st++) {
                    Stream stream = contents.elementAt(st);
                    InputStream input = stream.getInputStreamForDecodedStreamBytes();
                    //byte[] streamBytes = stream.getBytes();
                    //ByteArrayInputStream input = new ByteArrayInputStream(streamBytes);
                    inputStreamsVec.add(input);
/*
                    InputStream input = stream.getInputStreamForDecodedStreamBytes();
                    InputStream[] inArray = new InputStream[] { input };////
                    String content = Utils.getContentAndReplaceInputStream( inArray, false );
                    input = inArray[0];
                    System.out.println("Page.init()  Stream: " + stream);
                    System.out.println("Page.init()  Content: " + content);
*/
                }
                SequenceInputStream sis = new SequenceInputStream(inputStreamsVec.iterator());

                // push the library and resources to the content parse
                // and return the the shapes vector for the screen elements
                // for the page/resources in question.
                try {
                    ContentParser cp = new ContentParser(library, resources);
                    shapes = cp.parse(sis);
                }
                catch (Exception e) {
                    shapes = new Shapes();
                    logger.log(Level.FINE, "Error initializing Page.", e);
                }
                finally {
                    try {
                        sis.close();
                    }
                    catch (IOException e) {
                         logger.log(Level.FINE, "Error closing page stream.", e);
                    }
                }
            }
            // empty page
            else {
                shapes = new Shapes();
            }


            // set the initiated flag
            isInited = true;
        }
    }

    public void paint(Graphics g, int renderHintType, final int boundary,
                      float userRotation, float userZoom) {
        paint(g, renderHintType, boundary, userRotation, userZoom, null);
    }

    /**
     * Paints the contents of this page to the graphics context using
     * the specified rotation, zoom, rendering hints and page boundary.
     *
     * @param g              graphics context to which the page content will be painted.
     * @param renderHintType Constant specified by the GraphicsRenderingHints class.
     *                       There are two possible entries, SCREEN and PRINT, each with configurable
     *                       rendering hints settings.
     * @param boundary       Constant specifying the page boundary to use when
     *                       painting the page content.
     * @param userRotation   Rotation factor, in degrees, to be applied to the rendered page
     * @param userZoom       Zoom factor to be applied to the rendered page
     * @param pagePainter    class which will receive paint events.
     */
    public void paint(Graphics g, int renderHintType, final int boundary,
                      float userRotation, float userZoom, PageViewComponentImpl.PagePainter pagePainter) {
        paint(g, renderHintType, boundary, userRotation, userZoom, pagePainter, true);
    }

    /**
     * Paints the contents of this page to the graphics context using
     * the specified rotation, zoom, rendering hints and page boundary.
     *
     * @param g                graphics context to which the page content will be painted.
     * @param renderHintType   Constant specified by the GraphicsRenderingHints class.
     *                         There are two possible entries, SCREEN and PRINT, each with configurable
     *                         rendering hints settings.
     * @param boundary         Constant specifying the page boundary to use when
     *                         painting the page content.
     * @param userRotation     Rotation factor, in degrees, to be applied to the rendered page
     * @param userZoom         Zoom factor to be applied to the rendered page
     * @param pagePainter      class which will receive paint events.
     * @param paintAnnotations true enables the painting of page annotations.  False
     *                         paints no annotaitons for a given page.
     */
    public void paint(Graphics g, int renderHintType, final int boundary,
                      float userRotation, float userZoom,
                      PageViewComponentImpl.PagePainter pagePainter, boolean paintAnnotations) {
        if (!isInited) {
            init();
        }

        Graphics2D g2 = (Graphics2D) g;
        GraphicsRenderingHints grh = GraphicsRenderingHints.getDefault();
        g2.setRenderingHints(grh.getRenderingHints(renderHintType));

        AffineTransform at = getPageTransform(boundary, userRotation, userZoom);
        g2.transform(at);

        PRectangle pageBoundary = getPageBoundary(boundary);
        float x = 0 - pageBoundary.x;
        float y = 0 - (pageBoundary.y - pageBoundary.height);

        // Draw the (typically white) background
        Color backgroundColor = grh.getPageBackgroundColor(renderHintType);
        if (backgroundColor != null) {
            g2.setColor(backgroundColor);
            g2.fillRect((int) (0 - x),
                    (int) (0 - y),
                    (int) pageBoundary.width,
                    (int) pageBoundary.height);
        }

        // We have to impose a page clip because some documents don't separate
        //  pages into separate Page objects, but instead reuse the Page object,
        //  but with a different clip
        // And we can't stomp over the clip, because the PageView might be
        //  trying to only draw a portion of the page for performance, or
        //  other reasons
        Rectangle2D rect = new Rectangle2D.Double(-x, -y, pageBoundary.width, pageBoundary.height);
        Shape oldClip = g2.getClip();
        if (oldClip == null) {
            g2.setClip(rect);
        } else {
            Area area = new Area(oldClip);
            area.intersect(new Area(rect));
            g2.setClip(area);
        }

        // draw page content
        if (shapes != null) {

            AffineTransform pageTransform = g2.getTransform();
            Shape pageClip = g2.getClip();

            shapes.setPageParent(this);
            shapes.paint(g2, pagePainter);
            shapes.setPageParent(null);

            g2.setTransform(pageTransform);
            g2.setClip(pageClip);
        }
        // paint annotation if available and desired.
        if (annotation != null && paintAnnotations) {
            float totalRotation = getTotalRotation(userRotation);
            int num = annotation.size();
            for (int i = 0; i < num; i++) {
                Annotation annot = annotation.get(i);
                annot.render(g2, renderHintType, totalRotation, userZoom, false);
            }
        }

        // one last repaint, just to be sure
        notifyPaintPageListeners();

    }

    /**
     * The Java Graphics coordinate system has the origin at the top-left
     * of the screen, with Y values increasing as one moves down the screen.
     * The PDF coordinate system has the origin at the bottom-left of the
     * document, with Y values increasing as one moved up the document.
     * As well, PDFs can be displayed both rotated and zoomed.
     * This method gives an AffineTransform which can be passed to
     * java.awt.Graphics2D.transform(AffineTransform) so that one can then
     * use that Graphics2D in the user-perspectived PDF coordinate space.
     *
     * @param boundary     Constant specifying the page boundary to use when
     *                     painting the page content.
     * @param userRotation Rotation factor, in degrees, to be applied to the rendered page
     * @param userZoom     Zoom factor to be applied to the rendered page
     * @return AffineTransform for translating from the rotated and zoomed PDF
     *         coordinate system to the Java Graphics coordinate system
     */
    public AffineTransform getPageTransform(final int boundary,
                                            float userRotation,
                                            float userZoom) {
        AffineTransform at = new AffineTransform();

        Rectangle2D.Double boundingBox = getBoundingBox(boundary, userRotation, userZoom);
        at.translate(0, boundingBox.getHeight());

        // setup canvas for PDF document orientation
        at.scale(1, -1);

        at.scale(userZoom, userZoom);

        float totalRotation = getTotalRotation(userRotation);
        PRectangle pageBoundary = getPageBoundary(boundary);

        if (totalRotation == 0) {
        } else if (totalRotation == 90) {
            at.translate(pageBoundary.height, 0);
        } else if (totalRotation == 180) {
            at.translate(pageBoundary.width, pageBoundary.height);
        } else if (totalRotation == 270) {
            at.translate(0, pageBoundary.width);
        } else {
            if (totalRotation > 0 && totalRotation < 90) {
                double xShift = pageBoundary.height * Math.cos(Math.toRadians(90 - totalRotation));
                at.translate(xShift, 0);
            } else if (totalRotation > 90 && totalRotation < 180) {
                double rad = Math.toRadians(180 - totalRotation);
                double cosRad = Math.cos(rad);
                double sinRad = Math.sin(rad);
                double xShift = pageBoundary.height * sinRad + pageBoundary.width * cosRad;
                double yShift = pageBoundary.height * cosRad;
                at.translate(xShift, yShift);
            } else if (totalRotation > 180 && totalRotation < 270) {
                double rad = Math.toRadians(totalRotation - 180);
                double cosRad = Math.cos(rad);
                double sinRad = Math.sin(rad);
                double xShift = pageBoundary.width * cosRad;
                double yShift = pageBoundary.width * sinRad + pageBoundary.height * cosRad;
                at.translate(xShift, yShift);
            } else if (totalRotation > 270 && totalRotation < 360) {
                double yShift = pageBoundary.width * Math.cos(Math.toRadians(totalRotation - 270));
                at.translate(0, yShift);
            }
        }

        // apply rotation on canvas, convert to Radians
        at.rotate(totalRotation * Math.PI / 180.0);

        // translate crop lower left corner back to where media box corner was
        float x = 0 - pageBoundary.x;
        float y = 0 - (pageBoundary.y - pageBoundary.height);
        at.translate(x, y);

        return at;
    }

    /**
     * This method returns a Shape that represents the outline of this Page,
     * after being rotated and zoomed.  It is used for clipping, and drawing
     * borders around the page rendering onscreen.
     *
     * @param boundary     Constant specifying the page boundary to use
     * @param userRotation Rotation factor, in degrees, to be applied
     * @param userZoom     Zoom factor to be applied
     * @return Shape outline of the rotated and zoomed portion of this Page
     *         corresponding to the specified boundary
     */
    public Shape getPageShape(int boundary, float userRotation, float userZoom) {
        AffineTransform at = getPageTransform(boundary, userRotation, userZoom);
        PRectangle pageBoundary = getPageBoundary(boundary);
        float x = 0 - pageBoundary.x;
        float y = 0 - (pageBoundary.y - pageBoundary.height);
        Rectangle2D rect = new Rectangle2D.Double(-x, -y, pageBoundary.width, pageBoundary.height);
        GeneralPath path = new GeneralPath(rect);
        return path.createTransformedShape(at);
    }

    /**
     * Gets a reference to the page's parent page tree.  A reference can be resolved
     * by the Library class.
     *
     * @return reference to parent page tree.
     * @see org.icepdf.core.util.Library
     */
    protected Reference getParentReference() {
        return (Reference) entries.get("Parent");
    }

    /**
     * Gets the page's parent page tree.
     *
     * @return parent page tree.
     */
    public PageTree getParent() {
        // retrieve a pointer to the pageTreeParent
        return (PageTree) library.getObject(entries, "Parent");
    }

    /**
     * Get the width and height that the page can occupy, given the userRotation,
     * page's own pageRotation and cropBox boundary. The page's default zoom of
     * 1.0f is used.
     *
     * @param userRotation Rotation factor specified by the user under which the
     *                     page will be rotated.
     * @return Dimension of width and height of the page represented in point
     *         units.
     * @see #getSize(float, float)
     */
    public PDimension getSize(float userRotation) {
        return getSize(BOUNDARY_CROPBOX, userRotation, 1.0f);
    }

    /**
     * Get the width and height that the page can occupy, given the userRotation,
     * userZoom, page's own pageRotation and cropBox boundary.
     *
     * @param userRotation rotation factor specified by the user under which the
     *                     page will be rotated.
     * @param userZoom     zoom factor specifed by the user under which the page will
     *                     be rotated.
     * @return Dimension of width and height of the page represented in point units.
     */
    public PDimension getSize(float userRotation, float userZoom) {
        return getSize(BOUNDARY_CROPBOX, userRotation, userZoom);
    }

    /**
     * Get the width and height that the page can occupy, given the userRotation,
     * userZoom, page's own pageRotation and cropBox boundary.
     *
     * @param boundary     boundary constant to specify which boundary to respect when
     *                     calculating the page's size.
     * @param userRotation rotation factor specified by the user under which the
     *                     page will be rotated.
     * @param userZoom     zoom factor specified by the user under which the page will
     *                     be rotated.
     * @return Dimension of width and height of the page represented in point units.
     */
    public PDimension getSize(final int boundary, float userRotation, float userZoom) {
        float totalRotation = getTotalRotation(userRotation);
        PRectangle pageBoundary = getPageBoundary(boundary);
        float width = pageBoundary.width * userZoom;
        float height = pageBoundary.height * userZoom;
        // No rotation, or flipped upside down
        if (totalRotation == 0 || totalRotation == 180) {
            // Do nothing
        }
        // Rotated sideways
        else if (totalRotation == 90 || totalRotation == 270) {
            float temp = width;
            width = height;
            height = temp;
        }
        // Arbitrary rotation
        else {
            AffineTransform at = new AffineTransform();
            double radians = Math.toRadians(totalRotation);
            at.rotate(radians);
            Rectangle2D.Double boundingBox = new Rectangle2D.Double(0.0, 0.0, 0.0, 0.0);
            Point2D.Double src = new Point2D.Double();
            Point2D.Double dst = new Point2D.Double();
            src.setLocation(0.0, height);    // Top left
            at.transform(src, dst);
            boundingBox.add(dst);
            src.setLocation(width, height);  // Top right
            at.transform(src, dst);
            boundingBox.add(dst);
            src.setLocation(0.0, 0.0);       // Bottom left
            at.transform(src, dst);
            boundingBox.add(dst);
            src.setLocation(width, 0.0);     // Bottom right
            at.transform(src, dst);
            boundingBox.add(dst);
            width = (float) boundingBox.getWidth();
            height = (float) boundingBox.getHeight();
        }
        return new PDimension(width, height);
    }

    /**
     * Get the bounding box that the page can occupy, given the userRotation and
     * page's own pageRotation. The boundary of BOUNDARY_CROPBOX, and the default
     * zoom of 1.0f are assumed.
     *
     * @param userRotation Rotation factor specified by the user under which the
     *                     page will be rotated.
     * @return Dimension of width and height of the page represented in point
     *         units.
     * @see #getSize(float, float)
     */
    public Rectangle2D.Double getBoundingBox(float userRotation) {
        return getBoundingBox(BOUNDARY_CROPBOX, userRotation, 1.0f);
    }

    /**
     * Get the bounding box that the page can occupy, given the userRotation,
     * userZoom, page's own pageRotation.
     *
     * @param userRotation rotation factor specified by the user under which the
     *                     page will be rotated.
     * @param userZoom     zoom factor specified by the user under which the page will
     *                     be rotated.
     * @return Rectangle encompassing the page represented in point units.
     */
    public Rectangle2D.Double getBoundingBox(float userRotation, float userZoom) {
        return getBoundingBox(BOUNDARY_CROPBOX, userRotation, userZoom);
    }

    /**
     * Get the bounding box that the page can occupy, given the userRotation,
     * userZoom, page's own pageRotation and cropBox boundary.
     *
     * @param boundary     boundary constant to specify which boundary to respect when
     *                     calculating the page's size.
     * @param userRotation rotation factor specified by the user under which the
     *                     page will be rotated.
     * @param userZoom     zoom factor specified by the user under which the page will
     *                     be rotated.
     * @return Rectangle encompassing the page represented in point units.
     */
    public Rectangle2D.Double getBoundingBox(final int boundary, float userRotation, float userZoom) {
        float totalRotation = getTotalRotation(userRotation);
        PRectangle pageBoundary = getPageBoundary(boundary);
        float width = pageBoundary.width * userZoom;
        float height = pageBoundary.height * userZoom;

        AffineTransform at = new AffineTransform();
        double radians = Math.toRadians(totalRotation);
        at.rotate(radians);
        Rectangle2D.Double boundingBox = new Rectangle2D.Double(0.0, 0.0, 0.0, 0.0);
        Point2D.Double src = new Point2D.Double();
        Point2D.Double dst = new Point2D.Double();
        src.setLocation(0.0, height);    // Top left
        at.transform(src, dst);
        boundingBox.add(dst);
        src.setLocation(width, height);  // Top right
        at.transform(src, dst);
        boundingBox.add(dst);
        src.setLocation(0.0, 0.0);       // Bottom left
        at.transform(src, dst);
        boundingBox.add(dst);
        src.setLocation(width, 0.0);     // Bottom right
        at.transform(src, dst);
        boundingBox.add(dst);

        return boundingBox;
    }

    /**
     * Utility method for appling the page boundary rules.
     *
     * @param specifiedBox page boundary constant
     * @return bounds of page after the chain of rules have been applied.
     */
    public PRectangle getPageBoundary(final int specifiedBox) {
        PRectangle userSpecifiedBox = null;
        // required property
        if (specifiedBox == BOUNDARY_MEDIABOX) {
            userSpecifiedBox = (PRectangle) getMediaBox();
        }
        // required property
        else if (specifiedBox == BOUNDARY_CROPBOX) {
            userSpecifiedBox = (PRectangle) getCropBox();
        }
        // optional, default value is crop box
        else if (specifiedBox == BOUNDARY_BLEEDBOX) {
            if (bleedBox != null)
                userSpecifiedBox = (PRectangle) getBleedBox();
        }
        // optional, default value is crop box
        else if (specifiedBox == BOUNDARY_TRIMBOX) {
            if (trimBox != null)
                userSpecifiedBox = (PRectangle) getTrimBox();
        }
        // optional, default value is crop box
        else if (specifiedBox == BOUNDARY_ARTBOX) {
            if (artBox != null)
                userSpecifiedBox = (PRectangle) getArtBox();
        }
        // encase of bad usage, default to crop box
        else {
            userSpecifiedBox = cropBox;
        }

        // just in case, make sure we return a non null boundary
        if (userSpecifiedBox == null) {
            userSpecifiedBox = (PRectangle) getCropBox();
        }

        return userSpecifiedBox;
    }

    /**
     * Returns a summary of the page dictionary entries.
     *
     * @return dictionary entries.
     */
    public String toString() {
        return "PAGE= " + entries.toString();
    }

    /**
     * Gets the total rotation factor of the page after applying a user rotation
     * factor.  This method will normalize rotation factors to be in the range
     * of 0 to 360 degrees.
     *
     * @param userRotation rotation factor to be applied to page
     * @return Total Rotation, representing pageRoation + user rotation
     *         factor applied to the whole document.
     */
    public float getTotalRotation(float userRotation) {
        float totalRotation = getPageRotation() + userRotation;

        // correct to keep in rotation in 360 range.
        totalRotation %= 360;

        if (totalRotation < 0)
            totalRotation += 360;

        // If they calculated the degrees from radians or whatever,
        // then we need to make our even rotation comparisons work
        if (totalRotation >= -0.001f && totalRotation <= 0.001f)
            return 0.0f;
        else if (totalRotation >= 89.99f && totalRotation <= 90.001f)
            return 90.0f;
        else if (totalRotation >= 179.99f && totalRotation <= 180.001f)
            return 180.0f;
        else if (totalRotation >= 269.99f && totalRotation <= 270.001f)
            return 270.0f;

        return totalRotation;
    }

    private float getPageRotation() {
        // Get the pages default orientation if available, if not defined
        // then it is zero.
        Object tmpRotation = library.getObject(entries, "Rotate");
        if (tmpRotation != null) {
            pageRotation = ((Number) tmpRotation).floatValue();
//            System.out.println("Page Rotation  " + pageRotation);
        }
        // check parent to see if value has been set
        else {
            PageTree pageTree = getParent();
            while (pageTree != null) {
                if (pageTree.isRotationFactor) {
                    pageRotation = pageTree.rotationFactor;
                    break;
                }
                pageTree = pageTree.getParent();
            }
        }
        // PDF specifies rotation as clockwise, but Java2D does it
        //  counter-clockwise, so normalise it to Java2D
        pageRotation = 360 - pageRotation;
        pageRotation %= 360;
//        System.out.println("New Page Rotation " + pageRotation);
        return pageRotation;
    }

    /**
     * Gets all annotation information associated with this page.  Each entry
     * in the vector represents one annotation. The size of the vector represents
     * the total number of annotations associated with the page.
     *
     * @return annotation associated with page; null, if there are no annotations.
     */
    public Vector getAnnotations() {
        if (!isInited) {
            init();
        }
        return annotation;
    }

    /**
     * Gets the media box boundary defined by this page.  The media box is a
     * required page entry and can be inherited from its parent page tree.
     *
     * @return media box boundary in user space units.
     */
    public Rectangle2D.Float getMediaBox() {
        // add all of the pages media box dimensions to a vector and process
        Vector boxDimensions = (Vector) (library.getObject(entries, "MediaBox"));
        if (boxDimensions != null) {
            mediaBox = new PRectangle(boxDimensions);
//            System.out.println("Page - MediaBox " + mediaBox);
        }
        // If mediaBox is null check with the parent pages, as media box is inheritable
        if (mediaBox == null) {
            PageTree pageTree = getParent();
            while (pageTree != null && mediaBox == null) {
                mediaBox = pageTree.getMediaBox();
                pageTree = pageTree.getParent();
            }
        }
        return mediaBox;
    }

    /**
     * Gets the crop box boundary defined by this page.  The media box is a
     * required page entry and can be inherited from its parent page tree.
     *
     * @return crop box boundary in user space units.
     */
    public Rectangle2D.Float getCropBox() {
        // add all of the pages crop box dimensions to a vector and process
        Vector boxDimensions = (Vector) (library.getObject(entries, "CropBox"));
        if (boxDimensions != null) {
            cropBox = new PRectangle(boxDimensions);
//            System.out.println("Page - CropBox " + cropBox);
        }
        // If mediaBox is null check with the parent pages, as media box is inheritable
        if (cropBox == null) {
            PageTree pageTree = getParent();
            while (pageTree != null && cropBox == null) {
                if (pageTree.getCropBox() == null) {
                    break;
                }
                cropBox = pageTree.getCropBox();
                pageTree = pageTree.getParent();
            }
        }
        // Default value of the cropBox is the MediaBox if not set implicitly
        PRectangle mediaBox = (PRectangle) getMediaBox();
        if (cropBox == null && mediaBox != null) {
            cropBox = (PRectangle) mediaBox.clone();
        } else if (mediaBox != null) {
            // PDF 1.5 spec states that the media box should be intersected with the
            // crop box to get the new box. But we only want to do this if the
            // cropBox is not the same as the mediaBox
            cropBox = mediaBox.createCartesianIntersection(cropBox);
        }
        return cropBox;
    }

    /**
     * Gets the art box boundary defined by this page.  The art box is a
     * required page entry and can be inherited from its parent page tree.
     *
     * @return art box boundary in user space units.
     */
    public Rectangle2D.Float getArtBox() {
        // get the art box vector value
        Vector boxDimensions = (Vector) (library.getObject(entries, "ArtBox"));
        if (boxDimensions != null) {
            artBox = new PRectangle(boxDimensions);
//            System.out.println("Page - ArtBox " + artBox);
        }
        // Default value of the artBox is the bleed if not set implicitly
        if (artBox == null) {
            artBox = (PRectangle) getCropBox();
        }
        return artBox;
    }

    /**
     * Gets the bleed box boundary defined by this page.  The bleed box is a
     * required page entry and can be inherited from its parent page tree.
     *
     * @return bleed box boundary in user space units.
     */
    public Rectangle2D.Float getBleedBox() {
        // get the art box vector value
        Vector boxDimensions = (Vector) (library.getObject(entries, "BleedBox"));
        if (boxDimensions != null) {
            bleedBox = new PRectangle(boxDimensions);
//            System.out.println("Page - BleedBox " + bleedBox);
        }
        // Default value of the bleedBox is the bleed if not set implicitly
        if (bleedBox == null) {
            bleedBox = (PRectangle) getCropBox();
        }
        return bleedBox;
    }

    /**
     * Gets the trim box boundary defined by this page.  The trim box is a
     * required page entry and can be inherited from its parent page tree.
     *
     * @return trim box boundary in user space units.
     */
    public Rectangle2D.Float getTrimBox() {
        // get the art box vector value
        Vector boxDimensions = (Vector) (library.getObject(entries, "TrimBox"));
        if (boxDimensions != null) {
            trimBox = new PRectangle(boxDimensions);
//            System.out.println("Page - TrimBox " + trimBox);
        }
        // Default value of the trimBox is the bleed if not set implicitly
        if (trimBox == null) {
            trimBox = (PRectangle) getCropBox();
        }
        return trimBox;
    }

    /**
     * Gets a vector of Strings where each index represents a text block inside
     * this page's content streams.  Due to limitations in how PDF documents are encoded,
     * it is not always possible to extract valid ASCII or unicode text and order
     * is not always guaranteed.
     *
     * @return vector of Strings of all text objects inside the specified page.
     */
    public Vector<StringBuffer> getText() {

        // we only do this once per page
        if (extractedText != null) {
            return extractedText;
        }

        /**
         * Finally iterate through the contents vector and concat all of the
         * the resouse streams together so that the contant parser can
         * go to town and build all of the pages shapes.
         */
        if (contents == null) {
            // Get the value of the page's content entry
            initPageContents();
        }

        if (resources == null) {
            // get pages resources
            initPageResources();
        }

        if (contents != null) {
            Vector<InputStream> inputStreamsVec =
                    new Vector<InputStream>(contents.size());
            for (int st = 0, max = contents.size(); st < max; st++) {
                Stream stream = contents.elementAt(st);
                InputStream input = stream.getInputStreamForDecodedStreamBytes();
                inputStreamsVec.add(input);
            }
            SequenceInputStream sis = new SequenceInputStream(inputStreamsVec.iterator());

            // push the library and resources to the content parse
            // and return the the shapes vector for the screen elements
            // for the page/resources in question.
            try {
                ContentParser cp = new ContentParser(library, resources);
                // custom parsing for text extraction, should be faster
                extractedText = cp.parseTextBlocks(sis);
            }
            catch (Exception e) {
                logger.log(Level.FINE, "Error getting page text.", e);
            }
            finally {
                try {
                    sis.close();
                }
                catch (IOException e) {
                    logger.log(Level.FINE, "Error closing page stream.", e);
                }
            }
        }
        return extractedText;
    }

    /**
     * Gets a vector of Images where each index represents an image  inside
     * this page.
     *
     * @return vector of Images inside the current page
     */
    public Vector getImages() {
        if (!isInited) {
            init();
        }
        return shapes.getImages();
    }

    public Resources getResources() {
        return resources;
    }

    /**
     * Reduces the amount of memory used by this object.
     */
    public void reduceMemory() {
        dispose(true);
    }

    public synchronized void addPaintPageListener(PaintPageListener listener) {
        // add a listener if it is not already registered
        if (!paintPageListeners.contains(listener)) {
            paintPageListeners.addElement(listener);
        }
    }

    public synchronized void removePaintPageListener(PaintPageListener listener) {
        // add a listener if it is not already registered
        if (paintPageListeners.contains(listener)) {
            paintPageListeners.removeElement(listener);
        }
    }

    public void notifyPaintPageListeners() {
        // create the event object
        PaintPageEvent evt = new PaintPageEvent(this);

        // make a copy of the listener object vector so that it cannot
        // be changed while we are firing events
        // NOTE: this is good practise, but most likely a little to heavy
        //       for this event type
//        Vector v;
//        synchronized (this) {
//            v = (Vector) paintPageListeners.clone();
//        }
//
//        // fire the event to all listeners
//        PaintPageListener client;
//        for (int i = v.size() - 1; i >= 0; i--) {
//            client = (PaintPageListener) v.elementAt(i);
//            client.paintPage(evt);
//        }

        // fire the event to all listeners
        PaintPageListener client;
        for (int i = paintPageListeners.size() - 1; i >= 0; i--) {
            client = paintPageListeners.elementAt(i);
            client.paintPage(evt);
        }
    }
}
