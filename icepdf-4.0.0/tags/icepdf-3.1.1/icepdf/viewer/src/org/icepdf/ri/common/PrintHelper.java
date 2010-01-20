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
package org.icepdf.ri.common;

import org.icepdf.core.pobjects.PDimension;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.pobjects.PageTree;
import org.icepdf.core.util.GraphicsRenderingHints;
import org.icepdf.core.views.DocumentViewController;

import javax.print.*;
import javax.print.attribute.HashDocAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.*;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>The <code>PrintHelper</code> class is utility class to aid developers in
 * printing PDF document content.  The PrintHelper takes advantage of the
 * Pageable and Printable interfaces availabe in Java 2.</p>
 *
 * @since 2.0
 */
public class PrintHelper implements Printable {

    private static final Logger logger =
            Logger.getLogger(PrintHelper.class.toString());

    private DocumentViewController viewController;
    private PageTree pageTree;
    private float userRotation;

    private boolean printFitToMargin = false;
    private int printingCurrentPage;
    private int totalPagesToPrint;

    private static PrintService[] services;
    private PrintService printService;
    private HashDocAttributeSet docAttributeSet;
    private HashPrintRequestAttributeSet printRequestAttributeSet;

    /**
     * Creates a new <code>PrintHelper</code> instance.
     *
     * @param viewController document view controller
     * @param pageTree       doucment page tree.
     */
    public PrintHelper(DocumentViewController viewController, PageTree pageTree) {
        this.viewController = viewController;
        this.pageTree = pageTree;
        this.userRotation = this.viewController.getRotation();

        // find available printers
        services =
                PrintServiceLookup.lookupPrintServices(
                        DocFlavor.SERVICE_FORMATTED.PRINTABLE, null);
        // check for a default service and make sure it is at index 0. the lookupPrintServices does not
        // aways put the default printer first in the array. 
        PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
        if (defaultService != null && services.length > 1){
            PrintService printService;
            for (int i=1, max= services.length; i < max; i++){
                printService = services[i];
                if (printService.equals(defaultService)){
                    // found the default printer, now swap it with the first index. 
                    PrintService tmp = services[0];
                    services[0] = defaultService;
                    services[i] = tmp;
                    break;
                }
            }
        }

        // default printing properties.
        // Print and document attributes sets.
        printRequestAttributeSet =
                new HashPrintRequestAttributeSet();
        docAttributeSet = new HashDocAttributeSet();

        // unix compression attribute where applicable
        printRequestAttributeSet.add(PrintQuality.DRAFT);

        // change paper
        printRequestAttributeSet.add(MediaSizeName.NA_LETTER);
        docAttributeSet.add(MediaSizeName.NA_LETTER);

        // setting margins to full paper size as PDF have their own margins
        MediaSize mediaSize =
                MediaSize.getMediaSizeForName(MediaSizeName.NA_LEGAL);
        float[] size = mediaSize.getSize(MediaSize.INCH);
        printRequestAttributeSet
                .add(new MediaPrintableArea(0, 0, size[0], size[1],
                        MediaPrintableArea.INCH));
        docAttributeSet.add(new MediaPrintableArea(0, 0, size[0], size[1],
                MediaPrintableArea.INCH));

        // default setup, all pages, shrink to fit and no dialog.
        setupPrintService(0, this.pageTree.getNumberOfPages(), 1, true, false);

        // display paper size.
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Paper Size: " + MediaSizeName.NA_LEGAL.getName() +
                    " " + size[0] + " x " + size[1]);
        }
    }

    /**
     * Creates a new <code>PrintHelper</code> instance.
     *
     * @param controller dcoument controller.
     */
    public PrintHelper(SwingController controller) {
        this(controller.getDocumentViewController(), controller.getPageTree());
    }

    /**
     * Gets the page number of the page currently being spooled by the Printable
     * interface.
     *
     * @return current page being spooled by printer.
     */
    public int getCurrentPage() {
        return printingCurrentPage;
    }

    /**
     * Number of total pages being printed.
     *
     * @return total pages being printed.
     */
    public int getNumberOfPages() {
        return totalPagesToPrint;
    }

    /**
     * Prints the page at the specified index into the specified
     * java.awt.Graphics context in the specified format.
     *
     * @param printGraphics paper graphics context.
     * @param pageFormat    print attributes translated from PrintService
     * @param pageIndex     page to print, zero based.
     * @return A status code of Printable.NO_SUCH_PAGE or Printable.PAGE_EXISTS
     */
    public int print(Graphics printGraphics, PageFormat pageFormat, int pageIndex) {

        // update the pageCount
        if (printingCurrentPage != pageIndex) {
            printingCurrentPage = pageIndex + 1;
        }

        // Throws NO_SUCH_PAGE to printable interface,  out of page range
        if (pageIndex < 0 || pageIndex >= pageTree.getNumberOfPages()) {
            return Printable.NO_SUCH_PAGE;
        }

        // Initiate the Page to print, not adding to the pageTree cache purposely,
        // after we finish using it we'll dispose it.
        Page currentPage = pageTree.getPage(pageIndex, this);
        PDimension pageDim = currentPage.getSize(userRotation);

        // Grab default page width and height
        float pageWidth = pageDim.getWidth();
        float pageHeight = pageDim.getHeight();

        // Default zoom factor
        float zoomFactor = 1.0f;

        Point imageablePrintLocation = new Point();

        // detect if page is being drawn in landscape, if so then we should
        // should be rotating the page so that it prints correctly
        float rotation = userRotation;
        boolean isDefaultRotation = true;
        if (pageWidth > pageHeight) {
            // rotate clockwise 90 degrees
            isDefaultRotation = false;
            rotation -= 90;
        }

        // if true, we want to shrink out page to the new area.
        if (printFitToMargin) {

            // Get location of imageable area from PageFormat object
            Dimension imageablePrintSize;
            // correct scale to fit calculation for a possible automatic
            // rotation.
            if (isDefaultRotation) {
                imageablePrintSize = new Dimension(
                        (int) pageFormat.getImageableWidth(),
                        (int) pageFormat.getImageableHeight());
            } else {
                imageablePrintSize = new Dimension(
                        (int) pageFormat.getImageableHeight(),
                        (int) pageFormat.getImageableWidth());

            }
            float zw = imageablePrintSize.width / pageWidth;
            float zh = imageablePrintSize.height / pageHeight;
            zoomFactor = Math.min(zw, zh);
            imageablePrintLocation.x = (int) pageFormat.getImageableX();
            imageablePrintLocation.y = (int) pageFormat.getImageableY();
        }
        // apply imageablePrintLocation, normally (0,0)
        printGraphics.translate(imageablePrintLocation.x,
                imageablePrintLocation.y);

        // Paint the page content
        currentPage.paint(printGraphics,
                Page.BOUNDARY_CROPBOX,
                GraphicsRenderingHints.PRINT,
                rotation, zoomFactor);

        pageTree.releasePage(currentPage, this);

        return Printable.PAGE_EXISTS;
    }

    /**
     * Configures the PrinterJob instance with the specified parameters.
     *
     * @param startPage             start of page range, zero-based index.
     * @param endPage               end of page range, one-based index.
     * @param copies                number of copies of pages in print range.
     * @param shrinkToPrintableArea true, to enable shrink to fit printable area;
     *                              false, otherwise.
     * @param showPrintDialog       true, to display a print setup dialog when this method
     *                              is initiated; false, otherwise.  This dialog will be shown after the
     *                              page dialog if it is visible.
     * @return true if print setup should continue, false if printing was cancelled
     *         by user interaction with optional print dialog.
     */
    public boolean setupPrintService(int startPage,
                                     int endPage,
                                     int copies,
                                     boolean shrinkToPrintableArea,
                                     boolean showPrintDialog) {

        // make sure our printable doc knows how many pages to print
        // Has to be set before printerJob.printDialog(), so it can show to
        //  the user which pages it can print
        printFitToMargin = shrinkToPrintableArea;

        // set the number of pages
        printRequestAttributeSet.add(new PageRanges(startPage + 1, endPage + 1));
        // setup number of
        printRequestAttributeSet.add(new Copies(copies));

        // show the print dialog, return false if the user cancels/closes the
        // dialog. 
        if (showPrintDialog) {
            printService = getSetupDialog();
            return printService != null;
        } else {// no dialog and thus printing will continue.
            return true;
        }
    }

    public void showPrintSetupDialog() {
        PrinterJob pj = PrinterJob.getPrinterJob();
        if (printService == null && services != null &&
                services.length > 0 && services[0] != null) {
            printService = services[0];
        }
        try {
            pj.setPrintService(printService);
            // Step 2: Pass the settings to a page dialog and print dialog.
            pj.pageDialog(printRequestAttributeSet);
        } catch (Throwable e) {
            logger.log(Level.FINE, "Error creating page setup dialog.", e);
        }
    }

    private PrintService getSetupDialog() {
        final int offset = 50;
        return ServiceUI.printDialog(null,
                viewController.getViewContainer().getX() + offset,
                viewController.getViewContainer().getY() + offset,
                services, services[0],
                DocFlavor.SERVICE_FORMATTED.PRINTABLE,
                printRequestAttributeSet);
    }

    /**
     * Print a range of pages from the document as specified by #setupPrintService.
     *
     * @throws PrintException if a default printer could not be found or some
     *                        other printing related error.
     */
    public void print() throws PrintException {

        // make sure we have a service, if not we assign the default printer 
        if (printService == null && services != null &&
                services.length > 0 && services[0] != null) {
            printService = services[0];
        }

        if (printService != null) {

            // calculate total pages being printed
            calculateTotalPagesToPrint();

            printService.createPrintJob().print(
                    new SimpleDoc(this,
                            DocFlavor.SERVICE_FORMATTED.PRINTABLE,
                            docAttributeSet),
                    printRequestAttributeSet);
        } else {
            logger.fine("No print could be found to print to.");
        }

    }

    public CancelablePrintJob cancelablePrint() throws PrintException {

        // make sure we have a service, if not we assign the default printer
        if (printService == null && services != null &&
                services.length > 0 && services[0] != null) {
            printService = services[0];
        }

        if (printService != null) {

            // calculate total pages being printed
            calculateTotalPagesToPrint();

            DocPrintJob printerJob = printService.createPrintJob();
            printerJob.print(
                    new SimpleDoc(this,
                            DocFlavor.SERVICE_FORMATTED.PRINTABLE,
                            docAttributeSet),
                    printRequestAttributeSet);

            return (CancelablePrintJob) printerJob;
        } else {
            return null;
        }
    }

    public void print(PrintJobWatcher printJobWatcher) throws PrintException {

        // make sure we have a service, if not we assign the default printer
        if (printService == null && services != null &&
                services.length > 0 && services[0] != null) {
            printService = services[0];
        }

        if (printService != null) {

            // calculate total pages being printed
            calculateTotalPagesToPrint();

            DocPrintJob printerJob = printService.createPrintJob();
            printJobWatcher.setPrintJob(printerJob);

            printerJob.print(
                    new SimpleDoc(this,
                            DocFlavor.SERVICE_FORMATTED.PRINTABLE,
                            docAttributeSet),
                    printRequestAttributeSet);

            printJobWatcher.waitForDone();
        } else {
            logger.fine("No print could be found to print to.");
        }

    }


    private void calculateTotalPagesToPrint(){
        // iterate over page ranges to find out how many pages are to
        // be printed
        PageRanges pageRanges = (PageRanges)
                printRequestAttributeSet.get(PageRanges.class);
        totalPagesToPrint =  0;
        for (int[] ranges : pageRanges.getMembers()){
            totalPagesToPrint += ranges[1] - ranges[0] + 1;
        }
    }

}
