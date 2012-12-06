package edu.gwu.election.authoring;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.MediaTray;
import javax.print.attribute.standard.Sides;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFRenderer;

/**
 * Converts the PDF content into printable format
 * Source: http://webmoli.com/2008/11/03/java-print-pdf/
 */
public class PrintPdf {

	private float paperHeight = 11f;
	private float paperWidth = 8.5f;
	private PageFormat pf;
	
	private PrinterJob pjob = null;
	private HashPrintRequestAttributeSet aSet = new HashPrintRequestAttributeSet();
	public static void main(String[] args) throws IOException, PrinterException {
		if (args.length != 1) {
			System.err.println("The first parameter must have the location of the PDF file to be printed");
		}
		System.out.println("Printing: " + args[0]);
		// Create a PDFFile from a File reference
		FileInputStream fis = new FileInputStream(args[0]);
		PrintPdf printPDFFile = new PrintPdf(fis, "Test Print PDF", 8.5f, 11f);
		printPDFFile.print();
		//tests all possible trays to print from
		//I assume that the trays that a printer possesses is dependent on the printer itself
		printPDFFile.setTray("bottom");
		printPDFFile.print(true);
		printPDFFile.setTray("EnVelope");
		printPDFFile.print(true);
		printPDFFile.setTray("");
		printPDFFile.print(true);
		printPDFFile.setTray("Large_Capacity");
		printPDFFile.print(true);
		printPDFFile.setTray("Main");
		printPDFFile.print(true);
		printPDFFile.setTray("Manual");
		printPDFFile.print(true);
		printPDFFile.setTray("middle");
		printPDFFile.print(true);
		printPDFFile.setTray("Side");
		printPDFFile.print(true);
		printPDFFile.setTray("top");
		printPDFFile.print(true);
	}

	/**
	 * Constructs the print job based on the input stream
	 * 
	 * @param inputStream
	 * @param jobName
	 * @throws IOException
	 * @throws PrinterException
	 */
	public PrintPdf(InputStream inputStream, String jobName, float width, float height) throws IOException, PrinterException {
		paperWidth = width;
		paperHeight = height;
		
		byte[] pdfContent = new byte[inputStream.available()];
		inputStream.read(pdfContent, 0, inputStream.available());
		initialize(pdfContent, jobName);
	}

	/**
	 * Constructs the print job based on the byte array content
	 * 
	 * @param content
	 * @param jobName
	 * @throws IOException
	 * @throws PrinterException
	 */
	public PrintPdf(byte[] content, String jobName) throws IOException, PrinterException {
		initialize(content, jobName);
	}

	/**
	 * Initializes the job
	 * 
	 * @param pdfContent
	 * @param jobName
	 * @throws IOException
	 * @throws PrinterException
	 */
	private void initialize(byte[] pdfContent, String jobName) throws IOException, PrinterException {
		ByteBuffer bb = ByteBuffer.wrap(pdfContent);
		// Create PDF Print Page
		PDFFile pdfFile = new PDFFile(bb);
		PDFPrintPage pages = new PDFPrintPage(pdfFile);

		// Create Print Job
		pjob = PrinterJob.getPrinterJob();
		pf = PrinterJob.getPrinterJob().defaultPage();
		pjob.setJobName(jobName);
		Book book = new Book();
		book.append(pages, pf, pdfFile.getNumPages());
		pjob.setPageable(book);

		// to remove margins
		Paper paper = new Paper();
		paper.setSize(paperWidth * 72, paperHeight * 72);
//		paper.setSize(6 * 72, 6 * 72);
		paper.setImageableArea(0, 0, paper.getWidth(), paper.getHeight());
		pf.setPaper(paper);
	}

	/**
	 * Sets whether this should print DUPLEX or ONE_SIDED.
	 * @param sided whether to print double sided or on a single side.
	 */
	public void setOrientation(String orient) {
		if (orient.equalsIgnoreCase("LANDSCAPE")) {
			pf.setOrientation(PageFormat.LANDSCAPE);
		} else if (orient.equalsIgnoreCase("PORTRAIT")) {
			pf.setOrientation(PageFormat.LANDSCAPE);
		}
	}
	
	/**
	 * Sets whether this should print DUPLEX or ONE_SIDED.
	 * @param sided whether to print double sided or on a single side.
	 */
	public void setSided(String sided) {
		if (sided.equalsIgnoreCase("DUPLEX")) {
			aSet.add(Sides.DUPLEX);
		} else if (sided.equalsIgnoreCase("ONE_SIDED")) {
			aSet.add(Sides.ONE_SIDED);
		}
	}
	
	/**
	 * Sets the printer to print using a specific tray
	 * @param tray
	 */
	public void setTray(String tray){
		
		if(tray.equalsIgnoreCase("BOTTOM"))
			aSet.add(MediaTray.BOTTOM);
		if(tray.equalsIgnoreCase("ENVELOPE"))
			aSet.add(MediaTray.ENVELOPE);
		if(tray.equalsIgnoreCase("LARGE_CAPACITY"))
			aSet.add(MediaTray.LARGE_CAPACITY);
		if(tray.equalsIgnoreCase("MAIN"))
			aSet.add(MediaTray.MAIN);
		if(tray.equalsIgnoreCase("MANUAL"))
			aSet.add(MediaTray.MANUAL);
		if(tray.equalsIgnoreCase("MIDDLE"))
			aSet.add(MediaTray.MIDDLE);
		if(tray.equalsIgnoreCase("SIDE"))
			aSet.add(MediaTray.SIDE);
		if(tray.equalsIgnoreCase("TOP"))
			aSet.add(MediaTray.TOP);
	}
	public void print() throws PrinterException {
		// Send print job to default printer
		pjob.print();
	}
	/**
	 * sends the print job to the default printer
	 * @param multipleTrays true if the tray has been set
	 */
	public void print(boolean multipleTrays) throws PrinterException{
		if(multipleTrays)
			pjob.print(aSet);
		else
			pjob.print();
	}
}

/**
 * Class that actually converts the PDF file into Printable format
 */
class PDFPrintPage implements Printable {

	private PDFFile file;

	PDFPrintPage(PDFFile file) {
		this.file = file;
	}

	public int print(Graphics g, PageFormat format, int index) throws PrinterException {
		int pagenum = index + 1;
		if ((pagenum >= 1) && (pagenum <= file.getNumPages())) {
			Graphics2D g2 = (Graphics2D) g;
			PDFPage page = file.getPage(pagenum);

			// fit the PDFPage into the printing area
			Rectangle imageArea = new Rectangle((int) format.getImageableX(), (int) format.getImageableY(),
					(int) format.getImageableWidth(), (int) format.getImageableHeight());
			g2.translate(0, 0);
			PDFRenderer pgs = new PDFRenderer(page, g2, imageArea, null, null);
			try {
				page.waitForFinish();
				pgs.run();
			} catch (InterruptedException ie) {
				// nothing to do
			}
			return PAGE_EXISTS;
		} else {
			return NO_SUCH_PAGE;
		}
	}
}
