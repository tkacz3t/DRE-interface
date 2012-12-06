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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

import edu.gwu.election.Globals;
import edu.gwu.election.Print;

/**
 * The finite state machine that holds all the states and transitions It
 * contains wrappers for accessing and working with the states inside.
 * 
 * @author Alex Florescu
 * 
 */
public class FiniteStateMachine {

	private Vector<Transitionable> states;
	private Transitionable startState = null, currentState = null,
			stopState = null;
	private boolean isRunning = false;
	private int id = 0;
	private Vector<TransitionArray> transitions = null;
	private int maxTimeouts = Globals.DEFAULT_MAX_TIMEOUTS; // default maximum
															// timeouts to 3
															// unless otherwise
															// changed
	private InputStream inputStream = System.in; // default
	private InputStreamReader inputStreamReader = null;
	private boolean inputTaken = false;
	private boolean actionBeingPerformed = false;

	/**
	 * Initializez a new finite state machine
	 * 
	 */
	public FiniteStateMachine() {
		states = new Vector<Transitionable>();
		transitions = new Vector<TransitionArray>();
	}

	/**
	 * Adds (and constructs) a new state to the finite state machine with more
	 * than one sound file
	 * 
	 * @param validInputs
	 *            array of valid inputs
	 * @param dir
	 *            a 1-element array containing the directory where the content
	 *            files are located; must be an array of strings with only one
	 *            element
	 * @param wavs
	 *            array of strings representing the filenames of the sound files
	 * @param interruptable
	 *            whether or not this state can be interrupted by new input
	 * @param stateAction
	 *            the action that the state will execute on input
	 */
	public void addState(int[] validInputs, String[] dir, String[] wavs,
			boolean interruptable, StateAction stateAction) {
		states.add(new State(id++, validInputs, maxTimeouts, dir, wavs,
				interruptable, stateAction));
		transitions.add(new TransitionArray());
	}

	/**
	 * Adds (and constructs) a new state to the finite state machine with only
	 * one sound file
	 * 
	 * @param validInputs
	 *            array of valid inputs
	 * @param dir
	 *            a 1-element array containing the directory where the content
	 *            files are located; must be an array of strings with only one
	 *            element
	 * @param wavs
	 *            string representing the filename of the sound file
	 * @param interruptable
	 *            whether or not this state can be interrupted by new input
	 * @param stateAction
	 *            the action that the state will execute on input
	 */
	public void addState(int[] validInputs, String[] dir, String wav,
			boolean interruptable, StateAction sa) {
		states.add(new State(id++, validInputs, maxTimeouts, dir, wav,
				interruptable, sa));
		transitions.add(new TransitionArray());
	}

	/**
	 * Adds (and constructs) a new state to the finite state machine with only
	 * one sound file, this is for states without actions, assumes the
	 * StateAction is null
	 * 
	 * @param validInputs
	 *            array of valid inputs
	 * @param dir
	 *            a 1-element array containing the directory where the content
	 *            files are located; must be an array of strings with only one
	 *            element
	 * @param wavs
	 *            string representing the filename of the sound file
	 * @param interruptable
	 *            whether or not this state can be interrupted by new input
	 */
	public void addState(int[] validInputs, String[] dir, String wav,
			boolean interruptable) {
		states.add(new State(id++, validInputs, maxTimeouts, dir, wav,
				interruptable, null));
		transitions.add(new TransitionArray());
	}

	/**
	 * Adds (and constructs) a new state to the finite state machine with more
	 * than one sound file, this is for states without actions, assumes the
	 * StateAction is null
	 * 
	 * @param validInputs
	 *            array of valid inputs
	 * @param dir
	 *            a 1-element array containing the directory where the content
	 *            files are located; must be an array of strings with only one
	 *            element
	 * @param wavs
	 *            array of strings representing the filenames of the sound files
	 * @param interruptable
	 *            whether or not this state can be interrupted by new input
	 * @param stateAction
	 *            the action that the state will execute on input
	 */
	public void addState(int[] validInputs, String[] dir, String[] wavs,
			boolean interruptable) {
		states.add(new State(id++, validInputs, maxTimeouts, dir, wavs,
				interruptable, null));
		transitions.add(new TransitionArray());
	}

	/**
	 * Returns state with a given id
	 * 
	 * @param stateId
	 *            the id of the state
	 * @return the state object
	 */
	public Transitionable getState(int stateId) {
		return states.get(stateId);
	}

	public int size() {
		return states.size();
	}

	/**
	 * Gets the valid input of the current state
	 * 
	 * @return The bitmask representing the valid input for the current state;
	 *         if not current state, then it returns 0 (no valid inputs)
	 */
	public int getValidInput() {
		if (currentState != null)
			return currentState.getValidInput();
		else
			return 0;
	}

	/**
	 * Adds transition from previousState to currentState with given input
	 * 
	 * @param previousStateId
	 *            id of the previous state
	 * @param currentStateId
	 *            id of the currentState
	 * @param input
	 *            the input that performs the transition
	 */
	public void addTransition(int previousStateId, int currentStateId, int input) {
		Print.debug("Adding transition from " + previousStateId + " to " + currentStateId + " with input " + input);
		transitions.get(previousStateId).addTransition(currentStateId, input);
	}

	/**
	 * Returns the transition for a given state and input
	 * 
	 * @param stateId
	 *            the state id
	 * @param input
	 *            the input fed to the state
	 * @return the state id that that the given state + input would transition
	 *         to
	 */
	public int getTransition(int stateId, int input) {
		return transitions.get(stateId).getTransition(input);
	}

	/**
	 * Set the number of maximum timeouts allowed for this FSM
	 * 
	 * @param maxTimeouts
	 *            number of timeouts
	 */
	public void setMaxTimeouts(int maxTimeouts) {
		if (maxTimeouts < 0) {
			throw new IllegalArgumentException("Invalid number for timeouts");
		}
		this.maxTimeouts = maxTimeouts;
	}

	/**
	 * Set the beginning state
	 * 
	 * @param stateId
	 *            id of the first state
	 */
	public void setStartState(int stateId) {
		startState = states.get(stateId);
		currentState = startState;
	}

	/**
	 * Start the finite state machine
	 * 
	 */
	public void start() {
		if (startState == null) {
			throw new NullPointerException("Starting state not defined!");
		}
		if (stopState == null) {
			throw new NullPointerException("Stop state is not defined!");
		}
		if (inputStream == null) {
			throw new NullPointerException("Input stream is not set!");
		}
		isRunning = true;
		currentState = startState;
		currentState.showContent();

		int i;
		inputStreamReader = new InputStreamReader(inputStream);
		// loop waits for input which it then feeds to the FSM using giveinput()
		while (isRunning || currentState.isPlaying())
			try {
				if (inputStreamReader.ready()) {
					char c = (char) inputStreamReader.read();
					if (c == '*')
						i = 10;
					else if (Character.isDigit(c))
						i = (int) Character.getNumericValue(c);
					else
						i = 0;
					// TODO if ran on linux only one such character should be
					// needed - i think
					inputStreamReader.read(); // the enter key
					inputStreamReader.read(); // the enter key
					giveInput(i);
				}
			} catch (IOException e) {
				throw new RuntimeException("Problems with the input stream", e);
			}
	}

	/**
	 * Set the final state
	 * 
	 * @param id
	 *            id of the final state
	 */
	public void setStopState(int id) {
		// stopState = states.get(0);
		stopState = states.get(id);
	}

	/**
	 * Stop the finite state machine
	 * 
	 */
	public void stop() {
		isRunning = false;
		if (currentState != null)
			currentState.stopWav();
	}

	/**
	 * Returns whether the FiniteStateMachine is currently running (it has been
	 * previously started and it has not finished yet)
	 * 
	 * @return
	 */
	public boolean isRunning() {
		return isRunning;
	}
	
	/**
	 * Sets the gain of the current state
	 */
	public void setGain(double gain){
		if(currentState != null){
			((State)currentState).setGain(gain);
		}
	}
	

	/**
	 * Sets the input stream where the FSM will look for input (default is
	 * System.in)
	 * 
	 * @param stream
	 *            the InputStream
	 */
	public void setInputStream(InputStream stream) {
		inputStream = stream;
	}

	/**
	 * Feeds the input to the current state and performs the action and
	 * transition on that input
	 * 
	 * @param input
	 */
	public synchronized void giveInput(int input) {
		Print.debug("Giving input " + input);
		
		if (!isRunning) {
			throw new NullPointerException(
					"FSM not initialized! Call start() first!");
		}
		Print.debug("Input is valid: " + currentState.isValidInput(input));
		if(inputTaken){
			Print.debug("Not giving input to FSM, since inputTaken is true!");
		}
		if (currentState.isValidInput(input) && actionBeingPerformed == false && !inputTaken) {
			inputTaken = true;
			// if state is interruptable, stop the wav // if state is
			// interruptable, stop the wav
			if (currentState.isInterruptable()) {
				currentState.stopWav();
				try {
					Thread.sleep(Globals.OFFSET);
				} catch (InterruptedException e) {
					throw new RuntimeException("Thread can't sleep", e);
				}
			} else // otherwise let wav finish then ask for input again
			if (currentState.isPlaying()) {
				// ignore all inputs while waiting
				Print.debug("Wait for the message to be played.");
				while (currentState.isPlaying()) {
					try {
						while (inputStreamReader.ready())
							inputStreamReader.read();
					} catch (IOException e) {
						throw new RuntimeException(
								"Problems with the input stream", e);
					}
					return;
				}
			}
			Print.clearTextArea();
			// execute current's state action
			actionBeingPerformed = true;
			currentState.action(input);
			actionBeingPerformed = false;
			// transition to the next state
			currentState = states
					.get(getTransition(currentState.getId(), input));
			// check to see if the state is the stopstate
			if (currentState == stopState) {
				Print.debug("Stopstate reached");
				currentState.showContent(false);
				// wait until the stopstate finishes playing
				// ignore all inputs while waiting
				Print.debug("Wait for the message to be played.");
				while (currentState.isPlaying()) {
					try {
						while (inputStreamReader.ready())
							inputStreamReader.read();
					} catch (IOException e) {
						throw new RuntimeException(
								"Problems with the input stream", e);
					}
					isRunning = false;
					return;
				}
				stop();
			}
			// if it's not the stopstate, show the next state's content
			else
				inputTaken = false;
				currentState.showContent();
		} else
			throw new RuntimeException("FSM: Invalid input.");
	}

	/**
	 * Print the entire transition array
	 * 
	 * @return string representation of the transition array
	 */
	public String printTransitionsToString() {
		StringBuffer a = new StringBuffer();
		int x = 0;
		for (TransitionArray i : transitions) {
			a.append(x++ + " : ");
			a.append(i.toString());
			a.append("\n");
		}
		return a.toString();
	}
}
