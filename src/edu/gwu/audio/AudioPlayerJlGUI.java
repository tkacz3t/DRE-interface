package edu.gwu.audio;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.tritonus.share.sampled.file.TAudioFileFormat;

import javazoom.jlgui.basicplayer.*;

import edu.gwu.election.Globals;
import edu.gwu.election.Print;

public class AudioPlayerJlGUI implements AudioPlayer {

	BasicPlayer player = null;

	File file;
	boolean withSpeed;

	public AudioPlayerJlGUI(String filename, boolean speedOption) {

		Logger.getLogger(BasicPlayer.class.getName()).setLevel(Level.OFF);
		if (player == null) {
			player = new BasicPlayer();
		}

		if (speedOption && !filename.contains("+")) {
			filename = "" + filename.substring(0, filename.length() - 4) + "_"
					+ Integer.toString(Globals.SOUND_SPEED) + ".mp3";
		}

		withSpeed = speedOption;

		Print.debug("Opening file: " + filename);
		file = new File(filename);

		if (!file.exists()) {
			Print.debug("File does not exist!");
			int length = filename.length();
			String extension = "wav";
			File testFile = new File(filename.substring(0, length - 3)
					+ extension);
			
			if (!testFile.exists()) {
				Print.debug("File does not exist in any supported file types!"
						+ filename);

			} else {
				file = testFile;
			}
		}
	}

	private BasicPlayer doOpenFile()
			throws IOException {

		// BasicPlayer tempPlayer = new BasicPlayer();

		try {
			player.open(file);
		} catch (BasicPlayerException e) {

			Print.debug("BasicPlayerException in AudioPlayerJlGUI" + e);
			e.printStackTrace();
		}

		return player;
	}

	public void startPlaying() {
		try {
			doOpenFile();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (player == null) {
			Print.debug("PLAYER WAS NULL IN STARTPLAYING");
		}
		try {
			player.play();
			player.setGain(Globals.GAIN);
		} catch (BasicPlayerException e) {
			e.printStackTrace();
		}

	}

	public boolean isPlaying() {
		return player.getStatus() == BasicPlayer.PLAYING;
	}

	public void stop() {
		try {
			player.stop();
		} catch (BasicPlayerException e) {
			e.printStackTrace();
		}
	}

	public void setFramePosition(int frame) {
		// TODO Implement this?

	}

	public long getDuration() {
		AudioFileFormat fileFormat;
		try {
			fileFormat = AudioSystem.getAudioFileFormat(file);
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

	public void releaseResources() {
		// player.closeStream();
	}

	public void setGain(double d) {
		try {
			player.setGain(d);
		} catch (BasicPlayerException e) {
			e.printStackTrace();
		}
	}

	public float getMaximumGain() {
		return player.getMaximumGain();
	}

	public float getMinimumGain() {
		return player.getMinimumGain();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		long duration;
		AudioPlayerJlGUI ap = new AudioPlayerJlGUI(
				"C:/Users/John Wittrock/workspace/eTegrityDRE/TPTestMP3/content/en/extras/1th_1.mp3",
				false);
		duration = ap.getDuration();
		System.out.println(Long.toString(duration));
		ap.startPlaying();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		AudioPlayerJlGUI ap2 = new AudioPlayerJlGUI(
				"C:/Users/John Wittrock/workspace/eTegrityDRE/TPTestMP3/content/en/extras/1th_1.mp3",
				false);
		System.out.println(Long.toString(ap.getDuration()));
		System.out.println("Min gain: " + ap2.getMinimumGain());
		// ap2.setGain(0);
		ap2.startPlaying();
		ap2.setGain(.25);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		AudioPlayerJlGUI ap3 = new AudioPlayerJlGUI(
				"C:/Users/John Wittrock/workspace/eTegrityDRE/TPTestMP3/content/en/extras/1th_1.mp3",
				false);
		System.out.println(Long.toString(ap.getDuration()));

		System.out.println("Max gain: " + ap3.getMinimumGain());
		ap3.startPlaying();
		ap3.setGain(1);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//
		// AudioPlayerJlGUI ap3 = new AudioPlayerJlGUI(
		// "C:/Users/John Wittrock/Desktop/a0-0_1.mp3", false);
		// System.out.println(ap3.getDuration());
		// ap3.startPlaying();
		//
		// AudioPlayerJlGUI ap4 = new AudioPlayerJlGUI(
		// "C:/Users/John Wittrock/Desktop/a1-0_1.mp3", false);
		// System.out.println(ap4.getDuration());
		// ap4.startPlaying();
		//
		// AudioPlayerJlGUI ap2 = new AudioPlayerJlGUI(
		// "C:/Users/John Wittrock/workspace/eTegrityDRE/TPTestMP3/content/en/cleaned/a0-0.mp3",
		// false);
		// System.out.println(ap2.getDuration());
		// ap2.startPlaying();
	}

	public void openFile(String filename) {
		// TODO Auto-generated method stub
		
	}

	public void openFileNoSpeedOption(String fileName) {
		// TODO Auto-generated method stub
		
	}

}
