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

/**
 * Interface defining what can be part of a FiniteStateMachine
 * 
 * @author Alex Florescu
 *
 */

public interface Transitionable {
	/**
	 * Returns the state id
	 * @return id of the state
	 */
    public int getId();
    
    /**
     * Action performed on input from user
     * @param input the key pressed (0-9 and 10 for "star")
     */
    public void action (int input);
    
    /**
     * Display the text content and play the audio files
     *
     */
    public void showContent();
    public void showContent(boolean allowMultiples);
    /**
     * Stop playing the current sound file
     *
     */
    public void stopWav(); 
    
    /**
     * Is the current state interruptable
     * @return true if state can be interrupted 
     */
    public boolean isInterruptable();
    
    /**
     * Is the current sound file playing
     * @return true if the sounds is playing
     */
    public boolean isPlaying();
    
    /**
     * Validates a given input
     * @param a the input from the user
     * @return true if the input is valid
     */
    public boolean isValidInput(int a);
    
    public int getValidInput();
}
