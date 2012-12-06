/*******************************************************************************
 * Copyright (c) 2011 Alex Florescu, Jan Rubio, John Wittrock, Tyler Kaczmarek.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Alex Florescu, Jan Rubio - initial API and implementation
 ******************************************************************************/
package edu.gwu.election;

import java.util.Properties;

/**
 * Class containing all the global constants used by the project
 * @author Alex Florescu
 *
 */
public class Globals {

	/**
	 *  The maximum number of candidates the system will support (based on hardware limitations)
	 */
	static final int MAX_CANDIDATES=6;
	/**
	 * The offset in miliseconds between the sounds being played in a state
	 */
	public static final int OFFSET = 500;
	/**
	 * Debug mode
	 */
	static boolean DEBUG = false;
	/**
	 * Maximum possible number of inputs. Should be 12 or less ( 0-9, *, # ), since these are the possible keys on the numpad. 
	 * * is mapped to 10 
	 */
	public static final int MAX_INPUT=12;
	
	/**
	 * Default number of maximum timeouts that can occur with a state before the program exits 
	 */
	public static final int DEFAULT_MAX_TIMEOUTS=50;
	
	public static Properties PROPERTIES;
	
	public static int SOUND_SPEED = 1;
	
	public static final int MIN_SOUND_SPEED = 0;
	public static final int MAX_SOUND_SPEED = 2;
	
	public static final int SKIP_TO_BALLOT_VERIFICATION = 6;
	public static final int REPEAT_THIS_MESSAGE = 0;
	public static final int TO_CONTINUE = 11;
	public static final int TO_GO_BACK = 10;
	public static final int SKIP_CONTEST = 9;
	
	public static double GAIN = 1;
	
	
	static void updateDebugStatus(){
  		DEBUG = PROPERTIES.getProperty("Debug").equalsIgnoreCase("TRUE");
	}
	
	
}
