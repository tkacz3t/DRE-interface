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

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import edu.gwu.election.Globals;
import edu.gwu.election.Print;

/**
 * @author John Wittrock
 * 
 */
public class AudioPlayerWav implements AudioPlayer {

	private static final int EXTERNAL_BUFFER_SIZE = 1024; // A variable which
															// allows us to set
															// the size of the
															// buffer that will
															// hold audio data.
	private SourceDataLine line;
	private AudioInputStream audioInputStream;
	AudioFormat audioFormat;
	private boolean isPlaying = false;
	Thread playThread;

	public AudioPlayerWav() {
	}

	/**
	 * @param file
	 *            Automatically tries to open the file given.
	 * @param speedOption
	 */
	public AudioPlayerWav(String file, boolean speedOption) {
		if (speedOption)
			openFile(file);
		else
			openFileNoSpeedOption(file);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.gwu.audio.AudioPlayer#openFile(java.lang.String)
	 */
	public void openFile(String filename) {
		doOpenFile(filename, true);
	}

	private void doOpenFile(String filename, boolean speedOption) {
		if (speedOption && !filename.contains("recording.wav") && !filename.contains("+")) {
			filename = "" + filename.substring(0, filename.length() - 4) + "_"
					+ Integer.toString(Globals.SOUND_SPEED) + ".wav";
		}
		try {
			File file = new File(filename);
			if(!file.exists()){
				int length = filename.length();
				String extension = "mp3";
				File testFile = new File(filename.substring(0, length - 3) + extension);
				if(!testFile.exists()){
					Print.debug("File does not exist in any supported file types: " + testFile.getAbsolutePath());
					throw new IOException();
				}
				else{
					file = testFile;
				}
			}
			audioInputStream = AudioSystem.getAudioInputStream(file);
			audioFormat = audioInputStream.getFormat();
			DataLine.Info info = new DataLine.Info(SourceDataLine.class,
					audioFormat);
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(audioFormat);
			line.start(); // activate the line to pass data to the sound card.
		} catch (UnsupportedAudioFileException e) {
			throw new AudioPlayerException(e);
		} catch (IOException e) {
			throw new AudioPlayerException(e);
		} catch (LineUnavailableException e) {
			throw new AudioPlayerException(e);
		}
	}

	public void openFileNoSpeedOption(String fileName) {
		// TODO Auto-generated method stub
		doOpenFile(fileName, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.gwu.audio.AudioPlayer#startPlaying()
	 */
	public void startPlaying() {
		//Print.debug("Playing wav!");
		playThread = new Thread() {
			public void run() {
				isPlaying = true;
				int nBytesRead = 0;
				byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];
				while (nBytesRead != -1 && isPlaying) {

					try {
						nBytesRead = audioInputStream.read(abData, 0,
								abData.length);
					} catch (IOException e) {
						// throw new AudioPlayerException(
						// "Caught IOException in AudioPlayerWav.startPlaying",
						// e);
					}
					if (nBytesRead >= 0) {
						line.write(abData, 0, nBytesRead);
					}
				}
				if (line != null) {
					line.drain();
				}
				if (line != null) {
					line.close();
				}
				try {
					if (audioInputStream != null)
						audioInputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				audioInputStream = null;
				line = null;
				// have to run the garbage collector to be able to delete the
				// file later. specifically recording.wav
				System.gc();
				isPlaying = false;
			}
		};
		playThread.start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.gwu.audio.AudioPlayer#isPlaying()
	 */
	public boolean isPlaying() {
		return isPlaying;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.gwu.audio.AudioPlayer#stop()
	 */
	public void stop() {
		// line.drain();
		if (isPlaying()) {
			isPlaying = false;
			line.stop();
			// line.drain();
			line.flush();
			line.close();
			try {
				audioInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			audioInputStream = null;
			line = null;
			// have to run the garbage collector to be able to delete the
			// file later. specifically recording.wav
			System.gc();

		}
	}

	// TODO: implement this?
	public void setFramePosition(int frame) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.gwu.audio.AudioPlayer#getDuration()
	 */
	public long getDuration() {
		return (long) Math.ceil(audioInputStream.getFrameLength()
				/ audioFormat.getSampleRate()) * 1000;
	}

	/**
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException {
		AudioPlayerWav test = new AudioPlayerWav();
		// String file =
		// "E:/Eclipse/workspace/eTegrityDRE/SDElection/content/en/extras/FinalConfirmation1.wav";
		String file = "C:/Users/anothem/work/eTegrityDRE/SDElection/content/en/extras/instructions.wav";
		test.openFile(file);
		test.startPlaying();
		// int i = 0;
		System.out.println(test.getDuration());
		// System.out.println(i);
		// while(test.isPlaying()){
		// System.out.println(i++);
		// }
		// Thread.currentThread().sleep(5000);
		/*
		 * test.stop(); test.openFile(file); test.startPlaying();
		 */
	}

	public void releaseResources() {

		if (line != null) {
			line.flush();
			line.close();
		}
		if (audioInputStream != null) {
			try {
				audioInputStream.close();
			} catch (IOException e) {

				e.printStackTrace();
			}
		}
		audioInputStream = null;
		line = null;
		// have to run the garbage collector to be able to delete the
		// file later. specifically recording.wav
		System.gc();

	}

}
