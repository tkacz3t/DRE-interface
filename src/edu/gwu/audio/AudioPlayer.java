/*******************************************************************************
 * Copyright (c) 2011 Alex Florescu, Jan Rubio, John Wittrock, Tyler Kaczmarek.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     John Wittrock - initial API and implementation
 ******************************************************************************/
package edu.gwu.audio;

public interface AudioPlayer {

	/**
	 * Initializes the AudioPlayer on a specific file. Should default to
	 * allowing the use of variable speed (i.e. recording.wav becomes
	 * recording_X.wav by default, where X is Globals.SOUND_SPEED).
	 * 
	 * @param filename
	 *            The file to be opened
	 */
	public abstract void openFile(String filename);

	/**
	 * Initializes the AudioPlayer on a specific file, but without the use of
	 * the speed option, so the filename will be played as-is. Should be used when a file is needed to be played exactly as-is, with no speed adjustments made.
	 * 
	 * @param fileName
	 */
	public abstract void openFileNoSpeedOption(String fileName);

	/**
	 * Actually starts playing the sound file given as filename to openFile().
	 */
	public abstract void startPlaying();

	/**
	 * 
	 * @return true if the sound file is playing, false otherwise.
	 */
	public abstract boolean isPlaying();

	/**
	 * If the sound file is playing, stops it.
	 */
	public abstract void stop();

	/**
	 * Sets the starting frame. May not be implemented in all subclasses.
	 * 
	 * @param frame
	 *            - an integer indicating the frame to start playing at
	 */
	public abstract void setFramePosition(int frame);

	/**
	 * Gets duration of audio file in seconds
	 * 
	 * @return Duration in seconds (at least 1 second)
	 */
	public abstract long getDuration();

	/**
	 * Releases all resources related to this AudioPlayer.
	 */
	public abstract void releaseResources();

}
