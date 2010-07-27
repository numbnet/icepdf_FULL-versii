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
package org.icepdf.ri.common;

import org.icepdf.core.Controller;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.pobjects.*;
import org.icepdf.core.pobjects.annotations.AnnotationState;
import org.icepdf.core.pobjects.actions.Action;
import org.icepdf.core.pobjects.actions.GoToAction;
import org.icepdf.core.pobjects.actions.URIAction;
import org.icepdf.core.pobjects.fonts.FontFactory;
import org.icepdf.core.pobjects.security.Permissions;
import org.icepdf.core.search.DocumentSearchController;
import org.icepdf.core.util.Library;
import org.icepdf.core.util.PropertyConstants;
import org.icepdf.core.views.DocumentView;
import org.icepdf.core.views.swing.AnnotationComponentImpl;
import org.icepdf.ri.common.search.DocumentSearchControllerImpl;
import org.icepdf.ri.common.views.DocumentViewControllerImpl;
import org.icepdf.ri.common.views.DocumentViewModelImpl;
import org.icepdf.ri.common.annotation.AnnotationPanel;
import org.icepdf.ri.util.*;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SwingController is the meat of a PDF viewing application. It is the Controller
 * aspect of the Model-View-Controller (MVC) framework.<br>
 * <br>
 * SwingController acts as a bridge between a Swing user interface, as built by
 * SwingViewerBuilder; the Document class, which is the root accessor to the PDF content;
 * and the ViewerModel, which maintains the state of the user's perspective of said Document.
 *
 * @author Mark Collette
 * @see SwingViewBuilder
 * @see org.icepdf.core.pobjects.Document
 * @see ViewModel
 * @since 2.0
 */
public class SwingController
        implements Controller, ActionListener, FocusListener, ItemListener,
        TreeSelectionListener, WindowListener, DropTargetListener,
        KeyListener, PropertyChangeListener {

    private static final Logger logger =
            Logger.getLogger(SwingController.class.toString());

    public static final int CURSOR_OPEN_HAND = 1;

    public static final int CURSOR_CLOSE_HAND = 2;

    public static final int CURSOR_ZOOM_IN = 3;

    public static final int CURSOR_ZOOM_OUT = 4;

    public static final int CURSOR_WAIT = 6;

    public static final int CURSOR_SELECT = 7;

    public static final int CURSOR_DEFAULT = 8;

    private static final int MAX_SELECT_ALL_PAGE_COUNT = 250;

    private JMenuItem openFileMenuItem;
    private JMenuItem openURLMenuItem;
    private JMenuItem closeMenuItem;
    private JMenuItem saveAsFileMenuItem;
    private JMenuItem exportTextMenuItem;
    private JMenuItem exportSVGMenuItem;
    private JMenuItem permissionsMenuItem;
    private JMenuItem informationMenuItem;
    private JMenuItem printSetupMenuItem;
    private JMenuItem printMenuItem;
    private JMenuItem exitMenuItem;

    private JMenuItem undoMenuItem;
    private JMenuItem redoMenuItem;

    private JMenuItem copyMenuItem;
    private JMenuItem deleteMenuItem;

    private JMenuItem selectAllMenuItem;
    private JMenuItem deselectAllMenuItem;

    private JMenuItem fitActualSizeMenuItem;
    private JMenuItem fitPageMenuItem;
    private JMenuItem fitWidthMenuItem;
    private JMenuItem zoomInMenuItem;
    private JMenuItem zoomOutMenuItem;
    private JMenuItem rotateLeftMenuItem;
    private JMenuItem rotateRightMenuItem;
    private JMenuItem showHideToolBarMenuItem;
    private JMenuItem showHideUtilityPaneMenuItem;

    private JMenuItem firstPageMenuItem;
    private JMenuItem previousPageMenuItem;
    private JMenuItem nextPageMenuItem;
    private JMenuItem lastPageMenuItem;
    private JMenuItem searchMenuItem;
    private JMenuItem goToPageMenuItem;

    private JMenuItem minimiseAllMenuItem;
    private JMenuItem bringAllToFrontMenuItem;
    private List windowListMenuItems;

    private JMenuItem aboutMenuItem;

    private JButton openFileButton;
    private JButton saveAsFileButton;
    private JButton printButton;
    private JButton searchButton;
    private JButton showHideUtilityPaneButton;

    private JButton firstPageButton;
    private JButton previousPageButton;
    private JButton nextPageButton;
    private JButton lastPageButton;
    private JTextField currentPageNumberTextField;
    private JLabel numberOfPagesLabel;

    private JButton zoomInButton;
    private JButton zoomOutButton;
    private JComboBox zoomComboBox;

    private JToggleButton fitActualSizeButton;
    private JToggleButton fitHeightButton;
    private JToggleButton fitWidthButton;

    private JToggleButton fontEngineButton;

    private JToggleButton facingPageViewContinuousButton;
    private JToggleButton singlePageViewContinuousButton;
    private JToggleButton facingPageViewNonContinuousButton;
    private JToggleButton singlePageViewNonContinuousButton;

    private JButton rotateLeftButton;
    private JButton rotateRightButton;

    private JToggleButton panToolButton;
    private JToggleButton textSelectToolButton;
    private JToggleButton zoomInToolButton;
    private JToggleButton zoomOutToolButton;

    private JToggleButton selectToolButton;
    private JToggleButton linkAnnotationToolButton;

    private JToolBar completeToolBar;

    // Printing in background thread monitors
    private ProgressMonitor printProgressMonitor;
    private Timer printActivityMonitor;

    private JTree outlinesTree;
    private JScrollPane outlinesScrollPane;
    private SearchPanel searchPanel;
    private AnnotationPanel annotationPanel;
    private JTabbedPane utilityTabbedPane;

    private JSplitPane utilityAndDocumentSplitPane;
    private int utilityAndDocumentSplitPaneLastDividerLocation;

    private JLabel statusLabel;

    private JFrame viewer;


    private WindowManagementCallback windowManagementCallback;
    // simple model for swing controller, mainly printer and  file loading state.
    private ViewModel viewModel;

    // subcontroller for document view or document page views.
    private DocumentViewControllerImpl documentViewController;

    // subcontroller for document text searching.
    private DocumentSearchController documentSearchController;

    // todo subcontroller for document annotations creation.


    private Document document;

    private boolean disposed;

    // internationalization messages, loads message for default JVM locale.
    private ResourceBundle messageBundle = null;

    private PropertiesManager propertiesManager;

    /**
     * Create a SwingController object, and its associated ViewerModel
     *
     * @see ViewModel
     */
    public SwingController() {
        this(null);
    }

    public SwingController(ResourceBundle messageBundle) {
        viewModel = new ViewModel();
        // page view controller
        documentViewController = new DocumentViewControllerImpl(this);
        // document search controller.
        documentSearchController = new DocumentSearchControllerImpl(this);

        // register Property change listeners, for zoom, rotation, current page changes
        documentViewController.addPropertyChangeListener(this);

        // load the resource bundle using the default local
        if (messageBundle != null) {
            this.messageBundle = messageBundle;
        } else {
            this.messageBundle = ResourceBundle.getBundle(
                    PropertiesManager.DEFAULT_MESSAGE_BUNDLE);
        }
    }

    /**
     * Gets controller responsible for the page multiple page views.
     *
     * @return page view controller.
     */
    public org.icepdf.core.views.DocumentViewController getDocumentViewController() {
        return documentViewController;
    }

    /**
     * Gets controller responsible for the document text searches.
     *
     * @return page view controller.
     */
    public DocumentSearchController getDocumentSearchController() {
        return documentSearchController;
    }

    /**
     * Gets the message bundle used by this class.  Message bundle resources
     * are loaded via the JVM default locale.
     *
     * @return message bundle used by this class.
     */
    public ResourceBundle getMessageBundle() {
        return messageBundle;
    }

    /**
     * The WindowManagementCallback is used for creating new Document windows,
     * and quitting the application
     *
     * @param wm The new WindowManagementCallback
     * @see #getWindowManagementCallback
     */
    public void setWindowManagementCallback(WindowManagementCallback wm) {
        windowManagementCallback = wm;
    }

    /**
     * The WindowManagementCallback is used for creating new Document windows,
     * and quitting the application
     *
     * @return The current WindowManagementCallback
     * @see #setWindowManagementCallback
     */
    public WindowManagementCallback getWindowManagementCallback() {
        return windowManagementCallback;
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController has access to all properties
     */
    public void setPropertiesManager(PropertiesManager propertiesManager) {
        this.propertiesManager = propertiesManager;
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setOpenFileMenuItem(JMenuItem mi) {
        openFileMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setOpenURLMenuItem(JMenuItem mi) {
        openURLMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setCloseMenuItem(JMenuItem mi) {
        closeMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setSaveAsFileMenuItem(JMenuItem mi) {
        saveAsFileMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setExportTextMenuItem(JMenuItem mi) {
        exportTextMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setExportSVGMenuItem(JMenuItem mi) {
        exportSVGMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setPermissionsMenuItem(JMenuItem mi) {
        permissionsMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setInformationMenuItem(JMenuItem mi) {
        informationMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setPrintSetupMenuItem(JMenuItem mi) {
        printSetupMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setPrintMenuItem(JMenuItem mi) {
        printMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setExitMenuItem(JMenuItem mi) {
        exitMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setUndoMenuItem(JMenuItem mi) {
        undoMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setReduMenuItem(JMenuItem mi) {
        redoMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setCopyMenuItem(JMenuItem mi) {
        copyMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setDeleteMenuItem(JMenuItem mi) {
        deleteMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setSelectAllMenuItem(JMenuItem mi) {
        selectAllMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setDselectAllMenuItem(JMenuItem mi) {
        deselectAllMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setFitActualSizeMenuItem(JMenuItem mi) {
        fitActualSizeMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setFitPageMenuItem(JMenuItem mi) {
        fitPageMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setFitWidthMenuItem(JMenuItem mi) {
        fitWidthMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setZoomInMenuItem(JMenuItem mi) {
        zoomInMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setZoomOutMenuItem(JMenuItem mi) {
        zoomOutMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setRotateLeftMenuItem(JMenuItem mi) {
        rotateLeftMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setRotateRightMenuItem(JMenuItem mi) {
        rotateRightMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setShowHideToolBarMenuItem(JMenuItem mi) {
        showHideToolBarMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setShowHideUtilityPaneMenuItem(JMenuItem mi) {
        showHideUtilityPaneMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setFirstPageMenuItem(JMenuItem mi) {
        firstPageMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setPreviousPageMenuItem(JMenuItem mi) {
        previousPageMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setNextPageMenuItem(JMenuItem mi) {
        nextPageMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setLastPageMenuItem(JMenuItem mi) {
        lastPageMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setSearchMenuItem(JMenuItem mi) {
        searchMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setGoToPageMenuItem(JMenuItem mi) {
        goToPageMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setMinimiseAllMenuItem(JMenuItem mi) {
        minimiseAllMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setBringAllToFrontMenuItem(JMenuItem mi) {
        bringAllToFrontMenuItem = mi;
        mi.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setWindowListMenuItems(List menuItems) {
        windowListMenuItems = menuItems;
        int count = (windowListMenuItems != null) ? windowListMenuItems.size() : 0;
        for (int i = 0; i < count; i++) {
            JMenuItem mi = (JMenuItem) windowListMenuItems.get(i);
            mi.addActionListener(this);
        }
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setAboutMenuItem(JMenuItem mi) {
        aboutMenuItem = mi;
        mi.addActionListener(this);
    }


    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setOpenFileButton(JButton btn) {
        openFileButton = btn;
        btn.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setSaveAsFileButton(JButton btn) {
        saveAsFileButton = btn;
        btn.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setPrintButton(JButton btn) {
        printButton = btn;
        btn.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setSearchButton(JButton btn) {
        searchButton = btn;
        btn.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setShowHideUtilityPaneButton(JButton btn) {
        showHideUtilityPaneButton = btn;
        btn.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setFirstPageButton(JButton btn) {
        firstPageButton = btn;
        btn.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setPreviousPageButton(JButton btn) {
        previousPageButton = btn;
        btn.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setNextPageButton(JButton btn) {
        nextPageButton = btn;
        btn.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setLastPageButton(JButton btn) {
        lastPageButton = btn;
        btn.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setCurrentPageNumberTextField(JTextField textField) {
        currentPageNumberTextField = textField;
        currentPageNumberTextField.addActionListener(this);
        currentPageNumberTextField.addFocusListener(this);
        currentPageNumberTextField.addKeyListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setNumberOfPagesLabel(JLabel lbl) {
        numberOfPagesLabel = lbl;
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setZoomOutButton(JButton btn) {
        zoomOutButton = btn;
        btn.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setZoomComboBox(JComboBox zcb, float[] zl) {
        zoomComboBox = zcb;
        documentViewController.setZoomLevels(zl);
        zoomComboBox.setSelectedItem(NumberFormat.getPercentInstance().format(1.0));
        zoomComboBox.addItemListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setZoomInButton(JButton btn) {
        zoomInButton = btn;
        btn.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setFitActualSizeButton(JToggleButton btn) {
        fitActualSizeButton = btn;
        btn.addItemListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setFitHeightButton(JToggleButton btn) {
        fitHeightButton = btn;
        btn.addItemListener(this);
    }

    /**
     * Called by SwingViewBuilder, so that SwingController can setup event handling
     */
    public void setFontEngineButton(JToggleButton btn) {
        fontEngineButton = btn;
        btn.addItemListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setFitWidthButton(JToggleButton btn) {
        fitWidthButton = btn;
        btn.addItemListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setRotateLeftButton(JButton btn) {
        rotateLeftButton = btn;
        btn.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setRotateRightButton(JButton btn) {
        rotateRightButton = btn;
        btn.addActionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setPanToolButton(JToggleButton btn) {
        panToolButton = btn;
        btn.addItemListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setZoomInToolButton(JToggleButton btn) {
        zoomInToolButton = btn;
        btn.addItemListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setTextSelectToolButton(JToggleButton btn) {
        textSelectToolButton = btn;
        btn.addItemListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setSelectToolButton(JToggleButton btn) {
        selectToolButton = btn;
        btn.addItemListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setLinkAnnotationToolButton(JToggleButton btn) {
        linkAnnotationToolButton = btn;
        btn.addItemListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setZoomOutToolButton(JToggleButton btn) {
        zoomOutToolButton = btn;
        btn.addItemListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setCompleteToolBar(JToolBar toolbar) {
        completeToolBar = toolbar;
    }


    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setOutlineComponents(JTree tree, JScrollPane scroll) {
        outlinesTree = tree;
        outlinesScrollPane = scroll;
        outlinesTree.addTreeSelectionListener(this);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setSearchPanel(SearchPanel sp) {
        searchPanel = sp;
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setAnnotationPanel(AnnotationPanel lp) {
        annotationPanel = lp;
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setUtilityTabbedPane(JTabbedPane util) {
        utilityTabbedPane = util;
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setIsEmbeddedComponent(boolean embeddableComponent) {
        if (embeddableComponent) {
            documentViewController.setViewKeyListener(this);
            documentViewController.getViewContainer().addKeyListener(this);
        }
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setUtilityAndDocumentSplitPane(JSplitPane splitpane) {
        utilityAndDocumentSplitPane = splitpane;
        setUtilityPaneVisible(false);
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setStatusLabel(JLabel lbl) {
        statusLabel = lbl;
    }

    /**
     * Called by SwingViewerBuilder, so that SwingController can setup event handling
     */
    public void setViewerFrame(JFrame v) {
        viewer = v;
        viewer.addWindowListener(this);
        // add drag and drop listeners
        new DropTarget(viewer, // component
                DnDConstants.ACTION_COPY_OR_MOVE, // actions
                this); // DropTargetListener
        reflectStateInComponents();
    }

    /**
     * Not all uses of SwingController would result in there existing a Viewer Frame,
     * so this may well return null.
     */
    public JFrame getViewerFrame() {
        return viewer;
    }


    /**
     * Utility method to set the state of all the different GUI elements. Mainly
     * to enable/disable the GUI elements when a file is opened/closed respectively.
     */
    private void reflectStateInComponents() {
        boolean opened = document != null;
        int nPages = (getPageTree() != null) ? getPageTree().getNumberOfPages() : 0;

        // get security information for printing and text extraction
        boolean canPrint = havePermissionToPrint();
        boolean canExtract = havePermissionToExtractContent();
        boolean canModify = havePermissionToModifyDocument();

        reflectPageChangeInComponents();

        // menu items.
        setEnabled(closeMenuItem, opened);
        setEnabled(saveAsFileMenuItem, opened);
        setEnabled(exportTextMenuItem, opened && canExtract);
        // Exporting to SVG creates output as if we printed,
        //   which is not the same as extracting text
        setEnabled(exportSVGMenuItem, opened && canPrint);
        setEnabled(permissionsMenuItem, opened);
        setEnabled(informationMenuItem, opened);
        // Printer setup is global to all PDFs, so don't limit it by this one PDF
        setEnabled(printSetupMenuItem, opened && canPrint);
        setEnabled(printMenuItem, opened && canPrint);

        // set initial sate for undo/redo edit, afterwards state is set by
        // valueChange events depending on tool selection.
        setEnabled(undoMenuItem, false);
        setEnabled(redoMenuItem, false);
        setEnabled(copyMenuItem, false);
        setEnabled(deleteMenuItem, false);

        setEnabled(selectAllMenuItem, opened && canExtract);
        setEnabled(deselectAllMenuItem, false);


        setEnabled(fitActualSizeMenuItem, opened);
        setEnabled(fitPageMenuItem, opened);
        setEnabled(fitWidthMenuItem, opened);

        setEnabled(zoomInMenuItem, opened);
        setEnabled(zoomOutMenuItem, opened);

        setEnabled(rotateLeftMenuItem, opened);
        setEnabled(rotateRightMenuItem, opened);

//        setEnabled(facingPageViewContinuousMenuItem , opened );
//        setEnabled(singlePageViewContinuousMenuItem , opened );
//        setEnabled(facingPageViewNonContinuousMenuItem , opened );
//        setEnabled(singlePageViewNonContinuousMenuItem , opened );

        setEnabled(fitPageMenuItem, opened);
        setEnabled(fitWidthMenuItem, opened);
        if (showHideToolBarMenuItem != null) {
            boolean vis = (completeToolBar != null) && completeToolBar.isVisible();
            showHideToolBarMenuItem.setText(
                    vis ? messageBundle.getString("viewer.toolbar.hideToolBar.label") :
                            messageBundle.getString("viewer.toolbar.showToolBar.label"));
        }
        setEnabled(showHideToolBarMenuItem, completeToolBar != null);
        if (showHideUtilityPaneMenuItem != null) {
            boolean vis = isUtilityPaneVisible();
            showHideUtilityPaneMenuItem.setText(
                    (opened && vis) ?
                            messageBundle.getString("viewer.toolbar.hideUtilityPane.label") :
                            messageBundle.getString("viewer.toolbar.showUtilityPane.label"));
        }
        setEnabled(showHideUtilityPaneMenuItem, opened && utilityTabbedPane != null);
        setEnabled(searchMenuItem, opened && searchPanel != null);
        setEnabled(goToPageMenuItem, opened && nPages > 1);

        setEnabled(saveAsFileButton, opened);
        setEnabled(printButton, opened && canPrint);
        setEnabled(searchButton, opened && searchPanel != null);
        setEnabled(showHideUtilityPaneButton, opened && utilityTabbedPane != null);
        setEnabled(currentPageNumberTextField, opened && nPages > 1);
        if (numberOfPagesLabel != null) {

            Object[] messageArguments = new Object[]{String.valueOf(nPages)};
            MessageFormat formatter =
                    new MessageFormat(
                            messageBundle.getString("viewer.toolbar.pageIndicator"));
            String numberOfPages = formatter.format(messageArguments);

            numberOfPagesLabel.setText(
                    opened ? numberOfPages : "");
        }
        setEnabled(zoomInButton, opened);
        setEnabled(zoomOutButton, opened);
        setEnabled(zoomComboBox, opened);
        setEnabled(fitActualSizeButton, opened);
        setEnabled(fitHeightButton, opened);
        setEnabled(fitWidthButton, opened);
        setEnabled(rotateLeftButton, opened);
        setEnabled(rotateRightButton, opened);
        setEnabled(panToolButton, opened);
        setEnabled(zoomInToolButton, opened);
        setEnabled(zoomOutToolButton, opened);
        setEnabled(textSelectToolButton, opened && canExtract);
        setEnabled(selectToolButton, opened && canModify);
        setEnabled(linkAnnotationToolButton, opened && canModify);
        setEnabled(fontEngineButton, opened);
        setEnabled(facingPageViewContinuousButton, opened);
        setEnabled(singlePageViewContinuousButton, opened);
        setEnabled(facingPageViewNonContinuousButton, opened);
        setEnabled(singlePageViewNonContinuousButton, opened);

        if (opened) {
            reflectZoomInZoomComboBox();
            reflectFitInFitButtons();
            reflectDocumentViewModeInButtons();
            reflectToolInToolButtons();
        }
    }

    private void reflectPageChangeInComponents() {
        boolean opened = document != null;
        int nPages = (getPageTree() != null) ? getPageTree().getNumberOfPages() : 0;
        int currentPage = isCurrentPage() ?
                documentViewController.getCurrentPageDisplayValue() : 0;

        setEnabled(firstPageMenuItem, opened && currentPage != 1);
        setEnabled(previousPageMenuItem, opened && currentPage != 1);
        setEnabled(nextPageMenuItem, opened && currentPage != nPages);
        setEnabled(lastPageMenuItem, opened && currentPage != nPages);

        setEnabled(firstPageButton, opened && currentPage != 1);
        setEnabled(previousPageButton, opened && currentPage != 1);
        setEnabled(nextPageButton, opened && currentPage != nPages);
        setEnabled(lastPageButton, opened && currentPage != nPages);

        if (currentPageNumberTextField != null) {
            currentPageNumberTextField.setText(
                    opened ? Integer.toString(currentPage) : "");
        }
    }

    public boolean havePermissionToPrint() {
        if (document == null)
            return false;
        org.icepdf.core.pobjects.security.SecurityManager securityManager =
                document.getSecurityManager();
        if (securityManager == null)
            return true;
        Permissions permissions = securityManager.getPermissions();
        return permissions == null ||
                permissions.getPermissions(Permissions.PRINT_DOCUMENT);
    }

    public boolean havePermissionToExtractContent() {
        if (document == null)
            return false;
        org.icepdf.core.pobjects.security.SecurityManager securityManager =
                document.getSecurityManager();
        if (securityManager == null)
            return true;
        Permissions permissions = securityManager.getPermissions();
        return permissions == null ||
                permissions.getPermissions(Permissions.CONTENT_EXTRACTION);
    }

    public boolean havePermissionToModifyDocument() {
        if (document == null)
            return false;
        org.icepdf.core.pobjects.security.SecurityManager securityManager =
                document.getSecurityManager();
        if (securityManager == null)
            return true;
        Permissions permissions = securityManager.getPermissions();
        return permissions == null ||
                permissions.getPermissions(Permissions.MODIFY_DOCUMENT);
    }

    private void setEnabled(JComponent comp, boolean ena) {
        if (comp != null)
            comp.setEnabled(ena);
    }

    private void setZoomFromZoomComboBox() {
        if (reflectingZoomInZoomComboBox)
            return;
        final int selIndex = zoomComboBox.getSelectedIndex();
        float[] zoomLevels = documentViewController.getZoomLevels();
        if (selIndex >= 0 && selIndex < zoomLevels.length) {
            float zoom = 1.0f;
            try {
                zoom = zoomLevels[selIndex];
            } catch (IndexOutOfBoundsException ex) {
                logger.log(Level.FINE, "Error apply zoom levels");
            } finally {
                if (zoom != documentViewController.getZoom()) {
                    setZoom(zoom);
                }
            }
        } else {
            boolean success = false;
            try {
                Object selItem = zoomComboBox.getSelectedItem();
                if (selItem != null) {
                    String str = selItem.toString();
                    str = str.replace('%', ' ');
                    str = str.trim();
                    float zoom = Float.parseFloat(str);
                    zoom /= 100.0f;
                    if (zoom != documentViewController.getZoom()) {
                        setZoom(zoom);
                    }
                    success = true;
                }
            }
            catch (Exception e) {
                // Most likely a NumberFormatException
                success = false;
            }
            if (!success) {
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }

    /**
     * Method to determine if the Undo and Redo menu items can be enabled
     * This will query the UndoCaretaker for the status of the queue first
     */
    public void reflectUndoCommands(){
         UndoCaretaker undoCaretaker = ((DocumentViewModelImpl)
                 documentViewController.getDocumentViewModel()).
                        getAnnotationCareTaker();
        setEnabled(undoMenuItem, undoCaretaker.isUndo());
        setEnabled(redoMenuItem, undoCaretaker.isRedo());
    }

    private void reflectZoomInZoomComboBox() {
        if (reflectingZoomInZoomComboBox)
            return;
        if (document == null)
            return;
        int index = -1;
        final float zoom = documentViewController.getZoom();
        final float belowZoom = zoom * 0.99f;
        final float aboveZoom = zoom * 1.01f;
        float[] zoomLevels = documentViewController.getZoomLevels();
        if (zoomLevels != null) {
            for (int i = 0; i < zoomLevels.length; i++) {
                final float curr = zoomLevels[i];
                if (curr >= belowZoom && curr <= aboveZoom) {
                    index = i;
                    break;
                }
            }
        }
        try {
            reflectingZoomInZoomComboBox = true;

            if (zoomComboBox != null) {
                if (index > -1) {
                    zoomComboBox.setSelectedIndex(index);
                } else {
                    zoomComboBox.setSelectedItem(NumberFormat.getPercentInstance().format(zoom));
                }
            }
            // upatdate the page fit values if they are in the correct zoom range
//            if( viewModel.fitPageFlag == .PAGE_FIT_NONE ) {
//                float fitActualZoom = calcZoomForFitActualSize();
//                if( fitActualZoom >= belowZoom && fitActualZoom <= aboveZoom )
//                    viewModel.fitPageFlag = ViewModel.PAGE_FIT_ACTUAL_SIZE;
//                else {
//                    float fitPageZoom = calcZoomForFitPage();
//                    if( fitPageZoom >= belowZoom && fitPageZoom <= aboveZoom )
//                        viewModel.fitPageFlag = ViewModel.PAGE_FIT_IN_WINDOW;
//                    else {
//                        float fitWidthZoom = calcZoomForFitWidth();
//                        if( fitWidthZoom >= belowZoom && fitWidthZoom <= aboveZoom )
//                            viewModel.fitPageFlag = ViewModel.PAGE_FIT_WINDOW_WIDTH;
//                    }
//                }
//            }
        }
        finally {
            reflectingZoomInZoomComboBox = false;
        }
    }

    private boolean reflectingZoomInZoomComboBox = false;


    /**
     * Gets the current display tool value for the display panel.
     *
     * @return constant representing the state of the display tool for the
     *         display panel.
     * @see #setDisplayTool
     */
    public int getDocumentViewToolMode() {
        return documentViewController.getToolMode();
    }

    /**
     * Sets the display tool used when the document is viewed in interactive
     * mode.  A display changes the icon of the mouse when it is over the panel
     * that displays a document page.  There are currently four possible tool
     * modes:
     * <ul>
     * <li>DISPLAY_TOOL_PAN - Changes the mouse icon to a hand and allows
     * the user to click and drag the document view (Pan).  This pan feature
     * is only available when the display window has scrollbars. </li>
     * <li>DISPLAY_TOOL_ZOOM_IN - Changes the mouse icon to a magnifying glass
     * and adds a left mouse click listener to the display panel.  One left mouse
     * click increases the zoom factor by 20%. </li>
     * <li>DISPLAY_TOOL_ZOOM_OUT - Changes the mouse icon to a magnifying glass
     * and adds a left mouse click listener to the display panel.  One left
     * mouse click decreases the zoom factor by 20%. </li>
     * <li>DISPLAY_TOOL_NONE - Changes the mouse icon to the default icon
     * and removes mouse properties from the display panel. </li>
     * </ul>
     *
     * @see #getDocumentViewToolMode
     */
    public void setDisplayTool(final int argToolName) {

        boolean actualToolMayHaveChanged = false;
        if (argToolName == DocumentViewModelImpl.DISPLAY_TOOL_PAN) {
            actualToolMayHaveChanged =
                    documentViewController.setToolMode(DocumentViewModelImpl.DISPLAY_TOOL_PAN);
            documentViewController.setViewCursor(org.icepdf.core.views.DocumentViewController.CURSOR_HAND_OPEN);
            setCursorOnComponents(org.icepdf.core.views.DocumentViewController.CURSOR_DEFAULT);
        } else if (argToolName == DocumentViewModelImpl.DISPLAY_TOOL_TEXT_SELECTION) {
            actualToolMayHaveChanged =
                    documentViewController.setToolMode(DocumentViewModelImpl.DISPLAY_TOOL_TEXT_SELECTION);
            documentViewController.setViewCursor(org.icepdf.core.views.DocumentViewController.CURSOR_SELECT);
            setCursorOnComponents(org.icepdf.core.views.DocumentViewController.CURSOR_DEFAULT);
        }  else if (argToolName == DocumentViewModelImpl.DISPLAY_TOOL_SELECTION) {
            actualToolMayHaveChanged =
                    documentViewController.setToolMode(DocumentViewModelImpl.DISPLAY_TOOL_SELECTION);
            documentViewController.setViewCursor(org.icepdf.core.views.DocumentViewController.CURSOR_SELECT);
            setCursorOnComponents(org.icepdf.core.views.DocumentViewController.CURSOR_DEFAULT);
            showAnnotationPanel(null);
        } else if (argToolName == DocumentViewModelImpl.DISPLAY_TOOL_LINK_ANNOTATION) {
            actualToolMayHaveChanged =
                    documentViewController.setToolMode(DocumentViewModelImpl.DISPLAY_TOOL_LINK_ANNOTATION);
            documentViewController.setViewCursor(org.icepdf.core.views.DocumentViewController.CURSOR_SELECT);
            setCursorOnComponents(org.icepdf.core.views.DocumentViewController.CURSOR_DEFAULT);
            showAnnotationPanel(null);
        } else if (argToolName == DocumentViewModelImpl.DISPLAY_TOOL_ZOOM_IN) {
            actualToolMayHaveChanged =
                    documentViewController.setToolMode(
                            DocumentViewModelImpl.DISPLAY_TOOL_ZOOM_IN);
            documentViewController.setViewCursor(org.icepdf.core.views.DocumentViewController.CURSOR_ZOOM_IN);
            setCursorOnComponents(org.icepdf.core.views.DocumentViewController.CURSOR_DEFAULT);
        } else if (argToolName == DocumentViewModelImpl.DISPLAY_TOOL_ZOOM_OUT) {
            actualToolMayHaveChanged =
                    documentViewController.setToolMode(
                            DocumentViewModelImpl.DISPLAY_TOOL_ZOOM_OUT);
            documentViewController.setViewCursor(org.icepdf.core.views.DocumentViewController.CURSOR_ZOOM_OUT);
            setCursorOnComponents(org.icepdf.core.views.DocumentViewController.CURSOR_DEFAULT);
        } else if (argToolName == DocumentViewModelImpl.DISPLAY_TOOL_WAIT) {
            setCursorOnComponents(org.icepdf.core.views.DocumentViewController.CURSOR_WAIT);
        } else if (argToolName == DocumentViewModelImpl.DISPLAY_TOOL_NONE) {
            setCursorOnComponents(org.icepdf.core.views.DocumentViewController.CURSOR_DEFAULT);
        }
        if (actualToolMayHaveChanged){
            reflectToolInToolButtons();
        }

        // disabled the annotation edit panels, selection will activate them again. 
        if (annotationPanel != null){
            annotationPanel.setEnabled(false);
        }

        // repaint the page views. 
        documentViewController.getViewContainer().repaint();
    }


    private void setCursorOnComponents(final int cursorType) {
        Cursor cursor = documentViewController.getViewCursor(cursorType);
        if (utilityTabbedPane != null)
            utilityTabbedPane.setCursor(cursor);
//        if( documentViewController != null ) {
//            documentViewController.setViewCursor( cursorType );
//        }
        if (viewer != null)
            viewer.setCursor(cursor);
    }

    /**
     * Sets the state of the "Tools" buttons. This ensure that correct button
     * is depressed when the state of the Document class specifies it.
     */
    private void reflectToolInToolButtons() {
        reflectSelectionInButton(panToolButton,
                documentViewController.isToolModeSelected(
                        DocumentViewModelImpl.DISPLAY_TOOL_PAN
                ));
        reflectSelectionInButton(textSelectToolButton,
                documentViewController.isToolModeSelected(
                        DocumentViewModelImpl.DISPLAY_TOOL_TEXT_SELECTION
                ));
        reflectSelectionInButton(selectToolButton,
                documentViewController.isToolModeSelected(
                        DocumentViewModelImpl.DISPLAY_TOOL_SELECTION
                ));
        reflectSelectionInButton(linkAnnotationToolButton,
                documentViewController.isToolModeSelected(
                        DocumentViewModelImpl.DISPLAY_TOOL_LINK_ANNOTATION
                ));
        reflectSelectionInButton(zoomInToolButton,
                documentViewController.isToolModeSelected(
                        DocumentViewModelImpl.DISPLAY_TOOL_ZOOM_IN
                ));
        reflectSelectionInButton(zoomOutToolButton,
                documentViewController.isToolModeSelected(
                        DocumentViewModelImpl.DISPLAY_TOOL_ZOOM_OUT
                ));
    }

    /**
     * Sets the state of the "Fit" buttons.  This ensure that correct button
     * is depressed when the state of the Document class specifies it.
     */
    private void reflectFitInFitButtons() {
        if (document == null) {
            return;
        }
        reflectSelectionInButton(fitWidthButton,
                isDocumentFitMode(org.icepdf.core.views.DocumentViewController.PAGE_FIT_WINDOW_WIDTH));
        reflectSelectionInButton(fitHeightButton,
                isDocumentFitMode(org.icepdf.core.views.DocumentViewController.PAGE_FIT_WINDOW_HEIGHT));
        reflectSelectionInButton(fitActualSizeButton,
                isDocumentFitMode(org.icepdf.core.views.DocumentViewController.PAGE_FIT_ACTUAL_SIZE));
    }

    /**
     * Sets the state of the "Document View" buttons.  This ensure that correct button
     * is depressed when the state of the view controller class specifies it.
     */
    private void reflectDocumentViewModeInButtons() {
        if (document == null) {
            return;
        }
        reflectSelectionInButton(
                singlePageViewContinuousButton, isDocumentViewMode(
                        DocumentViewControllerImpl.ONE_COLUMN_VIEW));
        reflectSelectionInButton(
                facingPageViewNonContinuousButton, isDocumentViewMode(
                        DocumentViewControllerImpl.TWO_PAGE_RIGHT_VIEW));
        reflectSelectionInButton(
                facingPageViewContinuousButton, isDocumentViewMode(
                        DocumentViewControllerImpl.TWO_COLUMN_RIGHT_VIEW));
        reflectSelectionInButton(
                singlePageViewNonContinuousButton, isDocumentViewMode(
                        DocumentViewControllerImpl.ONE_PAGE_VIEW));
    }

    private void reflectSelectionInButton(AbstractButton btn, boolean selected) {
        if (btn != null) {
            if (btn.isSelected() != selected) {
                btn.setSelected(selected);
            }

            btn.setBorder(
                    selected ?
                            BorderFactory.createLoweredBevelBorder() :
                            BorderFactory.createEmptyBorder());
        }
    }

    /**
     * Utility method for opening a file. Shows a dialog for the user to
     * select which file to open.
     */
    public void openFile() {
        // Create and display a file open dialog
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.addChoosableFileFilter(FileExtensionUtils.getPDFFileFilter());
        if (ViewModel.getDefaultFile() != null) {
            fileChooser.setCurrentDirectory(ViewModel.getDefaultFile());
            fileChooser.setSelectedFile(ViewModel.getDefaultFile());
            fileChooser.ensureFileIsVisible(ViewModel.getDefaultFile());
        }
        // show the dialog
        fileChooser.setDialogTitle(messageBundle.getString("viewer.dialog.openFile.title"));
        int returnVal = fileChooser.showOpenDialog(viewer);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File file = fileChooser.getSelectedFile();
            // trying to get rid of shadow left by file chooser
            fileChooser.setVisible(false);
            // make sure file being opened is valid
            String extension = FileExtensionUtils.getExtension(file);
            if (extension != null) {
                if (extension.equals(FileExtensionUtils.pdf)) {
                    if (viewer != null) {
                        viewer.toFront();
                        viewer.requestFocus();
                        Graphics g = viewer.getGraphics();
                        if (g != null)
                            viewer.paint(g);
                    }
                    openFileInSomeViewer(file);
                } else {
                    org.icepdf.ri.util.Resources.showMessageDialog(viewer,
                            JOptionPane.INFORMATION_MESSAGE,
                            messageBundle,
                            "viewer.dialog.openFile.error.title",
                            "viewer.dialog.openFile.error.msg",
                            file.getPath());
                }

                // save the default directory
                ViewModel.setDefaultFile(file);
            }
        }
        fileChooser.setVisible(false);
    }

    private void openFileInSomeViewer(File file) {
        // openDocument the file
        if (document == null) {
            openDocument(file.getPath());
        } else if (windowManagementCallback != null) {
            int oldTool = SwingController.this.getDocumentViewToolMode();
            setDisplayTool(DocumentViewModelImpl.DISPLAY_TOOL_WAIT);
            try {
                windowManagementCallback.newWindow(file.getPath());
            }
            finally {
                setDisplayTool(oldTool);
            }
        }
    }

    public void openFileInSomeViewer(String filename) {
        try {
            File pdfFile = new File( filename);
            openFileInSomeViewer(pdfFile);
        } catch (Exception e) {

        }
    }


    /**
     * Open a file specified by the given path name.
     *
     * @param pathname String representing a valid file path
     */
    public void openDocument(String pathname) {
        if (pathname != null && pathname.length() > 0) {
            try {
                // dispose a currently open document, if one.
                if (document != null) {
                    closeDocument();
                }

                setDisplayTool(DocumentViewModelImpl.DISPLAY_TOOL_WAIT);

                // load the document
                document = new Document();
                // create default security callback is user has not created one
                if (documentViewController.getSecurityCallback() == null){
                    document.setSecurityCallback(
                            new MyGUISecurityCallback(viewer, messageBundle));
                }
                document.setFile(pathname);

                commonNewDocumentHandling(pathname);
            }
            catch (PDFException e) {
                org.icepdf.ri.util.Resources.showMessageDialog(
                        viewer,
                        JOptionPane.INFORMATION_MESSAGE,
                        messageBundle,
                        "viewer.dialog.openDocument.pdfException.title",
                        "viewer.dialog.openDocument.pdfException.msg",
                        pathname);
                document = null;
                logger.log(Level.FINE, "Error opening document.", e);
            }
            catch (PDFSecurityException e) {
                org.icepdf.ri.util.Resources.showMessageDialog(
                        viewer,
                        JOptionPane.INFORMATION_MESSAGE,
                        messageBundle,
                        "viewer.dialog.openDocument.pdfSecurityException.title",
                        "viewer.dialog.openDocument.pdfSecurityException.msg",
                        pathname);
                document = null;
                logger.log(Level.FINE, "Error opening document.", e);
            }
            catch (Exception e) {
                org.icepdf.ri.util.Resources.showMessageDialog(
                        viewer,
                        JOptionPane.INFORMATION_MESSAGE,
                        messageBundle,
                        "viewer.dialog.openDocument.exception.title",
                        "viewer.dialog.openDocument.exception.msg",
                        pathname);
                document = null;
                logger.log(Level.FINE, "Error opening document.", e);
            }
            finally {
                setDisplayTool(DocumentViewModelImpl.DISPLAY_TOOL_PAN);
            }
        }
    }

    /**
     * Utility method for opening a URL. Shows a dialog for the user to
     * type what URL to open.
     */
    public void openURL() {
        String urlLocation = ((ViewModel.getDefaultURL() != null) ? ViewModel.getDefaultURL() : "");
        // display url input dialog
        Object o = JOptionPane.showInputDialog(
                viewer,
                "URL:",
                "Open URL",
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                urlLocation);
        if (o != null) {
            URLAccess urlAccess = URLAccess.doURLAccess(o.toString());
            urlAccess.closeConnection();
            if (urlAccess.errorMessage != null) {
                org.icepdf.ri.util.Resources.showMessageDialog(
                        viewer,
                        JOptionPane.INFORMATION_MESSAGE,
                        messageBundle,
                        "viewer.dialog.openURL.exception.title",
                        "viewer.dialog.openURL.exception.msg",
                        urlAccess.errorMessage,
                        urlAccess.urlLocation
                );
            } else {
                if (viewer != null) {
                    viewer.toFront();
                    viewer.requestFocus();
                    Graphics g = viewer.getGraphics();
                    if (g != null)
                        viewer.paint(g);
                }
                openURLInSomeViewer(urlAccess.url);
            }
            ViewModel.setDefaultURL(urlAccess.urlLocation);
            urlAccess.dispose();
        }
    }

    private void openURLInSomeViewer(URL url) {
        // openDocument the URL
        if (document == null) {
            openDocument(url);
        } else if (windowManagementCallback != null) {
            int oldTool = SwingController.this.getDocumentViewToolMode();
            setDisplayTool(DocumentViewModelImpl.DISPLAY_TOOL_WAIT);
            try {
                windowManagementCallback.newWindow(url);
            }
            finally {
                setDisplayTool(oldTool);
            }
        }
    }

    /**
     * Open a URL specifed by the location variable.
     *
     * @param location location of a valid PDF document
     */
    public void openDocument(URL location) {
        if (location != null) {
            try {
                // dispose a currently open document, if one.
                if (document != null) {
                    closeDocument();
                }

                setDisplayTool(DocumentViewModelImpl.DISPLAY_TOOL_WAIT);

                // load the document
                document = new Document();
                // create default security callback is user has not created one
                if (documentViewController.getSecurityCallback() == null){
                    document.setSecurityCallback(
                            new MyGUISecurityCallback(viewer, messageBundle));
                }
                document.setUrl(location);

                commonNewDocumentHandling(location.getPath());
            }
            catch (PDFException e) {
                org.icepdf.ri.util.Resources.showMessageDialog(
                        viewer,
                        JOptionPane.INFORMATION_MESSAGE,
                        messageBundle,
                        "viewer.dialog.openDocument.pdfException.title",
                        "viewer.dialog.openDocument.pdfException.msg",
                        location);
                document = null;
                logger.log(Level.FINE, "Error opening document.", e);
            }
            catch (PDFSecurityException e) {
                org.icepdf.ri.util.Resources.showMessageDialog(
                        viewer,
                        JOptionPane.INFORMATION_MESSAGE,
                        messageBundle,
                        "viewer.dialog.openDocument.pdfSecurityException.title",
                        "viewer.dialog.openDocument.pdfSecurityException.msg",
                        location);
                document = null;
                logger.log(Level.FINE, "Error opening document.", e);
            }
            catch (Exception e) {
                org.icepdf.ri.util.Resources.showMessageDialog(
                        viewer,
                        JOptionPane.INFORMATION_MESSAGE,
                        messageBundle,
                        "viewer.dialog.openDocument.exception.title",
                        "viewer.dialog.openDocument.exception.msg",
                        location);
                document = null;
                logger.log(Level.FINE, "Error opening document.", e);
            }
            finally {
                setDisplayTool(DocumentViewModelImpl.DISPLAY_TOOL_PAN);
            }
        }
    }

    /**
     * Opens a Document via the specified InputStream. This method is a convenience method provided for
     * backwards compatibility.
     * <p/>
     * <p><b>Note:</b> This method is less efficient than
     * {@see #openDocument(Stringpathname)} or {@see #openDocument(URLlocation)} as it
     * may have to do intermediary data copying, using more memory.
     *
     * @param inputStream InputStream containing a valid PDF document.
     * @param description When in the GUI for describing this document.
     * @param pathOrURL   Either a file path, or file name, or URL, describing the
     *                    origin of the PDF file. This is typically null. If non-null, it is
     *                    used to populate the default file name in the File..Save a Copy
     *                    dialog summoned in saveFile()
     */
    public void openDocument(InputStream inputStream, String description, String pathOrURL) {
        if (inputStream != null) {
            try {
                // dispose a currently open document, if one.
                if (document != null) {
                    closeDocument();
                }

                setDisplayTool(DocumentViewModelImpl.DISPLAY_TOOL_WAIT);

                // load the document
                document = new Document();
                // create default security callback is user has not created one
                if (documentViewController.getSecurityCallback() == null){
                    document.setSecurityCallback(
                            new MyGUISecurityCallback(viewer, messageBundle));
                }
                document.setInputStream(inputStream, pathOrURL);

                commonNewDocumentHandling(description);
            }
            catch (PDFException e) {
                org.icepdf.ri.util.Resources.showMessageDialog(
                        viewer,
                        JOptionPane.INFORMATION_MESSAGE,
                        messageBundle,
                        "viewer.dialog.openDocument.pdfException.title",
                        "viewer.dialog.openDocument.pdfException.msg",
                        description);
                document = null;
                logger.log(Level.FINE, "Error opening document.", e);
            }
            catch (PDFSecurityException e) {
                org.icepdf.ri.util.Resources.showMessageDialog(
                        viewer,
                        JOptionPane.INFORMATION_MESSAGE,
                        messageBundle,
                        "viewer.dialog.openDocument.pdfSecurityException.title",
                        "viewer.dialog.openDocument.pdfSecurityException.msg",
                        description);
                document = null;
                logger.log(Level.FINE, "Error opening document.", e);
            }
            catch (Exception e) {
                org.icepdf.ri.util.Resources.showMessageDialog(
                        viewer,
                        JOptionPane.INFORMATION_MESSAGE,
                        messageBundle,
                        "viewer.dialog.openDocument.exception.title",
                        "viewer.dialog.openDocument.exception.msg",
                        description);
                document = null;
                logger.log(Level.FINE, "Error opening document.", e);
            }
            finally {
                setDisplayTool(DocumentViewModelImpl.DISPLAY_TOOL_PAN);
            }
        }
    }

    /**
     * Opens a Document via the specified byte array.
     *
     * @param data        Byte array containing a valid PDF document.
     * @param offset      the index into the byte array where the PDF data begins
     * @param length      the number of bytes in the byte array belonging to the PDF data
     * @param description When in the GUI for describing this document.
     * @param pathOrURL   Either a file path, or file name, or URL, describing the
     *                    origin of the PDF file. This is typically null. If non-null, it is
     *                    used to populate the default file name in the File..Save a Copy
     *                    dialog summoned in saveFile()
     */
    public void openDocument(byte[] data, int offset, int length, String description, String pathOrURL) {
        if (data != null) {
            try {
                // dispose a currently open document, if one.
                if (document != null) {
                    closeDocument();
                }

                setDisplayTool(DocumentViewModelImpl.DISPLAY_TOOL_WAIT);

                // load the document
                document = new Document();
                // create default security callback is user has not created one
                if (documentViewController.getSecurityCallback() == null){
                    document.setSecurityCallback(
                            new MyGUISecurityCallback(viewer, messageBundle));
                }
                document.setByteArray(data, offset, length, pathOrURL);

                commonNewDocumentHandling(description);
            }
            catch (PDFException e) {
                org.icepdf.ri.util.Resources.showMessageDialog(
                        viewer,
                        JOptionPane.INFORMATION_MESSAGE,
                        messageBundle,
                        "viewer.dialog.openDocument.pdfException.title",
                        "viewer.dialog.openDocument.pdfException.msg",
                        description);
                document = null;
                logger.log(Level.FINE, "Error opening document.", e);
            }
            catch (PDFSecurityException e) {
                org.icepdf.ri.util.Resources.showMessageDialog(
                        viewer,
                        JOptionPane.INFORMATION_MESSAGE,
                        messageBundle,
                        "viewer.dialog.openDocument.pdfSecurityException.title",
                        "viewer.dialog.openDocument.pdfSecurityException.msg",
                        description);
                document = null;
                logger.log(Level.FINE, "Error opening document.", e);
            }
            catch (Exception e) {
                org.icepdf.ri.util.Resources.showMessageDialog(
                        viewer,
                        JOptionPane.INFORMATION_MESSAGE,
                        messageBundle,
                        "viewer.dialog.openDocument.exception.title",
                        "viewer.dialog.openDocument.exception.msg",
                        description);
                document = null;
                logger.log(Level.FINE, "Error opening document.", e);
            }
            finally {
                setDisplayTool(DocumentViewModelImpl.DISPLAY_TOOL_PAN);
            }
        }
    }

    private void commonNewDocumentHandling(String fileDescription) {
        // setup custom search utility tool
        if (searchPanel != null)
            searchPanel.setDocument(document);

        // utility pane visibility
        boolean showUtilityPane = false;

        // get data on how the view should look from the document dictionary
        // if no data, use settings from last viewed document, fit and view type
        Catalog catalog = document.getCatalog();

        // Page layout, the default value is singlePage, but we currently
        // remember the users last view mode via the properties manager.  Possible
        // values are SinglePage, OnceColumn, TwoColumnLeft, TwoColumRight,
        // TwoPageLeft, TwoPageRight.
        Object tmp = catalog.getObject("PageLayout");
        if (tmp != null && tmp instanceof Name) {
            String pageLayout = ((Name) tmp).getName();
            int viewType = DocumentViewControllerImpl.ONE_PAGE_VIEW;
            if (pageLayout.equalsIgnoreCase("OneColumn")) {
                viewType = DocumentViewControllerImpl.ONE_COLUMN_VIEW;
            } else if (pageLayout.equalsIgnoreCase("TwoColumnLeft")) {
                viewType = DocumentViewControllerImpl.TWO_COLUMN_LEFT_VIEW;
            } else if (pageLayout.equalsIgnoreCase("TwoColumnRight")) {
                viewType = DocumentViewControllerImpl.TWO_COLUMN_RIGHT_VIEW;
            } else if (pageLayout.equalsIgnoreCase("TwoPageLeft")) {
                viewType = DocumentViewControllerImpl.TWO_PAGE_LEFT_VIEW;
            } else if (pageLayout.equalsIgnoreCase("TwoPageRight")) {
                viewType = DocumentViewControllerImpl.TWO_PAGE_RIGHT_VIEW;
            }
            documentViewController.setViewType(viewType);
        }

        if (utilityTabbedPane != null) {
            // Page mode by default is UseNone, where other options are, UseOutlines,
            // UseThumbs, FullScreen (ignore), UseOC(ignore), UseAttachements(ignore);
            tmp = catalog.getObject("PageMode");
            if (tmp != null && tmp instanceof Name) {
                String pageMode = ((Name) tmp).getName();
                showUtilityPane = pageMode.equalsIgnoreCase("UseOutlines");
            }
        }

        // initiates the view layout model, page coordinates and preferred size
        documentViewController.setDocument(document);

        // Refresh the properties manager object if we don't already have one
        // This would be not null if the UI was constructed manually
        if ((propertiesManager == null) && (windowManagementCallback != null)) {
            propertiesManager = windowManagementCallback.getProperties();
        }

        // Set the default zoom level from the properties file
        float defaultZoom = (float)PropertiesManager.checkAndStoreDoubleProperty(propertiesManager,
                                                                                 PropertiesManager.PROPERTY_DEFAULT_ZOOM_LEVEL);
        documentViewController.setZoom(defaultZoom);

        // Set the default page fit mode
        setPageFitMode(PropertiesManager.checkAndStoreIntegerProperty(propertiesManager,
                                                                      PropertiesManager.PROPERTY_DEFAULT_PAGEFIT,
                                                                      org.icepdf.core.views.DocumentViewController.PAGE_FIT_NONE), false);

        // Apply any ViewerPreferences from the doc
        applyViewerPreferences(catalog, propertiesManager);

        // Only show utility panel if there is an outline
        OutlineItem item = null;
        Outlines outlines = document.getCatalog().getOutlines();
        if (outlines != null && outlinesTree != null)
            item = outlines.getRootOutlineItem();
        if (item != null) {
            outlinesTree.setModel(new DefaultTreeModel(new OutlineItemTreeNode(item)));
            outlinesTree.setRootVisible(!item.isEmpty());
            outlinesTree.setShowsRootHandles(true);
            if (utilityTabbedPane != null && outlinesScrollPane != null) {
                if (utilityTabbedPane.indexOfComponent(outlinesScrollPane) > -1) {
                    utilityTabbedPane.setEnabledAt(
                            utilityTabbedPane.indexOfComponent(outlinesScrollPane),
                            true);
                    utilityTabbedPane.setSelectedComponent(outlinesScrollPane);
                }
            }
        } else {
            if (utilityTabbedPane != null && outlinesScrollPane != null) {
                if (utilityTabbedPane.indexOfComponent(outlinesScrollPane) > -1) {
                    utilityTabbedPane.setEnabledAt(
                            utilityTabbedPane.indexOfComponent(outlinesScrollPane),
                            false);
                }
            }

            // Try to select the search panel
            if (!safelySelectUtilityPanel(searchPanel)) {
                // If that fails, try to select the annotationPanel
                safelySelectUtilityPanel(annotationPanel);
            }
        }
        setUtilityPaneVisible(showUtilityPane);

        // add to the main pdfContentPanel the document peer
        if (viewer != null) {
            Object[] messageArguments = new Object[]{fileDescription};
            MessageFormat formatter = new MessageFormat(
                    messageBundle.getString("viewer.window.title.open.default"));
            viewer.setTitle(formatter.format(messageArguments));

        }

        // disable the annotation properties panel by default
        if (annotationPanel != null) {
            annotationPanel.setEnabled(false);
        }

        // set the go to page combo box in the mainToolbar
        reflectStateInComponents();
        updateDocumentView();
    }

    /**
     * Close the currently opened PDF Document.  The toolbar component's states
     * are also changed to their default values and made inactive.<br>
     * <br>
     * Note: If you create several SwingControllers to manipulate a single
     * Document, and each SwingController would be disposed of at a different
     * time, while the others continue to use that same shared Document, then
     * you should not call Document.dispose() inside of here, or alternatively
     * implement reference counting, so that only the last SwingController would
     * call Document.dispose()
     *
     * @see Document
     */
    public void closeDocument() {
        // Clear the SearchPane, but also stop any search in progress
        if (searchPanel != null)
            searchPanel.setDocument(null);

        // set the default cursor.  
        documentViewController.closeDocument();

        // clear search controller caches.
        documentSearchController.dispose();

        // free the document
        if (document != null) {
            document.dispose();
            document = null;
        }

        // remove the page numbers in the go to page combo box in the mainToolbar
        if (currentPageNumberTextField != null)
            currentPageNumberTextField.setText("");
        if (numberOfPagesLabel != null)
            numberOfPagesLabel.setText("");
        if (currentPageNumberTextField != null)
            currentPageNumberTextField.setEnabled(false);
        if (statusLabel != null)
            statusLabel.setText(" ");
        // set the scale level back to 100%, default
        if (zoomComboBox != null)
            zoomComboBox.setSelectedItem(NumberFormat.getPercentInstance().format(1.0));
        // update thew view to show no pages in the view
        updateDocumentView();

        TreeModel treeModel = (outlinesTree != null) ? outlinesTree.getModel() : null;
        if (treeModel != null) {
            OutlineItemTreeNode root = (OutlineItemTreeNode) treeModel.getRoot();
            if (root != null)
                root.recursivelyClearOutlineItems();
            outlinesTree.getSelectionModel().clearSelection();
            outlinesTree.getSelectionModel().setSelectionPath(null);
            outlinesTree.setSelectionPath(null);
            outlinesTree.setModel(null);
        }
        setUtilityPaneVisible(false);
        if (viewer != null) {
            viewer.setTitle(messageBundle.getString("viewer.window.title.default"));
            viewer.invalidate();
            viewer.validate();
            viewer.getContentPane().repaint();
        }

        reflectStateInComponents();
    }

    /**
     * Way to dispose of all memory references, and clean up the Document resources<br>
     * <br>
     * Note: If you create several SwingControllers to manipulate a single
     * Document, and each SwingController would be disposed of at a different
     * time, while the others continue to use that same shared Document, then
     * you should not call Document.dispose() inside of here. Alternatively,
     * implement reference counting, so that only the last SwingController would
     * call Document.dispose()
     */
    public void dispose() {
        if (disposed)
            return;
        disposed = true;

        closeDocument();

        openFileMenuItem = null;
        openURLMenuItem = null;
        closeMenuItem = null;
        saveAsFileMenuItem = null;
        exportTextMenuItem = null;
        exportSVGMenuItem = null;
        permissionsMenuItem = null;
        informationMenuItem = null;
        printSetupMenuItem = null;
        printMenuItem = null;
        exitMenuItem = null;

        fitActualSizeMenuItem = null;
        fitPageMenuItem = null;
        fitWidthMenuItem = null;
        zoomInMenuItem = null;
        zoomOutMenuItem = null;
        rotateLeftMenuItem = null;
        rotateRightMenuItem = null;
        showHideToolBarMenuItem = null;
        showHideUtilityPaneMenuItem = null;

        firstPageMenuItem = null;
        previousPageMenuItem = null;
        nextPageMenuItem = null;
        lastPageMenuItem = null;
        searchMenuItem = null;
        goToPageMenuItem = null;

        minimiseAllMenuItem = null;
        bringAllToFrontMenuItem = null;
        windowListMenuItems = null;

        aboutMenuItem = null;

        openFileButton = null;
        saveAsFileButton = null;
        printButton = null;
        searchButton = null;
        showHideUtilityPaneButton = null;

        firstPageButton = null;
        previousPageButton = null;
        nextPageButton = null;
        lastPageButton = null;
        if (currentPageNumberTextField != null) {
            currentPageNumberTextField.removeActionListener(this);
            currentPageNumberTextField.removeFocusListener(this);
            currentPageNumberTextField.removeKeyListener(this);
            currentPageNumberTextField = null;
        }
        numberOfPagesLabel = null;

        zoomInButton = null;
        zoomOutButton = null;
        if (zoomComboBox != null) {
            zoomComboBox.removeItemListener(this);
            zoomComboBox = null;
        }

        fitActualSizeButton = null;
        fitHeightButton = null;
        fitWidthButton = null;

        rotateLeftButton = null;
        rotateRightButton = null;

        panToolButton = null;
        zoomInToolButton = null;
        zoomOutToolButton = null;
        textSelectToolButton = null;
        selectToolButton = null;
        linkAnnotationToolButton = null;

        fontEngineButton = null;

        completeToolBar = null;

        outlinesTree = null;
        if (outlinesScrollPane != null) {
            outlinesScrollPane.removeAll();
            outlinesScrollPane = null;
        }
        if (searchPanel != null) {
            searchPanel.dispose();
            searchPanel = null;
        }
        if (utilityTabbedPane != null) {
            utilityTabbedPane.removeAll();
            utilityTabbedPane = null;
        }

        // Clean up the document view controller
        if (documentViewController != null) {
            documentViewController.dispose();
        }

        // clean up search controller
        if (documentSearchController != null) {
            documentSearchController.dispose();
        }

        if (utilityAndDocumentSplitPane != null) {
            utilityAndDocumentSplitPane.removeAll();
        }

        statusLabel = null;
        if (viewer != null) {
            viewer.removeWindowListener(this);
            viewer.removeAll();
        }
        viewModel = null;

        windowManagementCallback = null;
    }

    /**
     * Utility method for saving a copy of the currently opened
     * PDF to a file. This will check all valid permissions and 
     * show a file save dialog for the user to select where to 
     * save the file to, and what name to give it.
     */
    public void saveFile() {
        // Ensure we actually CAN save the document in the first place
        if (!havePermissionToModifyDocument()) {
            org.icepdf.ri.util.Resources.showMessageDialog(
                    viewer,
                    JOptionPane.INFORMATION_MESSAGE,
                    messageBundle,
                    "viewer.dialog.saveAs.noPermission.title",
                    "viewer.dialog.saveAs.noPermission.msg");
            return;
        }
        
        // Create and display a file saving dialog
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(messageBundle.getString("viewer.dialog.saveAs.title"));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.addChoosableFileFilter(FileExtensionUtils.getPDFFileFilter());
        if (ViewModel.getDefaultFile() != null) {
            fileChooser.setCurrentDirectory(ViewModel.getDefaultFile());
        }

        // See if we can come up with a default file name
        // We want the bytes from whence, but the file name of origin
        String origin = document.getDocumentOrigin();
        String originalFileName = null;
        if (origin != null) {
            int lastSeparator = Math.max(
                    Math.max(
                            origin.lastIndexOf("/"),
                            origin.lastIndexOf("\\")),
                    origin.lastIndexOf(File.separator) // Might not be / or \
            );
            if (lastSeparator >= 0) {
                originalFileName = origin.substring(lastSeparator + 1);
                if (originalFileName != null && originalFileName.length() > 0) {
                    // Set the selected file to a slightly modified name of the original
                    fileChooser.setSelectedFile(new File(generateNewSaveName(originalFileName)));
                }
                else {
                    originalFileName = null;
                }
            }
        }

        // show the dialog
        int returnVal = fileChooser.showSaveDialog(viewer);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            // make sure file path being saved to is valid
            String extension = FileExtensionUtils.getExtension(file);
            if (extension == null) {
                org.icepdf.ri.util.Resources.showMessageDialog(
                        viewer,
                        JOptionPane.INFORMATION_MESSAGE,
                        messageBundle,
                        "viewer.dialog.saveAs.noExtensionError.title",
                        "viewer.dialog.saveAs.noExtensionError.msg");
                saveFile();
            } else if (!extension.equals(FileExtensionUtils.pdf)) {
                org.icepdf.ri.util.Resources.showMessageDialog(
                        viewer,
                        JOptionPane.INFORMATION_MESSAGE,
                        messageBundle,
                        "viewer.dialog.saveAs.extensionError.title",
                        "viewer.dialog.saveAs.extensionError.msg",
                        file.getName());
                saveFile();
            } else if ((originalFileName != null) &&
                       (originalFileName.equalsIgnoreCase(file.getName()))) {
                // Ensure a unique filename
                org.icepdf.ri.util.Resources.showMessageDialog(
                        viewer,
                        JOptionPane.INFORMATION_MESSAGE,
                        messageBundle,
                        "viewer.dialog.saveAs.noneUniqueName.title",
                        "viewer.dialog.saveAs.noneUniqueName.msg",
                        file.getName());
                saveFile();
            } else {
                // save file stream
                try {
                    // If we don't know where the file came from, it's because we
                    //  used Document.setInputStream() or Document.setByteArray(),
                    //  or we used setUrl() with disk caching disabled.
                    //  with no path or URL as the origin.
                    // Note that we used to detect scenarios where we could access
                    //  the file directly, or re-download it, to avoid locking our
                    //  internal data structures for long periods for large PDFs,
                    //  but that could cause problems with slow network links too,
                    //  and would complicate the incremental update code, so we're
                    //  harmonising on this approach.
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    BufferedOutputStream buf = new BufferedOutputStream(
                        fileOutputStream, 4096*2);

                    document.saveToOutputStream(buf);

                    buf.flush();
                    fileOutputStream.flush();
                    buf.close();
                    fileOutputStream.close();
                }
                catch (MalformedURLException e) {
                    logger.log(Level.FINE, "Malformed URL Exception ", e);
                }
                catch (IOException e) {
                    logger.log(Level.FINE, "IO Exception ", e);
                }
                // save the default directory
                ViewModel.setDefaultFile(file);
            }
        }
    }

    protected String generateNewSaveName(String fileName) {
        if (fileName != null) {
            // Return the file with "-new" in the filename, before the extension
            // For example Test.pdf would become Test-new.pdf
            return fileName.substring(0, fileName.toLowerCase().indexOf(FileExtensionUtils.pdf)-1) +
                   "-new." + FileExtensionUtils.pdf;
        }

        return fileName;
    }

    /**
     * Utility method for exporting all of a Document's text to a text file.
     * Shows a file save dialog for the user to select where to save the
     * exported text file to, and what name to give that file.
     */
    public void exportText() {
        // Create and display a file saving dialog
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(messageBundle.getString("viewer.dialog.exportText.title"));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.addChoosableFileFilter(FileExtensionUtils.getTextFileFilter());
        if (ViewModel.getDefaultFile() != null) {
            fileChooser.setCurrentDirectory(ViewModel.getDefaultFile());
        }
        // show the dialog
        int returnVal = fileChooser.showSaveDialog(viewer);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            // make sure file being opened is valid
            String extension = FileExtensionUtils.getExtension(file);
            if (extension != null) {
                // save the default directory
                ViewModel.setDefaultFile(file);

                TextExtractionTask textExtractionTask =
                        new TextExtractionTask(document, file, messageBundle);

                ProgressMonitor progressMonitor = new ProgressMonitor(
                        viewer, messageBundle.getString("viewer.dialog.exportText.progress.msg"),
                        "", 0, textExtractionTask.getLengthOfTask());
                progressMonitor.setProgress(0);
                progressMonitor.setMillisToDecideToPopup(0);

                TextExtractionGlue glue = new TextExtractionGlue(textExtractionTask, progressMonitor);
                Timer timer = new Timer(1000, glue); // 1000 milliseconds
                glue.setTimer(timer);

                textExtractionTask.go();
                timer.start();
            } else {
                org.icepdf.ri.util.Resources.showMessageDialog(
                        viewer,
                        JOptionPane.INFORMATION_MESSAGE,
                        messageBundle,
                        "viewer.dialog.exportText.noExtensionError.title",
                        "viewer.dialog.exportText.noExtensionError.msg");
                exportText();
            }
        }
    }

    /**
     * Utility method for exporting the current page of the Document to an SVG file.
     * Shows a file save dialog for the user to select where to save the
     * exported SVG file to, and what name to give that file.
     */
    public void exportSVG() {
        // Create and display a file saving dialog
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(messageBundle.getString("viewer.dialog.exportSVG.title"));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.addChoosableFileFilter(FileExtensionUtils.getSVGFileFilter());
        if (ViewModel.getDefaultFile() != null) {
            fileChooser.setCurrentDirectory(ViewModel.getDefaultFile());
        }
        // show the dialog
        int returnVal = fileChooser.showSaveDialog(viewer);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File file = fileChooser.getSelectedFile();
            // make sure file being opened is valid
            String extension = FileExtensionUtils.getExtension(file);
            if (extension != null) {
                if (extension.equals(FileExtensionUtils.svg)) {
                    final Document doc = document;
                    final int pageIndex = documentViewController.getCurrentPageIndex();

                    if (statusLabel != null) {
                        Object[] messageArguments = new Object[]{
                                String.valueOf(pageIndex + 1),
                                file.getName()
                        };
                        MessageFormat formatter = new MessageFormat(
                                messageBundle.getString("viewer.dialog.exportSVG.status.exporting.msg"));
                        statusLabel.setText(formatter.format(messageArguments));
                    }

                    SwingWorker worker = new SwingWorker() {
                        public Object construct() {
                            // save the file
                            String error;
                            try {
                                // It is important to create a UTF-8 encoded file.
                                OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
                                SVG.createSVG(
                                        doc,
                                        pageIndex,
                                        out);
                                out.close();
                                error = null;
                            }
                            catch (Throwable e) {
                                error = e.getMessage();
                                logger.log(Level.FINE, "Error exporting to SVG");
                            }
                            final String tmpMsg;
                            // finsished message
                            if (error != null) {
                                Object[] messageArguments = new Object[]{
                                        String.valueOf(pageIndex + 1),
                                        file.getName()
                                };
                                MessageFormat formatter =
                                        new MessageFormat(
                                                messageBundle.getString("viewer.dialog.exportSVG.status.exporting.msg"));
                                tmpMsg = formatter.format(messageArguments);
                            }
                            // problem message
                            else {
                                Object[] messageArguments = new Object[]{
                                        String.valueOf(pageIndex + 1),
                                        file.getName(),
                                        error
                                };
                                MessageFormat formatter = new MessageFormat(
                                        messageBundle.getString("viewer.dialog.exportSVG.status.error.msg"));
                                tmpMsg = formatter.format(messageArguments);
                            }
                            final String msg = tmpMsg;

                            Runnable doSwingWork = new Runnable() {
                                public void run() {
                                    if (statusLabel != null)
                                        statusLabel.setText(msg);
                                }
                            };
                            SwingUtilities.invokeLater(doSwingWork);
                            return null;
                        }
                    };
                    worker.setThreadPriority(Thread.MIN_PRIORITY);
                    worker.start();
                } else {
                    org.icepdf.ri.util.Resources.showMessageDialog(
                            viewer,
                            JOptionPane.INFORMATION_MESSAGE,
                            messageBundle,
                            "viewer.dialog.exportSVG.exportError.title",
                            "viewer.dialog.exportSVG.exportError.msg",
                            file.getName());
                }
                // save the default directory
                ViewModel.setDefaultFile(file);
            } else {
                org.icepdf.ri.util.Resources.showMessageDialog(
                        viewer,
                        JOptionPane.INFORMATION_MESSAGE,
                        messageBundle,
                        "viewer.dialog.exportSVG.noExtensionError.title",
                        "viewer.dialog.exportSVG.noExtensionError.msg");
                exportSVG();
            }
        }
    }

    /**
     * If there is a WindowManagementCallback in place, then this will invoke its quit method
     *
     * @see #setWindowManagementCallback
     * @see #getWindowManagementCallback
     */
    public void exit() {
        if (windowManagementCallback != null)
            windowManagementCallback.quit(this, viewer, null);
    }

    /**
     * Flips the visibility of the toolbar to the opposite of what it was
     *
     * @see #setToolBarVisible(boolean)
     */
    public void toggleToolBarVisibility() {
        if (completeToolBar != null)
            setToolBarVisible(!completeToolBar.isVisible());
    }

    /**
     * Sets the visibility of the toolbar
     *
     * @param show The new visibility of the toolbar
     */
    public void setToolBarVisible(boolean show) {
        if (completeToolBar != null)
            completeToolBar.setVisible(show);
        reflectStateInComponents();
    }

    /**
     * Show the About dialog. Subclasses may override this method to show an
     * alternate About dialog
     */
    public void showAboutDialog() {
        // Added to swing thread to ensure it shows up on top of main
        // browser window
        Runnable doSwingWork = new Runnable() {
            public void run() {
                AboutDialog ad = new AboutDialog(viewer, messageBundle, true,
                    AboutDialog.OK, AboutDialog.NO_TIMER);
                ad.setVisible(true);
            }
        };
        SwingUtilities.invokeLater(doSwingWork);
    }

    /**
     * Show the permissions set in the PDF file's Document, as relates to encryption,
     * altering, or extracting information from, the Document
     */
    public void showDocumentPermissionsDialog() {
        PermissionsDialog pd = new PermissionsDialog(
                viewer, document, messageBundle);
        pd.setVisible(true);
    }

    /**
     * Show information about the PDF file's Document, such as the title,
     * subject, author, keywords, creator, producer, creation date, and
     * last modification date
     */
    public void showDocumentInformationDialog() {
        DocumentInformationDialog did =
                new DocumentInformationDialog(viewer, document, messageBundle);
        did.setVisible(true);
    }

    /**
     * Show a print setup dialog, to alter the ViewerModel's PageFormat
     *
     * @see ViewModel
     */
    public void showPrintSetupDialog() {
        PrintHelper printHelper = viewModel.getPrintHelper();
        // create a new pring helper for this document instance. 
        printHelper = new PrintHelper(documentViewController, getPageTree());
        viewModel.setPrintHelper(printHelper);
        viewModel.getPrintHelper().showPrintSetupDialog();
    }

    /**
     * @param withDialog If should show a print dialog before starting to print
     */
    public void print(final boolean withDialog) {
        if (printMenuItem != null) {
            printMenuItem.setEnabled(false);
        }
        if (printButton != null) {
            printButton.setEnabled(false);
        }

        Runnable runner = new Runnable() {
            public void run() {
                initialisePrinting(withDialog);
            }
        };
        Thread t = new Thread(runner);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    /**
     * If the <code>withDialog</code> parameter is true, show a print dialog,
     * defaulted to print all pages. If the click Ok, then print the page range
     * they have specified, else if they clicked Cancel, then abort the printing
     * <p/>
     * If the <code>withDialog</code> parameter is false, then print all pages of
     * the PDF Document without showing and print dialogs
     *
     * @param withDialog If should show a print dialog before starting to print
     */
    private void initialisePrinting(final boolean withDialog) {
        boolean canPrint = havePermissionToPrint();
        if (!canPrint) {
            reenablePrintUI();
            return;
        }
        // create a new print helper, one-to-one with document
        PrintHelper printHelper = viewModel.getPrintHelper();
        printHelper = new PrintHelper(documentViewController, getPageTree());
        viewModel.setPrintHelper(printHelper);

        // set the printer to show a print dialog
        canPrint = printHelper.setupPrintService(
                0,
                document.getNumberOfPages() - 1,
                1,           // default number of copies.
                true,        // shrink to printable area
                withDialog  // show print dialogl
        );
        // if user cancelled the print job from the dialog, don't start printing
        // in the background.
        if (!canPrint) {
            reenablePrintUI();
            return;
        }
        startBackgroundPrinting(printHelper);
    }

    private void reenablePrintUI() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // enable print UI controls.
                if (printMenuItem != null) {
                    printMenuItem.setEnabled(true);
                }
                if (printButton != null) {
                    printButton.setEnabled(true);
                }
            }
        });
    }

    /**
     * Utility method to setup a printer job to run as a background process.
     *
     * @param printHelper a PrintHelper object which is already setup and ready
     *                    to be printed to.
     */
    private void startBackgroundPrinting(final PrintHelper printHelper) {
        // Create the ProgressMonitor in the Swing thread
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // launch progress dialog
                printProgressMonitor = new ProgressMonitor(viewer,
                        messageBundle.getString("viewer.dialog.printing.status.start.msg"),
                        "", 0, printHelper.getNumberOfPages());
            }
        });

        final Thread printingThread = Thread.currentThread();

        // create background printer job
        final PrinterTask printerTask = new PrinterTask(printHelper);
        // create activity monitor
        printActivityMonitor = new Timer(500,
                new ActionListener() {
                    public void actionPerformed(ActionEvent event) {

                        int limit = printHelper.getNumberOfPages();
                        int current = printHelper.getCurrentPage();
                        // update progress and label
                        printProgressMonitor.setProgress(current);

                        // progress bar for printing
                        Object[] messageArguments = new Object[]{
                                String.valueOf(current),
                                String.valueOf(limit)
                        };
                        MessageFormat formatter =
                                new MessageFormat(
                                        messageBundle.getString("viewer.dialog.printing.status.progress.msg"));
                        printProgressMonitor.setNote(formatter.format(messageArguments));

                        // check for job completed or cancelled.
                        if (!printingThread.isAlive() || printProgressMonitor.isCanceled()) {
                            // stop the timers, monitors and thread.
                            printProgressMonitor.close();
                            printActivityMonitor.stop();
                            printerTask.cancel();
                            // enable print UI controls.
                            if (printMenuItem != null) {
                                printMenuItem.setEnabled(true);
                            }
                            if (printButton != null) {
                                printButton.setEnabled(true);
                            }
                        }
                    }
                });
        // start the timer.
        printActivityMonitor.start();

        // start print job
        printerTask.run();
    }


    /**
     * Takes the page number that the user has typed into the text field,
     * converts it into a page index, and then displays that page
     */
    public void showPageFromTextField() {
        String ob = currentPageNumberTextField.getText();
        if (ob != null) {
            try {
                int pageIndex = Integer.parseInt(ob) - 1;
                showPage(pageIndex);
            }
            catch (NumberFormatException nfe) {
                logger.log(Level.FINE, "Error converting page number.");
            }
        }
    }

    /**
     * When the user selects an OutlineItem from the Outlines (Bookmarks) JTree,
     * this displays the relevant target portion of the PDF Document
     */
    public void followOutlineItem(OutlineItem o) {
        if (o == null)
            return;

        // capture the action if no destination is found and point to the
        // actions destination information
        Destination dest = o.getDest();
        if (o.getAction() != null) {
            Action action = o.getAction();
            if (action instanceof GoToAction) {
                dest = ((GoToAction) action).getDestination();
            } else if (action instanceof URIAction) {
                BareBonesBrowserLaunch.openURL(
                        ((URIAction) action).getURI());
            } else {
                Library library = action.getLibrary();
                Hashtable entries = action.getEntries();
                dest = new Destination(library, library.getObject(entries, "D"));
            }
        }

        // Process the destination information
        if (dest == null)
            return;

        int oldTool = getDocumentViewToolMode();
        try {
            // set hour glass
            setDisplayTool(DocumentViewModelImpl.DISPLAY_TOOL_WAIT);
            // let the document view controller resolve the destination
            documentViewController.setDestinationTarget(dest);
        }
        finally {
            // set the icon back to the pointer
            setDisplayTool(oldTool);
        }
    }

    // Utility method which alows copy or move drag actions
    private boolean isDragAcceptable(DropTargetDragEvent event) {
        // check to make sure that we only except the copy action
        return (event.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) != 0;
    }

    // Utility method which allows copy or move drop actions
    private boolean isDropAcceptable(DropTargetDropEvent event) {
        // check to make sure that we only except the copy action
        return (event.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) != 0;
    }

    /**
     * Increases the current page visualization zoom factor by 20%.
     */
    public void zoomIn() {

        // zoom in the view
        documentViewController.setZoomIn();

//        doCommonZoomUIUpdates();
    }

    /**
     * Decreases the current page visualization zoom factor by 20%.
     */
    public void zoomOut() {
        documentViewController.setZoomOut();
//        doCommonZoomUIUpdates();
    }

    /**
     * Zoom to a new zoom level, without centering on any new specific point
     *
     * @param zoom
     */
    public void setZoom(float zoom) {
        documentViewController.setZoom(zoom);
    }

    public void doCommonZoomUIUpdates(boolean becauseOfValidFitMode) {
        // update gui
        reflectZoomInZoomComboBox();    // Might change fit value
        if (!becauseOfValidFitMode)
            setPageFitMode(org.icepdf.core.views.DocumentViewController.PAGE_FIT_NONE, false);
    }

    /**
     * Returns tree if there is a current page associated with this controller.
     *
     * @return true if their is a currentpage, otherwise false.
     */
    public boolean isCurrentPage() {
        PageTree pageTree = getPageTree();
        if (pageTree == null)
            return false;
        Page page = pageTree.getPage(documentViewController.getCurrentPageIndex(), this);
        boolean isCurrentPage = page != null;
        pageTree.releasePage(page, this);
        return isCurrentPage;
    }

    /**
     * Gives access to the currently openned Document's Catalog's PageTree
     *
     * @return PageTree
     */
    public PageTree getPageTree() {
        if (document == null)
            return null;
        return document.getPageTree();
    }

    /**
     * Sets the ViewerModel's current page index, and updates the display
     * to show the newly selected page
     *
     * @param nPage Index of the Page to show
     * @see org.icepdf.ri.common.views.DocumentViewControllerImpl#setCurrentPageIndex
     */
    public void showPage(int nPage) {
        if (nPage >= 0 && nPage < getPageTree().getNumberOfPages()) {
            releaseOldPage();
            documentViewController.setCurrentPageIndex(nPage);
            updateDocumentView();
        }
    }

    /**
     * Adds delta to the ViewerModel's current page index, and updates the display
     * to show the newly selected page. A positive delta indicates moving to later pages,
     * and a negative delta would move to a previous page
     *
     * @param delta Signed integer that's added to the current page index
     * @see org.icepdf.ri.common.views.DocumentViewControllerImpl#getCurrentPageIndex
     * @see org.icepdf.ri.common.views.DocumentViewControllerImpl#setCurrentPageIndex
     */
    public void goToDeltaPage(int delta) {
        int currPage = documentViewController.getCurrentPageIndex();
        int nPage = currPage + delta;
        int totalPages = getPageTree().getNumberOfPages();
        if (totalPages == 0)
            return;
        if (nPage >= totalPages)
            nPage = totalPages - 1;
        if (nPage < 0)
            nPage = 0;
        if (nPage != currPage) {
            releaseOldPage();
            documentViewController.setCurrentPageIndex(nPage);
            updateDocumentView();
        }
    }

    private void releaseOldPage() {
        PageTree pages = getPageTree();
        if (pages != null)
            pages.releasePage(documentViewController.getCurrentPageIndex(), this);
    }

    public void updateDocumentView() {

        if (disposed)
            return;
        int oldTool = getDocumentViewToolMode();
        try {
            setDisplayTool(DocumentViewModelImpl.DISPLAY_TOOL_WAIT);

            reflectPageChangeInComponents();


            PageTree pageTree = getPageTree();

            if (currentPageNumberTextField != null)
                currentPageNumberTextField.setText(Integer.toString(documentViewController.getCurrentPageDisplayValue()));
            if (numberOfPagesLabel != null) {

                if (pageTree != null) {
                    Object[] messageArguments = new Object[]{String.valueOf(pageTree.getNumberOfPages())};
                    MessageFormat formatter =
                            new MessageFormat(
                                    messageBundle.getString("viewer.toolbar.pageIndicator"));
                    String numberOfPages = formatter.format(messageArguments);

                    numberOfPagesLabel.setText(numberOfPages);
                }
            }

            if (statusLabel != null) {
                if (pageTree != null) {
                    // progress bar for printing
                    Object[] messageArguments = new Object[]{
                            String.valueOf(documentViewController.getCurrentPageDisplayValue()),
                            String.valueOf(pageTree.getNumberOfPages())
                    };
                    MessageFormat formatter = new MessageFormat(
                            messageBundle.getString("viewer.statusbar.currentPage"));
                    statusLabel.setText(formatter.format(messageArguments));
                }
            }
        }
        catch (Exception e) {
            logger.log(Level.FINE, "Error updating page view.", e);
        }
        finally {
            setDisplayTool(oldTool);
        }
    }

    /**
     * Rotates the page visualization by 90 degrees in a counter-clockwise
     * direction.
     */
    public void rotateLeft() {
        documentViewController.setRotateLeft();
        // rest fit page mode, if any
        setPageFitMode(documentViewController.getFitMode(), true);
    }

    /**
     * Rotates the page visualization by 90 degrees in a clockwise
     * direction.
     */
    public void rotateRight() {
        documentViewController.setRotateRight();
        // rest fit page mode, if any
        setPageFitMode(documentViewController.getFitMode(), true);
    }


    public boolean isDocumentFitMode(final int fitMode) {
        return (documentViewController.getFitMode() == fitMode);
    }

    public boolean isDocumentViewMode(final int viewMode) {
        return (documentViewController.getViewMode() == viewMode);
    }

    public void setPageViewSinglePageConButton(JToggleButton btn) {
        singlePageViewContinuousButton = btn;
        btn.addItemListener(this);
    }

    public void setPageViewFacingPageConButton(JToggleButton btn) {
        facingPageViewContinuousButton = btn;
        btn.addItemListener(this);
    }

    public void setPageViewSinglePageNonConButton(JToggleButton btn) {
        singlePageViewNonContinuousButton = btn;
        btn.addItemListener(this);
    }

    public void setPageViewFacingPageNonConButton(JToggleButton btn) {
        facingPageViewNonContinuousButton = btn;
        btn.addItemListener(this);
    }

    /**
     * Set the ViewerModel's fit setting to fit the whole page, and update the display
     */
    public void setPageFitMode(final int fitMode, boolean refresh) {

        if (!refresh && documentViewController.getFitMode() == fitMode) {
            return;
        }

        documentViewController.setFitMode(fitMode);

        // update button state.
        reflectZoomInZoomComboBox();
        reflectFitInFitButtons();
    }

    public void setPageViewMode(final int viewMode, boolean refresh) {

        if (!refresh && documentViewController.getViewMode() == viewMode) {
            return;
        }

        documentViewController.setViewType(viewMode);

        // update button state.
        reflectDocumentViewModeInButtons();
        reflectFitInFitButtons();
    }

    public void setDocumentToolMode(final int toolType) {
        if (documentViewController.isToolModeSelected(toolType))
            return;

        documentViewController.setToolMode(toolType);

        reflectToolInToolButtons();
    }

    /**
     * If the utility pane is currently visible
     * @return true if pane is visilbe false otherwise.
     */
    public boolean isUtilityPaneVisible() {
        return (utilityTabbedPane != null) && utilityTabbedPane.isVisible();
    }

    /**
     * Makes the component visible or invisible.
     *
     * @param visible true to make the component visible; false to make it
     *                invisible.
     */
    public void setUtilityPaneVisible(boolean visible) {
        if (utilityTabbedPane != null) {
            utilityTabbedPane.setVisible(visible);
        }
        if (utilityAndDocumentSplitPane != null) {
            if (visible) {
                utilityAndDocumentSplitPane.setDividerLocation(
                        utilityAndDocumentSplitPaneLastDividerLocation);
                if (utilityAndDocumentSplitPane.getDividerLocation() < 5)
                    utilityAndDocumentSplitPane.setDividerLocation(260);
                utilityAndDocumentSplitPane.setDividerSize(8);
            } else {
                int divLoc = utilityAndDocumentSplitPane.getDividerLocation();
                if (divLoc > 5)
                    utilityAndDocumentSplitPaneLastDividerLocation = divLoc;
                utilityAndDocumentSplitPane.setDividerSize(0);
            }
        }
        reflectStateInComponents();
    }

    /**
     * Flips the visibility of the utility pane to the opposite of what it was
     *
     * @see #setUtilityPaneVisible(boolean)
     */
    public void toggleUtilityPaneVisibility() {
        setUtilityPaneVisible(!isUtilityPaneVisible());
    }

    /**
     * Method to select the currently visible tab in the utility pane
     * Because tabs can be hidden via the properties file, we'll want to check first
     *  whether the desired panel even exists
     *
     * @param comp to select
     * @return true on successful selection
     */
    protected boolean safelySelectUtilityPanel(Component comp) {
        if ((utilityTabbedPane != null) && (comp != null)) {
            if (utilityTabbedPane.indexOfComponent(comp) > -1) {
                utilityTabbedPane.setSelectedComponent(comp);

                return true;
            }
        }

        return false;
    }

    /**
     * Make the Search pane visible, and if necessary, the utility pane that encloses it
     *
     * @see #setUtilityPaneVisible(boolean)
     */
    public void showSearchPanel() {
        if (utilityTabbedPane != null && searchPanel != null) {
            // toggle the utility pane visibility
            toggleUtilityPaneVisibility();

            // if utility pane is shown then select the search tab and request focus
            if (isUtilityPaneVisible()) {
                if (utilityTabbedPane.getSelectedComponent() != searchPanel) {
                    // select the search panel
                    safelySelectUtilityPanel(searchPanel);
                }
                
                // request focus
                searchPanel.requestFocus();
            }
        }
    }

    /**
     * Make the Annotation Link Panel visible, and if necessary, the utility pane that encloses it
     *
     * @param selectedAnnotation the annotation to show in the panel
     * @see #setUtilityPaneVisible(boolean)
     */
    public void showAnnotationPanel(AnnotationComponentImpl selectedAnnotation) {
        if (utilityTabbedPane != null && annotationPanel != null) {
            // Pass the selected annotation to the link panel
            if (selectedAnnotation != null){
                annotationPanel.setEnabled(true);
                annotationPanel.setAnnotationComponent(selectedAnnotation);
            }
            // make sure the utility pane is visible
            if (!isUtilityPaneVisible()){
                setUtilityPaneVisible(true);
            }

            // select the annotationPanel tab
            if (utilityTabbedPane.getSelectedComponent() != annotationPanel) {
                safelySelectUtilityPanel(annotationPanel);
            }

        }
    }

    /**
     * Show a dialog, listing every page in the PDF Document, for the user
     * to select which page to show.
     *
     * @see #showPage(int)
     */
    public void showPageSelectionDialog() {
        int numPages = getPageTree().getNumberOfPages();
        Object[] s = new Object[numPages];
        for (int i = 0; i < numPages; i++) {
            s[i] = Integer.toString(i + 1);
        }
        Object initialSelection = s[documentViewController.getCurrentPageIndex()];
        Object ob = JOptionPane.showInputDialog(
                viewer,
                messageBundle.getString("viewer.dialog.goToPage.description.label"),
                messageBundle.getString("viewer.dialog.goToPage.title"),
                JOptionPane.QUESTION_MESSAGE,
                null,
                s,
                initialSelection);
        if (ob != null) {
            try {
                int pageIndex = Integer.parseInt(ob.toString()) - 1;
                showPage(pageIndex);
            }
            catch (NumberFormatException nfe) {
                logger.log(Level.FINE, "Error selecting page number.");
            }
        }
    }

    /**
     * Method to try to read any ViewerPreferences present in the document, and apply them
     * Otherwise we will try to check the properties file for any overriding to these values
     *
     * @param catalog to lookup view preferences from
     * @param propertiesManager to check properties in
     */
    protected void applyViewerPreferences(Catalog catalog, PropertiesManager propertiesManager) {
        if (catalog == null) {
            return;
        }

        ViewerPreferences viewerPref = catalog.getViewerPreferences();

        // Hide the toolbar?
        if ((viewerPref != null) && (viewerPref.hasHideToolbar())) {
            if (viewerPref.getHideToolbar()) {
                if (completeToolBar != null) {
                    completeToolBar.setVisible(false);
                }
            }
        }
        else {
            if (completeToolBar != null) {
                completeToolBar.setVisible(
                    !PropertiesManager.checkAndStoreBooleanProperty(propertiesManager,
                                                                    PropertiesManager.PROPERTY_VIEWPREF_HIDETOOLBAR,
                                                                    false));
            }
        }

        // Hide the menubar?
        if ((viewerPref != null) && (viewerPref.hasHideMenubar())) {
            if (viewerPref.getHideMenubar()) {
                if ((viewer != null) && (viewer.getJMenuBar() != null)) {
                    viewer.getJMenuBar().setVisible(false);
                }
            }
        }
        else {
            if ((viewer != null) && (viewer.getJMenuBar() != null)) {
                viewer.getJMenuBar().setVisible(
                    !PropertiesManager.checkAndStoreBooleanProperty(propertiesManager,
                                                                    PropertiesManager.PROPERTY_VIEWPREF_HIDEMENUBAR,
                                                                    false));
            }
        }

        // Fit the GUI frame to the size of the document?
        if ((viewerPref != null) && (viewerPref.hasFitWindow())) {
            if (viewerPref.getFitWindow()) {
                if (viewer != null) {
                    viewer.setSize(
                            documentViewController.getDocumentView().getDocumentSize());
                }
            }
        }
        else {
            if ((PropertiesManager.checkAndStoreBooleanProperty(propertiesManager,
                                                                PropertiesManager.PROPERTY_VIEWPREF_FITWINDOW,
                                                                false)) && (viewer != null)) {
                viewer.setSize(
                        documentViewController.getDocumentView().getDocumentSize());
            }
        }
    }

    /**
     * Gives access to this SwingController's ViewerModel
     *
     * @return The SwingController's ViewerModel
     * @see ViewModel
     */
    public ViewModel getViewModel() {
        return viewModel;
    }


    //
    // Controller interface
    //

    /**
     * A Document is the root of the object hierarchy, giving access
     * to the contents of a PDF file.
     * Significantly: getDocument().getCatalog().getPageTree().getPage(int pageIndex)
     * gives access to each Page, so that it might be drawn.
     *
     * @return Document root of the PDF file
     */
    public Document getDocument() {
        return document;
    }

    /**
     * When viewing a PDF file, one or more pages may be viewed at
     * a single time, but this is the single page which is most
     * predominantly being displayed.
     *
     * @return The zero-based index of the current Page being displayed
     */
    public int getCurrentPageNumber() {
        return documentViewController.getCurrentPageIndex();
    }

    /**
     * Each Page may have its own rotation, but on top of that, the user
     * may select to have the Page further rotated, by 0, 90, 180, 270 degrees
     *
     * @return The user's requested rotation
     */
    public float getUserRotation() {
        return documentViewController.getRotation();
    }

    /**
     * The Page being shown may be zoomed in or out, to show more detail,
     * or provide an overview.
     *
     * @return The user's requested zoom
     */
    public float getUserZoom() {
        return documentViewController.getZoom();
    }


    //
    // ActionListener interface
    //

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        if (source == null)
            return;

        boolean cancelSetFocus = false;

        try {
            if (source == openFileMenuItem || source == openFileButton) {
                Runnable doSwingWork = new Runnable() {
                    public void run() {
                        openFile();
                    }
                };
                SwingUtilities.invokeLater(doSwingWork);
            } else if (source == openURLMenuItem) {
                Runnable doSwingWork = new Runnable() {
                    public void run() {
                        openURL();
                    }
                };
                SwingUtilities.invokeLater(doSwingWork);
            } else if (source == closeMenuItem) {
                closeDocument();
            } else if (source == saveAsFileMenuItem || source == saveAsFileButton) {
                Runnable doSwingWork = new Runnable() {
                    public void run() {
                        saveFile();
                    }
                };
                SwingUtilities.invokeLater(doSwingWork);
            } else if (source == exportTextMenuItem) {
                Runnable doSwingWork = new Runnable() {
                    public void run() {
                        exportText();
                    }
                };
                SwingUtilities.invokeLater(doSwingWork);
            } else if (source == exportSVGMenuItem) {
                Runnable doSwingWork = new Runnable() {
                    public void run() {
                        exportSVG();
                    }
                };
                SwingUtilities.invokeLater(doSwingWork);
            } else if (source == exitMenuItem) {
                exit();
            } else if (source == showHideToolBarMenuItem) {
                toggleToolBarVisibility();
            } else if (source == minimiseAllMenuItem) {
                Runnable doSwingWork = new Runnable() {
                    public void run() {
                        SwingController sc = SwingController.this;
                        if (sc.getWindowManagementCallback() != null)
                            sc.getWindowManagementCallback().minimiseAllWindows();
                    }
                };
                SwingUtilities.invokeLater(doSwingWork);
            } else if (source == bringAllToFrontMenuItem) {
                Runnable doSwingWork = new Runnable() {
                    public void run() {
                        SwingController sc = SwingController.this;
                        if (sc.getWindowManagementCallback() != null)
                            sc.getWindowManagementCallback().bringAllWindowsToFront(sc);
                    }
                };
                SwingUtilities.invokeLater(doSwingWork);
            } else if (windowListMenuItems != null && windowListMenuItems.contains(source)) {
                final int index = windowListMenuItems.indexOf(source);
                Runnable doSwingWork = new Runnable() {
                    public void run() {
                        SwingController sc = SwingController.this;
                        if (sc.getWindowManagementCallback() != null) {
                            sc.getWindowManagementCallback().bringWindowToFront(index);
                        }
                    }
                };
                SwingUtilities.invokeLater(doSwingWork);
            } else if (source == aboutMenuItem) {
                showAboutDialog();
            } else if (document != null) {
                // get document previous icon
                int documentIcon = getDocumentViewToolMode();
                try {
                    // set cursor for document view
                    setDisplayTool(DocumentViewModelImpl.DISPLAY_TOOL_WAIT);

                    if (source == permissionsMenuItem) {
                        Runnable doSwingWork = new Runnable() {
                            public void run() {
                                showDocumentPermissionsDialog();
                            }
                        };
                        SwingUtilities.invokeLater(doSwingWork);
                    } else if (source == informationMenuItem) {
                        Runnable doSwingWork = new Runnable() {
                            public void run() {
                                showDocumentInformationDialog();
                            }
                        };
                        SwingUtilities.invokeLater(doSwingWork);
                    } else if (source == printSetupMenuItem) {
                        Runnable doSwingWork = new Runnable() {
                            public void run() {
                                showPrintSetupDialog();
                            }
                        };
                        SwingUtilities.invokeLater(doSwingWork);
                    } else if (source == printMenuItem) {
                        print(true);
                    } else if (source == printButton) {
                        print(true); // Used to be 'false' PDF-86
                    } else if (source == undoMenuItem) {
                        documentViewController.undo();
                        // refresh undo buttons.
                        reflectUndoCommands();
                    } else if (source == redoMenuItem) {
                        documentViewController.redo();
                        reflectUndoCommands();
                    } else if (source == deleteMenuItem) {
                        documentViewController.deleteCurrentAnnotation();
                        reflectUndoCommands();
                    } else if (source == copyMenuItem) {
                        if (document != null &&
                                havePermissionToExtractContent() &&
                                !(documentViewController.getDocumentViewModel().isSelectAll() &&
                                document.getNumberOfPages() > MAX_SELECT_ALL_PAGE_COUNT)) {
                            // get the text.
                            StringSelection stringSelection = new StringSelection(
                                documentViewController.getSelectedText());
                            Toolkit.getDefaultToolkit().getSystemClipboard().
                                    setContents(stringSelection, null);
                        } else {
                            Runnable doSwingWork = new Runnable() {
                                public void run() {
                                    org.icepdf.ri.util.Resources.showMessageDialog(
                                            viewer,
                                            JOptionPane.INFORMATION_MESSAGE,
                                            messageBundle,
                                            "viewer.dialog.information.copyAll.title",
                                            "viewer.dialog.information.copyAll.msg",
                                            MAX_SELECT_ALL_PAGE_COUNT);
                                }
                            };
                            SwingUtilities.invokeLater(doSwingWork);
                        }
                    } else if (source == selectAllMenuItem) {
                        // check to see how many page are in the document
                        documentViewController.selectAllText();
                    } else if (source == deselectAllMenuItem) {
                        documentViewController.clearSelectedText();
                    } else if (source == fitActualSizeMenuItem) {
                        // Clicking only seems to invoke an itemStateChanged() event,
                        //  so this is probably redundant
                        setPageFitMode(org.icepdf.core.views.DocumentViewController.PAGE_FIT_ACTUAL_SIZE, false);
                    } else if (source == fitPageMenuItem) {
                        // Clicking only seems to invoke an itemStateChanged() event
                        //  so this is probably redundant
                        setPageFitMode(org.icepdf.core.views.DocumentViewController.PAGE_FIT_WINDOW_HEIGHT, false);
                    } else if (source == fitWidthMenuItem) {
                        // Clicking only seems to invoke an itemStateChanged() event
                        //  so this is probably redundant
                        setPageFitMode(org.icepdf.core.views.DocumentViewController.PAGE_FIT_WINDOW_WIDTH, false);
                    } else if (source == zoomInMenuItem || source == zoomInButton) {
                        zoomIn();
                    } else if (source == zoomOutMenuItem || source == zoomOutButton) {
                        zoomOut();
                    } else if (source == rotateLeftMenuItem || source == rotateLeftButton) {
                        rotateLeft();
                    } else if (source == rotateRightMenuItem || source == rotateRightButton) {
                        rotateRight();
                    } else if (source == showHideUtilityPaneMenuItem || source == showHideUtilityPaneButton) {
                        toggleUtilityPaneVisibility();
                    } else if (source == firstPageMenuItem || source == firstPageButton) {
                        showPage(0);
                    } else if (source == previousPageMenuItem || source == previousPageButton) {
                        DocumentView documentView = documentViewController.getDocumentView();
                        goToDeltaPage(-documentView.getPreviousPageIncrement());
                    } else if (source == nextPageMenuItem || source == nextPageButton) {
                        DocumentView documentView = documentViewController.getDocumentView();
                        goToDeltaPage(documentView.getNextPageIncrement());
                    } else if (source == lastPageMenuItem || source == lastPageButton) {
                        showPage(getPageTree().getNumberOfPages() - 1);
                    } else if (source == searchMenuItem || source == searchButton) {
                        cancelSetFocus = true;
                        showSearchPanel();
                    } else if (source == goToPageMenuItem) {
                        showPageSelectionDialog();
                    } else if (source == currentPageNumberTextField) {
                        showPageFromTextField();
                    } else {
                        logger.log(Level.FINE, "Unknown action event: " + source.toString());
                    }
                }
                finally {
                    // set view pane back to previous icon
                    setDisplayTool(documentIcon);
                }
            }
        } catch (Exception e) {
            final Exception f = e;
            Runnable doSwingWork = new Runnable() {
                public void run() {
                    org.icepdf.ri.util.Resources.showMessageDialog(
                            viewer,
                            JOptionPane.INFORMATION_MESSAGE,
                            messageBundle,
                            "viewer.dialog.error.exception.title",
                            "viewer.dialog.error.exception.msg",
                            f.getMessage());
                }
            };
            SwingUtilities.invokeLater(doSwingWork);
            logger.log(Level.FINE, "Error processing action event.", e);
        }

        if (!cancelSetFocus) {
            // setup focus to ensure page up and page down keys work
            documentViewController.requestViewFocusInWindow();
        }
    }

    //
    // FocusListener interface
    //

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void focusGained(FocusEvent e) {
    }

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void focusLost(FocusEvent e) {
        Object src = e.getSource();
        if (src == null)
            return;
        if (src == currentPageNumberTextField) {
            String fieldValue = currentPageNumberTextField.getText();
            String modelValue = Integer.toString(documentViewController.getCurrentPageDisplayValue());
            if (!fieldValue.equals(modelValue))
                currentPageNumberTextField.setText(modelValue);
        }
    }

    //
    // ItemListener interface
    //

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getSource();
        if (source == null)
            return;

        boolean doSetFocus = false;
        int tool = getDocumentViewToolMode();
        setDisplayTool(DocumentViewModelImpl.DISPLAY_TOOL_WAIT);
        try {
            if (source == zoomComboBox) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    setZoomFromZoomComboBox();
                    // Since combobox is an entry component, we don't force focus to the document
                }
            } else if (source == fitActualSizeButton) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    setPageFitMode(org.icepdf.core.views.DocumentViewController.PAGE_FIT_ACTUAL_SIZE, false);
                    doSetFocus = true;
                }
            } else if (source == fitHeightButton) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    setPageFitMode(org.icepdf.core.views.DocumentViewController.PAGE_FIT_WINDOW_HEIGHT, false);
                    doSetFocus = true;
                }
            } else if (source == fitWidthButton) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    setPageFitMode(org.icepdf.core.views.DocumentViewController.PAGE_FIT_WINDOW_WIDTH, false);
                    doSetFocus = true;
                }
            } else if (source == fontEngineButton) {
                if (e.getStateChange() == ItemEvent.SELECTED ||
                        e.getStateChange() == ItemEvent.DESELECTED) {
                    // get instance of the font factory
                    FontFactory.getInstance().toggleAwtFontSubstitution();
                    // refresh the document, refresh will happen by the component.
                    documentViewController.getDocumentView().getViewModel().invalidate();
                    doSetFocus = true;
                }

            } else if (source == panToolButton) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    tool = DocumentViewModelImpl.DISPLAY_TOOL_PAN;
                    setDocumentToolMode(DocumentViewModelImpl.DISPLAY_TOOL_PAN);
                    doSetFocus = true;
                }
            } else if (source == textSelectToolButton) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    tool = DocumentViewModelImpl.DISPLAY_TOOL_TEXT_SELECTION;
                    setDocumentToolMode(DocumentViewModelImpl.DISPLAY_TOOL_TEXT_SELECTION);
                    doSetFocus = true;
                }
            }  else if (source == selectToolButton) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    tool = DocumentViewModelImpl.DISPLAY_TOOL_SELECTION;
                    setDocumentToolMode(DocumentViewModelImpl.DISPLAY_TOOL_SELECTION);
                }
            } else if (source == linkAnnotationToolButton) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    tool = DocumentViewModelImpl.DISPLAY_TOOL_LINK_ANNOTATION;
                    setDocumentToolMode(DocumentViewModelImpl.DISPLAY_TOOL_LINK_ANNOTATION);
                }
            } else if (source == zoomInToolButton) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    tool = DocumentViewModelImpl.DISPLAY_TOOL_ZOOM_IN;
                    setDocumentToolMode(DocumentViewModelImpl.DISPLAY_TOOL_ZOOM_IN);
                    doSetFocus = true;
                }
            } else if (source == zoomOutToolButton) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    tool = DocumentViewModelImpl.DISPLAY_TOOL_ZOOM_OUT;
                    setDocumentToolMode(DocumentViewModelImpl.DISPLAY_TOOL_ZOOM_OUT);
                    doSetFocus = true;
                }
            } else if (source == facingPageViewNonContinuousButton) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    setPageViewMode(
                            DocumentViewControllerImpl.TWO_PAGE_RIGHT_VIEW,
                            false);
                    doSetFocus = true;
                }
            } else if (source == facingPageViewContinuousButton) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    setPageViewMode(
                            DocumentViewControllerImpl.TWO_COLUMN_RIGHT_VIEW,
                            false);
                    doSetFocus = true;
                }
            } else if (source == singlePageViewNonContinuousButton) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    setPageViewMode(
                            DocumentViewControllerImpl.ONE_PAGE_VIEW,
                            false);
                    doSetFocus = true;
                }
            } else if (source == singlePageViewContinuousButton) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    setPageViewMode(
                            DocumentViewControllerImpl.ONE_COLUMN_VIEW,
                            false);
                    doSetFocus = true;
                }
            }

            if (doSetFocus) {
                // setup focus to ensure page up and page down keys work
                documentViewController.requestViewFocusInWindow();
            }

        }
        finally {
            setDisplayTool(tool);
        }
    }

    //
    // TreeSelectionListener interface
    //

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void valueChanged(TreeSelectionEvent e) {
        if (outlinesTree == null)
            return;
        TreePath treePath = outlinesTree.getSelectionPath();
        if (treePath == null)
            return;
        OutlineItemTreeNode node = (OutlineItemTreeNode) treePath.getLastPathComponent();
        OutlineItem o = node.getOutlineItem();

        followOutlineItem(o);

        // return focus so that arrow keys will work on list
        outlinesTree.requestFocus();
    }


    //
    // WindowListener interface
    //

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void windowActivated(WindowEvent e) {
    }

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void windowClosed(WindowEvent e) {
    }

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void windowClosing(WindowEvent e) {
        // We have to call dispose() before using the WindowManagementCallback, because the
        //  WindowManagementCallback may well call System.exit().  If System.exit() is called
        //  before dispose() closes our temporary files, then they won't be deleted.
        // So, we need to temporarily save what we'll need, for our later invocation of
        //  WindowManagementCallback.disposeWindow(), that dispose() would otherwise trash
        WindowManagementCallback wc = windowManagementCallback;
        JFrame v = viewer;

        // assign view properties so that they can be saved on close
        org.icepdf.core.views.DocumentViewController viewControl = getDocumentViewController();
        Properties viewProperties = new Properties();
        viewProperties.setProperty(PropertiesManager.PROPERTY_DEFAULT_PAGEFIT, String.valueOf(viewControl.getFitMode()));
        viewProperties.setProperty("document.viewtype", String.valueOf(viewControl.getViewMode()));

        dispose();

        if (wc != null) {
            wc.disposeWindow(this, v, viewProperties);
        }
    }

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void windowDeactivated(WindowEvent e) {
    }

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void windowDeiconified(WindowEvent e) {
    }

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void windowIconified(WindowEvent e) {
    }

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void windowOpened(WindowEvent e) {
    }

    //
    // DropTargetListener interface
    //

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void dragEnter(DropTargetDragEvent event) {
        if (!isDragAcceptable(event)) {
            event.rejectDrag();
        }
    }

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void dragOver(DropTargetDragEvent event) {
    }

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void dropActionChanged(DropTargetDragEvent event) {
        if (!isDragAcceptable(event)) {
            event.rejectDrag();
        }
    }

    /**
     * Handle drop event when a user drags and drops one or more files onto the viewer frame.
     *
     * @param event information about the drag and drop data.
     */
    public void drop(DropTargetDropEvent event) {
        try {
            // check to make sure that event type is ok
            if (!isDropAcceptable(event)) {
                event.rejectDrop();
                return;
            }
            // accept the drop action, must do this to proceed
            event.acceptDrop(DnDConstants.ACTION_COPY);
            Transferable transferable = event.getTransferable();
            DataFlavor[] flavors = transferable.getTransferDataFlavors();
            for (DataFlavor dataFlavor : flavors) {
                // Check to see if a file was dropped on the viewer frame
                if (dataFlavor.equals(DataFlavor.javaFileListFlavor)) {
                    List fileList = (List) transferable.getTransferData(dataFlavor);
                    // load all the files that where dragged
                    for (Object aFileList : fileList) {
                        File file = (File) aFileList;
                        if (file.getName().toLowerCase().endsWith(".pdf")) {
                            openFileInSomeViewer(file);
                            ViewModel.setDefaultFile(file);
                        }
                    }
                } else if (dataFlavor.equals(DataFlavor.stringFlavor)) {
                    String s = (String) transferable.getTransferData(dataFlavor);
                    int startIndex = s.toLowerCase().indexOf("http://");
                    int endIndex = s.toLowerCase().indexOf(".pdf");
                    if (startIndex >= 0 && endIndex >= 0) {
                        s = s.substring(startIndex, endIndex + 4);
                        URL url;
                        try {
                            url = new URL(s);
                            openURLInSomeViewer(url);
                            ViewModel.setDefaultURL(s);
                        }
                        catch (MalformedURLException e) {
                            // eat the error
                        }
                    }
                }
            }
            event.dropComplete(true);

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (UnsupportedFlavorException ufe) {
            ufe.printStackTrace();
        }
    }

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void dragExit(DropTargetEvent event) {
    }


    //
    // KeyListener interface
    //

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void keyPressed(KeyEvent e) {
        if (document == null)
            return;
        int c = e.getKeyCode();
        int m = e.getModifiers();
        if ((c == KeyEventConstants.KEY_CODE_SAVE_AS && m == KeyEventConstants.MODIFIER_SAVE_AS) ||
                (c == KeyEventConstants.KEY_CODE_PRINT_SETUP && m == KeyEventConstants.MODIFIER_PRINT_SETUP) ||
                (c == KeyEventConstants.KEY_CODE_PRINT && m == KeyEventConstants.MODIFIER_PRINT) ||
                (c == KeyEventConstants.KEY_CODE_FIT_ACTUAL && m == KeyEventConstants.MODIFIER_FIT_ACTUAL) ||
                (c == KeyEventConstants.KEY_CODE_FIT_PAGE && m == KeyEventConstants.MODIFIER_FIT_PAGE) ||
                (c == KeyEventConstants.KEY_CODE_FIT_WIDTH && m == KeyEventConstants.MODIFIER_FIT_WIDTH) ||
                (c == KeyEventConstants.KEY_CODE_ZOOM_IN && m == KeyEventConstants.MODIFIER_ZOOM_IN) ||
                (c == KeyEventConstants.KEY_CODE_ZOOM_OUT && m == KeyEventConstants.MODIFIER_ZOOM_OUT) ||
                (c == KeyEventConstants.KEY_CODE_ROTATE_LEFT && m == KeyEventConstants.MODIFIER_ROTATE_LEFT) ||
                (c == KeyEventConstants.KEY_CODE_ROTATE_RIGHT && m == KeyEventConstants.MODIFIER_ROTATE_RIGHT) ||
                (c == KeyEventConstants.KEY_CODE_FIRST_PAGE && m == KeyEventConstants.MODIFIER_FIRST_PAGE) ||
                (c == KeyEventConstants.KEY_CODE_PREVIOUS_PAGE && m == KeyEventConstants.MODIFIER_PREVIOUS_PAGE) ||
                (c == KeyEventConstants.KEY_CODE_NEXT_PAGE && m == KeyEventConstants.MODIFIER_NEXT_PAGE) ||
                (c == KeyEventConstants.KEY_CODE_LAST_PAGE && m == KeyEventConstants.MODIFIER_LAST_PAGE) ||
                (c == KeyEventConstants.KEY_CODE_SEARCH && m == KeyEventConstants.MODIFIER_SEARCH) ||
                (c == KeyEventConstants.KEY_CODE_GOTO && m == KeyEventConstants.MODIFIER_GOTO)) {
            // get document previous icon
            int documentIcon = getDocumentViewToolMode();
            try {
                // set cursor for document view
                setDisplayTool(DocumentViewModelImpl.DISPLAY_TOOL_WAIT);

                if (c == KeyEventConstants.KEY_CODE_SAVE_AS &&
                        m == KeyEventConstants.MODIFIER_SAVE_AS) {
                    saveFile();
                } else if (c == KeyEventConstants.KEY_CODE_PRINT_SETUP &&
                        m == KeyEventConstants.MODIFIER_PRINT_SETUP) {
                    showPrintSetupDialog();
                } else if (c == KeyEventConstants.KEY_CODE_PRINT &&
                        m == KeyEventConstants.MODIFIER_PRINT) {
                    print(true);
                } else if (c == KeyEventConstants.KEY_CODE_FIT_ACTUAL &&
                        m == KeyEventConstants.MODIFIER_FIT_ACTUAL) {
                    setPageFitMode(org.icepdf.core.views.DocumentViewController.PAGE_FIT_ACTUAL_SIZE, false);
                } else if (c == KeyEventConstants.KEY_CODE_FIT_PAGE &&
                        m == KeyEventConstants.MODIFIER_FIT_PAGE) {
                    setPageFitMode(org.icepdf.core.views.DocumentViewController.PAGE_FIT_WINDOW_HEIGHT, false);
                } else if (c == KeyEventConstants.KEY_CODE_FIT_WIDTH &&
                        m == KeyEventConstants.MODIFIER_FIT_WIDTH) {
                    setPageFitMode(org.icepdf.core.views.DocumentViewController.PAGE_FIT_WINDOW_WIDTH, false);
                } else if (c == KeyEventConstants.KEY_CODE_ZOOM_IN &&
                        m == KeyEventConstants.MODIFIER_ZOOM_IN) {
                    zoomIn();
                } else if (c == KeyEventConstants.KEY_CODE_ZOOM_OUT &&
                        m == KeyEventConstants.MODIFIER_ZOOM_OUT) {
                    zoomOut();
                } else if (c == KeyEventConstants.KEY_CODE_ROTATE_LEFT &&
                        m == KeyEventConstants.MODIFIER_ROTATE_LEFT) {
                    rotateLeft();
                } else if (c == KeyEventConstants.KEY_CODE_ROTATE_RIGHT &&
                        m == KeyEventConstants.MODIFIER_ROTATE_RIGHT) {
                    rotateRight();
                } else if (c == KeyEventConstants.KEY_CODE_FIRST_PAGE &&
                        m == KeyEventConstants.MODIFIER_FIRST_PAGE) {
                    showPage(0);
                } else if (c == KeyEventConstants.KEY_CODE_PREVIOUS_PAGE &&
                        m == KeyEventConstants.MODIFIER_PREVIOUS_PAGE) {
                    DocumentView documentView = documentViewController.getDocumentView();
                    goToDeltaPage(-documentView.getPreviousPageIncrement());
                } else if (c == KeyEventConstants.KEY_CODE_NEXT_PAGE &&
                        m == KeyEventConstants.MODIFIER_NEXT_PAGE) {
                    DocumentView documentView = documentViewController.getDocumentView();
                    goToDeltaPage(documentView.getNextPageIncrement());
                } else if (c == KeyEventConstants.KEY_CODE_LAST_PAGE &&
                        m == KeyEventConstants.MODIFIER_LAST_PAGE) {
                    showPage(getPageTree().getNumberOfPages() - 1);
                } else if (c == KeyEventConstants.KEY_CODE_SEARCH &&
                        m == KeyEventConstants.MODIFIER_SEARCH) {
                    showSearchPanel();
                } else if (c == KeyEventConstants.KEY_CODE_GOTO &&
                        m == KeyEventConstants.MODIFIER_GOTO) {
                    showPageSelectionDialog();
                }
            }
            finally {
                // set view pain back to previous icon
                setDisplayTool(documentIcon);
            }
        }
    }

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void keyReleased(KeyEvent e) {
    }

    /**
     * SwingController takes AWT/Swing events, and maps them to its own events
     * related to PDF Document manipulation
     */
    public void keyTyped(KeyEvent e) {
        if (currentPageNumberTextField != null &&
                e.getSource() == currentPageNumberTextField) {
            char c = e.getKeyChar();
            if (c == KeyEvent.VK_ESCAPE) {
                String fieldValue = currentPageNumberTextField.getText();
                String modelValue = Integer.toString(
                        documentViewController.getCurrentPageDisplayValue());
                if (!fieldValue.equals(modelValue))
                    currentPageNumberTextField.setText(modelValue);
            }
        }
    }

    /**
     * Listen for property change events from the page view.  This method
     * acts like a mediator passing on the new states to the interested parties.
     *
     * @param evt property change event 
     */
    public void propertyChange(PropertyChangeEvent evt) {
        Object newValue = evt.getNewValue();
        Object oldValue = evt.getOldValue();
        if (evt.getPropertyName().equals(PropertyConstants.DOCUMENT_CURRENT_PAGE)) {
            if (currentPageNumberTextField != null && newValue instanceof Integer) {
                updateDocumentView();
            }
        }
        // text selected,
        else if (evt.getPropertyName().equals(PropertyConstants.TEXT_SELECTED)){
            // enable the copy menu
            boolean canExtract = havePermissionToExtractContent();
            setEnabled(copyMenuItem, canExtract);
            setEnabled(deselectAllMenuItem, canExtract);
        }
        // text deselected
        else if (evt.getPropertyName().equals(PropertyConstants.TEXT_DESELECTED)){
            // disable the copy menu
            boolean canExtract = havePermissionToExtractContent();
            setEnabled(copyMenuItem, false);
            setEnabled(deselectAllMenuItem, false);
            setEnabled(selectAllMenuItem, canExtract);
        }
        // select all
        else if (evt.getPropertyName().equals(PropertyConstants.TEXT_SELECT_ALL)){
            boolean canExtract = havePermissionToExtractContent();
            setEnabled(selectAllMenuItem, false);
            setEnabled(deselectAllMenuItem, canExtract);
            setEnabled(copyMenuItem, canExtract);
        }
        // annotation is selected
        else if (evt.getPropertyName().equals(PropertyConstants.ANNOTATION_SELECTED)){
            // enable the delete menu
            setEnabled(deleteMenuItem, true);
            // get the current selected tool, we only care about the select tool or
            // link annotation tool.
            if (documentViewController.getToolMode() ==
                            DocumentViewModelImpl.DISPLAY_TOOL_SELECTION){
                AnnotationComponentImpl annotationComponent =
                        (AnnotationComponentImpl)newValue;
                if (annotationComponent != null &&
                    annotationComponent.getAnnotation() != null ){
                    // set the annotationPane with the new annotation component
                    if (logger.isLoggable(Level.FINE)){
                        logger.fine("selected annotation " + annotationComponent);
                    }
                    showAnnotationPanel(annotationComponent);
                }
            }
        }
        // annotation is deselected
        else if (evt.getPropertyName().equals(PropertyConstants.ANNOTATION_DESELECTED)){
            if (documentViewController.getToolMode() ==
                        DocumentViewModelImpl.DISPLAY_TOOL_SELECTION) {
                if (logger.isLoggable(Level.FINE)){
                    logger.fine("Deselected current annotation");
                }
                // disable the delete menu
                setEnabled(deleteMenuItem, false);
                if (annotationPanel != null) {
                    annotationPanel.setEnabled(false);
                }
            }
        }
        // annotation bounds have changed.
        else if (evt.getPropertyName().equals(PropertyConstants.ANNOTATION_BOUNDS)){
            if (documentViewController.getToolMode() ==
                            DocumentViewModelImpl.DISPLAY_TOOL_SELECTION){
                AnnotationState oldAnnotationState = (AnnotationState)oldValue;
                AnnotationState newAnnotationState = (AnnotationState)newValue;

                // saves the state changes back to the document structure.
                newAnnotationState.apply(newAnnotationState);
                newAnnotationState.restore();

                // add new states to care taker implementation.
                documentViewController.getDocumentViewModel()
                        .addMemento(oldAnnotationState,
                                newAnnotationState);
            }
            // check to see if undo/redo can be enabled/disabled.
            reflectUndoCommands();
        }
    }
}
