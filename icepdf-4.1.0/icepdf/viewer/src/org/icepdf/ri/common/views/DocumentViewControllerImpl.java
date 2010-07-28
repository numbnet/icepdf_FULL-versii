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
package org.icepdf.ri.common.views;

import org.icepdf.core.AnnotationCallback;
import org.icepdf.core.Controller;
import org.icepdf.core.SecurityCallback;
import org.icepdf.core.pobjects.Destination;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.PageTree;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.pobjects.annotations.AnnotationState;
import org.icepdf.core.search.DocumentSearchController;
import org.icepdf.core.util.ColorUtil;
import org.icepdf.core.util.Defs;
import org.icepdf.core.util.PropertyConstants;
import org.icepdf.core.views.DocumentView;
import org.icepdf.core.views.DocumentViewController;
import org.icepdf.core.views.DocumentViewModel;
import org.icepdf.core.views.PageViewComponent;
import org.icepdf.core.views.swing.AbstractPageViewComponent;
import org.icepdf.core.views.swing.AnnotationComponentImpl;
import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.images.Images;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>The DocumentViewControllerImpl is responsible for controlling the four
 * default view models specified by the PDF specification.  This class is used
 * associated with the SwingController, but all view specific control is passed
 * to this class. </p>
 *
 * @since 2.5
 */
public class DocumentViewControllerImpl
        implements DocumentViewController, ComponentListener {

    private static final Logger logger =
            Logger.getLogger(DocumentViewControllerImpl.class.toString());

    /**
     * Displays a one page at a time view.
     */
    public static final int ONE_PAGE_VIEW = 1;
    /**
     * Displays a the pages in one column.
     */
    public static final int ONE_COLUMN_VIEW = 2;

    /**
     * Displays the pages two at a time, with odd-numbered pages on the left.
     */
    public static final int TWO_PAGE_LEFT_VIEW = 3;
    /**
     * Displays the pages in two columns, with odd-numbered pages on the left.
     */
    public static final int TWO_COLUMN_LEFT_VIEW = 4;

    /**
     * Displays the pages two at a time, with event-numbered pages on the left.
     */
    public static final int TWO_PAGE_RIGHT_VIEW = 5;
    /**
     * Displays the pages in two columns, with even-numbered pages on the left.
     */
    public static final int TWO_COLUMN_RIGHT_VIEW = 6;

    /**
     * Zoom factor used when zooming in or out.
     */
    public static final float ZOOM_FACTOR = 1.2F;

    /**
     * Rotation factor used with rotating document.
     */
    public static final float ROTATION_FACTOR = 90F;

    // background colour
    public static Color backgroundColor;

    static {
        // sets the shadow colour of the decorator.
        try {
            String color = Defs.sysProperty(
                    "org.icepdf.core.views.background.color", "#808080");
            int colorValue = ColorUtil.convertColor(color);
            backgroundColor =
                    new Color(colorValue > 0 ? colorValue :
                            Integer.parseInt("808080", 16));
        } catch (NumberFormatException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.warning("Error reading page shadow colour");
            }
        }
    }

    private float[] zoomLevels;

    private Document document;

    private DocumentViewModelImpl documentViewModel;
    private AbstractDocumentView documentView;

    private JScrollPane documentViewScrollPane;

    protected int viewportWidth, oldViewportWidth;
    protected int viewportHeight, oldViewportHeight;
    protected int viewType, oldViewType;

    protected int viewportFitMode, oldViewportFitMode;

    protected SwingController viewerController;

    protected AnnotationCallback annotationCallback;
    protected SecurityCallback securityCallback;

    protected PropertyChangeSupport changes = new PropertyChangeSupport(this);


    public DocumentViewControllerImpl(final SwingController viewerController) {

        this.viewerController = viewerController;

        documentViewScrollPane = new JScrollPane();
        documentViewScrollPane.getViewport().setBackground(backgroundColor);

        // set scroll bar speeds
        documentViewScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        documentViewScrollPane.getHorizontalScrollBar().setUnitIncrement(20);

        // add a delete key functionality for annotation edits.
        Action deleteAnnotation = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (documentViewModel != null){
                    deleteCurrentAnnotation();
                    viewerController.reflectUndoCommands();
                }
            }
        };
        InputMap inputMap = documentViewScrollPane.getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke("DELETE"),
                                    "removeSelecteAnnotation");
        documentViewScrollPane.getActionMap().put("removeSelecteAnnotation",
                                     deleteAnnotation);
    }

    public Document getDocument() {
        return document;
    }


    public void setDocument(Document newDocument) {
        // clean up any previous documents
        if (document != null) {
            // parent should dispose, we just want to break reference
            document = null;
        }
        document = newDocument;

        // clean up old document model and create a new one
        if (documentViewModel != null) {
            documentViewModel.dispose();
            documentViewModel = null;
        }
        documentViewModel = new DocumentViewModelImpl(document, documentViewScrollPane);

        // setup view type
        setViewType();

        // remove re-size listener.
        documentViewScrollPane.addComponentListener(this);
        documentViewScrollPane.validate();
    }

    // we should be resetting some view settings, mainly zoom, rotation, tool and current page
    // Also, null document but do not dispose, this is the responsibility of Controller, we might
    // want to inject another document to view.
    public void closeDocument() {

        // remove re-size listener.
        documentViewScrollPane.removeComponentListener(this);

        // dispose the view
        if (documentView != null) {
            documentViewScrollPane.remove(documentView);
            documentView.dispose();
            documentView = null;
        }

        // close current document
        if (documentViewModel != null) {
            documentViewModel.dispose();
            documentViewModel = null;
        }


//        setFitMode(PAGE_FIT_NONE);
        setCurrentPageIndex(0);
        setZoom(1);
        setRotation(0);
//        setToolMode(DocumentViewModelImpl.DISPLAY_TOOL_NONE);
        setViewCursor(DocumentViewControllerImpl.CURSOR_DEFAULT);

    }

    public Adjustable getHorizontalScrollBar() {
        return documentViewScrollPane.getHorizontalScrollBar();
    }

    public Adjustable getVerticalScrollBar() {
        return documentViewScrollPane.getVerticalScrollBar();
    }

    /**
     * Set an annotation callback.
     *
     * @param annotationCallback annotation callback associated with this document
     *                           view.
     */
    public void setAnnotationCallback(AnnotationCallback annotationCallback) {
        this.annotationCallback = annotationCallback;
    }

    public void setSecurityCallback(SecurityCallback securityCallback){
        this.securityCallback = securityCallback;
    }

    public void clearSelectedAnnotations(){
        if (documentViewModel.getCurrentAnnotation() != null){
            documentViewModel.getCurrentAnnotation().setSelected(false);
            // fire change event
            firePropertyChange(PropertyConstants.ANNOTATION_DESELECTED,
                        documentViewModel.getCurrentAnnotation(),
                        null);
            documentViewModel.setCurrentAnnotation(null);
        }
    }

    public void assignSelectedAnnotation(AnnotationComponentImpl annotationComponent){
        firePropertyChange(PropertyConstants.ANNOTATION_SELECTED,
                documentViewModel.getCurrentAnnotation(),
                annotationComponent);
        documentViewModel.setCurrentAnnotation(annotationComponent);
    }

    /**
     * Clear selected text in all pages that make up the current document
     */
    public void clearSelectedText() {
        ArrayList<WeakReference<AbstractPageViewComponent>> selectedPages =
                documentViewModel.getSelectedPageText();
        documentViewModel.setSelectAll(false);
        if (selectedPages != null &&
                selectedPages.size() > 0) {
            for (WeakReference<AbstractPageViewComponent> page : selectedPages) {
                PageViewComponent pageComp = page.get();
                if (pageComp != null) {
                    pageComp.clearSelectedText();
                }
            }
            selectedPages.clear();
            documentView.repaint();
        }
        // fire property change
        firePropertyChange(PropertyConstants.TEXT_DESELECTED,
                    null,
                    null);

    }

    /**
     * Clear highlighted text in all pages that make up the current document
     */
    public void clearHighlightedText() {
        DocumentSearchController searchController =
                viewerController.getDocumentSearchController();
        searchController.clearAllSearchHighlight();
        documentView.repaint();
    }

    /**
     * Sets the selectall status flag as true.  Text selection requires that
     * a pages content has been parsed and can be quite expensive for long
     * documents. The page component will pick up on this plag and paint the
     * selected state.  If the content is copied to the clipboard we go
     * thought he motion of parsing every page.
     */
    public void selectAllText() {
        documentViewModel.setSelectAll(true);
        documentView.repaint();
        firePropertyChange(PropertyConstants.TEXT_SELECT_ALL, null,null);
    }

    public String getSelectedText() {

        StringBuilder selectedText = new StringBuilder();
        // regular page selected by user mouse, keyboard or api
        if (!documentViewModel.isSelectAll()) {
            ArrayList<WeakReference<AbstractPageViewComponent>> selectedPages =
                    documentViewModel.getSelectedPageText();
            if (selectedPages != null &&
                    selectedPages.size() > 0) {
                for (WeakReference<AbstractPageViewComponent> page : selectedPages) {
                    AbstractPageViewComponent pageComp = page.get();
                    if (pageComp != null) {
                        int pageIndex = pageComp.getPageIndex();
                        selectedText.append(document.getPageText(pageIndex).getSelected());
                    }
                }
            }
        }
        // select all text
        else {
            Document document = documentViewModel.getDocument();
            // iterate over each page in the document
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                selectedText.append(viewerController.getDocument().getPageText(i));
            }
        }
        return selectedText.toString();
    }

    /**
     * Gets the annotation callback.
     *
     * @return annotation callback associated with this document.
     */
    public AnnotationCallback getAnnotationCallback() {
        return annotationCallback;
    }

    /**
     * Gets the security callback.
     *
     * @return security callback associated with this document.
     */
    public SecurityCallback getSecurityCallback(){
        return securityCallback;
    }

    public DocumentView getDocumentView() {
        return documentView;
    }

    public synchronized void setViewKeyListener(KeyListener l) {
        if (documentView != null)
            documentView.addKeyListener(l);
    }

    public void setDestinationTarget(Destination destination) {

        if (documentView == null || documentViewModel == null) {
            return;
        }

        if (destination == null) {
            return;
        }

        // get the page number associated with the destination
        int pageNumber = getPageTree().getPageNumber(destination.getPageReference());
        if (pageNumber < 0) {
            return;
        }

        // ready our view port for manipulation
        JViewport documentViewport = (documentViewScrollPane != null) ?
                documentViewScrollPane.getViewport() : null;

        if (documentViewport != null) {

            // get location of page in document view
            Rectangle pageBounds = documentViewModel.getPageBounds(pageNumber);

            // Only apply destination if rotation is 0
            // todo: implement rotation calculation for destination offset
            if (documentViewModel.getViewRotation() == 0 && pageBounds != null) {

                setCurrentPageIndex(pageNumber);

                // apply zoom, from destination
                if (destination.getZoom() != null &&
                        destination.getZoom() > 0.0f) {
                    setZoom(destination.getZoom(), null, false);
                }
                Point newViewPosition = new Point(pageBounds.getLocation());
                float zoom = getZoom();

                // Process top destination coordinate
                Rectangle viewportBounds = documentView.getBounds();
                Rectangle viewportRect = documentViewport.getViewRect();
//                System.out.println("viewPort bounds " + viewportBounds);
//                System.out.println("viewPort rect " + viewportRect);
//                System.out.println("page bounds " + pageBounds);
//                System.out.println("page " + pageNumber);
//                System.out.println("top/left " + destination.getTop() + " " + destination.getLeft());
                if (destination.getTop() != null && destination.getTop() != 0) {
                    // calculate potential new y value
                    newViewPosition.y = pageBounds.y + pageBounds.height - (int) (destination.getTop() * zoom);
                }
                if ((newViewPosition.y + viewportRect.height) > viewportBounds.height) {
                    newViewPosition.y = viewportBounds.height - viewportRect.height;
                }

                // Process left destination coordinate
                if (destination.getLeft() != null && destination.getLeft() != 0) {
                    // calculate potential new y value
                    newViewPosition.x = pageBounds.x + (int) (destination.getLeft() * zoom);
                }
                if ((newViewPosition.x + viewportRect.width) > viewportBounds.width) {
                    newViewPosition.x = viewportBounds.width - viewportRect.width;
                }

                // make sure documentViewport is not negative
                if (newViewPosition.x < 0)
                    newViewPosition.x = 0;
                if (newViewPosition.y < 0)
                    newViewPosition.y = 0;

                // finally apply the documentViewport position
                documentViewport.setViewPosition(newViewPosition);
                int oldPageIndex = documentViewModel.getViewCurrentPageIndex();
                documentViewModel.setViewCurrentPageIndex(pageNumber);
                firePropertyChange(PropertyConstants.DOCUMENT_CURRENT_PAGE,
                        oldPageIndex, pageNumber);
            }
            // Otherwise go to the indented page number with out applying
            // destination coordinates.
            else {
                setCurrentPageIndex(pageNumber);
            }

            viewerController.updateDocumentView();
        }
    }

    public void dispose() {
        if (documentView != null) {
            documentView.dispose();
            documentView = null;
        }
        if (documentViewModel != null) {
            documentViewModel.dispose();
            documentViewModel = null;
        }
    }

    /**
     * The controller will own the scrollpane and will insert different views
     * into it.
     */
    public Container getViewContainer() {
        return documentViewScrollPane;
    }

    public Controller getParentController() {
        return viewerController;
    }


    public int getViewMode() {
        return viewType;
    }

    /**
     * View Builder for known doc view types
     *
     * @param documentViewType view type,
     */
    public void setViewType(final int documentViewType) {
        oldViewType = viewType;
        viewType = documentViewType;
        // build the new view;
        setViewType();
    }

    private void setViewType() {

        // check if there is current view, if so dispose it
        if (documentView != null) {
            documentViewScrollPane.remove(documentView);
            documentViewScrollPane.validate();
            documentView.dispose();
        }

        if (documentViewModel == null) {
            return;
        }

        // create the desired view with the current viewModel.
        if (viewType == ONE_COLUMN_VIEW) {
            documentView =
                    new OneColumnPageView(this, documentViewScrollPane, documentViewModel);
        } else if (viewType == ONE_PAGE_VIEW) {
            documentView =
                    new OnePageView(this, documentViewScrollPane, documentViewModel);
        } else if (viewType == TWO_COLUMN_LEFT_VIEW) {
            documentView =
                    new TwoColumnPageView(this, documentViewScrollPane,
                            documentViewModel,
                            DocumentView.LEFT_VIEW);
        } else if (viewType == TWO_PAGE_LEFT_VIEW) {
            documentView =
                    new TwoPageView(this, documentViewScrollPane,
                            documentViewModel,
                            DocumentView.LEFT_VIEW);
        } else if (viewType == TWO_COLUMN_RIGHT_VIEW) {
            documentView =
                    new TwoColumnPageView(this, documentViewScrollPane,
                            documentViewModel,
                            DocumentView.RIGHT_VIEW);
        } else if (viewType == TWO_PAGE_RIGHT_VIEW) {
            documentView =
                    new TwoPageView(this, documentViewScrollPane,
                            documentViewModel,
                            DocumentView.RIGHT_VIEW);
        } else {
            documentView =
                    new OneColumnPageView(this, documentViewScrollPane, documentViewModel);
        }

        // add the new view the scroll pane
        documentViewScrollPane.setViewportView(documentView);
        documentViewScrollPane.validate();

        // re-apply the fit mode
        viewerController.setPageFitMode(viewportFitMode, true);

        // set current page
        setCurrentPageIndex(documentViewModel.getViewCurrentPageIndex());

    }


    public boolean setFitMode(final int fitMode) {

        if (documentViewModel == null) {
            return false;
        }

        boolean changed = fitMode != viewportFitMode;
        viewportFitMode = fitMode;

        if (document != null) {

            // update fit
            float newZoom = documentViewModel.getViewZoom();
            if (viewportFitMode == PAGE_FIT_ACTUAL_SIZE) {
                newZoom = 1.0f;
            } else if (viewportFitMode == PAGE_FIT_WINDOW_HEIGHT) {
                if (documentView != null && documentViewScrollPane != null) {
                    float viewportHeight = documentViewScrollPane.getViewport().getViewRect().height;
                    float pageViewHeight = documentView.getDocumentSize().height;

                    // pageViewHeight insert padding on each side.
                    pageViewHeight += AbstractDocumentView.layoutInserts *2;

                    if (viewportHeight > 0) {
                        newZoom = (viewportHeight / pageViewHeight);
                    } else {
                        newZoom = 1.0f;
                    }
                }
            } else if (viewportFitMode == PAGE_FIT_WINDOW_WIDTH) {
                if (documentView != null && documentViewScrollPane != null) {
                    float viewportWidth = documentViewScrollPane.getViewport().getViewRect().width;
                    float pageViewWidth = documentView.getDocumentSize().width;

                    // add insert padding on each side.
                    pageViewWidth += AbstractDocumentView.layoutInserts *2;

                    if (viewportWidth > 0) {
                        newZoom = (viewportWidth / pageViewWidth);
                    } else {
                        newZoom = 1.0f;
                    }
                }
            }

            // If we're scrolled all the way to the top, center to top of document when zoom,
            //  otherwise the view will zoom into the general center of the page
            if (getVerticalScrollBar().getValue() == 0) {
                setZoom(newZoom, new Point(0, 0), true);
            }
            else {
                setZoom(newZoom, null, true);
            }
        }

        return changed;
    }

    public int getFitMode() {
        return viewportFitMode;
    }

    public void setDocumentViewType(final int documentView, final int fitMode) {
        setViewType(documentView);
        setFitMode(fitMode);
    }

    public boolean setCurrentPageIndex(int pageIndex) {

        if (documentViewModel == null) {
            return false;
        }

        boolean changed;
        // make sure that new index is a valid choice.
        if (pageIndex < 0) {
            pageIndex = 0;
        } else if (pageIndex > document.getNumberOfPages() - 1) {
            pageIndex = document.getNumberOfPages() - 1;
        }
        int oldPageIndex = documentViewModel.getViewCurrentPageIndex();
        changed = documentViewModel.setViewCurrentPageIndex(pageIndex);

        if (documentView != null) {
            documentView.updateDocumentView();
        }

        // get location of page in view port
        Rectangle perferedPageOffset = documentViewModel.getPageBounds(getCurrentPageIndex());
        if (perferedPageOffset != null) {
            // scroll the view port to the correct location
            Rectangle currentViewSize = documentView.getBounds();

            // check to see of the perferedPageOffset will actually be possible.  If the
            // pages is smaller then the view port we need to correct x,y coordinates.
            if (perferedPageOffset.x + perferedPageOffset.width >
                    currentViewSize.width) {
                perferedPageOffset.x = currentViewSize.width - perferedPageOffset.width;
            }

            if (perferedPageOffset.y + perferedPageOffset.height >
                    currentViewSize.height) {
                perferedPageOffset.y = currentViewSize.height - perferedPageOffset.height;
            }

            documentViewScrollPane.getViewport().setViewPosition(perferedPageOffset.getLocation());
            documentViewScrollPane.revalidate();
        }
        firePropertyChange(PropertyConstants.DOCUMENT_CURRENT_PAGE,
                oldPageIndex, pageIndex);

        return changed;
    }

    public int setCurrentPageNext() {
        int increment = 0;
        if (documentViewModel != null) {
            increment = documentView.getNextPageIncrement();
            int current = documentViewModel.getViewCurrentPageIndex();
            if ((current + increment) < document.getNumberOfPages()) {
                documentViewModel.setViewCurrentPageIndex(current + increment);
            } else {
                documentViewModel.setViewCurrentPageIndex(document.getNumberOfPages() - 1);
            }
        }
        return increment;
    }

    public int setCurrentPagePrevious() {
        int decrement = 0;
        if (documentViewModel != null) {
            decrement = documentView.getPreviousPageIncrement();
            int current = documentViewModel.getViewCurrentPageIndex();
            if ((current - decrement) >= 0) {
                documentViewModel.setViewCurrentPageIndex(current - decrement);
            } else {
                documentViewModel.setViewCurrentPageIndex(0);
            }
        }
        return decrement;
    }

    public int getCurrentPageIndex() {
        if (documentViewModel == null) {
            return -1;
        }
        return documentViewModel.getViewCurrentPageIndex();
    }

    public int getCurrentPageDisplayValue() {
        if (documentViewModel == null) {
            return -1;
        }
        return documentViewModel.getViewCurrentPageIndex() + 1;
    }

    public float[] getZoomLevels() {
        return zoomLevels;
    }

    public void setZoomLevels(float[] zoomLevels) {
        this.zoomLevels = zoomLevels;
    }

    /**
     * Sets the zoom factor of the page visualization. A zoom factor of 1.0f
     * is equal to 100% or actual size.  A zoom factor of 0.5f is equal to 50%
     * of the original size.
     *
     * @param viewZoom zoom factor
     * @return if zoom actually changed
     */
    public boolean setZoom(float viewZoom) {
        return setZoom(viewZoom, null, false);
    }

    public boolean setZoomIn() {
        return setZoomIn(null);
    }

    public boolean setZoomOut() {
        return setZoomOut(null);
    }

    public float getZoom() {
        if (documentViewModel != null) {
            return documentViewModel.getViewZoom();
        } else {
            return 0;
        }
    }

    /**
     * Returns the zoom factor of the page visualization.  A zoom factor of 1.0f
     * is equal to 100% or actual size.  A zoom factor of 0.5f is equal to 50%
     * of the original size.
     *
     * @return zoom factor
     */
    public float getRotation() {
        if (documentViewModel == null) {
            return -1;
        }
        return documentViewModel.getViewRotation();
    }

    public float setRotateRight() {
        if (documentViewModel == null) {
            return -1;
        }
        float viewRotation = documentViewModel.getViewRotation();
        viewRotation -= ROTATION_FACTOR;
        if (viewRotation < -0)
            viewRotation += 360;
        documentViewModel.setViewRotation(viewRotation);
        documentViewScrollPane.revalidate();
        return viewRotation;
    }

    public float setRotateLeft() {
        if (documentViewModel == null) {
            return -1;
        }
        float viewRotation = documentViewModel.getViewRotation();
        viewRotation += ROTATION_FACTOR;
        viewRotation %= 360;
        documentViewModel.setViewRotation(viewRotation);
        documentViewScrollPane.revalidate();
        return viewRotation;
    }

    public boolean setRotation(float viewRotation) {
        if (documentViewModel == null) {
            return false;
        }
        boolean changed = documentViewModel.setViewRotation(viewRotation);
        documentViewModel.setViewRotation(viewRotation);
        documentViewScrollPane.revalidate();
        return changed;

    }

//    public void updateDocumentView(){
//        if (documentView != null)
//            documentView.updateDocumentView();
//    }

    public boolean setToolMode(final int viewToolMode) {
        return documentViewModel != null &&
                documentViewModel.setViewToolMode(viewToolMode);
    }

    public boolean isToolModeSelected(final int viewToolMode) {
        return getToolMode() == viewToolMode;
    }

    public int getToolMode() {
        if (documentViewModel == null) {
            return DocumentViewModelImpl.DISPLAY_TOOL_NONE;
        }
        return documentViewModel.getViewToolMode();
    }

    public void setViewCursor(final int currsorType) {
        Cursor cursor = getViewCursor(currsorType);
        if (documentViewScrollPane != null) {
            //documentViewScrollPane.setViewCursor( cursor );
            if (documentViewScrollPane.getViewport() != null)
                documentViewScrollPane.getViewport().setCursor(cursor);
        }
    }

    public Cursor getViewCursor(final int currsorType) {
        Cursor c;
        String imageName;

        if (currsorType == CURSOR_DEFAULT) {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        } else if (currsorType == CURSOR_WAIT) {
            return Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        } else if (currsorType == CURSOR_SELECT) {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        } else if (currsorType == CURSOR_HAND_OPEN) {
            imageName = "hand_open.gif";
        } else if (currsorType == CURSOR_HAND_CLOSE) {
            imageName = "hand_closed.gif";
        } else if (currsorType == CURSOR_ZOOM_IN) {
            imageName = "zoom_in.gif";
        } else if (currsorType == CURSOR_ZOOM_OUT) {
            imageName = "zoom_out.gif";
        } else if (currsorType == CURSOR_HAND_ANNOTATION) {
            return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        } else if (currsorType == CURSOR_TEXT_SELECTION) {
            return Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
        } else {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }

        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension bestsize = tk.getBestCursorSize(24, 24);
        if (bestsize.width != 0) {

            Point cursorHotSpot = new Point(12, 12);
            try {
                ImageIcon cursorImage = new ImageIcon(Images.get(imageName));
                c = tk.createCustomCursor(cursorImage.getImage(), cursorHotSpot, imageName);
            } catch (RuntimeException ex) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE,
                            "Trying to load image: " + imageName, ex);
                }
                throw ex;
            }
        } else {
            c = Cursor.getDefaultCursor();
            logger.warning("System does not support custom cursors");
        }
        return c;
    }

    public void requestViewFocusInWindow() {
        if (documentViewScrollPane != null)
            documentViewScrollPane.requestFocus();
    }

    /**
     * Increases the current page visualization zoom factor by 20%.
     *
     * @param p Recenter the scrollpane here
     */
    public boolean setZoomIn(Point p) {
        float zoom = getZoom() * ZOOM_FACTOR;
        return setZoom(zoom, p, false);
    }

    /**
     * Decreases the current page visualization zoom factor by 20%.
     *
     * @param p Recenter the scrollpane here
     */
    public boolean setZoomOut(Point p) {
        float zoom = getZoom() / ZOOM_FACTOR;
        return setZoom(zoom, p, false);
    }

    /**
     * Utility function for centering the viewport around the given point.
     *
     * @param centeringPoint which the view is to be centered on.
     */
    private void zoomCenter(Point centeringPoint) {
        // make sure the point is not null
        if (centeringPoint == null) {
            centeringPoint = getCenteringPoint();
        }

        if (centeringPoint == null || documentViewScrollPane == null)
            return;

        // get view port information
        int scrollpaneWidth = documentViewScrollPane.getViewport().getWidth();
        int scrollpaneHeight = documentViewScrollPane.getViewport().getHeight();

        int scrollPaneX = documentViewScrollPane.getViewport().getViewPosition().x;
        int scrollPaneY = documentViewScrollPane.getViewport().getViewPosition().y;

        Dimension pageSize = documentView.getPreferredSize();
        int pageWidth = pageSize.width;
        int pageHeight = pageSize.height;

        // calculate center coordinates of view port x,y
        centeringPoint.setLocation(centeringPoint.x - (scrollpaneWidth / 2),
                centeringPoint.y - (scrollpaneHeight / 2));

        // compensate centering point to make sure that preferred site is
        // respected when moving the view port x,y.

        // Special case when page height or width is smaller then the viewport
        // size.  Respect the zoom but don't try and center on the click
        if (pageWidth < scrollpaneWidth || pageHeight < scrollpaneHeight) {
            if (centeringPoint.x >= pageWidth - scrollpaneWidth ||
                    centeringPoint.x < 0) {
                centeringPoint.x = scrollPaneX;
            }

            if (centeringPoint.y >= pageHeight - scrollpaneHeight ||
                    centeringPoint.y < 0) {
                centeringPoint.y = scrollPaneY;
            }
        }
        // Special case 2: compensate for click where it is not possible to center
        // the page with out shifting the view port paste the pages width
        else {
            // adjust horizontal
            if (centeringPoint.x + scrollpaneWidth > pageWidth) {
                centeringPoint.x = (pageWidth - scrollpaneWidth);
            } else if (centeringPoint.x < 0) {
                centeringPoint.x = 0;
            }

            // adjust vertical
            if (centeringPoint.y + scrollpaneHeight > pageHeight) {
                centeringPoint.y = (pageHeight - scrollpaneHeight);
            } else if (centeringPoint.y < 0) {
                centeringPoint.y = 0;
            }
        }
        // not sure why, but have to set twice for reliable results
        documentViewScrollPane.getViewport().setViewPosition(centeringPoint);
        documentViewScrollPane.getViewport().setViewPosition(centeringPoint);
    }


    /**
     * Zoom to a new zoom level, centered at a specific point.
     *
     * @param zoom                  zoom level which should be in the range of zoomLevels array
     * @param becauseOfValidFitMode true will update ui elements with zoom state.
     * @param centeringPoint        point to center on.
     * @return true if the zoom level changed, false otherwise.
     */
    private boolean setZoom(float zoom, Point centeringPoint, boolean becauseOfValidFitMode) {
        if (documentViewModel == null) {
            return false;
        }
        // make sure the zoom falls in between the zoom range
        if (zoomLevels != null) {
            if (zoom < zoomLevels[0])
                zoom = zoomLevels[0];
            else if (zoom > zoomLevels[zoomLevels.length - 1])
                zoom = zoomLevels[zoomLevels.length - 1];
        }

        // set a default centering point if null
        if (centeringPoint == null) {
            centeringPoint = getCenteringPoint();
        }
        // grab previous zoom so that zoom factor can be calculated
        float previousZoom = getZoom();

        // apply zoom
        boolean changed = documentViewModel.setViewZoom(zoom);
        // get the view port validate the viewport and shift the components
        documentViewScrollPane.revalidate();

        // center zoom calculation, find current center and pass
        // it along to zoomCenter function.
        if (changed) {
            float zoomFactor = zoom / previousZoom;
            if (centeringPoint != null) {
                centeringPoint.setLocation(
                        centeringPoint.x * zoomFactor,
                        centeringPoint.y * zoomFactor);
            }
        }
        // still center on click
        zoomCenter(centeringPoint);

        // update the UI controls
        if (viewerController != null) {
            viewerController.doCommonZoomUIUpdates(becauseOfValidFitMode);
        }

        return changed;
    }

    /**
     * Utility method for finding the center point of the viewport
     *
     * @return current center of view port.
     */
    private Point getCenteringPoint() {
        Point centeringPoint = null;
        if (documentViewScrollPane != null) {
            int x = documentViewScrollPane.getViewport().getViewPosition().x +
                    (documentViewScrollPane.getViewport().getWidth() / 2);
            int y = documentViewScrollPane.getViewport().getViewPosition().y +
                    (documentViewScrollPane.getViewport().getHeight() / 2);
            centeringPoint = new Point(x, y);
        }
        return centeringPoint;
    }

    /**
     * Gives access to the currently openned Document's Catalog's PageTree
     *
     * @return PageTree
     */
    private PageTree getPageTree() {
        if (document == null)
            return null;
        return document.getPageTree();
    }

    public DocumentViewModel getDocumentViewModel() {
        return documentViewModel;
    }

//    private Page getPageLock(int pageNumber) {
//        PageTree pageTree = getPageTree();
//        if (pageTree == null)
//            return null;
//        return pageTree.getPage(pageNumber, this);
//    }
//
//    private void removePageLock(Page page) {
//        PageTree pageTree = getPageTree();
//        if (pageTree != null) {
//            pageTree.releasePage(page, this);
//        }
//    }
    //
    // ComponentListener interface
    //

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void componentHidden(ComponentEvent e) {
    }

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void componentMoved(ComponentEvent e) {
    }

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void componentResized(ComponentEvent e) {
        Object src = e.getSource();
        if (src == null)
            return;
        // we need to update the document view, if fit width of fit height is
        // selected we need to adjust the zoom level appropriately.
        if (src == documentViewScrollPane) {
            setFitMode(getFitMode());
        }
    }

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void componentShown(ComponentEvent e) {

    }

    public void firePropertyChange(String event, int oldValue, int newValue) {
        changes.firePropertyChange(event, oldValue, newValue);
    }

    /**
     * Fires property change events for Page view UI changes such as:
     * <li>focus gained/lost</li>
     * <li>annotation state change such as move or resize</li>
     * <li>new annotation crreated, currently only for new link annotations</li>
     * <li></li>
     *
     * @param event property being changes
     * @param oldValue old value, null if no old value
     * @param newValue new annotation value.
     */
    public void firePropertyChange(String event, Object oldValue,
                                   Object newValue) {
        changes.firePropertyChange(event,  oldValue, newValue);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        changes.addPropertyChangeListener(l);
    }

    public void deleteCurrentAnnotation() {
        // make sure there is a current annotation in model
        AnnotationComponentImpl annotationComponent =
                documentViewModel.getCurrentAnnotation();
        if (documentViewModel != null && annotationComponent !=null ){

            // parent component
            AbstractPageViewComponent pageComponent =
                    annotationComponent.getPageViewComponent();

            // store the annotation state in the caretaker
            AnnotationState preDeleteState =
                    new AnnotationState(annotationComponent);

            // remove annotation
            Document document = getDocument();
            PageTree pageTree = document.getPageTree();
            Page page = pageTree.getPage(pageComponent.getPageIndex(), this);
            // remove from page
            page.deleteAnnotation(annotationComponent.getAnnotation());
            // remove from page view.
            pageComponent.removeAnnotation(annotationComponent);
            // release the page
            pageTree.releasePage(pageComponent.getPageIndex(), this);

            // store the post delete state.
            AnnotationState postDeleteState =
                    new AnnotationState(annotationComponent);

            documentViewModel.getAnnotationCareTaker().addState(preDeleteState,
                    postDeleteState);

            // fire event notification
            firePropertyChange(PropertyConstants.ANNOTATION_DELETED,
                    documentViewModel.getCurrentAnnotation(),
                    null);

            // clear previously selected annotation and fire event.
            assignSelectedAnnotation(null);

            // repaint the view.
            documentView.repaint();
        }
    }

    public void undo() {
        // reloads the last modified annotations state.
        documentViewModel.getAnnotationCareTaker().undo();

        // repaint the view.
        documentView.repaint();
    }

    public void redo() {
        // tries to redo a previously undo command, may not do anything
        documentViewModel.getAnnotationCareTaker().redo();

        // repaint the view.
        documentView.repaint();
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        changes.removePropertyChangeListener(l);
    }
}
