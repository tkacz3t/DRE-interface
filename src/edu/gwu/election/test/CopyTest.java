package edu.gwu.election.test;

import edu.gwu.election.Election;

public class CopyTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Election.copyFile("C:/Users/John Wittrock/Desktop/a0-0_1.mp3", "C:/Users/John Wittrock/Desktop/test.mp3");
		Election.copyFile("C:/Users/John Wittrock/Desktop/a0-2_1.mp3", "C:/Users/John Wittrock/Desktop/test2.mp3");
		Election.copyFile("C:/Users/John Wittrock/Desktop/AlreadyHaveRecording_1.mp3", "C:/Users/John Wittrock/Desktop/test3.mp3");
		Election.copyFile("C:/Users/John Wittrock/Desktop/AlreadyHaveRecording.wav", "C:/Users/John Wittrock/Desktop/test4.mp3");

	}

}
