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
package edu.gwu.election.fsm;

import edu.gwu.election.Globals;

/**
 * A class for storing transitions between states for various inputs 
 * A TransitionArray needs to be created for each state
 * 
 * @author Alex Florescu
 */

public class TransitionArray {

	private int[] transitionArray;
	
	/**
	 * initializes the TransitionArray to a default size
	 *
	 */
    public TransitionArray () { 
    	transitionArray=new int[Globals.MAX_INPUT];
    }
    
    /**
     * Adds transition from current state to given state with given input
     * @param state the id of the state to transition to
     * @param input the input on which to perform the transition
     */
    public void addTransition(int state, int input) {
    	transitionArray[input]=state;
    }
    
    /**
     * Returns the id of the state that is transitioned to on a given input
     * @param x	The given input
     * @return	The state to transition to
     */
    public int getTransition(int input) {
    	return transitionArray[input];
    }
    
    /**
     * Method for printing out the transition array
     */
    public String toString() {
    	StringBuffer output=new StringBuffer(27);
    	for (int i=0; i<transitionArray.length; i++)
    		output.append(transitionArray[i]+" ");
    	return output.toString();
    }
}
