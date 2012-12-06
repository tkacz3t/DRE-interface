package temp;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.TreeMap;
import java.util.Vector;

import javax.imageio.*;

import org.gwu.voting.standardFormat.basic.Question;
import org.gwu.voting.standardFormat.electionSpecification.ElectionSpecification;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BarcodePDF417;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import software.common.BallotGeometry;
import software.common.BallotGeometryMap;
import software.common.Cluster;
import software.common.InputConstants;
import software.common.Prow;
import software.scanner.ScannedBallot.TypeOfVotes;
import software.scanner.scantegrity.ScannedBallot;

import com.lowagie.text.*;
import java.awt.geom.Rectangle2D;

/*
 * After the ballot is scanned, fetch the selected codes (using ballot geometry) and create a PDF to print receipt
 */

public class ScannedBallotWithReceipt extends software.scanner.scantegrity.ScannedBallotWithBarcodes {
	
	public static final boolean debug=false;
	static final int offset=7;
	
	public ScannedBallotWithReceipt(BallotGeometry geom, ElectionSpecification es) {
		super(geom, es);
	}

    
	public static void printPdfSilently(String file) {
	       try {
	    	   Runtime.getRuntime().exec("cmd.exe /C start acrord32 /p /h " + file);
	       } 
	       catch (IOException e) {
	                     e.printStackTrace();
	               }
	}         

	
	public static void main(String[] args) throws Exception {
		/*		
				String dir="Elections/VoComp/scantegrity/";
				BallotGeometry geom=new BallotGeometry(dir+"geometry.xml");
				ElectionSpecification es= new ElectionSpecification(dir+"../ElectionSpec.xml");
				ScannedBallot sb=new ScannedBallot(geom,es);
				BufferedImage img=ImageIO.read(new File(dir+"scannes/ballot0004.JPG"));
				sb.detect(img);
				System.out.println(sb.toProw());
		*/		
		
		String dir=InputConstants.publicFolder;
		BallotGeometry geom=new BallotGeometry(dir+"niubaloate/geometry.xml");
		ElectionSpecification es= new ElectionSpecification(dir+"niubaloate/ElectionSpec.xml");
		BallotGeometryMap bgm = new BallotGeometryMap(geom,es);
		
		BufferedImage img=null;
		//img=ImageIO.read(new File("aps.bmp"));
		img=ImageIO.read(new File("ad1.jpg"));

		double dpi=img.getWidth()/bgm.getWidth();
		System.out.println(dpi+" "+img.getWidth());
		
			
		ScannedBallotWithReceipt sb=new ScannedBallotWithReceipt(geom,es);
		//sb.setMailIn(false);
		sb.detect(img);
		System.out.println(sb.getSerial());
		System.out.println(sb.toProw());		
		dpi=sb.getDpi();
		System.out.println("Dpi is "+dpi);
		
		
		//float w=(float)bgm.getWidth()*72;
		//float h=(float)bgm.getHeight()*72;
		int pdpi=72;
		float w=6*pdpi;
		float h=4*pdpi;
		Rectangle pageSize = new Rectangle(w,h);
		com.lowagie.text.Document document = new com.lowagie.text.Document(pageSize);
		
		BufferedImage part=null;

		    PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream("receipt.pdf"));            
            document.open();
            PdfContentByte cb = writer.getDirectContent();
            //PdfTemplate template = cb.createTemplate(w,h);
            document.add(new Paragraph("This is your receipt."));
            document.add(new Paragraph("Serial number: "+sb.getSerial()));
            document.add(new Paragraph("Confirmation code(s) follow"));
            
		    //TreeMap<Integer, TreeMap<Integer,TreeMap<Integer,Cluster>>>markedContests= bgm.getMarkedContests();
            sb.setMailIn(true);
    		
        Vector<Cluster> votes = sb.getVotes(TypeOfVotes.Vote);

        System.out.println(votes.toString());
		int i=0;
		Image pic=null;
		for (Cluster x:votes) {
			i++;
			//part=img.getSubimage((int)Math.floor(x.getX()*dpi),(int)Math.floor(x.getY()*dpi),(int)Math.ceil(x.getWidth()*dpi),(int)Math.ceil(x.getHeight()*dpi));
			part=img.getSubimage((int)Math.floor(x.getXmin()*dpi),(int)Math.floor(x.getYmin()*dpi),(int)Math.ceil((x.getXmax()-x.getXmin())*dpi)+offset,(int)Math.ceil((x.getYmax()-x.getYmin())*dpi)+offset);
			if (debug) {
				ImageIO.write(part, "JPEG", new File("part"+i+".jpg"));
				pic= Image.getInstance("part"+i+".jpg");
			}
			else
			{
				ImageIO.write(part, "JPEG", new File("part.jpg"));
				pic= Image.getInstance("part.jpg");
			}
			document.add(pic);
			
		}
		document.close();
		
		printPdfSilently("D:/Punchscan/distribution/receipt.pdf");
	}
            
            
            
            
            
     /*     
            float xsize=r;
            float x,y;  
            
            TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, Cluster>>> contests=bgm.getMarkedContests();
            
        	TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, TypeOfVotes>>> markedContests=sb.getAllContests();
        	
    		for (Integer contest:markedContests.keySet()) {
    			for (Integer row:markedContests.get(contest).keySet()) {
    				for (Integer candidate:markedContests.get(contest).get(row).keySet()) {
    					Cluster c=contests.get(contest).get(row).get(candidate);
    					//proper vote
    					if (markedContests.get(contest).get(row).get(candidate).equals(TypeOfVotes.Vote)
    							|| markedContests.get(contest).get(row).get(candidate).equals(TypeOfVotes.UnderVote)
    							) {
    		    			template.setColorStroke(Color.GREEN);   			
    		    			template.circle((float)c.getCenterOfMass().getX()*72f, (float)(h-c.getCenterOfMass().getY()*72f), r);
    		    			template.stroke();    						
    					} else {
        					//overvotes
        					if (markedContests.get(contest).get(row).get(candidate).equals(TypeOfVotes.OverVote)
        							||
            					markedContests.get(contest).get(row).get(candidate).equals(TypeOfVotes.NoVote)) {
            	     				x=(float)c.getCenterOfMass().getX()*72f;
            	    				y=h-(float)c.getCenterOfMass().getY()*72f;
            	    				template.setColorStroke(Color.BLACK);
            	    				template.moveTo(x-xsize,y+xsize);
            	    				template.lineTo(x+xsize, y-xsize);
            	    				template.moveTo(x-xsize, y-xsize);
            	    				template.lineTo(x+xsize,y+xsize);
            	        			template.stroke();
            	                    if (sb.getSelectedPage().equals(Prow.ChosenPage.TOP)
            	                    		&& sb.getClass().toString().equals("class software.scanner.ScannedBallot")
            	                    		) {
            	        				template.setColorStroke(Color.WHITE);
            	        				template.setColorFill(Color.WHITE);
            	        				template.circle(x,y,r);
            	        				template.fillStroke();
            	        			}
            					}
    					}
    				}
    			}			
    		}

    		
    		//write the serial number and the votes on a corner
    		String compactRepresentation=sb.getCompactRepresentation();
    		//show string under signature
           	BaseFont symbolFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED);
            int symbolFontSize = 10;
            template.setFontAndSize(symbolFont,symbolFontSize);
            template.setColorStroke(Color.BLACK);
            template.setColorFill(Color.BLACK);
            template.beginText();
    		//print the text on the lower right corner
    		template.showTextAligned(PdfContentByte.ALIGN_LEFT, compactRepresentation.substring(0,compactRepresentation.indexOf(" ")), w-1.1f*72f,0.6f*72f,45);
    		template.showTextAligned(PdfContentByte.ALIGN_LEFT, compactRepresentation.substring(compactRepresentation.indexOf(" ")+1), w-1.3f*72f,0.65f*72f,45);
    		//template.showTextAligned(PdfContentByte.ALIGN_LEFT, s, 0.2f*72f,0.2f*72f,0);
            template.endText();

            try {
	    		String signature=new String(signatureValue.getValue());
	    		
	    		//show signature
	    		BarcodePDF417 codeEAN = new BarcodePDF417();
	    		codeEAN.setText(signature);
	    		Image imageEAN = codeEAN.getImage();
	    		template.addImage(imageEAN, imageEAN.getWidth(), 0f, 0f, imageEAN.getHeight(), 0.5f*72f,0.8f*72f);
            } catch (Exception e) {
    			e.printStackTrace();
    		}
            
            cb.addTemplate(template,0,0);
            //UPSIDEDOWNcb.addTemplate(template,-1,0,0,-1,w,h);
        }
        catch(DocumentException de) {
            System.err.println(de.getMessage());
        }
        catch(IOException ioe) {
            System.err.println(ioe.getMessage());
        }     
        document.close();        
	}	*/
}
