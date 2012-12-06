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
//TODO Find out what happens when you stop playing and then start again. 
/*
 * This class uses a library called JLayer. It is free and open source. 
 * It can be found at http://www.javazoom.net/javalayer/javalayer.html
 */

package edu.gwu.audio;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;

import org.tritonus.share.sampled.file.TAudioFileFormat;

import edu.gwu.election.Globals;
import edu.gwu.election.Print;

/**
 * @author John Wittrock
 * 
 */

public class AudioPlayerMPEG implements AudioPlayer {

	String file;
	boolean playing = false;
	AdvancedPlayer player;
	int start = 0;

	public AudioPlayerMPEG(String filename, boolean speedOption) {
		// file = filename;
		if (speedOption && !filename.contains("+")) {
			filename = "" + filename.substring(0, filename.length() - 4) + "_"
					+ Integer.toString(Globals.SOUND_SPEED) + ".mp3";
		}
		file = filename;
		if (speedOption)
			openFile(filename);
		else
			openFileNoSpeedOption(filename);
	}

	/**
	 * @return 0 if there was an error or the file actually has a duration of
	 *         zero, the duration of the file otherwise.
	 * @throws UnsupportedAudioFileException
	 */
	public long getDuration() {
		AudioFileFormat fileFormat;
		try {
			fileFormat = AudioSystem.getAudioFileFormat(new File(file));
			// System.out.println(fileFormat.getClass().getName());
			if (fileFormat instanceof TAudioFileFormat) {
				Map<?, ?> properties = ((TAudioFileFormat) fileFormat)
						.properties();
				String key = "duration";
				// System.out.println("properties: " + properties);
				Long microseconds = (Long) properties.get(key);
				long mili = (microseconds / 1000);
				// int milis = (int) mili;
				// int sec = (milis / 1000) % 60;
				// int min = (milis / 1000) / 60;
				// System.out.println("time = " + min + ":" + sec);
				return mili;
			} else {
				Print.debug("ERROR IN GETDURATION");
			}
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.gwu.audio.AudioPlayer#isPlaying()
	 */
	public boolean isPlaying() {
		return playing;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.gwu.audio.AudioPlayer#openFile(java.lang.String)
	 */
	public void openFile(String filename) {
		file = filename;
		try {
			doOpenFile(filename, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void openFileNoSpeedOption(String filename) {
		file = filename;
		try {
			doOpenFile(filename, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void doOpenFile(String filename, boolean speedOption)
			throws IOException {
		// Print.debug(filename);
		File file = new File(filename);
		
		if (!file.exists()) {
			Print.debug("File does not exist!");
			int length = filename.length();
			String extension = "wav";
			File testFile = new File(filename.substring(0, length - 3)
					+ extension);
			if (!testFile.exists()) {
				Print.debug("File does not exist in any supported file types!" + filename);
				throw (new IOException());
			} else {
				filename = testFile.getAbsolutePath();
			}
		}

		// Print.debug(filename);
		FileInputStream fis;
		try {
			fis = new FileInputStream(filename);
			BufferedInputStream bis = new BufferedInputStream(fis);
			player = new AdvancedPlayer(bis);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new AudioPlayerException(
					"FileNotFoundException in JLayerAudioPlayer.openFile()\n",
					e);
		} catch (JavaLayerException e) {
			e.printStackTrace();
			throw new AudioPlayerException(
					"JavaLayerException in JLayerAudioPlayer.getDuration()\n",
					e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.gwu.audio.AudioPlayer#setFramePosition(int)
	 */
	public void setFramePosition(int frame) {
		start = frame;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.gwu.audio.AudioPlayer#startPlaying()
	 */
	public void startPlaying() {
		// run in new thread to play in background
		if (player == null) {
			openFile(file);
		}
		new Thread() {
			public void run() {
				playing = true;
				try {
					// TODO need to figure out a way to make this
					// work from the set frame position.
					player.play();
				} catch (JavaLayerException e) {
					e.printStackTrace();
					throw new AudioPlayerException(
							"Exception in JLayerAudioPlayer.startPlaying()\n",
							e);
				}
				playing = false;
			}
		}.start();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.gwu.audio.AudioPlayer#stop()
	 */
	public void stop() {
		if (player != null) {
			player.close();
			playing = false;
		}
	}

	/*
	 * Quick demo.
	 */
	public static void main(String[] args) {
		// AudioPlayerAlex ap=new
		// AudioPlayerAlex("../../../../SDElection/content/en/extras/instructions.wav");

		AudioPlayerMPEG ap = new AudioPlayerMPEG(
				"C:/Users/John Wittrock/Desktop/all/cleaned/BallotNotYetCast_1.mp3",
				false);
		Print.debug(Long.toString(ap.getDuration()));
		ap.startPlaying();
//
//		AudioPlayerMPEG ap3 = new AudioPlayerMPEG(
//				"C:/Users/John Wittrock/Desktop/a0-0_1.mp3", false);
//		System.out.println(ap3.getDuration());
//		ap3.startPlaying();
//
//		AudioPlayerMPEG ap4 = new AudioPlayerMPEG(
//				"C:/Users/John Wittrock/Desktop/a1-0_1.mp3", false);
//		System.out.println(ap4.getDuration());
//		ap4.startPlaying();
//
//		AudioPlayerMPEG ap2 = new AudioPlayerMPEG(
//				"E:/Eclipse/workspace/DREInterface/TPTestMP3/content/en/cleaned/a0-0.mp3",
//				false);
//		System.out.println(ap2.getDuration());
//		ap2.startPlaying();
//		// int counter = 0;
//		// while (ap.isPlaying()) {
//		// if (counter % 1000 == 0) {
//		// System.out.println("Playing...");
//		// // ap.stop();
//		// // ap.startPlaying();
//		// // break;
//		// }
//		// counter++;
//		// }
	}

	public void releaseResources() {
		if (player != null) {
			player.close();
			player = null;
		}
	}

}
//