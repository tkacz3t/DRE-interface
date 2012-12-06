/*******************************************************************************
 * Copyright (c) 2011 Alex Florescu, Jan Rubio, John Wittrock, Tyler Kaczmarek.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Jan Rubio - initial API and implementation
 ******************************************************************************/
package edu.gwu.election.authoring;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import org.gwu.voting.standardFormat.basic.Question;
import org.gwu.voting.standardFormat.electionSpecification.ElectionSpecification;
import org.xml.sax.SAXException;

//import software.common.BallotGeometry;
//import software.common.Util;
import org.scantegrity.common.Util;
import org.scantegrity.common.BallotGeometry;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfGState;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.ImgJBIG2;
import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.text.pdf.qrcode.EncodeHintType;
import com.itextpdf.text.pdf.qrcode.ErrorCorrectionLevel;
//import com.itextpdf.text.pdf.PdfWriter;

import edu.gwu.election.Globals;
import edu.gwu.election.Print;

/**
 * Geometry and background files should be placed in the election folder root.
 * Empty background filename in the property file will print only the marks.
 */
public class PrintableBallotMarker extends org.scantegrity.authoring.invisibleink.PrintableBallotMaker {

	/**
	 * Temporary PDF file produced to send to the printer. File must be written
	 * then sent to the printer. File is deleted once sent.
	 */
	private static final String TEMP_PRINT_FILE = "marks";
	
	/**
	 * Filename of the blank PDF file that is generated if no background is
	 * provided.
	 */
	private static final String BLANK_PDF = "__BlankPdf.pdf";
	
	/** The opacity of the text and filled background within the oval/rectangle. */
	private static final float OPACITY = 1.0f;

	/**
	 * Characters drawn behind the actual confirmation code text to fill in
	 * <code>ovalColor</code> the background with same font style blocks.
	 * Characters increase in pixel width as index increases starting with 4
	 * pixels.
	 */
	private static final String[] DUMMY_BACKGROUNDS = 
									{"!","\"","#","$","%","&","'","(",")"};
	
	/**
	 * Character to fill the left and right sides of the oval before the
	 * confirmation codes start and after they end.
	 */
	private static final String BAR_CHAR = "|";
	
	/** Color of the background fill within the oval/rectangle. */
	private static BaseColor OVAL_COLOR = new BaseColor(51,51,51);
//	(0,255,255);	//100% Cyan
//	(255,0,255); 	//100% Yellow
//	(255,255,2);	//80%
//	(255,255,153);	//40%  Yellow
//	(230,255,102);	//60%  Yellow 	10% Cyan
//	(153,255,102);	//60%  Yellow 	40% Cyan
//	(0,255,0);		//100% Yellow 	100% Cyan
//	(255,255,128);	//50%  Yellow
//	(255,255,204);	//20%  Yellow
//	(255,255,102);	//60%  Yellow
//	(255,255,77);	//70%  Yellow
//	(255,255,51);	//70%  Yellow

	/** Color of the confirmation codes. */
	private static BaseColor SYMBOL_COLOR = new BaseColor(178,178,36);
//	(0,255,255); 		//100% Magenta
//	(255, 48, 255); 	// 80%
//	(255, 153, 255); 	// 40%  Magenta
//	(230, 102, 255); 	// 60%  Magenta 10%Cyan
//	(153, 102, 255); 	// 60%  Magenta 40%Cyan
//	(0, 0, 255); 		// 100% Magenta 100%Cyan
//	(255, 255, 0); 		// 100% Yellow
//	(255, 128, 255);	// 50%  Magenta
//	(255, 102, 255); 	// 60%  Magenta
//	(255, 204, 255); 	// 20%  Magenta
//	(128, 255, 255); 	// 50%  Cyan
//	(255, 77, 255); 	// 70%  Magenta
//	(255, 51, 255); 	// 80%  Magenta
//	(255, 128, 255); 	// 50%  Magenta
	
	/**
	 * Contains details about printing how to print(using ovals or not), where
	 * to print(which tray), and with what background. These details are
	 * automatically set from the DRE properties file. 
	 */
	public static enum PrintSetting {
		BALLOT	("Ballot"), 
		CARD	("Card");
		
		/**
		 * Filename of the PDF file used as the background. Non existent file or
		 * empty name will print on a blank background.
		 */
		private String background;
		
		/** The tray destination to be printing to. */
		private String tray;
		
		/** Whether to print the as an oval or leave as rectangle. */
		private boolean useOval;

		/** Path to the geometry file. */
		private String geometry;
		
		/**
		 * Sets the printing details according to the specified values in the
		 * property files. The <code>prefix</code> selects which property keys
		 * are read. 
		 * @param prefix Sets the details based on this prefix.
		 */
		PrintSetting(String prefix) {
			String basePath = Globals.PROPERTIES.getProperty("ElectionFolder");
			if(prefix.equalsIgnoreCase("card")){
				Print.debug(basePath + "private/" + Globals.PROPERTIES.getProperty("Ward")
						+ "/" + Globals.PROPERTIES.getProperty("Language") + Globals.PROPERTIES.getProperty(prefix + "Background"));
			background = basePath + "private/" + Globals.PROPERTIES.getProperty("Ward")
					+ "/" + Globals.PROPERTIES.getProperty("Language") + Globals.PROPERTIES.getProperty(prefix + "Background");
			geometry = basePath + "private/" + Globals.PROPERTIES.getProperty("Ward")
					+ "/" + Globals.PROPERTIES.getProperty("Language") + Globals.PROPERTIES.getProperty(prefix + "Geometry");
			
			tray = Globals.PROPERTIES.getProperty(prefix + "Tray");
			useOval = Globals.PROPERTIES.getProperty(prefix + "UseOval")
					.equalsIgnoreCase("TRUE");
			}
			else{
				Print.debug("I am in ward " + Globals.PROPERTIES.getProperty("Ward"));
				background = basePath + "/private/" + Globals.PROPERTIES.getProperty("Ward")
					+ "/" + Globals.PROPERTIES.getProperty(prefix + "Background");
					geometry = basePath + "/private/" + Globals.PROPERTIES.getProperty("Ward")
					+ "/" + Globals.PROPERTIES.getProperty(prefix + "Geometry");
		
				tray = Globals.PROPERTIES.getProperty(prefix + "Tray");
				useOval = Globals.PROPERTIES.getProperty(prefix + "UseOval")
					.equalsIgnoreCase("TRUE");
			}
		}
	}
	
	/**
	 * The specific details about which tray to use, which geometry file, to use
	 * ovals or rectangles, and the background PDF to use.
	 */
	private PrintSetting printSetting;

	/** Specification used to match votes with questions and choices. */
	private org.scantegrity.common.ballotstandards.electionSpecification.ElectionSpecification es;

	/**
	 * Creates a new ballot marker for election <code>es</code> with settings
	 * <code>printDetail</code>.
	 * @param es Specification for this election.
	 * @param printSetting Settings for the printer and output.
	 * @throws Exception
	 */
	public PrintableBallotMarker(org.scantegrity.common.ballotstandards.electionSpecification.ElectionSpecification es,
			PrintSetting printSetting) throws Exception {
		super(es, new BallotGeometry(printSetting.geometry));
		this.es = es;
		this.printSetting = printSetting;
	}
	
	/**
	 * Sets the PrintSetting to print to the new settings. 
	 * @param printDetail Preset details specified in the DRE property file.
	 * @throws IOException File could not be found.
	 * @throws SAXException Error parsing the file.
	 */
	public void setPrintSetting(PrintSetting printSetting) throws SAXException, IOException{
		this.printSetting = printSetting;
		if(this.printSetting == PrintSetting.BALLOT){
			String basePath = Globals.PROPERTIES.getProperty("ElectionFolder");
			Print.debug("I am in ward " + Globals.PROPERTIES.getProperty("Ward"));
			this.printSetting.background = basePath + "/private/" + Globals.PROPERTIES.getProperty("Ward")
				+ "/" + Globals.PROPERTIES.getProperty("BallotBackground");
			this.printSetting.geometry = basePath + "/private/" + Globals.PROPERTIES.getProperty("Ward")
				+ "/" + Globals.PROPERTIES.getProperty("BallotGeometry");
		}
		this.geom = new BallotGeometry(printSetting.geometry);
	}

	/**
	 * Override parent method to do nothing (leave variable jsFunctions as empty
	 * string). Parent reads the file jsFunctions.js and saves to to variable
	 * jsFunctions.
	 */
	protected void loadJavaScript() {
		return;
	}
	
	/**
	 * Creats a blank PDF page to write the marks on.
	 * @author Scantegrity PrintableBallotMaker
	 * @param w Width of the page.
	 * @param h Height of the page.
	 * @throws DocumentException 
	 */
	private PdfReader createBlankBackgroundPage(float w, float h) throws IOException, DocumentException {
		com.itextpdf.text.Document document = 
			new com.itextpdf.text.Document(new Rectangle(w, h));
		
		PdfWriter.getInstance(document, new FileOutputStream(BLANK_PDF));
		document.open();
		
		/* 
		 * Write a space so that an actual page is generated. If nothing is
		 * written, then no page is added so there will be no blank page.
		 */
		document.add(new Paragraph(" "));
		document.close();

		return new PdfReader(BLANK_PDF);
	}

	/**
	 * Draws a white outline of an oval at the given position. This mask makes
	 * the underlying rectangle appear to be an oval.
	 * @author Scantegrity PrintableBallotMaker
	 * @param cb The output to write to.
	 * @param possition The position of the oval.
	 */
	protected void fillOval(PdfContentByte cb, Rectangle possition) {
		if (possition.getHeight() > possition.getWidth()) {
			throw new IllegalArgumentException(
					"Cannot draw jelly bean; the height is greater then the widtd for "
							+ possition);
		}

		// Draw a white line to mask the rectangle into an oval.
		float widthWhite = possition.getHeight() * 0.32f;
		cb.saveState();
		cb.setCMYKColorStrokeF(0, 0, 0, 0);
		cb.setLineWidth(widthWhite);
		cb.roundRectangle(possition.getLeft() - widthWhite / 2,
				possition.getBottom() - widthWhite / 2, 
				possition.getWidth() + widthWhite, 
				possition.getHeight() + widthWhite, 
				possition.getHeight() / 2 + widthWhite / 2);
		cb.stroke();
		
		cb.rectangle(possition.getLeft() - widthWhite / 2,
				possition.getBottom() - widthWhite / 2, 
				possition.getWidth() + widthWhite, 
				possition.getHeight() + widthWhite);
		cb.stroke();
		cb.restoreState();

		// Draw the black oval around the outline.
		cb.saveState();
		cb.setCMYKColorStrokeF(0, 0, 0, 1.0f);
		cb.roundRectangle(possition.getLeft(), 
				possition.getBottom(), 
				possition.getWidth(), 
				possition.getHeight(), 
				possition.getHeight() / 2);
		cb.stroke();		
		cb.restoreState();		
	}

	/**
	 * Writes <code>numPixels</code> columns of pixels at the given location. 
	 * @param cb The output to be written.
	 * @param font Font of the character.
	 * @param barWidth Width of the cloumn.
	 * @param fontSize Size of the font.
	 * @param numPixels Number of columns to write.
	 * @param startx Start x position to write.
	 * @param y Y position of writing.
	 */
	private void addBarText(PdfContentByte cb, BaseFont font, float barWidth, int fontSize, float numPixels, float startx, float y, BaseColor bg) {
		for (int i = 0; i < numPixels; i++) {
			cb.saveState();
			{
				PdfGState state=new PdfGState();
				state.setFillOpacity(OPACITY);
				state.setStrokeOpacity(OPACITY);
				cb.setGState(state);
			}		
			cb.beginText();
			cb.setColorFill(bg);
			cb.moveText(startx, y);
			cb.setFontAndSize(font, fontSize);
			cb.showText(BAR_CHAR);
			cb.endText();
			cb.restoreState();
			
			startx += barWidth;
		}
	}
	
	/**
	 * Writes centered text to the given position and fills the background with
	 * color <code>OVAL_COLOR</code>.<br/><br/>Reivew 
	 * http://en.wikipedia.org/wiki/Typeface for font metrics. 
	 * @author Scantegrity PrintableBallotMaker
	 * 
	 * @param cb The output to write to.
	 * @param possition Position of the text.
	 * @param font Font to use for the text.
	 * @param XXXfontSize Size of the text.
	 * @param textColor Color of the font.
	 * @param text The string to be written.
	 */
	public void addTextCentered(PdfContentByte cb, Rectangle possition,
			BaseFont font, int XXXfontSize, BaseColor textColor, BaseColor bg, String text) {

		/**
		 * FIXME - now we have a fixed size font, based on the size of the oval
		 * 0.38 X 0.13 inches in the definition file
		 */
		int fontSize = XXXfontSize;
		
		// Fill a white background
		cb.saveState();
		cb.setColorFill(BaseColor.WHITE);
		cb.setColorStroke(BaseColor.WHITE);
		cb.rectangle(possition.getLeft(), 
				possition.getBottom(), 
				possition.getWidth(), 
				possition.getHeight());
		cb.fillStroke();
		cb.restoreState();
		
		/*
		 * Check how many | characters will fit in the xoffset. Write this many
		 * bar characters before and after the text
		 */
		float rectLeft = possition.getLeft();
		float rectBottom = possition.getBottom();
		
		float textHeigthPoints = Math.abs(font.getAscentPoint(text, fontSize));
		float barWidth = Math.abs(font.getWidthPoint(BAR_CHAR,fontSize));
		
		float xBar = rectLeft;
		float yBar = rectBottom + (possition.getHeight() - textHeigthPoints) / 2;
		
		int noTotalPixels = Math.round((possition.getWidth() / barWidth));
		int noPixelsTheTextIsWide = Math.round((
				Math.abs(font.getWidthPoint(text, fontSize)) / barWidth));

		// Offset calculated depending on the size of the text. Random offset was removed.
		int randomOffset = 2 + (int)(0.4 * 
				Math.max(0, ((noTotalPixels - noPixelsTheTextIsWide) - 4)));

		// Draw the bar character until the offset is reached.
		int noPixelsSoFar = 0;
		float x = xBar;
		float y = yBar;
		String t = null;
		
		// Add the bar pixels before the text and adjust the counters 
		addBarText(cb, font, barWidth, fontSize, randomOffset, x, y, bg);
		x += barWidth * randomOffset;
		noPixelsSoFar += randomOffset;
		
		// Draw the text, one letter at a time.
		for (int i = 0; i < text.length(); i++) {
			t = Character.toString(text.charAt(i));
			int noPixelsTheLetterIsWide = Math.round((
					Math.abs(font.getWidthPoint(t, fontSize)) / barWidth));
			noPixelsSoFar += noPixelsTheLetterIsWide;

			/*
			 * Find and draw the dummy character with the the exact size/width
			 * as the letter being drawn. This acts as the background fill.
			 */
			cb.saveState();
			{
				PdfGState state = new PdfGState();
				state.setFillOpacity(OPACITY);
				state.setStrokeOpacity(OPACITY);
				cb.setGState(state);
			}
			cb.beginText();
			cb.setColorFill(bg);
			cb.moveText(x, y);
			cb.setFontAndSize(font, fontSize);
			cb.showText(DUMMY_BACKGROUNDS[noPixelsTheLetterIsWide - 4]);
			cb.endText();
			cb.restoreState();
				
			// Draw single character
			cb.saveState();
			{
				PdfGState state = new PdfGState();
				state.setFillOpacity(OPACITY);
				state.setStrokeOpacity(OPACITY);
				cb.setGState(state);
			}
			cb.beginText();
			cb.setColorFill(textColor);
			cb.moveText(x, y);
			cb.setFontAndSize(font,fontSize);
			cb.showText(t);
			cb.endText();
			cb.restoreState();
			
			// Shift to the next character
			x += noPixelsTheLetterIsWide * barWidth;
		}
		
		// Add the bar pixels after the text
		addBarText(cb, font, barWidth, fontSize, 
				noTotalPixels - noPixelsSoFar, x, y, bg);
	}
	
	/**
	 * Draw text starting on the left hand side of the rectangle.
	 * @param text Text to be written
	 * @param possition Position of the text
	 * @param font Font to be printed
	 * @param fontSize Size of font
	 */
	private void drawRegularText(String text, Rectangle possition,BaseFont font,int fontSize) {
		cb.beginText();
		cb.setFontAndSize(font,fontSize);
		font.correctArabicAdvance();		
		cb.showTextAligned(PdfContentByte.ALIGN_LEFT, text, possition.getLeft(), possition.getBottom(), 0);
		cb.endText();
	}

	/**
	 * Prints the confirmation codes according to the settings specified by
	 * PrintSetting. Single, multiple and ranked votes are handled the same.
	 * @param votes
	 *            The plaintext votes that were stored by the DRE
	 * @param confirmationCodes
	 *            Adds text to the corresponding votes. If null, no codes will
	 *            be written.
	 * @throws DocumentException Failed to create the PDF file on the 
	 * 			  temporary output file. 
	 * @throws PrinterException Failed to print the file.
	 * @throws IOException Failed to read the background file as well 
	 * 			  as failed to create a blank background.
	 */
	public void printMarks(BallotRow ballot, int[][] votes,
			TreeMap<Integer, TreeMap<Integer, String>> confirmationCodes)
			throws DocumentException, IOException, PrinterException {

		/* 
		 * Read the background PDF or create a blank/white background if no
		 * background was specified.
		 */ 
		PdfReader backgroundReader;
		try {
			Print.debug("The background is " + this.printSetting.background);
			backgroundReader = new PdfReader(printSetting.background);
		} catch (IOException e) {
			backgroundReader = createBlankBackgroundPage(
					geom.getWidth() * 72,
					geom.getHeight() * 72);
		}
		
		// Create the PDF with the specified background in the DRE properties.
		Rectangle pdfPageSize = backgroundReader.getPageSize(1);
		Document document = new Document(pdfPageSize);
		
		/* 
		 * Find a file that hasn't been created yet and set up the PDF writer.
		 * File needs to be written to disk, then sent to the printer.
		 * File is deleted once sent to the printer.
		 */
		File f;
		for (int i = 0; (f = new File(TEMP_PRINT_FILE + i
				+ ".pdf")).exists(); i++);
		
		writer = PdfWriter.getInstance(document, new FileOutputStream(f));
		writer.setPdfVersion(PdfWriter.VERSION_1_4);
		writer.setPDFXConformance(PdfWriter.PDFXNONE);
		
		// Create a single page for the ballot
		document.open();
        cb = writer.getDirectContent();
		PdfImportedPage page1 = writer.getImportedPage(backgroundReader, 1);
		document.setMargins(0,0,0,0);
		cb.addTemplate(page1,0,0);

		// If printing on the card, swap the colors
		if (printSetting == PrintSetting.CARD) {
			BaseColor temp = OVAL_COLOR;
			OVAL_COLOR = SYMBOL_COLOR;
			SYMBOL_COLOR = temp;
		}
		
		// For each question, check which candidates were chosen.
		org.scantegrity.common.ballotstandards.basic.Question[] ques = es.getOrderedQuestions();
		for (org.scantegrity.common.ballotstandards.basic.Question qu: ques) {
			int qno = Integer.parseInt(qu.getId());
			
			// For each choice, check if a candidate was chosen.
			for (int choice = 0; choice < votes[qno].length; choice++) {
				int cand = votes[qno][choice];
			//checks to see if a writeIn has ever been written
				boolean usedWriteIn = false;
				// If a candidate was selected, add the confirmation code.
				if (cand > -1) {

					/* 
					 * If cand is greater than number of candidates, this must
					 * have been a write in vote, set candidate to the last
					 * position which is the write in box.
					 */
					if (cand >= 6) {
						int writeIn = cand;
						usedWriteIn = true;
						cand = votes[qno].length - 1;
						/*
						 *  If printing a ballot, also get the position of 
						 *  the write in box '+ 2' because the first two 
						 *  serials are reserved for the stub number 
						 *  and the web serial
						 */
						if (printSetting == PrintSetting.BALLOT) {
							Print.debug(qno + 2 + " IS QNO + 2 " + writeIn);
							rect = geom.getSerialTop(qno + 2 + "");
							Print.debug((rect == null) + " rect is null");
							addTextCentered(cb, rect, serialFont, 14, 
									OVAL_COLOR, SYMBOL_COLOR, writeIn + "");
						}
					} else {
						// No write in but printing on ballot, fill in the empty box
						if (printSetting == PrintSetting.BALLOT && !usedWriteIn) {
							rect = geom.getSerialTop(qno + 2 + "");
							//addTextCentered(cb, rect, serialFont, 14, 
							//		BaseColor.WHITE, BaseColor.WHITE, "1");
						}						
					}
					
					/*
					 * If printing mode is set to ballot, print out in the
					 * candidate order else print out in the order of choice.
					 */
					if (printSetting == PrintSetting.BALLOT) {
						
						if (qu.getTypeOfAnswer().equals("ranked")) {
							// Ranked, fill in on separate ranks.
							rect = geom.getTop(qno+"", choice+"", cand+"");
						} else {
							// Multiple vote, fill in, in the same rank.
							rect = geom.getTop(qno+"", 0+"", cand+"");
						}
					} else if (printSetting == PrintSetting.CARD) {
						Print.debug("Printing on the card");
						rect = geom.getTop(qno+"", 0+"", choice+"");
					}
					
					if (confirmationCodes != null) {
						
						// Adjust the confirmation code to read from the appropriate rank
						int adjCand = cand;
						if (qu.getTypeOfAnswer().equals("ranked")) {
							adjCand = adjCand * votes[qno].length + choice;
						}
						
						String confCode = confirmationCodes.get(qno).get(adjCand);
						if(rect != null && confCode != null)
							addTextCentered(cb, rect, serialFont, 14, 
								SYMBOL_COLOR, OVAL_COLOR, confCode);
					}
				}
				
				// For each question, and each rank, create ovals for every candidate
				if (printSetting.useOval) {
					int numCands = confirmationCodes.get(qno).size();
					for (int cands = 0; cands < numCands; cands++){						
						rect = geom.getTop(qno+"", choice+"", cands+""); 

						if (rect != null) {
							
							if (cands != cand) {
								/* 
								 * Need to print something to have it properly
								 * print out the box.
								 */
								addTextCentered(cb, rect, serialFont, 14, 
										BaseColor.WHITE, BaseColor.WHITE, "1");
							}
							
							fillOval(cb, rect);
						}
					}
				}
			}
		}
		
		// Print the webserial
    	rect =geom.getSerialTop("0");
    	drawWhiteRectangle(rect);
    	String webSerial = ballot.getWebSerial();
    	if (printSetting == PrintSetting.BALLOT) {
    		drawRegularText(webSerial, rect, this.helv, 12);
    	} else {
			addTextCentered(cb, rect, serialFont, 12, 
					SYMBOL_COLOR, OVAL_COLOR, webSerial);
    	}
    	
		/*
		 *  If printing a ballot, print the ballot on the 2nd page, so
		 *  add a blank first page.
		 *  
		 *  Also add barcode, alignment marks, and stub serial
		 */
		if (printSetting == PrintSetting.BALLOT) {
			// Print Alignment mark
			addAlignment(0+"");
			
	    	// Print the stub serial
	    	rect=geom.getSerialTop("1");
	    	drawWhiteRectangle(rect);
	    	String printingKey1= ballot.getStubSerial();
	    	drawRegularText(printingKey1, rect, this.helv, 12);
			
	    	// Draw the 2D barcode
	       /* Image barcode=null;
	        try {
	        	// Generate a 2D barcode
	        	BufferedImage bi=ImageIO.read(new URL("http://chart.apis.google.com/chart?cht=qr&chs=120x120&chl=" + ballot.getBarcodeSerial()));
	        	System.out.println(bi.toString() + " ");
				barcode=Util.CMYKBufferedImageToCMYKImage(
						Util.RGBBufferedImageToCMYKBufferedImage(bi));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} */
	        //add the 2D barcode        
	    	Rectangle r;
	        r=geom.makeRectangle(geom.getSerialBulletedNode());
	        //drawWhiteRectangle(rect);
	        //barcode.setAbsolutePosition(rect.getLeft(),rect.getBottom());
	        float w=r.getWidth()*1.25f; 
	        float h=r.getHeight()*1.25f;
	        float x = r.getLeft()-((w-r.getWidth())/2);
	        float y = r.getBottom()-((h-r.getHeight())/2);
	        BarcodeQRCode barcode;
	        try {
	            // Level H is the highest level of error correction. 
	            Map<EncodeHintType, Object> l_hints = 
	                        new Hashtable<EncodeHintType, Object>();
	            l_hints.put( EncodeHintType.ERROR_CORRECTION, 
	                            ErrorCorrectionLevel.H );
	            
	            barcode = 
	                                new BarcodeQRCode(ballot.getBarcodeSerial(),
	                                                    (int)Math.max(w,h), 
	                                                    (int)Math.max(w,h), 
	                                                    l_hints);
	            com.itextpdf.text.Image l_img = barcode.getImage();
	            l_img.setDpi(600, 600);
	            l_img.setAbsolutePosition(x, y);
	            //l_img.scaleAbsolute(Math.max(w,h), Math.max(w,h));
	            //Image k_img = imageConvert(l_img);
	            //k_img.setDpi(600, 600);
	            //k_img.setAbsolutePosition(x, y);
	            cb.addImage(l_img);
	        } catch (DocumentException e) {
	            e.printStackTrace();
	            
			// Find the position and draw it			
//			document.newPage();
	//		document.add(new Paragraph(" "));
		}
		}
		// Done writing to the PDF document.
		document.close();
		
		// Read the PDF and send to the printer.
		FileInputStream fis = new FileInputStream(f);
		PrintPdf printPDFFile;
		printPDFFile = new PrintPdf(fis, f.getName(), geom.getWidth(), geom.getHeight());
		printPDFFile.setTray(printSetting.tray);
		
		if (printSetting == PrintSetting.BALLOT) {
//			printPDFFile.setOrientation("PORTRAIT");
//			printPDFFile.setSided("DUPLEX");
		} else {
			printPDFFile.setOrientation("LANDSCAPE");
		}
		
		printPDFFile.print(true);
		fis.close();
		
		// Delete the temporary PDF file once sent to the printer.
		f.delete();
		
		//if a card is being printed, swap the colours back
		if (printSetting == PrintSetting.CARD) {
			BaseColor temp = OVAL_COLOR;
			OVAL_COLOR = SYMBOL_COLOR;
			SYMBOL_COLOR = temp;
			Print.debug("The background is " + this.printSetting.background);
		}
	}
	
	public static void testPrintMarks() throws Exception {
		FileInputStream in = new FileInputStream("DreProperties.properties");
		Globals.PROPERTIES = new Properties();
		Globals.PROPERTIES.load(in);
		
    	// Test the ballot marker
    	String publicFolder= "TakomaParkDemo/";
		org.scantegrity.common.ballotstandards.electionSpecification.ElectionSpecification es = new org.scantegrity.common.ballotstandards.electionSpecification.ElectionSpecification(publicFolder+"ElectionSpec.xml");
		
		// Initialize votes and confirmation codes to be written
		int[][] votes = new int[2][];
		votes[0] = makeEmptyArr(3);
		votes[1] = makeEmptyArr(2);
		
		votes[0][0] = 1491;
		votes[0][1] = 0;
		votes[0][2] = 1;
		
		votes[1][0] = 0;
		votes[1][1] = 794932;
		
		TreeMap<Integer, TreeMap<Integer, String>> confirmationCodes = new TreeMap<Integer, TreeMap<Integer, String>>(); 
		TreeMap<Integer, String> q1ans = new TreeMap<Integer, String>();
		q1ans.put(0, "736");
		q1ans.put(1, "835");
		q1ans.put(2, "071");
		TreeMap<Integer, String> q2ans = new TreeMap<Integer, String>();
		q2ans.put(0, "945");
		q2ans.put(1, "239");
//		TreeMap<Integer, String> q3ans = new TreeMap<Integer, String>();
//		q3ans.put(0, "A30");
//		q3ans.put(2, "A32");
		
		confirmationCodes.put(0, q1ans);
		confirmationCodes.put(1, q2ans);
//		confirmationCodes.put(2, q3ans);
		
		// Print the marked ballot
    	PrintableBallotMarker pbm =new PrintableBallotMarker(es, 
    			PrintableBallotMarker.PrintSetting.CARD);
//    	pbm.printMarks(votes, confirmationCodes);
		
		pbm.setPrintSetting(PrintableBallotMarker.PrintSetting.BALLOT);
		BallotRow ballot = new BallotRow();
		ballot.setStubSerial("1234");
		ballot.setWebSerial("5678");
		
		pbm.printMarks(ballot, votes, confirmationCodes);
	}
	
    public static void main(String[] args) throws Exception {
    	testPrintMarks();
    }
    
    public com.itextpdf.text.Image imageConvert(com.itextpdf.text.Image input){
    	com.itextpdf.text.Image output = null;
    	try {
			output = com.itextpdf.text.Image.getInstance((int)(input.getWidth()),(int)(input.getHeight()), 1, input.getBpc(), input.getOriginalData());
		} catch (com.itextpdf.text.BadElementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	/*
       	FileOutputStream out = null;
		try {
			out = new FileOutputStream("tempBarcode.pdf");
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	com.itextpdf.text.Document doc = new com.itextpdf.text.Document(new Rectangle(input.getWidth(), input.getHeight()));
    	com.itextpdf.text.pdf.PdfWriter tempFile = null;
    	
    	try {
			tempFile = com.itextpdf.text.pdf.PdfWriter.getInstance(doc, out);
		} catch (com.itextpdf.text.DocumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		tempFile.setPdfVersion(PdfWriter.VERSION_1_4);
		tempFile.setPDFXConformance(PdfWriter.PDFXNONE);
    	try {
			output = Image.getInstance("tempBarcode.pdf");
		} catch (com.itextpdf.text.BadElementException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
    	return output;
    }
    /**
     * Create an empty int array with default -1 values
     * @param len Size of the array.
     * @return
     */
	public static int[] makeEmptyArr(int len){
		int[] ret = new int[len];
		for (int i = 0; i < len; i++) {
			ret[i] = -1;
		}
		return ret;
	}
}
