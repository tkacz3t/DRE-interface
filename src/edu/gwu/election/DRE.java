/*******************************************************************************
 * Copyright (c) 2011 Alex Florescu, Jan Rubio, John Wittrock, Tyler Kaczmarek.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Alex Florescu, Jan Rubio - initial API and implementation
 *     Tyler Kaczmarek - PROPERTIES
 ******************************************************************************/
package edu.gwu.election;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.crypto.spec.SecretKeySpec;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import org.gwu.voting.standardFormat.electionSpecification.ElectionSpecification;
import org.gwu.voting.standardFormat.electionSpecification.exceptions.ESException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.scantegrity.common.Util;
import edu.gwu.election.authoring.BallotRow;
import edu.gwu.election.authoring.PrintableBallotMarker;
import edu.gwu.election.fsm.FiniteStateMachine;
import edu.gwu.election.fsm.State;
import edu.gwu.election.fsm.StateAction;

/**
 * Top-most class, runs during the day of the election. Creates "eTegrity"
 * objects for each voter.
 * 
 * @author Alex Florescu
 * 
 */
public class DRE extends JFrame implements ActionListener {
	KeyEventDispatcher dispatcher;
	boolean haveQuit = false;
	int fontsize = 20;

	/**
	 * This internal class allows global Keyboard shortcuts. With a KeyListener,
	 * only the individual component that has focus receives KeyEvents.
	 */
	private class MyDispatcher implements KeyEventDispatcher {
		// the mappings for keys 0-9, and star, respectively.
		private int[] keyMap = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
		private DRE dre;

		// reversed keyMap
		// private int[] keyMap = { 0, 7, 8, 9, 4, 5, 6, 1, 2, 3, 10 };

		public MyDispatcher(DRE dreParam) {
			super();
			dre = dreParam;
		}

		private boolean keyMapSet = false;

		public void setKeyMap() {
			if (properties.getProperty("KeyMapping").equals("Reversed")) {
				// keyMap[0] = { 0, 7, 8, 9, 4, 5, 6, 1, 2, 3, 10 };
				keyMap[0] = 0;
				keyMap[1] = 7;
				keyMap[2] = 8;
				keyMap[3] = 9;
				keyMap[4] = 4;
				keyMap[5] = 5;
				keyMap[6] = 6;
				keyMap[7] = 1;
				keyMap[8] = 2;
				keyMap[9] = 3;
				keyMap[10] = 10;
				keyMap[11] = 11;
			}
			if (properties.getProperty("KeyMapping").equals("Bonkers")) {
				// keyMap[0] = { 0, 7, 8, 9, 4, 5, 6, 1, 2, 3, 10 };
				keyMap[0] = 0;
				keyMap[1] = 1;
				keyMap[2] = 2;
				keyMap[3] = 3;
				keyMap[4] = 4;
				keyMap[5] = 5;
				keyMap[6] = 6;
				keyMap[7] = 7;
				keyMap[8] = 8;
				keyMap[9] = 9;
				keyMap[10] = 10;
				keyMap[11] = 11;
			}
			keyMapSet = true;
		}

		public boolean dispatchKeyEvent(final KeyEvent e) {
			if (!this.keyMapSet) {
				this.setKeyMap();
			}

			if (e.getID() == KeyEvent.KEY_PRESSED) {
				SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

					@Override
					protected String doInBackground() throws Exception {
						try {
							String key = KeyEvent.getKeyText(e.getKeyCode());
							int keyNum = -1;

							Print.debug(key);
							if (properties.getProperty("KeyMapping").equals(
									"Bonkers")) {
								// Still need to figure out where the
								// tempo-adjustment keys are going for the
								// bonkers layout.
								if (key.equals("NumPad-3")) {
									keyNum = 0;
								} else if (key.equals("NumPad /")) {
									keyNum = 1;
								} else if (key.equals("NumPad *")) {
									keyNum = 2;
								} else if (key.equals("Backspace")) {
									keyNum = 3;
								} else if (key.equals("NumPad-8")) {
									keyNum = 4;
								} else if (key.equals("NumPad-9")) {
									keyNum = 5;
								} else if (key.equals("NumPad -")) {
									keyNum = 6;
								} else if (key.equals("NumPad-5")) {
									keyNum = 7;
								} else if (key.equals("NumPad-6")) {
									keyNum = 8;
								} else if (key.equals("NumPad +")) {
									keyNum = 9;
								} else if (key.equals("NumPad-2")) {
									keyNum = 10;
								} else if (key.equals("Enter")) {
									keyNum = 11;
								}

							}

							else {
								if (key.equals("NumPad /")) {
									dre.gainUp();
									return null;
								} else if (key.equals("NumPad .")) {
									dre.gainDown();
									return null;
								}

								if (key.equals("NumPad +")) {
									// try to speed up
									DRE.speedUpSound();
									keyNum = Globals.REPEAT_THIS_MESSAGE;
								} else if (key.equals("NumPad -")) {
									// try to slow down.
									DRE.slowDownsound();
									keyNum = Globals.REPEAT_THIS_MESSAGE;
								}

							}
							if (key.equals("Enter")) {
								Print.debug("Caught enter, sending 11!");
								keyNum = 11;
							}

							if (keyNum == -1) {
								// if we still haven't found a value for the
								// key, do it the standard way.
								if (key.equals("NumPad *") || key.equals("*")) {
									keyNum = 10;
								} else if (key.length() > 7
										&& keyNum == -1
										&& key.substring(0, 7)
												.equals("NumPad-")) {
									keyNum = Integer.parseInt(key.substring(7,
											8));
								}
							}

							// We do the translation from key entered to key
							// actually
							// interpreted as input below.
							if (keyNum != -1) {
								keyNum = keyMap[keyNum];
							}

							if (keyNum == -1) {
								Print.debug("Invalid Input!");
								return null;
							}

							if (!keyPressVector.contains(keyNum)) {
								Print.debug("Giving input to FSM: " + keyNum);
								fsm.giveInput(keyNum);
								updateEnabledButtons(fsm.getValidInput());
							}
							return null;

						} catch (NumberFormatException e1) {
							// The key pressed was not a number. So we do
							// nothing.
							Print.debug("Key pressed in Etegrity. Not valid input!");
						}
						return null;
					}

				};
				worker.execute();
			}

			return false;
		}
	}

	public void gainUp() {
		double newGain = Math.min(Globals.GAIN + .1, 1.0);
		Globals.GAIN = newGain;
		fsm.setGain(newGain);
		Print.debug("New gain: " + newGain);
	}

	public void gainDown() {
		double newGain = Math.max(Globals.GAIN - .1, 0.0);
		Globals.GAIN = newGain;
		fsm.setGain(newGain);
		Print.debug("New gain: " + newGain);
	}

	public static void speedUpSound() {
		 int curSpeed = Globals.SOUND_SPEED;
		 if (curSpeed < Globals.MAX_SOUND_SPEED)
		 Globals.SOUND_SPEED = curSpeed + 1;
	}

	public static void slowDownsound() {
		 int curSpeed = Globals.SOUND_SPEED;
		 if (curSpeed > Globals.MIN_SOUND_SPEED)
		 Globals.SOUND_SPEED = curSpeed - 1;
	}

	private static Vector<Integer> keyPressVector = new Vector<Integer>();

	public static void addKeyPress(int key) {
		keyPressVector.add(key);
	}

	public static void clearKeyPresses() {
		keyPressVector.clear();
	}

	public static void popKeyPress() {
		if (keyPressVector.size() > 0)
			keyPressVector.remove(keyPressVector.size() - 1);
	}

	private BallotRow currBallot = null;
	int ballotSerial;
	int pid;

	private int[][] votes = null;

	private TreeMap<Integer, TreeMap<Integer, String>> confirmationCodes;
	private TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, String>>> allCodes = null;
	private TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, String>>> allCommitments = null;
	private TreeMap<Integer, BallotRow> ballotRows = null;

	Set<Integer> keys;
	Iterator<Integer> keyIterator = null;

	private static byte[] MK1 = "G7S-)bj^l;q1800]".getBytes();
	private static byte[] MK2 = "K*dst>p9H6c38?[!".getBytes();
	// public static byte[] C = Base64.decode("RGVtbyBCZW4gV2FyZCA0IA==");
	private static byte[] C = "Demo Ben Ward 4 ".getBytes();
	private static int NoBallots = 100;

	private SecretKeySpec mk1 = null;
	private SecretKeySpec mk2 = null;
	private int[] serialNumbers = null;

	private FiniteStateMachine fsm = null;
	private String contentPath = null;
	private String[] electionDir = new String[1];

	private String path = null;
	private JPanel numPad;
	private JButton startButton;
	private JButton quitButton;
	private JButton[] buttons;
	private JScrollPane scroll;
	private JTextArea textArea;
	private Properties properties;

	/**
	 * initializes everything with a given path
	 * 
	 * @param path
	 *            path to the location of the election files
	 */
	public DRE(String path) {
		this.mk1 = new SecretKeySpec(MK1, "AES");
		this.mk2 = new SecretKeySpec(MK2, "AES");
		this.path = path;

		// Begin constructing the interface
		// setSize(300, 300);

		quitButton = new JButton("Quit");
		quitButton.setBackground(Color.RED);
		quitButton.addActionListener(this);
		quitButton.addKeyListener(new KeyListener() {

			public void keyPressed(KeyEvent e) {
				// startElection();
			}

			public void keyReleased(KeyEvent e) {
				// startElection();
			}

			public void keyTyped(KeyEvent e) {
				startElection();
			}
		});

		startButton = new JButton("Start");
		startButton.setBackground(Color.GREEN);
		startButton.setEnabled(false);
		startButton.addActionListener(this);

		getContentPane().add(startButton, BorderLayout.CENTER);
		getContentPane().add(quitButton, BorderLayout.SOUTH);

		this.setTitle("DRE");
		// exit program when window gets closed
		WindowListener l = new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		};
		this.addWindowListener(l);
		this.setVisible(true);
		this.properties = new Properties();
		try {
			FileInputStream in = new FileInputStream("DreProperties.properties");
			properties.load(in);
		} catch (IOException e) {
			// throw new IOException("Failed to retrieve properties ", e);
		}
		contentPath = path + properties.getProperty("ContentFolder");

		// Begin constructing the interface
		setSize(1000, 730);
		fontsize = 20;
		textArea = new JTextArea(20, 20);
		textArea.setWrapStyleWord(true);
		textArea.setLineWrap(true);
		textArea.setEditable(false);
		textArea.setFont(new Font("Helvetica", Font.PLAIN, fontsize));
		textArea.setBackground(new Color(255, 255, 239));
		textArea.setVisible(false);

		// textArea.addKeyListener(this);

		getContentPane().add(textArea, BorderLayout.NORTH);

		// getContentPane().addKeyListener(this);

		// Tell the print function where to place the text
		Print.setOutputTextArea(textArea);

		JButton eTegQuitButton = new JButton("Quit");
		eTegQuitButton.setBackground(Color.RED);
		eTegQuitButton.addActionListener(this);

		// numpad
		numPad = new JPanel();
		numPad.setLayout(new GridLayout(4, 3));

		buttons = new JButton[11];
		for (int i = 0; i < 10; i++) {
			buttons[i] = new JButton(i + "");
			buttons[i].setFont(new Font("Arial", Font.BOLD, 14));
		}
		buttons[10] = new JButton("*");

		for (int i = 0; i < 11; i++)
			buttons[i].addActionListener(this);

		for (int i = 1; i < 10; i++)
			numPad.add(buttons[i]);
		numPad.add(buttons[10]);
		numPad.add(buttons[0]);
		numPad.add(eTegQuitButton);
		numPad.setVisible(false);
		getContentPane().add(numPad, BorderLayout.SOUTH);

		this.setTitle("Scantegrity Acessible Interface");
		scroll = new JScrollPane(textArea);
		this.getContentPane().add(scroll, BorderLayout.CENTER);
		// exit program when window gets closed
		WindowListener win = new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		};
		this.addWindowListener(win);
		// this.setVisible(true);

		KeyboardFocusManager manager = KeyboardFocusManager
				.getCurrentKeyboardFocusManager();
		dispatcher = new MyDispatcher(this);
		manager.addKeyEventDispatcher(dispatcher);
	}

	/**
	 * generate barcode serial numbers
	 * 
	 */
	private void generateSerialNumbers() {
		try {
			serialNumbers = software.engine.RowPermutation
					.generateBarcodeSerialNumbers(mk1, mk2, C, 100000, 999999,
							NoBallots);
		} catch (Exception e) {
			throw new ScantegrityException("Cannot generate serial numbers", e);
		}
		Print.debug(Arrays.toString(serialNumbers));
	}

	/**
	 * Fetches a ballot from all the available ones
	 * 
	 */
	public void getBallotAndRun() throws IOException {
		startButton.setVisible(false);
		quitButton.setVisible(false);
		textArea.setVisible(true);
		// numPad.setVisible(true);
		this.initialize();
		FileOutputStream out = null;
		String ward = properties.getProperty("Ward");
		System.out.println(ward);
		out = new FileOutputStream("DreProperties.properties");
		// grab some ballot
		int lastUsedPid = 0;
		if (keyIterator == null) {
			keyIterator = keys.iterator();
		}
		// check to make sure the ballot has not yet been used this assumes that
		// pid is strictly increasing
		for (int i = 0; i < properties.getProperty("LastUsedBallot" + ward)
				.length(); i++) {
			lastUsedPid += (int) ((properties.getProperty("LastUsedBallot"
					+ ward).charAt(i)) - 48)
					* Math.pow(
							10,
							Globals.PROPERTIES.getProperty(
									"LastUsedBallot" + ward).length()
									- i - 1);
		}
		Print.debug("pid/lastPid: " + pid + " " + lastUsedPid);
		while (keyIterator.hasNext() && pid <= lastUsedPid)
			pid = keyIterator.next();
		Print.debug("newPid: " + pid);
		// get the ballot information
		currBallot = ballotRows.get(pid);
		Globals.PROPERTIES.setProperty("LastUsedBallot" + ward, "" + pid);
		Globals.PROPERTIES
				.store(out,
						"-------------------------------------------------------------------------------\n Copyright (c) 2011 Alex Florescu, Jan Rubio, John Wittrock, Tyler Kaczmarek.\n All rights reserved. This program and the accompanying materials\n are made available under the terms of the GNU Public License v2.0\n which accompanies this distribution, and is available at\n http://www.gnu.org/licenses/old-licenses/gpl-2.0.html\n     Contributors:\n Tyler Kaczmarek - initial API and implementation\n -------------------------------------------------------------------------------\n");
		Print.debug("Property has been set to " + pid);
		// get serial number
		ballotSerial = serialNumbers[pid];

		if (keyIterator.hasNext())
			pid = keyIterator.next();

		// get the ballot information
		currBallot = ballotRows.get(pid);

		// get serial number
		ballotSerial = serialNumbers[pid];

		// get ballot confirmation codes
		TreeMap<Integer, TreeMap<Integer, String>> ballotConfirmationCodes = allCodes
				.get(ballotSerial);

		// get ballot commitments
		// TreeMap<Integer, TreeMap<Integer, String>> ballotCommitments =
		// allCommitments

		// load eTegrity

		// voterApp = new Etegrity(path, this);
		// ballotCommitments);
		this.setBallot(pid, ballotSerial, ballotConfirmationCodes);
		Print.debug("BallotConfirmations totally arent null right?"
				+ (ballotConfirmationCodes != null));
		try {
			this.runElection(Globals.PROPERTIES.getProperty("ElectionSpec"));
		} catch (ESException e) {
			throw new IOException("Problems with the ElectionSpecification", e);
		}
		if (!haveQuit) {
			this.quit();
		}
	}

	/**
	 * Grab the commitments from the XML
	 */
	public void getCommitments() throws Exception {
		// commitments are in MeetingTwoOutCommitments.xml
		// allCommitments = parseXml(path + "MeetingTwoOutCommitments.xml", "c",
		// false);
		// keys = allCommitments.keySet();
		// System.out.println(keys);
	}

	/**
	 * Parses an XML to obtain some specific information from it Code is taken
	 * from the Scantegrity package
	 * 
	 * @param pathToPrintsFile
	 *            path to XML file
	 * @param field
	 *            the specific field that is requested
	 * @param serial
	 *            whether the XML contains files refered to by serial (true) or
	 *            pid (false)
	 * @return a 3-level treemap containing ballots->questions->field
	 * @throws Exception
	 */
	public TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, String>>> parseXml(
			String pathToPrintsFile, String field, boolean serial)
			throws Exception {
		TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, String>>> result = new TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, String>>>();
		org.w3c.dom.Document doc = Util.DomParse(pathToPrintsFile);
		int printedSerial = -1;
		int qid = -1;
		int sid = -1;
		keys = new HashSet<Integer>();
		NodeList ballots = doc.getElementsByTagName("ballot");
		serialNumbers = new int[ballots.getLength()];
		ballotRows = new TreeMap<Integer, BallotRow>();

		for (int b = 0; b < ballots.getLength(); b++) {
			BallotRow ballotRow = new BallotRow(ballots.item(b));
			if (serial) {
				// printedSerial=Integer.parseInt(ballotRow.getBarcodeSerial());
				printedSerial = Integer.parseInt(ballotRow.getBarcodeSerial()
						.substring(2)); // removes the ward number and dash
			} else {
				printedSerial = ballotRow.getPid();
			}
			ballotRows.put(b, ballotRow);

			TreeMap<Integer, TreeMap<Integer, String>> ballotFields = new TreeMap<Integer, TreeMap<Integer, String>>();
			for (Node question = ballots.item(b).getFirstChild(); question != null; question = question
					.getNextSibling()) {
				if (question.getNodeName().compareTo("question") == 0) {
					qid = Integer.parseInt(question.getAttributes()
							.getNamedItem("id").getNodeValue());
					TreeMap<Integer, String> values = new TreeMap<Integer, String>();
					for (Node symbol = question.getFirstChild(); symbol != null; symbol = symbol
							.getNextSibling()) {
						if (symbol.getNodeName().compareTo("symbol") == 0) {
							sid = Integer.parseInt(symbol.getAttributes()
									.getNamedItem("id").getNodeValue());
							values.put(sid, symbol.getAttributes()
									.getNamedItem(field).getNodeValue());
						}
					}
					ballotFields.put(qid, values);
				}
			}
			keys.add(b);
			serialNumbers[b] = printedSerial;
			result.put(printedSerial, ballotFields);
		}
		return result;
	}

	public TreeMap<Integer, TreeMap<Integer, TreeMap<String, String>>> parseSaltsXml(
			String pathToPrintsFile, String field) throws Exception {
		TreeMap<Integer, TreeMap<Integer, TreeMap<String, String>>> result = new TreeMap<Integer, TreeMap<Integer, TreeMap<String, String>>>();
		org.w3c.dom.Document doc = Util.DomParse(pathToPrintsFile);
		int printedSerial = -1;
		int qid = -1;
		String sid = "";
		NodeList ballots = doc.getElementsByTagName("ballot");
		for (int b = 0; b < ballots.getLength(); b++) {
			BallotRow ballotRow = new BallotRow(ballots.item(b));

			printedSerial = ballotRow.getPid();

			TreeMap<Integer, TreeMap<String, String>> ballotFields = new TreeMap<Integer, TreeMap<String, String>>();
			for (Node question = ballots.item(b).getFirstChild(); question != null; question = question
					.getNextSibling()) {
				if (question.getNodeName().compareTo("question") == 0) {
					qid = Integer.parseInt(question.getAttributes()
							.getNamedItem("id").getNodeValue());
					TreeMap<String, String> values = new TreeMap<String, String>();
					for (Node symbol = question.getFirstChild(); symbol != null; symbol = symbol
							.getNextSibling()) {
						if (symbol.getNodeName().compareTo("symbol") == 0) {
							sid = symbol.getAttributes().getNamedItem("code")
									.getNodeValue();
							// sid=Integer.parseInt(symbol.getAttributes().getNamedItem("id").getNodeValue());
							values.put(sid, symbol.getAttributes()
									.getNamedItem(field).getNodeValue());
						}
					}
					ballotFields.put(qid, values);
				}
			}
			result.put(printedSerial, ballotFields);
		}
		return result;
	}

	public TreeMap<Integer, TreeMap<Integer, TreeMap<String, Integer>>> parseCodesToIdsXml(
			String pathToPrintsFile, String field) throws Exception {
		TreeMap<Integer, TreeMap<Integer, TreeMap<String, Integer>>> result = new TreeMap<Integer, TreeMap<Integer, TreeMap<String, Integer>>>();
		org.w3c.dom.Document doc = Util.DomParse(pathToPrintsFile);
		int printedSerial = -1;
		int qid = -1;
		String sid = "";
		NodeList ballots = doc.getElementsByTagName("ballot");
		for (int b = 0; b < ballots.getLength(); b++) {
			BallotRow ballotRow = new BallotRow(ballots.item(b));

			printedSerial = ballotRow.getPid();

			TreeMap<Integer, TreeMap<String, Integer>> ballotFields = new TreeMap<Integer, TreeMap<String, Integer>>();
			for (Node question = ballots.item(b).getFirstChild(); question != null; question = question
					.getNextSibling()) {
				if (question.getNodeName().compareTo("question") == 0) {
					qid = Integer.parseInt(question.getAttributes()
							.getNamedItem("id").getNodeValue());
					TreeMap<String, Integer> values = new TreeMap<String, Integer>();
					for (Node symbol = question.getFirstChild(); symbol != null; symbol = symbol
							.getNextSibling()) {
						if (symbol.getNodeName().compareTo("symbol") == 0) {
							sid = symbol.getAttributes().getNamedItem("code")
									.getNodeValue();
							// sid=Integer.parseInt(symbol.getAttributes().getNamedItem("id").getNodeValue());
							values.put(
									sid,
									Integer.parseInt(symbol.getAttributes()
											.getNamedItem("id").getNodeValue()));
						}
					}
					ballotFields.put(qid, values);
				}
			}
			result.put(printedSerial, ballotFields);
		}
		return result;
	}

	/**
	 * Reads the confirmation codes from the private xml file
	 * 
	 * @throws Exception
	 *             if it can't parse XML
	 */
	public void getConfirmationCodes() throws Exception {
		allCodes = parseXml(
				path + "private/" + Globals.PROPERTIES.getProperty("Ward")
						+ "/MeetingTwoPrints.xml", "code", true);
	}

	/**
	 * Uploads the codes to the website
	 * 
	 * @author Jan Rubio
	 * @param serial
	 *            The web serial of the ballot to be uploaded
	 * @param confirmationCodes
	 *            The confirmation codes that appear on the ballot
	 * @throws IOException
	 *             Error connecting to the website or in sending the post data
	 * @throws UnsupportedEncodingException
	 *             Error in encoding the post data
	 */
	public static void uploadCodes(String serial,
			TreeMap<Integer, TreeMap<Integer, String>> confirmationCodes)
			throws IOException {

		String data = "";
		URL myURL = null;
		URLConnection ucon = null;

		data = URLEncoder.encode("upload", "UTF-8") + "="
				+ URLEncoder.encode("1", "UTF-8");
		data += "&" + URLEncoder.encode("serial", "UTF-8") + "="
				+ URLEncoder.encode(serial, "UTF-8");

		Set<Integer> questionKeys = confirmationCodes.keySet();
		Iterator<Integer> iter = questionKeys.iterator();
		while (iter.hasNext()) {
			Integer questionid = iter.next();
			// Use this as the identifier so that it's easier to figure out
			// server side
			int symbolIndex = 0;

			TreeMap<Integer, String> codes = confirmationCodes.get(questionid);
			Set<Integer> codesKeys = codes.keySet();
			Iterator<Integer> codesIter = codesKeys.iterator();
			while (codesIter.hasNext()) {
				Integer codeid = codesIter.next();
				String code = codes.get(codeid);

				data += "&"
						+ URLEncoder.encode("code" + questionid.intValue() + ""
								+ symbolIndex + "id", "UTF-8") + "="
						+ URLEncoder.encode("" + codeid.intValue(), "UTF-8");
				data += "&"
						+ URLEncoder.encode("code" + questionid.intValue() + ""
								+ symbolIndex + "code", "UTF-8") + "="
						+ URLEncoder.encode(code, "UTF-8");
				symbolIndex++;
			}
		}

		// Define the URL we want to load data from
		myURL = new URL(Globals.PROPERTIES.getProperty("CheckCodesURL"));
		// Open a connection to that URL
		ucon = myURL.openConnection();
		ucon.setDoOutput(true);
		OutputStreamWriter wr = new OutputStreamWriter(ucon.getOutputStream());
		wr.write(data);
		wr.flush();

		// Make sure that we're getting a response and the code has been
		// uploaded
		Scanner sc = new Scanner(ucon.getInputStream());
		sc.next();
	}

	public void setReady() {
		this.setVisible(false);
		this.setVisible(true);
		startButton.setEnabled(true);
	}

	/**
	 * Test method
	 * 
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		// reading in the PROPERTIES
		FileInputStream in = new FileInputStream("DreProperties.properties");
		Globals.PROPERTIES = new Properties();
		Globals.PROPERTIES.load(in);
		DRE dre = new DRE(Globals.PROPERTIES.getProperty("ElectionFolder"));

		Globals.updateDebugStatus();
		dre.setReady();
	}

	/**
	 * Called to exit the application cleanly
	 * 
	 */
	public void quit() {
		// hide window
		// if quit is not selected during an election exit program. Otherwise,
		// return to start/quit screen
		if (startButton.isVisible())
			System.exit(0);
		else {
			haveQuit = true;
			fsm.stop();
			numPad.setVisible(false);
			textArea.setText(null);
			textArea.setVisible(false);
			quitButton.setVisible(true);
			startButton.setVisible(true);
		}
	}

	/**
	 * Handles events for various buttons pressed
	 */
	public void actionPerformed(final ActionEvent event) {

		SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

			@Override
			protected String doInBackground() throws Exception {
				String s = event.getActionCommand();
				if (s.equals("Start")) {
					startElection();
				}
				if (s.equals("Quit")) {
					quit();
					return null;
				}

				// convert star to "10" to help handle input
				if (s.equals("*")) {
					s = new String("10");
				}

				try {
					Integer i = new Integer(s);
					// textArea.setText(null);

					fsm.giveInput(i.intValue());

					updateEnabledButtons(fsm.getValidInput());
				} catch (NumberFormatException e) {
					// should only be reached when starting, otherwise a
					// numerical value
					// should always be handled
					if (s.equals("Start")) {
						// Do nothing
					} else {
						throw new RuntimeException("Unknown error in the UI ",
								e);
					}
				}
				return null;
			}

		};
		worker.execute();
	}

	/**
	 * Sets which buttons are enabled according to what valid inputs the current
	 * state has
	 * 
	 * @param validInputs
	 *            the bitmask for the current input
	 * @see State.java
	 */
	public void updateEnabledButtons(int validInputs) {
		for (int i = 0; i < 11; i++) {
			buttons[i].setEnabled((validInputs & (1 << (i + 1))) != 0);
		}
	}

	/**
	 * Perform the first steps - setting the language, etc
	 * 
	 */
	public void initialize() {
		contentPath = path + properties.getProperty("ContentFolder");
		electionDir[0] = contentPath;
		fontsize = 20;
		textArea.setFont(new Font("Helvetica", Font.PLAIN, fontsize));
		setLang();
		setWard();
		setZoom();
		haveQuit = false;
		// before we get a ballot, we have to figure out the ward and language
		// its in
		// new content path for the selected ward + language
		Print.debug("This is the content path : " + contentPath);
		this.generateSerialNumbers();
		// dre.getCommitments();
		try {
			this.getConfirmationCodes();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// select language
		// int[] a={1,2,10};
		// fsm.addState(a, electionDir, "lang.wav", true, new StateAction() {
		// public void doAction(int x) {
		// if (x==1) electionDir[0]=electionDir[0]+"en/";
		// if (x==2) electionDir[0]=electionDir[0]+"es/";
		// }
		// });
		// electionDir[0] = electionDir[0] + "en/";
	}

	/**
	 * Add the state that reads the confirmation codes
	 * 
	 */
	public void readConfirmationCodes() {
		// at this stage, quitting should not be allowed
		numPad.getComponent(11).setVisible(false);
		fsm = new FiniteStateMachine();
		if (confirmationCodes == null) {
			throw new NullPointerException(
					"Confirmation codes not initialized!");
		}
		String[] content = { contentPath };
		// possible inputs: 1 to continue and finish or * to repeat
		int[] inputs = { Globals.TO_CONTINUE, Globals.REPEAT_THIS_MESSAGE };

		int numQuestions = confirmationCodes.size();

		// for(int i = 0; i < numQuestions; i++){

		// }

		// wavs.add("extras/Goodbye.mp3");
		// wavs.add("extras/newline.mp3");
		// wavs.add("extras/newline.mp3");

		for (int i = 0; i < numQuestions; i++) {
			// int numAnswers=confirmationCodes.get(i).size();
			ArrayList<String> wavs = new ArrayList<String>();
			wavs.add(Globals.PROPERTIES.getProperty("Language")
					+ "/extras/ConfirmationNumbers.mp3"); // confirmation
			// code(s) for
			// question i
			if (i == 0) {
				wavs.add(Globals.PROPERTIES.getProperty("Language")
						+ "/extras/mayoral.mp3");
			} else if (i == 1) {
				wavs.add(Globals.PROPERTIES.getProperty("Language")
						+ "/extras/CouncilMember.mp3");
			} else {
				Print.debug("THERE HAS BEEN AN ERROR AND THERE ARE MORE THAN ONE CONTEST");
			}
			// wavs.add("extras/" + (i + 1) + ".mp3");
			if (votes[i][0] == -1) {
				wavs.add(Globals.PROPERTIES.getProperty("Language")
						+ "/extras/are.mp3");
				wavs.add(Globals.PROPERTIES.getProperty("Language")
						+ "/extras/newline.mp3");
				wavs.add(Globals.PROPERTIES.getProperty("Language")
						+ "/extras/NoSelections.mp3");
				wavs.add(Globals.PROPERTIES.getProperty("Language")
						+ "/extras/newline.mp3");

			} else {
				if (votes[i].length == 1) { // single answer
					wavs.add(Globals.PROPERTIES.getProperty("Language")
							+ "/extras/Is.mp3");
					if (votes[i][0] != -1) {
						String code = confirmationCodes.get(i).get(votes[i][0]);
						for (int k = 0; k < code.length(); k++) {
							char c = code.charAt(k);
							String wav = (Globals.PROPERTIES
									.getProperty("Language") + "/extras/" + c + ".mp3"); // ++
							wavs.add(wav);
						}
					} else
						wavs.add(Globals.PROPERTIES.getProperty("Language")
								+ "/extras/skip.mp3");

				} else { // multiple answers
					wavs.add(Globals.PROPERTIES.getProperty("Language")
							+ "/extras/are.mp3");
					wavs.add(Globals.PROPERTIES.getProperty("Language")
							+ "/extras/newline.mp3");
					int n = votes[i].length;
					for (int j = 0; j < n; j++) {
						if (votes[i][j] == -1) {
							break;
						}
						if (votes[i][j] > -1 && votes[i][j] < 6) {
							// because the codes are stored linearly from
							// 0->n*n, we
							// must pick the code we need
							// TODO make the confirmation code retrieval less
							// hackish
							// FIX FOR JUNE 9 THIS WILL ASSUME YOU ARE RANKING
							// ALL
							// MULTIPLE VOTES
							Print.debug("votes final: "
									+ Arrays.toString(votes[i]));
							Print.debug(confirmationCodes.toString());
							String code = confirmationCodes.get(i).get(
									votes[i][j] * votes[i].length + j);
							Print.debug("Code is not null?" + (code != null)
									+ " val is " + votes[i][j]
									* votes[i].length + j);
							// if(code != null){
							for (int k = 0; k < code.length(); k++) {
								char c = code.charAt(k);
								String wav = (Globals.PROPERTIES
										.getProperty("Language")
										+ "/extras/"
										+ c + ".mp3"); // ++
								wavs.add(wav);
							}// }
						} else if (votes[i][j] > 5) {
							String code = confirmationCodes.get(i)
									.get((votes[i].length - 1)
											* votes[i].length + j);
							for (int k = 0; k < code.length(); k++) {
								char c = code.charAt(k);
								String wav = (Globals.PROPERTIES
										.getProperty("Language")
										+ "/extras/"
										+ c + ".mp3"); // ++
								wavs.add(wav);
							}
						} else {
							wavs.add(Globals.PROPERTIES.getProperty("Language")
									+ "/extras/skip.mp3");
						}
						if (j < (n - 1) && votes[i][j + 1] != -1) {
							wavs.add(Globals.PROPERTIES.getProperty("Language")
									+ "/extras/And.mp3");
							wavs.add(Globals.PROPERTIES.getProperty("Language")
									+ "/extras/newline.mp3");
						}

					}
				}
			}

			wavs.add(Globals.PROPERTIES.getProperty("Language")
					+ "/extras/newline.mp3");
			wavs.add(Globals.PROPERTIES.getProperty("Language")
					+ "/extras/newline.mp3");
			wavs.add(Globals.PROPERTIES.getProperty("Language")
					+ "/extras/ToContinue.mp3");
			wavs.add(Globals.PROPERTIES.getProperty("Language")
					+ "/extras/newline.mp3");
			wavs.add(Globals.PROPERTIES.getProperty("Language")
					+ "/extras/ToHearAgain.mp3");
			String[] wavsArray = new String[wavs.size()];
			wavsArray = wavs.toArray(wavsArray);

			fsm.addState(inputs, content, wavsArray, true, new StateAction() {
				public void doAction(int x) {
					System.out.println("Codes have been read");
				}
			});

			fsm.addTransition(fsm.size() - 1, fsm.size() - 1,
					Globals.REPEAT_THIS_MESSAGE);
			if (i > 0) {
				fsm.addTransition(fsm.size() - 2, fsm.size() - 1,
						Globals.TO_CONTINUE);

			}
		}
		fsm.setStartState(0);

		// goodbye
		int[] a2 = { Globals.TO_CONTINUE, Globals.REPEAT_THIS_MESSAGE };
		int[] a3 = { Globals.REPEAT_THIS_MESSAGE };

		Vector<String> instructionSoundFiles = new Vector<String>();

		instructionSoundFiles.add(Globals.PROPERTIES.getProperty("Language")
				+ "/extras/BallotNotYetCast.mp3");
		instructionSoundFiles.add(Globals.PROPERTIES.getProperty("Language")
				+ "/extras/ThereIsAPrinter.mp3");
		instructionSoundFiles.add(Globals.PROPERTIES.getProperty("Language")
				+ "/extras/right.mp3");
		// instructionSoundFiles.add(Globals.PROPERTIES.getProperty("Language")
		// + "/extras/PrinterWillPrint.mp3");
		// instructionSoundFiles.add(Globals.PROPERTIES.getProperty("Language")
		// + "/extras/TheBallot.mp3");
		// instructionSoundFiles.add(Globals.PROPERTIES.getProperty("Language")
		// + "/extras/PrintedOutInthe.mp3");
		// instructionSoundFiles.add(Globals.PROPERTIES.getProperty("Language")
		// + "/extras/UpperTray.mp3");
		// instructionSoundFiles.add(Globals.PROPERTIES.getProperty("Language")
		// + "/extras/TheBallotAnd.mp3");
		// instructionSoundFiles.add(Globals.PROPERTIES.getProperty("Language")
		// + "/extras/UpperTray.mp3");
		instructionSoundFiles.add(Globals.PROPERTIES.getProperty("Language")
				+ "/extras/TakeBoth.mp3");
		instructionSoundFiles.add(Globals.PROPERTIES.getProperty("Language")
				+ "/extras/ElectionOfficialWillGuide.mp3");
		instructionSoundFiles.add(Globals.PROPERTIES.getProperty("Language")
				+ "/extras/VerificationCardHome.mp3");

		instructionSoundFiles.add(Globals.PROPERTIES.getProperty("Language")
				+ "/extras/newline.mp3");
		instructionSoundFiles.add(Globals.PROPERTIES.getProperty("Language")
				+ "/extras/newline.mp3");
		instructionSoundFiles.add(Globals.PROPERTIES.getProperty("Language")
				+ "/extras/EndAudioSession.mp3");
		instructionSoundFiles.add(Globals.PROPERTIES.getProperty("Language")
				+ "/extras/newline.mp3");
		// instructionSoundFiles.add(Globals.PROPERTIES.getProperty("Language")
		// + "/extras/ToContinue.mp3");

		instructionSoundFiles.add(Globals.PROPERTIES.getProperty("Language")
				+ "/extras/ToRepeatThisMessage.mp3");

		String[] instructionFiles = new String[instructionSoundFiles.size()];
		instructionFiles = instructionSoundFiles.toArray(instructionFiles);

		fsm.addState(a2, content, instructionFiles, true);
		fsm.addTransition(fsm.size() - 2, fsm.size() - 1, Globals.TO_CONTINUE);

		fsm.addState(a3, content, Globals.PROPERTIES.getProperty("Language")
				+ "/extras/Bye.mp3", true);
		// fsm.addTransition(0, 1, 1);
		// fsm.addTransition(0, 0, 10);
		fsm.addTransition(fsm.size() - 2, fsm.size() - 1, Globals.TO_CONTINUE);
		fsm.addTransition(fsm.size() - 2, fsm.size() - 2,
				Globals.REPEAT_THIS_MESSAGE);
		fsm.addTransition(fsm.size() - 1, fsm.size() - 1,
				Globals.REPEAT_THIS_MESSAGE);
		fsm.setStopState(fsm.size() - 1);
		fsm.start();
		numPad.getComponent(11).setVisible(true);

	}

	/**
	 * Sets the info for this ballot
	 * 
	 */
	public void setBallot(int pid, int serial,
			TreeMap<Integer, TreeMap<Integer, String>> codes) {
		// TreeMap<Integer, TreeMap<Integer, String>> commitments) {
		this.confirmationCodes = codes;
	}

	/**
	 * Runs the election for a given election specification
	 * 
	 * @param electionSpec
	 *            The election specification filename
	 */
	public void runElection(String electionSpec) throws ESException {
		Print.debug("Starting the voting procedure..");
		Print.debug("The current path is: " + contentPath);
		Election e = new edu.gwu.election.Election(contentPath, electionSpec);
		textArea.setText(null);
		e.setConfirmationCodes(confirmationCodes);
		// TODO set language to whatever was selected in the first
		e.setLanguage();
		e.setWard();
		e.constructFSM();
		if (!haveQuit) {
			fsm = e.getFSM();
			updateEnabledButtons(fsm.getValidInput());
			e.startElection();
			votes = e.getVotes();
			updateEnabledButtons(fsm.getValidInput());
		}
		if (!haveQuit) {
			readConfirmationCodes();

			try {
				// Print the ballot
				org.scantegrity.common.ballotstandards.electionSpecification.ElectionSpecification es = new org.scantegrity.common.ballotstandards.electionSpecification.ElectionSpecification(
						path + electionSpec);
				PrintableBallotMarker pbm = new PrintableBallotMarker(es,
						PrintableBallotMarker.PrintSetting.BALLOT);
				// Print the card
				// set the ballot (for ward etc.)
				Print.debug("Printing the Ballot");
				pbm.setPrintSetting(PrintableBallotMarker.PrintSetting.BALLOT);
				pbm.printMarks(currBallot, votes, confirmationCodes);
				pbm.setPrintSetting(PrintableBallotMarker.PrintSetting.CARD);
				Print.debug("Printing the Crad");
				pbm.printMarks(currBallot, votes, confirmationCodes);

			} catch (Exception e1) {
				throw new ScantegrityException(
						"Failed to make and/or print ballot", e1);
			}

		}
		Print.debug("Voting is complete!");

		// KeyboardFocusManager manager = KeyboardFocusManager
		// .getCurrentKeyboardFocusManager();
		// manager.removeKeyEventDispatcher(dispatcher);
	}

	public void setZoom() {
		fsm = new FiniteStateMachine();
		String lan = Globals.PROPERTIES.getProperty("Language") + "/";

		int[] welcomeInputs = { Globals.TO_CONTINUE };
		fsm.addState(welcomeInputs, electionDir, lan
				+ "extras/WelcomePressEnter.mp3", true);

		int[] a = { 4, 7, Globals.TO_CONTINUE };

		String[] wavs = { lan + "extras/SetFont.mp3",
				lan + "extras/ToDecrease7.mp3", lan + "extras/FontSize.mp3",
				lan + "extras/BottomRight.mp3" };
		fsm.addState(a, electionDir, wavs, true, new StateAction() {
			public void doAction(int x) {
				if (x == 4) {
					fontsize = fontsize + 10;
					textArea.setFont(new Font("Helvetica", Font.PLAIN, fontsize));
				}
				if (x == 7) {
					fontsize = fontsize - 10;
					textArea.setFont(new Font("Helvetica", Font.PLAIN, fontsize));
				}
				Print.debug("an action has happened");
			}
		});
		int[] b = { 0 };
		fsm.addState(b, electionDir, "", true);
		fsm.setStopState(2);
		fsm.setStartState(0);
		fsm.addTransition(0, 1, Globals.TO_CONTINUE);
		fsm.addTransition(1, 1, 4);
		fsm.addTransition(1, 1, 7);
		fsm.addTransition(1, 2, Globals.TO_CONTINUE);
		updateEnabledButtons(fsm.getValidInput());
		fsm.setInputStream(System.in);
		fsm.start();
	}

	public void setLang() {
		fsm = new FiniteStateMachine();
		String[] content = { contentPath };
		int[] a = { 1, 2 };
		fsm.addState(a, content, "Language.mp3", true, new StateAction() {
			public void doAction(int x) {
				if (x == 1) {
					Globals.PROPERTIES.setProperty("Language", "en");
				}
				if (x == 2) {
					Globals.PROPERTIES.setProperty("Language", "es");
				}
				Print.debug("an action has happened");
			}
		});
		int[] b = { 0 };
		fsm.addState(b, content, "", true);
		fsm.setStopState(1);
		fsm.setStartState(0);
		fsm.addTransition(0, 1, 1);
		fsm.addTransition(0, 1, 2);
		updateEnabledButtons(fsm.getValidInput());
		fsm.setInputStream(System.in);
		fsm.start();
	}

	public void setWard() {
		fsm = new FiniteStateMachine();
		int[] a = { 1, 2, 3, 4, 5, 6 };
		// will prompt the voter (or an election official, as is more likely the
		// case) to set the ward
		fsm.addState(a, electionDir, "Ward.mp3", true, new StateAction() {
			public void doAction(int x) {
				// all inputs 1-6 are valid to set a ward
				if (x >= 1 && x <= 6) {
					Globals.PROPERTIES.setProperty("Ward", "ward" + x);
					Print.debug("Ward has been set to "
							+ Globals.PROPERTIES.getProperty("Ward"));
				}
			}
		});
		int[] b = { 0 };
		fsm.addState(b, electionDir, "", true);
		fsm.setStopState(1);
		fsm.setStartState(0);
		fsm.addTransition(0, 1, 1);
		fsm.addTransition(0, 1, 2);
		fsm.addTransition(0, 1, 3);
		fsm.addTransition(0, 1, 4);
		fsm.addTransition(0, 1, 5);
		fsm.addTransition(0, 1, 6);
		updateEnabledButtons(fsm.getValidInput());
		fsm.setInputStream(System.in);
		fsm.start();
	}

	public void startElection() {
		Thread t = new Thread() {
			public void run() {
				try {
					getBallotAndRun();
				} catch (IOException e) {
					// TODO Handle the exceptions gracefully
					e.printStackTrace();
					System.exit(ERROR);
				}
			}
		};
		t.start();
	}

}