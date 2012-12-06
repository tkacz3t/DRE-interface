package edu.gwu.election.test;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.TimeoutException;

class StreamGobbler extends Thread {
	InputStream is;
	String type;
	MockVoter voter;

	StreamGobbler(InputStream is, String type, MockVoter v) {
		this.is = is;
		this.type = type;
		this.voter = v;
	}

	public String getType() {
		return type;
	}

	public void run() {
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null) {
				// System.out.println(type + ">" + line);
				// output = output + line;
				System.out.println("Line: " + line);
				voter.addOutput(line);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}

public class MockVoter {

	char[] keyPresses;
	Vector<Vector<String>> files;
	Robot robot;
	String currentOutput;
	int waitTime;

	public MockVoter(char[] presses, Vector<Vector<String>> outputFiles,
			int time) {
		keyPresses = presses;
		currentOutput = "";
		files = outputFiles;
		waitTime = time;
	}

	public synchronized void addOutput(String line) {
		currentOutput = currentOutput + line;
	}

	public static int getKeyCode(char c) {
		switch (c) {
		case '0':
			return KeyEvent.VK_0;
		case '1':
			return KeyEvent.VK_1;
		case '2':
			return KeyEvent.VK_2;
		case '3':
			return KeyEvent.VK_3;
		case '4':
			return KeyEvent.VK_4;
		case '5':
			return KeyEvent.VK_5;
		case '6':
			return KeyEvent.VK_6;
		case '7':
			return KeyEvent.VK_7;
		case '8':
			return KeyEvent.VK_8;
		case '9':
			return KeyEvent.VK_9;
		default:
			return -1;
		}
	}

	public void runTest(String[] command) throws IOException, AWTException,
			InterruptedException, TimeoutException {
		try {
			robot = new Robot();
		} catch (AWTException e1) {
			throw new AWTException("Could not create the Robot!");
		}
		Runtime runtime = Runtime.getRuntime();
		// String[] args = { "java", "DRE" };
		Process DREProc;
		String linesToCheckAgainst = "";
		Calendar cal = Calendar.getInstance();

		// TODO make sure keypresses.length == files.size()

		try {
			DREProc = runtime.exec(command);
			StreamGobbler output = new StreamGobbler(DREProc.getInputStream(),
					"OUTPUT", this);
			output.start();
		} catch (IOException e) {
			throw new IOException("Could not run the DRE", e);
		}

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			throw new InterruptedException(
					"Runtime thread was interrupted before entry of data.");
		}
		for (int i = 0; i < keyPresses.length; i++) {
			char c = keyPresses[i];
			int keyCode = MockVoter.getKeyCode(c);
			if (keyCode != -1 && files != null) {
				// build up string to check against before you press a key.
				Scanner scanner;
				linesToCheckAgainst = "";

				for (int j = 0; j < files.get(i).size(); j++) {
					File file = new File(files.get(i).get(j));
					scanner = new Scanner(file);
					while (scanner.hasNextLine()) {

						// should use a string and simply check if read length <
						// check length
						// also need to use a timeout!
						linesToCheckAgainst = linesToCheckAgainst
								+ scanner.nextLine();
					}
				}
			}
			// Verification goes here.

			robot.keyPress(keyCode); // Press the key!

			if (files != null) {
				long startTime = cal.getTimeInMillis();
				while (true) {
					if (cal.getTimeInMillis() > startTime + 60000) {
						// if we've gone a minute without the right output,
						// we've timed out.
						// throw new TimeoutException
						throw new TimeoutException(
								"Robot running from MockVoter has timed out!");
					} else if (currentOutput.length() >= linesToCheckAgainst
							.length()
							&& !currentOutput.equals(linesToCheckAgainst)) {
						// fail. quit the program.
					} else if (currentOutput.equals(linesToCheckAgainst)) {
						// success!
						// remember to reset everything for the next keypress.
					}
				}
			}
			Thread.sleep(waitTime * 1000);
		}

	}

	public static void main(String[] args) {
		char[] keyPresses = { '1', '1','1','1','1','1','1','1','1'};
		String[] command = {
				"java",
				"-classpath",
				//"E:/Eclipse/workspace/DREInterface/bin;E:/Eclipse/workspace/DREInterface/extJars/xmlSig/dom.jar;E:/Eclipse/workspace/DREInterface/extJars/xmlSig/jaxp-api.jar;E:/Eclipse/workspace/DREInterface/extJars/xmlSig/xalan.jar;E:/Eclipse/workspace/DREInterface/extJars/xmlSig/xercesImpl.jar;E:/Eclipse/workspace/DREInterface/extJars/xmlSig/xmldsig.jar;E:/Eclipse/workspace/DREInterface/extJars/xmlSig/xmlsec.jar;E:/Eclipse/workspace/DREInterface/extJars/ballotStandard-1.0.jar;E:/Eclipse/workspace/DREInterface/extJars/bcmail-jdk15-135.jar;E:/Eclipse/workspace/DREInterface/extJars/bcprov-jdk15-135.jar;E:/Eclipse/workspace/DREInterface/extJars/ChoicePlusPro.jar;E:/Eclipse/workspace/DREInterface/extJars/core.jar;E:/Eclipse/workspace/DREInterface/extJars/iText-2.0.8.jar;E:/Eclipse/workspace/DREInterface/extJars/jai_codec.jar;E:/Eclipse/workspace/DREInterface/extJars/jai_core.jar;E:/Eclipse/workspace/DREInterface/extJars/javase.jar;E:/Eclipse/workspace/DREInterface/extJars/jbcl3.0.jar;E:/Eclipse/workspace/DREInterface/extJars/scantegrity.jar;E:/Eclipse/workspace/DREInterface/extJars/simplecaptcha-20050925.jar;E:/Eclipse/workspace/DREInterface/extJars/swing-layout-1.0.jar;E:/Eclipse/workspace/DREInterface/extJars/jl1.0.1.jar;E:/Eclipse/workspace/DREInterface/extJars/scantegrityAddOn.jar;E:/Eclipse/workspace/DREInterface/extJarsPDFRenderer.jar",
				"bin/;extJars/;extJars/*;extJars/xmlSig/*; ",
		"edu.gwu.etegrity.DRE" };
		MockVoter voter = new MockVoter(keyPresses, null, 10);

		try {
			voter.runTest(command);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (AWTException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
	}

}
