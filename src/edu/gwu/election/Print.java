/*******************************************************************************
 * Copyright (c) 2011 Alex Florescu, Jan Rubio, John Wittrock, Tyler Kaczmarek.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Alex Florescu - initial API and implementation
 ******************************************************************************/
package edu.gwu.election;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Helper class with various static print methods
 * @author Alex Florescu
 *
 */
public class Print {

	/**
	 * This JTextArea is used for printing the text from files directly in the GUI
	 */
	private static JTextArea textArea=null; 
		
	/**
	 * Used for printing non-fatal errors to the error stream
	 * @param error message to be printed
	 */
	private static void error (String error) {
		System.err.println(error);
	}
	
	/**
	 * Used for printing debug messages (if Globals.DEBUG is true) to the default output stream
	 * @param message
	 */
	public static void debug (String message) {
		
		if (Globals.DEBUG) System.out.println(message);
	}
	
	/**
	 * Used for printing file content to the default output stream and to the given textArea
	 * @param filename
	 */
	
	public static void file (String filename) {
		try {
			String[] path = filename.split("[/]");
			if(path[path.length - 1].equals("newline.txt")){
				textArea.append("\n");
				textArea.setCaretPosition(textArea.getDocument().getLength());

				Print.debug("Newline");
				return;
			}
			Scanner sc = new Scanner (new File(filename));
			while (sc.hasNext()) {
				String s=sc.nextLine();
				if ((s.length() <= 0) && s != null || s.equals("") || s.equals("\n") || s.equals(" ")) {
					textArea.append("\n");
					Print.debug("");
				}
				else{
				textArea.append(s+" ");
				}
				textArea.setCaretPosition(textArea.getDocument().getLength());
				Print.debug(s+" ");
			}
			//Print.refresh();
			
		} catch(FileNotFoundException e) {
			error(e.toString());
		}
	}	
	
	public static void refresh(){
		textArea.invalidate();
		textArea.validate();
		textArea.repaint();
		Print.debug("REFRESHING!");
	}
	
	public static void clearTextArea(){
		if(textArea != null){
			textArea.setText("");
			return;
		}
		else
			Print.error("Print.clearTextArea(): Text area is null! Cannot clear the text area.");
	}
	
	/**
	 * Sets the text area in the GUI where the text will printed
	 * @param jta the JTextArea (must be already initialized)
	 */
	public static void setOutputTextArea(JTextArea jta) {
		textArea=jta; 
	}
	
	/**
	 * Used for printing fatal errors to the error output stream. Program terminates with code -1 on this method
	 * @param error the error to be printed
	 */
//	public static void fatalError(String error) {
//		error(error);
//		System.exit(-1);
//	}

	/**
	 * The panic method is used for fatal errors that break the protocol thus representing a major security issue that must be reported
	 * @param error the error that occured
	 */
	static void panic(String error) {
		error("Panic: "+error);
		
		//TODO Add playing of error message here. 
	}
}
