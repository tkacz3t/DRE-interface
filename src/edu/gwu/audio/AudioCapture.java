/*
 * This is based off of the tutorial found here: 
 * http://www.developer.com/java/other/article.php/2105421/Java-Sound-Capturing-Microphone-Data-into-an-Audio-File.htm
 * 
 * 
 * NOTE: To determine which sound mixer is your microphone, run listMixers() in the main method. 
 * Then change the array value under the comment "Select one of the available mixers" (line 52 as of this writing) to the appropriate mixer.
 * This is the reason this code is hardware-dependent.
 */

package edu.gwu.audio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

import edu.gwu.election.Election;

public class AudioCapture {

	static Properties properties = null;
	private static int mixerIndex = 2; //defaults to 2. 
 
	public static void listMixers() {
		Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
		System.out.println("Available mixers:");
		for (int cnt = 0; cnt < mixerInfo.length; cnt++) {
			System.out.println(mixerInfo[cnt].getName());
		}// end for loop
	}

	/**
	 * @param seconds
	 *            The number of seconds of audio to record from the microphone.
	 * @param file
	 *            The filename to give. Must be a .wav file.
	 * @throws LineUnavailableException
	 * @throws IOException
	 */
	public static void captureAndSaveToFile(int seconds, String file)
			throws LineUnavailableException, IOException {
		if (properties == null) {
			properties = new Properties();
			try {
				FileInputStream in = new FileInputStream(
						"DreProperties.properties");
				properties.load(in);
				mixerIndex = Integer.parseInt(properties
						.getProperty("MicMixerIndex"));
			} catch (IOException e) {
				throw new IOException("Failed to retrieve properties ", e);
			}
		}
		RecordThread recordThread = new RecordThread(seconds, file, mixerIndex);

		System.out.println("Start Recording Now!");

		recordThread.start();

	}

	public static void main(String[] args) throws LineUnavailableException,
			IOException {
		listMixers();
		//AudioCapture.captureAndSaveToFile(2,
		//		"C:/Users/John Wittrock/Desktop/test.wav");
	}
}

class RecordThread extends Thread {
	private String fileName;
	private int seconds;
	private int mixerIndex;
	
	public static void copyFile(File sourceFile, File destFile) throws IOException {
		 if(!destFile.exists()) {
		  destFile.createNewFile();
		 }

		 FileChannel source = null;
		 FileChannel destination = null;
		 try {
		  source = new FileInputStream(sourceFile).getChannel();
		  destination = new FileOutputStream(destFile).getChannel();
		  destination.transferFrom(source, 0, source.size());
		 }
		 finally {
		  if(source != null) {
		   source.close();
		  }
		  if(destination != null) {
		   destination.close();
		  }
		}
	}
	
	public RecordThread(int time, String file, int mixerIndex) {
		if (file != null) {
			this.fileName = file;
		}
		this.seconds = time;
		this.mixerIndex = mixerIndex;
	}

	public void run() {
		if (fileName == null || fileName.equals("") || fileName.length() < 4) {
			return;
		}
		// check that the desired output name is actually a a wav file
		if (!fileName.substring(fileName.length() - 3, fileName.length())
				.equals("wav")) {
			fileName = fileName.substring(0, fileName.length() - 3) + "wav";
		}
		// setup for capture.
		File audioFile = new File(fileName);
		AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
		AudioFormat audioFormat = new AudioFormat(8000.0f, 16, 1, true, true);
		DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class,
				audioFormat);

		// Select one of the available mixers.
		// TODO You're going to have to list the mixers and then see which one
		// is your microphone.
		Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
		Mixer mixer = AudioSystem.getMixer(mixerInfo[this.mixerIndex]);

		// Get a TargetDataLine on the selected
		// mixer.
		TargetDataLine targetDataLine;
		try {
			targetDataLine = (TargetDataLine) mixer.getLine(dataLineInfo);
			targetDataLine.open(audioFormat);
			targetDataLine.start();
			CancelTask cancelTask = new CancelTask(targetDataLine);
			new Timer().schedule(cancelTask, seconds * 1000);
			
			File tempFile = File.createTempFile("wav", "tmp");
			AudioInputStream ais = new AudioInputStream(targetDataLine);
			
			AudioSystem.write(ais, fileType,
					tempFile);
			
			ais.close();
			Election.copyFile(tempFile, audioFile);
			tempFile.delete();
			return;
		} catch (LineUnavailableException e) {
			throw new AudioCaptureException("The line cannot be gotten.", e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new AudioCaptureException("IOException in AudioCapture", e);
		}

	}
}

class CancelTask extends TimerTask {
	TargetDataLine tdl;

	public CancelTask(TargetDataLine dataLine) {
		tdl = dataLine;
	}

	public void run() {
		System.out.println("Canceled recording!");
		tdl.stop();
		tdl.close();
	}
}
