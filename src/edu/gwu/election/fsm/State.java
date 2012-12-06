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

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import edu.gwu.audio.AudioPlayer;
import edu.gwu.audio.AudioPlayerFactory;
import edu.gwu.audio.AudioPlayerJlGUI;
import edu.gwu.election.Globals;
import edu.gwu.election.Print;
import edu.gwu.election.ScantegrityException;

/**
 * The class for each state used in the FiniteStateMachine.
 * 
 * @author Alex Florescu
 * 
 */

public class State implements Transitionable {

	private Timer timer;
	private int id;
	private int validInputs;
	private int counter;

	private int maxTimeouts;
	private boolean isValidInputSet = false;
	private String[] wavs;
	private String[] dir;
	private boolean interruptable;
	private AudioPlayer ap = null;
	private AudioPlayer[] apl;
	private StateAction stateAction;
	private int timeout = 10000;
	private int timesPlayed = 0;

	/**
	 * Constructor for a new State with more than one sound file
	 * 
	 * @param aId
	 *            id of the state (generally automatically given by the finite
	 *            state machine that adds the state)
	 * @param aValidInputs
	 *            array of valid inputs
	 * @param aMaxTimeouts
	 *            maximum number of times the state can timeout
	 * @param aDir
	 *            a 1-element array containing the directory where the content
	 *            files are located; must be an array of strings with only one
	 *            element
	 * @param aWavs
	 *            array of strings representing the filenames of the sound files
	 * @param aInterruptable
	 *            whether or not this state can be interrupted by new input
	 * @param aStateAction
	 *            the action that the state will execute on input
	 */
	public State(int aId, int[] aValidInputs, int aMaxTimeouts, String[] aDir,
			String[] aWavs, boolean aInterruptable, StateAction aStateAction) {
		id = aId;
		maxTimeouts = aMaxTimeouts;
		interruptable = aInterruptable;
		setValidInputs(aValidInputs);
		stateAction = aStateAction;
		// stateAction = null;
		wavs = aWavs;
		dir = aDir;
		apl = new AudioPlayer[aWavs.length];
	}

	/**
	 * Constructor for a new State with only one sound file
	 * 
	 * @param aId
	 *            id of the state (generally automatically given by the finite
	 *            state machine that adds the state)
	 * @param aValidInputs
	 *            array of valid inputs
	 * @param aMaxTimeouts
	 *            maximum number of times the state can timeout
	 * @param aDir
	 *            a 1-element array containing the directory where the content
	 *            files are located; must be an array of strings with only one
	 *            element
	 * @param aWav
	 *            string representing the filename of the sound file
	 * @param aInterruptable
	 *            whether or not this state can be interrupted by new input
	 * @param aStateAction
	 *            the action that the state will execute on input
	 */
	public State(int aId, int[] aValidInputs, int aMaxTimeouts, String[] aDir,
			String aWav, boolean aInterruptable, StateAction aStateAction) {
		id = aId;
		maxTimeouts = aMaxTimeouts;
		interruptable = aInterruptable;
		setValidInputs(aValidInputs);
		stateAction = aStateAction;
		String[] aWavs = new String[1];
		aWavs[0] = aWav;
		wavs = aWavs;
		dir = aDir;
		apl = new AudioPlayer[aWavs.length];
	}

	/**
	 * Set the valid inputs for this state
	 * 
	 * @param aValidInputs
	 *            array of valid inputs
	 */
	public void setValidInputs(int[] aValidInputs) {
		// the valid inputs are stored as a bitmask, here is the conversion
		validInputs = 0;
		for (int i = 0; i < aValidInputs.length; i++)
			validInputs |= 1 << (aValidInputs[i] + 1);
		isValidInputSet = true;
	}

	/**
	 * Adding a new valid input to the list.
	 * 
	 * @param x
	 *            the valid input to be added
	 */
	public void addValidInput(int input) {
		// use (input+1) in storage to allow for also storing zero
		if (!isValidInputSet) {
			isValidInputSet = true;
			validInputs = 0;
		}
		validInputs |= 1 << (input + 1);
	}

	/**
	 * Checks if a given input is valid
	 * 
	 * @param x
	 *            Input to be checked
	 * @return boolean True if valid input
	 */
	public boolean isValidInput(int x) {
		if (isValidInputSet)
			return (validInputs & (1 << (x + 1))) != 0;
		else {
			System.err.println("Valid inputs have not been specified!");
			return false;
		}
	}

	/**
	 * Gets the valid input bitmask
	 * 
	 * @return
	 */
	public int getValidInput() {
		return validInputs;
	}

	public int getId() {
		return id;
	}

	/**
	 * Wrapper for starting to play the current (selected) sound file
	 */
	private void playWav() {
		if (ap != null) {
			ap.setFramePosition(0);
			ap.startPlaying();
			Print.debug("State #" + id + ": Playing wav");
		}
	}

	/**
	 * Sets the volume gain on the current AudioPlayer if it is an instance of
	 * AudioPlayerJlGUI
	 * 
	 * @param gain
	 */
	public void setGain(double gain) {
		if (ap instanceof AudioPlayerJlGUI) {
			((AudioPlayerJlGUI) ap).setGain(gain);
		}
	}

	/**
	 * Plays all sound files for this state and also prints all text files (in
	 * sync). Finally sets a timer at the end of playtime. If it times out, the
	 * sounds will be played again.
	 */
	public void showContent() {
		showContent(true);
	}

	public void showContent(boolean allowRepeats) {

		if (allowRepeats)
			if (timesPlayed > maxTimeouts) {
				// TODO handle gracefully
				System.out.println("Too many timeouts");
				System.out.println("Goodbye");
				System.exit(0);
				return;
			}

		for (int temp = 0; temp < wavs.length; temp++) {
			System.out.println(temp + ": " + wavs[temp] + " / " + wavs.length);
		}

		// if there is content to show
		if (wavs[0] != null && !wavs[0].isEmpty()) {
			timer = new Timer();

			// for i=0, run before loop
			String filename = dir[0] + wavs[0];

			if (!filename.contains("recording.wav")) {
				String extension = filename.substring(filename.length() - 4,
						filename.length());
				String tempFileName = filename.substring(0,
						filename.length() - 4);
				String tempFilePath = tempFileName + "_" + Globals.SOUND_SPEED
						+ extension;

				File file = new File(tempFilePath);
				Print.debug("tempFilePath: " + tempFilePath);
				if (!(new File(filename).exists()) && !file.exists()) {
					Print.debug("State.java: file does not exist!" + filename);
					String newExtension = ".wav";
					if (extension.equals(".wav")) {
						newExtension = ".mp3";
					}
					int length = filename.length();
					String tempFilePathNoSpeed = tempFileName + newExtension;
					File testFileNoSpeed = new File(tempFilePathNoSpeed);
					if (!testFileNoSpeed.exists()) {

						File testFile = new File(tempFileName.substring(0,
								length - 4)
								+ "_"
								+ Globals.SOUND_SPEED
								+ newExtension);
						if (!testFile.exists()) {
							Print.debug("File does not exist in any supported file types! "
									+ testFile.getAbsolutePath());
							throw (new ScantegrityException(
									"Sound file does not exist in any supported file types!"
											+ filename));
						} else {

							filename = tempFileName + newExtension;
						}
					} else {
						Print.debug("Changing file extension!");
						filename = tempFilePathNoSpeed;
					}
				}
			}

			apl[0] = AudioPlayerFactory.getAudioPlayer(filename, true);
			// apl[0].openFile(dir[0]+wavs[0]);
			ap = apl[0];
			playWav();

			if (!wavs[0].equals("writeIns/recording.wav")) {
				Print.file(dir[0] + wavs[0].substring(0, wavs[0].length() - 3)
						+ "txt");
			}

			// duration of "last" started wav - the one that is still currently
			// playing
			long duration = ap.getDuration();
			Print.debug("first duration: " + duration / 1000);

			// we need this GLOBAL variable beacuse the "TimerTask" can't access
			// the local i variable in the loop
			counter = 1;

			// this loop plays all the sounds
			for (int i = 1; i < wavs.length; i++) {
				// if (!wavs[i].equals("extras/newline.wav")) {
				// initialize a new AudioPlayer for the current sound
				filename = dir[0] + wavs[i]; // TODO + "_" +
												// Integer.toString(Globals.SOUND_SPEED);

				if (!filename.contains("recording.wav")) {
					String extension = filename.substring(
							filename.length() - 4, filename.length());
					String tempFileName = filename.substring(0,
							filename.length() - 4);
					String tempFilePath = tempFileName + "_"
							+ Globals.SOUND_SPEED + extension;

					File file = new File(tempFilePath);
					// Print.debug("tempFilePath: " + tempFilePath);
					if (!(new File(filename).exists()) && !file.exists()) {
						Print.debug("State.java: file does not exist!"
								+ filename);

						String newExtension = ".wav";
						if (extension.equals(".wav")) {
							newExtension = ".mp3";
						}
						int length = filename.length();
						String tempFilePathNoSpeed = tempFileName
								+ newExtension;
						File testFileNoSpeed = new File(tempFilePathNoSpeed);
						// Print.debug("nospeed: " + tempFilePathNoSpeed);
						if (!testFileNoSpeed.exists()) {

							File testFile = new File(tempFileName.substring(0,
									length - 4)
									+ "_"
									+ Globals.SOUND_SPEED
									+ newExtension);
							if (!testFile.exists()) {
								Print.debug("File does not exist in any supported file types! "
										+ testFile.getAbsolutePath());
								throw (new ScantegrityException(
										"Sound file does not exist in any supported file types!"
												+ filename));
							} else {

								filename = tempFileName + newExtension;
							}
						} else {
							Print.debug("Changing file extension to "
									+ tempFilePathNoSpeed);
							filename = tempFilePathNoSpeed;
						}
					}
				}
				Print.debug("Getting audio player # " + i);
				apl[i] = AudioPlayerFactory.getAudioPlayer(filename, true);
				Print.debug("Got audio player # " + i);

				/*
				 * This is for the answer options, which have no speed option by
				 * default. All sorts of bugs would be introduced by giving them
				 * speed options.
				 */

				// schedule this sound to be played after the previous one
				// is
				// finished
				timer.schedule(new TimerTask() {
					public void run() {
						// Print.debug("RUNNING TASK");
						ap = null;
						System.gc();
						ap = apl[counter];
						counter++;
						try {
							playWav();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}, duration + Globals.OFFSET);
				duration += apl[i].getDuration();
				// }
				// print text corresponding to this sound file
				if (!wavs[i].equals("writeIns/recording.wav")) {
					Print.file(dir[0]
							+ wavs[i].substring(0, wavs[i].length() - 3)
							+ "txt");
				}
				// String[] splitFileName = wavs[i].split(".");
				// String saveFile =
				// fileWithoutExtension[fileWithoutExtension.length - 1];

			}
			Print.debug("SHOWED ALL CONTENT");
			// if timeout occurs and repeats are allowed, re-execute this method
			if (allowRepeats) {
				timer.schedule(new TimerTask() {
					public void run() {
						Print.clearTextArea();
						showContent();
					}
				}, timeout + duration);
				timesPlayed++;
			}
			// otherwise, stop the state after a timeout occurs
			else {
				timer.schedule(new TimerTask() {
					public void run() {
						// ap.stop();
					}
				}, duration);
			}
		}
	}

	/**
	 * Executes the stored action for a certain input.
	 * 
	 * @param input
	 *            Input received from FSM
	 */
	public void action(int input) {
		Print.debug("Entering action");
		timesPlayed = 0;
		timer.cancel();
		if (isValidInput(input))
			Print.debug("State #" + id + ": Doing action " + input);
		else
			System.out.println("Invalid input");
		// #4 debugging statement here (adding a new if statement to allow null
		// stateactions)
		if (stateAction != null)
			stateAction.doAction(input);
		Print.debug("Exiting action");
	}

	/**
	 * Wrapper to stop playing the sound.
	 */
	public void stopWav() {
		if (ap != null) {
			ap.stop();
			ap = null;
		}
		if (timer != null)
			timer.cancel();

		for (AudioPlayer ap : apl) {
			ap.releaseResources();
		}
		Print.debug("State #" + id + ": Stoppping wav");
	}

	/**
	 * Wrapper to return whether the State is currenty playing any sounds
	 */
	public boolean isPlaying() {
		return (!(ap == null) && (counter < wavs.length || ap.isPlaying()));
	}

	public boolean isInterruptable() {
		return interruptable;
	}
}
