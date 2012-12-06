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

package edu.gwu.election;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import javax.sound.sampled.LineUnavailableException;

import org.gwu.voting.standardFormat.basic.Answer;
import org.gwu.voting.standardFormat.basic.Question;
import org.gwu.voting.standardFormat.electionSpecification.ElectionSpecification;
import org.gwu.voting.standardFormat.electionSpecification.exceptions.ESException;

import edu.gwu.audio.AudioCapture;
import edu.gwu.audio.AudioPlayer;
import edu.gwu.audio.AudioPlayerFactory;
import edu.gwu.election.fsm.FiniteStateMachine;
import edu.gwu.election.fsm.StateAction;

/**
 * The class that generates a FiniteStateMachine based on a given
 * ElectionSpecification
 * 
 * @author Alex Florescu
 * 
 */
public class Election {

	// the finite state machine everything builds on
	private FiniteStateMachine fsm = null;

	// for holding the votes until the ballot is cast
	private int[][] votes = null;

	// for holding the votes while a state is executing, until they are
	// confirmed
	private int[] temporaryVotes = null;

	private int numQuestions = 0;
	private String[] electionDir = null;
	private ElectionSpecification electionSpecification = null;
	private Question[] questions = null;

	private int[][] questionIndicies = null;

	Properties properties = null;

	// global variable used for referencing the selection the voter makes
	private int selectedVote = -1;

	private int currentQuestion = 0;

	// index for multiple votes
	private int voteSubIndex = 0;

	private int qToGoBackTo = 0;

	private int startOfConfirmations = 0;

	private String wardRelativePath;
	private String writeInsRelativePath;

	TreeMap<Integer, TreeMap<Integer, String>> confirmationCodes = null;

	/**
	 * Constructs a new Election
	 * 
	 * @param aDir
	 *            directory where the files are located
	 * @param electionSpecification
	 *            filename where the ElectionSpecification.xml file is located
	 *            (relative to the given path)
	 */
	public Election(String aDir, String filename) throws ESException {
		electionDir = new String[2]; // must be made as an array so we can
										// simulate pass-by-reference
		electionDir[0] = new String(aDir);
		electionDir[1] = new String(aDir);

		electionSpecification = new ElectionSpecification(electionDir[0]
				+ Globals.PROPERTIES.getProperty("Ward") + "/" + filename);
		Print.debug("ElectionSpec: " + electionDir[0]
				+ Globals.PROPERTIES.getProperty("Ward") + "/" + filename);

		// get questions
		questions = electionSpecification.getOrderedQuestions();
		numQuestions = questions.length;

		questionIndicies = new int[numQuestions][2];

		// initialize vote array
		votes = new int[numQuestions][];
		for (int i = 0; i < numQuestions; i++) {
			votes[i] = new int[questions[i].getMax()];
			for (int j = 0; j < votes[i].length; j++)
				votes[i][j] = -1;
		}
	}

	/**
	 * Method used to set the language dir
	 * 
	 * @param language
	 *            directory name (in electionDir's path) with the content files
	 */
	public void setLanguage() {
		electionDir[0] = electionDir[0]
				+ Globals.PROPERTIES.getProperty("Language") + "/";
	}

	public void setWard() {
		electionDir[1] = electionDir[1]
				+ Globals.PROPERTIES.getProperty("Ward") + "/";
	}

	/**
	 * Thread used to cleanup temporary files on shutdown
	 */
	private Thread cleanupFilesOnShutdown = new Thread() {
		public void run() {
			File f;
			String s;
			for (int i = 0; i < numQuestions; i++) {
				if (questions[i].getTypeOfAnswer().equals("one_answer")) {
					s = new String(electionDir[1] + "a" + i + ".mp3");
					f = new File(s);
					if (f.exists())
						f.delete();
				} else
					for (int j = 0; j < questions[i].getMax(); j++) {
						s = new String(electionDir[1] + "a" + i + "+" + j
								+ ".mp3");
						// Print.debug("Cleaning up: " + s);
						f = new File(s);
						if (f.exists())
							f.delete();

						s = new String(electionDir[1] + "a" + i + "+" + j
								+ ".wav");
						f = new File(s);
						if (f.exists())
							f.delete();

						s = new String(electionDir[1] + "a" + i + "+" + j
								+ ".txt");
						f = new File(s);
						if (f.exists())
							f.delete();
					}
			}

			f = new File(electionDir[1] + "../../writeIns/recording.wav");
			if (f.exists()) {
				f.delete();
			}

			for (int i = 0; i < numQuestions; i++) {
				resetAnswerFiles(i, questions[i].getMax());
			}
			Print.debug("Cleanup complete!");
		}
	};

	private void getProperties() {
		if (properties == null) {
			properties = new Properties();
			try {
				FileInputStream in = new FileInputStream(
						"DreProperties.properties");
				properties.load(in);
			} catch (IOException e) {
				// throw new IOException("Failed to retrieve properties ", e);
			}
		}
	}

	/**
	 * Returns the FSM for this election
	 * 
	 * @return
	 */
	public FiniteStateMachine getFSM() {
		return fsm;
	}

	/**
	 * Main method that constructs the finite state machine
	 * 
	 */
	public void constructFSM() {
		// make sure the onShutdown thread gets called before program exits even
		// in case of an error
		wardRelativePath = "../" + Globals.PROPERTIES.getProperty("Ward") + "/"
				+ Globals.PROPERTIES.getProperty("Language") + "/";
		electionDir[1] = electionDir[1]
				+ Globals.PROPERTIES.getProperty("Language") + "/";
		writeInsRelativePath = "../writeIns/";
		Print.debug("ElectionDir 1: " + electionDir[1]);
		Runtime.getRuntime().addShutdownHook(cleanupFilesOnShutdown);

		fsm = new FiniteStateMachine();
		// select language
		int[] a = { Globals.TO_CONTINUE, Globals.REPEAT_THIS_MESSAGE };
		/*
		 * g.addState(a, electionDir, "lang.mp3", true, new StateAction() {
		 * public void doAction(int x) { if (x==1)
		 * electionDir[0]=electionDir[0]+"en/"; if (x==2)
		 * electionDir[0]=electionDir[0]+"es/"; } });
		 */

		// read instructions and run election
		int[] a2 = { Globals.TO_CONTINUE, Globals.REPEAT_THIS_MESSAGE };
		Vector<String> soundFiles = new Vector<String>();
		soundFiles.add("extras/MunicipalElection.mp3");

		String wardNum = Globals.PROPERTIES.getProperty("Ward");
		wardNum = wardNum.substring(wardNum.length() - 1, wardNum.length());
		Print.debug("Ward number: " + wardNum);
		soundFiles.add("extras/" + wardNum + ".mp3");

		soundFiles.add("extras/TheDateIs.mp3");
		soundFiles.add("extras/newline.mp3");
		soundFiles.add("extras/TheBallotContains.mp3");
		soundFiles.add("extras/2.mp3");
		soundFiles.add("extras/contests.mp3");
		soundFiles.add("extras/newline.mp3");
		soundFiles.add("extras/instructions.mp3");
		soundFiles.add("extras/ToContinue.mp3");

		String[] wavs1 = new String[soundFiles.size()];
		wavs1 = soundFiles.toArray(wavs1);

		fsm.addState(a2, electionDir, wavs1, true, new StateAction() {
			public void doAction(int x) {
				// initializes all final vote selections to "skipped"

				for (int i = 0; i < numQuestions; i++) {
					if (questions[i].getTypeOfAnswer().equals("one_answer")) {
						Election.copyFile(electionDir[0] + "extras/NoOne" + "_"
								+ Globals.SOUND_SPEED + ".mp3", electionDir[1]
								+ "a" + i + ".mp3");
						Election.copyFile(electionDir[0] + "extras/NoOne.txt",
								electionDir[1] + "a" + i + ".txt");
					} else
						// for multiple answers, initialize all the
						// possible "sub-votes"

						for (int j = 0; j < questions[i].getMax(); j++) {
							Election.copyFile(electionDir[0] + "extras/NoOne"
									+ "_" + Globals.SOUND_SPEED + ".mp3",
									electionDir[1] + "a" + i + "+" + j + ".mp3");
							Election.copyFile(electionDir[0]
									+ "extras/NoOne.txt", electionDir[1] + "a"
									+ i + "+" + j + ".txt");
						}
				}
				// reset counter
				currentQuestion = 0;
				System.out.println("DONE WITH FIRST STATE ACTION");
			}
		});
		fsm.setStartState(0);

		Vector<String> secondInstructionFiles = new Vector<String>();
		secondInstructionFiles.add("extras/instructionsMore.mp3");
		secondInstructionFiles.add("extras/newline.mp3");
		secondInstructionFiles.add("extras/newline.mp3");
		secondInstructionFiles.add("extras/ToContinue.mp3");
		String[] secondInstructionFilesArray = new String[secondInstructionFiles
				.size()];
		secondInstructionFilesArray = secondInstructionFiles
				.toArray(secondInstructionFilesArray);
		int[] secondInstructionInputs = { Globals.TO_CONTINUE };
		fsm.addState(secondInstructionInputs, electionDir,
				secondInstructionFilesArray, true, null);
		Print.debug("Second state added: " + (fsm.size() - 1));
		fsm.addTransition(fsm.size() - 2, fsm.size() - 1, Globals.TO_CONTINUE);

		// all questions get added here
		for (int i = 0; i < numQuestions; i++)
			addQuestionToGraph(fsm, questions[i]);

		fsm.addTransition(fsm.size() - 1, fsm.size(), Globals.TO_CONTINUE);

		// Add confirmation state
		int[] confirmationInputs = { Globals.TO_GO_BACK, Globals.TO_CONTINUE };
		// a[1] = 3;
		// a[2] = 10;
		String[] wavs = null;
		Vector<String> aWavs = new Vector<String>();

		startOfConfirmations = fsm.size();
		for (int i = 0; i < numQuestions; i++) {
			Print.debug("Adding a confirmation state at " + fsm.size());
			aWavs = new Vector<String>();
			if (i == 0) {
				aWavs.add("extras/FinalConfirmation1.mp3");
			}
			aWavs.add("extras/newline.mp3");
			aWavs.add("extras/newline.mp3");
			aWavs.add("extras/YourSelectionsFor.mp3");
			aWavs.add("extras/" + (i + 1) + "th.mp3");
			aWavs.add("extras/contest.mp3");
			aWavs.add("extras/q" + i + "confirmation.mp3");
			if (i > 0) {
				String ward = Globals.PROPERTIES.getProperty("Ward");
				ward = ward.substring(ward.length() - 1, ward.length());
				Print.debug("Ward number: " + ward);
				aWavs.add("extras/" + ward + ".mp3");
			}
			aWavs.add("extras/are.mp3");
			aWavs.add("extras/newline.mp3");

			for (int j = 0; j < questions[i].getMax(); j++) {
				aWavs.add("extras/" + (j + 1) + "th.mp3");
				aWavs.add("extras/choice.mp3");
				aWavs.add(wardRelativePath + "a" + i + "+" + j + ".mp3");
				aWavs.add("extras/newline.mp3");

			}

			aWavs.add("extras/newline.mp3");
			aWavs.add("extras/newline.mp3");
			aWavs.add("extras/ToChangeYourChoices.mp3");
			aWavs.add("extras/newline.mp3");
			aWavs.add("extras/ToContinue.mp3");
			wavs = new String[aWavs.size()];
			wavs = aWavs.toArray(wavs);
			// add question states with appropriate transitions. Make sure to
			// add the last one to the actual final confirmation state.
			fsm.addState(confirmationInputs, electionDir, wavs, true,
					new StateAction() {

						public void doAction(int input) {
							if (input == Globals.TO_CONTINUE) {
								// for (int j = 0; j < Math.min(
								// temporaryVotes.length,
								// votes[currentQuestion].length); j++) {
								// Print.debug("temporaryVotes: "
								// + Arrays.toString(temporaryVotes));
								// if (temporaryVotes[j] !=
								// votes[currentQuestion][j])
								// votes[currentQuestion][j] =
								// temporaryVotes[j];
								// }
							}

							if (input == Globals.TO_GO_BACK) {
								Print.debug("Current state: "
										+ (startOfConfirmations + qToGoBackTo));
								try {
									fsm.addTransition(
											questionIndicies[qToGoBackTo][1],
											startOfConfirmations + qToGoBackTo,
											Globals.TO_CONTINUE);
									Print.debug("Question to confirmation: "
											+ (startOfConfirmations + qToGoBackTo));
									currentQuestion = qToGoBackTo;
									voteSubIndex = 0;
								} catch (Exception e) {
									e.printStackTrace();
								}
							}

							if (input != Globals.REPEAT_THIS_MESSAGE) {
								Print.debug(fsm.printTransitionsToString());
								qToGoBackTo++;
							}
						}
					});
			fsm.addTransition(fsm.size() - 1, questionIndicies[i][0],
					Globals.TO_GO_BACK);
			Print.debug("Confirmation for question " + i + ": State "
					+ (fsm.size() - 1));
			if (i > 0)
				fsm.addTransition(fsm.size() - 2, fsm.size() - 1,
						Globals.TO_CONTINUE);
		}

		aWavs = new Vector<String>();
		aWavs.add("extras/FinalConfirmation2.mp3");
		aWavs.add("extras/newline.mp3");
		aWavs.add("extras/newline.mp3");
		aWavs.add("extras/ToContinue.mp3");
		aWavs.add("extras/newline.mp3");
		aWavs.add("extras/ToRepeatThisMessage.mp3");
		wavs = new String[aWavs.size()];
		wavs = aWavs.toArray(wavs);
		int inputs[] = { 0, 11 };
		Print.debug("Adding the second final confirmation at " + fsm.size());
		fsm.addState(inputs, electionDir, wavs, true);
		fsm.addTransition(fsm.size() - 2, fsm.size() - 1, 11);
		fsm.addTransition(fsm.size() - 1, fsm.size() - 1, Globals.REPEAT_THIS_MESSAGE);
		
		// aWavs.add("extras/FinalConfirmation1.mp3");
		//
		// for (int i = 0; i < numQuestions; i++) {
		// aWavs.add("extras/newline.mp3");
		// aWavs.add(new String("extras/YourSelectionsFor.mp3"));
		// aWavs.add("extras/" + (i + 1) + "th.mp3");
		// aWavs.add("extras/contest.mp3");
		// aWavs.add("q" + i + "confirmation.mp3");
		// aWavs.add(new String("extras/are.mp3"));
		// aWavs.add("extras/newline.mp3");
		// aWavs.add("extras/newline.mp3");
		//
		// if (questions[i].getTypeOfAnswer().equals("one_answer")) {
		// aWavs.add(new String("a" + i + ".mp3"));
		// aWavs.add("extras/newline.mp3");
		// }
		//
		// else if (questions[i].getTypeOfAnswer().equals("multiple_answer")) {
		// for (int j = 0; j < questions[i].getMax(); j++) {
		// // aWavs.add(new String("a" + i + "+" + j + ".mp3"));
		// // aWavs.add("extras/newline.mp3");
		// aWavs.add("extras/" + j + 1 + "th.mp3");
		// aWavs.add("extras/choice.mp3");
		// aWavs.add(new String("a" + i + "+" + j + ".mp3"));
		// aWavs.add("extras/newline.mp3");
		// }
		// }
		//
		// else {
		// for (int j = 0, m = questions[i].getMax(); j < m; j++) {
		// aWavs.add(new String("extras/" + (j + 1) + "th.mp3"));
		// aWavs.add(new String("extras/VoteOutOf.mp3"));
		// aWavs.add(new String("extras/" + m + ".mp3"));
		// aWavs.add(new String("a" + i + "+" + j + ".mp3"));
		// aWavs.add("extras/newline.mp3");
		// }
		// }
		// }
		// // aWavs.add("extras/FinalConfirmation2.mp3");
		// aWavs.add("extras/ToChangeYourChoices.mp3");
		// aWavs.add("extras/ToContinue.mp3");
		// wavs = new String[aWavs.size()];
		// wavs = aWavs.toArray(wavs);
		//
		// fsm.addState(a, electionDir, wavs, true, new StateAction() {
		// public void doAction(int x) {
		// // In the event that 0 is selected at some point during the FSM,
		// // the contents of temporary has not been put into votes, so
		// // we'll check the current questions
		// if (x == 1) {
		// if (currentQuestion != votes.length) {
		// for (int j = 0; j < Math.min(temporaryVotes.length,
		// votes[currentQuestion].length); j++) {
		// if (temporaryVotes[j] != votes[currentQuestion][j])
		// votes[currentQuestion][j] = temporaryVotes[j];
		// }
		// }
		// }
		// }
		// });
		//
		// fsm.addTransition(fsm.size() - 2, fsm.size() - 1, 1);
		//
		// // all prev states can get to "confirm ballot" with input 0
		// for (int i = 1; i < fsm.size() - 1; i++)
		// fsm.addTransition(i, fsm.size() - 1, 0);
		//
		// fsm.addState(a, electionDir, "extras/ConfirmRevote.mp3", false,
		// new StateAction() {
		// public void doAction(int x) {
		// if (x == 1) {
		// if (currentQuestion != votes.length) {
		// for (int j = 0; j < Math.min(
		// temporaryVotes.length,
		// votes[currentQuestion].length); j++) {
		// if (temporaryVotes[j] != votes[currentQuestion][j])
		// votes[currentQuestion][j] = temporaryVotes[j];
		// }
		// }
		//
		// for (int i = 0; i < votes.length; i++) {
		// for (int j = 0; j < votes[i].length; j++) {
		// if (votes[i][j] > 6) {
		// File f = new File(electionDir[0]
		// + "writeIns/" + votes[i][j]
		// + ".mp3");
		// if (f.exists()) {
		// f.delete();
		// }
		// }
		// }
		// }
		// }
		// }
		// });
		//
		// fsm.addTransition(fsm.size() - 2, fsm.size() - 1, 3);
		// fsm.addTransition(fsm.size() - 1, fsm.size() - 2, 3);
		// fsm.addTransition(fsm.size() - 1, 0, 1);

		// goodbye message
		// a = new int[2]; a[0]=1; a[1]=10;
		// fsm.addState(a,electionDir, "extras/Goodbye.mp3", true, new
		// StateAction() {
		// public void doAction(int x) {
		// Print.debug("Election has finished");
		// }
		// });

		// fsm.addTransition(fsm.size()-3, fsm.size()-1, 1);
		int[] b = { 11, 0 };
		// add blank stop state
		fsm.addState(b, electionDir, "", true, new StateAction() {
			public void doAction(int x) {

			}
		});
		// fsm.addTransition(fsm.size() - 3, fsm.size() - 1, 1);
		fsm.addTransition(fsm.size() - 2, fsm.size() - 1, 11);

		// set stop state
		fsm.setStopState(fsm.size() - 1);

		// '*' repeats current state for ALL states
		for (int i = 0; i < fsm.size(); i++)
			fsm.addTransition(i, i, 0);
	}

	/**
	 * Add the state that reads the confirmation codes
	 * 
	 */
	public void readConfirmationCodes(
			TreeMap<Integer, TreeMap<Integer, String>> confirmationCodes) {
		if (confirmationCodes == null) {
			throw new NullPointerException(
					"Confirmation codes not initialized!");
		}
		// possible inputs: 1 to continue and finish or * to repeat
		int[] inputs = { 1, 10 };

		int numQuestions = confirmationCodes.size();
		ArrayList<String> wavs = new ArrayList<String>();

		wavs.add("extras/Goodbye.mp3");
		for (int i = 0; i < numQuestions; i++) {
			// int numAnswers=confirmationCodes.get(i).size();
			wavs.add("extras/ConfirmationCodeForQuestion.mp3"); // confirmation
																// code(s) for
																// question i
			wavs.add("extras/" + (i + 1) + ".mp3");

			if (votes[i].length == 1) { // single answer
				wavs.add("extras/Is.mp3");
				if (votes[i][0] != -1) {
					String code = confirmationCodes.get(i).get(votes[i][0]);
					for (int k = 0; k < code.length(); k++) {
						char c = code.charAt(k);
						String wav = ("extras/" + c + ".mp3"); // ++
						wavs.add(wav);
					}
				} else
					wavs.add("extras/NoOne" + "_" + Globals.SOUND_SPEED
							+ ".mp3");
			} else { // multiple answers
				wavs.add("extras/are" + Globals.SOUND_SPEED + ".mp3");
				int n = votes[i].length;
				for (int j = 0; j < n; j++) {
					if (votes[i][j] != -1) {
						// because the codes are stored linearly from 0->n*n, we
						// must pick the code we need
						String code = confirmationCodes.get(i).get(votes[i][j]);
						for (int k = 0; k < code.length(); k++) {
							char c = code.charAt(k);
							String wav = ("extras/" + c + Globals.SOUND_SPEED + ".mp3"); // ++
							wavs.add(wav);
						}
					} else {
						wavs.add("extras/NoOne" + "_" + Globals.SOUND_SPEED
								+ ".mp3");
					}
					if (j < (n - 1))
						wavs.add("extras/And" + Globals.SOUND_SPEED + ".mp3");
				}
			}
		}
		wavs.add("extras/PressOneToContinue" + Globals.SOUND_SPEED + ".mp3");
		String[] wavsArray = new String[wavs.size()];
		wavsArray = wavs.toArray(wavsArray);
		fsm.addState(inputs, electionDir, wavsArray, true, new StateAction() {
			public void doAction(int x) {
				Print.debug("Codes have been read");
			}
		});
	}

	/**
	 * Adds a new question to the FSM
	 * 
	 * @param fsm
	 *            the finite state machine
	 * @param qu
	 *            question being added
	 */
	private void addQuestionToGraph(FiniteStateMachine fsm, Question qu) {
		String ans = qu.getTypeOfAnswer();

		if (ans.equals(new String("one_answer")))
			addSimpleQuestion(fsm, qu);
		else if (ans.equals(new String("multiple_answers")))
			addMultipleQuestion(fsm, qu);
		else if (ans.equals(new String("ranked")))
			addRankedQuestion(fsm, qu);
		else
			throw new RuntimeException("Invalid xml format");
	}

	/**
	 * Set the confirmation codes
	 * 
	 * @param ballotConfirmationCodes
	 */
	public void setConfirmationCodes(
			TreeMap<Integer, TreeMap<Integer, String>> ballotConfirmationCodes) {
		confirmationCodes = ballotConfirmationCodes;
	}

	/**
	 * Adds a multiple-answer question
	 * 
	 * @param g
	 *            FMS graph
	 * @param qu
	 *            question to be added
	 */
	private void addMultipleQuestion(FiniteStateMachine g, Question qu) {
		int[] inputs = { 1, 8, 10, 0 };
		int originalSize = g.size();
		int m = qu.getMax(); // we add one to allow for a write-in option at
								// the end of the voting. This allows more
								// choices than candidates.
								// String[] wavs = new String[2];
		String qId = qu.getId();

		int firstVoteForThisQuestion = g.size();

		// int mInc = 5;
		Answer[] a = qu.getOrderedAnswers();
		String[] wavs;
		for (int j = 1; j <= m; j++) {
			inputs = new int[a.length + 4];
			Print.debug("Adding each question");
			int wavsSize = 8 + a.length;
			int wavsOffset = 0;
			if (j == 1) {
				// if we're on the first choice of this question:
				// then we need to tell them how many candidates there are in
				// this contest.
				// We need to add four files for that.
				wavsSize += 4;
				wavsOffset = 4;

			}
			// wavs = new String[1 * a.length + 2 + mInc];
			Vector<String> wavsVector = new Vector<String>();
			// wavs = new String[wavsSize];

			wavsVector.add("extras/" + (Integer.parseInt(qu.getId()) + 1)
					+ "th.mp3");
			wavsVector.add("extras/contest.mp3");
			wavsVector.add("extras/newline.mp3");
			wavsVector.add("extras/WhoIsYour.mp3");
			wavsVector.add("extras/" + j + "th.mp3");
			wavsVector.add("extras/choice.mp3");
			wavsVector.add("extras/q" + qId + "confirmation.mp3");
			wavsVector.add("extras/newline.mp3");
			if (Integer.parseInt(qId) > 0) {
				String ward = Globals.PROPERTIES.getProperty("Ward");
				ward = ward.substring(ward.length() - 1, ward.length());
				Print.debug("Ward number: " + ward);
				wavsVector.add("extras/" + ward + ".mp3");
			}
			wavsVector.add("extras/newline.mp3");
			if (wavsOffset != 0) {
				int numCandidates = qu.getMax() - 1;
				if (numCandidates == 1) {
					wavsVector.add("extras/ThereIs.mp3");
				} else if (numCandidates < 4) {
					wavsVector.add("extras/ThereAre.mp3");
				} else {
					throw new ScantegrityException(
							"Cannot support this number of candidates! Num candidates provided: "
									+ numCandidates);
				}

				wavsVector.add("extras/" + numCandidates + ".mp3");

				if (numCandidates == 1) {
					wavsVector.add("extras/candidate.mp3");
				} else {
					wavsVector.add("extras/candidates.mp3");
				}

				wavsVector.add("extras/ForThisContest.mp3");

			}

			wavsVector.add("extras/DoNotWishToEnter.mp3");
			// wavsVector.add("extras/SkipToBallotVerification.mp3");
			wavsVector.add("extras/newline.mp3");
			wavsVector.add("extras/newline.mp3");
			for (int i = 0; i < a.length; i++) {
				wavsVector.add(wardRelativePath + "a" + qId + "-"
						+ a[i].getId() + "_concat" + ".mp3");
				inputs[i] = i + 1;
				wavsVector.add("extras/newline.mp3");
			}

			wavsVector.add("extras/newline.mp3");
			wavsVector.add("extras/ReRankAll.mp3");
			wavsVector.add("extras/newline.mp3");
			wavsVector.add("extras/ToRepeatThisMessage.mp3");
			wavs = new String[wavsVector.size()];
			wavs = wavsVector.toArray(wavs);
			// Print.debug("TEST TEST TEST");
			Print.debug("Wavs: ");
			Print.debug(Arrays.toString(wavs));

			// inputs[a.length] = 7;
			inputs[a.length] = Globals.SKIP_CONTEST;
			// inputs[a.length + 1] = Globals.SKIP_TO_BALLOT_VERIFICATION;
			inputs[a.length + 1] = Globals.REPEAT_THIS_MESSAGE;
			// inputs[a.length + 3] = 9;
			inputs[a.length + 2] = Globals.TO_GO_BACK;

			// Choice state
			g.addState(inputs, electionDir, wavs, true, new StateAction() {
				public void doAction(int x) {

					try {
						// If we need to initialize temporaryVotes
						if (voteSubIndex == 0) {
							temporaryVotes = new int[votes[currentQuestion].length];
						}

						selectedVote = x - 1;
						// if (x == Globals.SKIP_CONTEST) {
						// temporaryVotes[voteSubIndex] = -1;
						// }
						// if (selectedVote < 4) {
						// making the answer wav files
						String inputFile;
						if (selectedVote == Globals.SKIP_CONTEST - 1
								|| selectedVote == Globals.SKIP_TO_BALLOT_VERIFICATION - 1) {
							// Print.debug("SKIPPING CONTEST");
							String tempInput;
							inputFile = new String(electionDir[0]
									+ "extras/NoOne" + "_"
									+ Globals.SOUND_SPEED + ".mp3");

							for (int i = voteSubIndex; i < temporaryVotes.length; i++) {
								temporaryVotes[i] = -1;
								tempInput = new String(electionDir[0]
										+ "extras/NoOne" + "_"
										+ Globals.SOUND_SPEED + ".mp3");
								Election.copyFile(tempInput, electionDir[1]
										+ "a" + currentQuestion + "+" + i
										+ ".mp3");
								Election.copyFile(
										tempInput.substring(0,
												tempInput.length() - 6)
												+ ".txt", electionDir[1] + "a"
												+ currentQuestion + "+" + i
												+ ".txt");
							}
						} else if (x == Globals.TO_GO_BACK) {
							DRE.clearKeyPresses();
							for (int i = 0; i < temporaryVotes.length; i++) {
								if (temporaryVotes[i] > 6) {
									File f = new File(electionDir[1]
											+ "../../writeIns/"
											+ temporaryVotes[i] + ".wav");
									if (f.exists()) {
										f.delete();
									}
								}
							}

							// Clean up the files that we've changed over the
							// course of
							// the
							// question.
							// Set everything back to normal.
							for (int i = 0; i < temporaryVotes.length; i++) {
								if (x == Globals.TO_CONTINUE)
									votes[currentQuestion - 1][i] = temporaryVotes[i];
								else
									votes[currentQuestion][i] = temporaryVotes[i];
							}

							resetAnswerFiles(currentQuestion,
									temporaryVotes.length);

							File recordingWav = new File(electionDir[1]
									+ "../../writeIns/recording.wav");
							if (recordingWav.exists()) {
								recordingWav.delete();
							}
							voteSubIndex = 0;
							return;
						}

						else if (x != Globals.REPEAT_THIS_MESSAGE) {
							inputFile = new String(electionDir[1] + "a"
									+ currentQuestion + "-" + selectedVote
									+ "_" + Globals.SOUND_SPEED + ".mp3");

							Election.copyFile(inputFile, electionDir[1] + "a"
									+ currentQuestion + "+" + voteSubIndex
									+ ".mp3");

							Election.copyFile(
									inputFile.substring(0,
											inputFile.length() - 6) + ".txt",
									electionDir[1] + "a" + currentQuestion
											+ "+" + voteSubIndex + ".txt"); // The
																			// -6
																			// is
																			// to
																			// remove
																			// both
																			// the
																			// file
																			// extension
																			// and
																			// the
																			// speed
																			// designation.
						}
						if (x < 5 && x > 0) {
							temporaryVotes[voteSubIndex] = selectedVote;
							DRE.addKeyPress(x);

						}
						// }
						// else if (x == 9) {
						// // clean up any recordings made in this contest.
						// for (int i = 0; i < temporaryVotes.length; i++) {
						// if (temporaryVotes[i] > 6) {
						// File f = new File(electionDir[0]
						// + "writeIns/" + temporaryVotes[i]
						// + ".wav");
						// if (f.exists()) {
						// f.delete();
						// }
						// }
						// }
						// }

					}

					catch (Exception e) {
						e.printStackTrace();
					}
				}
			});

			if (j == 1)
				questionIndicies[Integer.parseInt(qu.getId())][0] = g.size() - 1;

			// transition to the first vote for this question from the
			// individual choice states
			g.addTransition(g.size() - 1, firstVoteForThisQuestion,
					Globals.TO_GO_BACK);

			// transition from the previous individual confirmation state.
			g.addTransition(g.size() - 2, g.size() - 1, Globals.TO_CONTINUE);

			// to restart the votes.
			// g.addTransition(g.size() - 1, firstVoteForThisQuestion, 9);

			// WRITE-IN OPTIONS
			addWriteInOption(g, firstVoteForThisQuestion, qu);

			// TODO Check this for errors.
			g.addTransition(g.size() - 4, g.size() - 3, Globals.TO_CONTINUE); // transition
																				// from
			// previous write-in
			// confirmation to
			// this question.

			// adding the second state now (the confirmation one)
			inputs = new int[3];
			inputs[0] = Globals.TO_CONTINUE;
			inputs[1] = Globals.TO_GO_BACK;
			inputs[2] = Globals.REPEAT_THIS_MESSAGE;

			// wavs = new String[6];
			// wavs[0] = "extras/YouHaveSelected.mp3";
			// wavs[1] = "extras/Your.mp3";
			// wavs[2] = "extras/" + j + "th.mp3";
			// wavs[3] = "extras/choice.mp3";
			// wavs[4] = "a" + qu.getId() + "+" + (j - 1) + ".mp3";
			// wavs[5] = "extras/ChoiceConfirmation.mp3";
			//
			// wavs = new String[9];
			Vector<String> wavsVector2 = new Vector<String>();
			wavsVector2.add("extras/YouHaveSelected.mp3");
			wavsVector2.add(wardRelativePath + "a" + qu.getId() + "+" + (j - 1)
					+ ".mp3");
			wavsVector2.add("extras/AsYour.mp3");
			wavsVector2.add("extras/" + j + "th" + ".mp3");
			wavsVector2.add("extras/choice.mp3");
			wavsVector2.add("extras/q" + qu.getId() + "confirmation" + ".mp3");
			if (Integer.parseInt(qId) > 0) {
				String ward = Globals.PROPERTIES.getProperty("Ward");
				ward = ward.substring(ward.length() - 1, ward.length());
				Print.debug("Ward number: " + ward);
				wavsVector2.add("extras/" + ward + ".mp3");
			}
			wavsVector2.add("extras/newline.mp3");
			wavsVector2.add("extras/newline.mp3");
			wavsVector2.add("extras/ToChangeSelection.mp3");
			wavsVector2.add("extras/newline.mp3");
			wavsVector2.add("extras/ToContinue.mp3");
			wavsVector2.add("extras/newline.mp3");
			wavsVector2.add("extras/ToRepeatThisMessage.mp3");

			wavs = new String[wavsVector2.size()];
			wavs = wavsVector2.toArray(wavs);

			g.addState(inputs, electionDir, wavs, true, new StateAction() {
				public void doAction(int x) {
					// if (x == 1)
					// votes[currentQuestion][voteSubIndex++] = selectedVote;
					Print.debug("tempVotes: " + Arrays.toString(temporaryVotes));
					if (x == Globals.TO_CONTINUE) {
						// Accepted their vote.

						String ansFilePath = electionDir[1] + "a"
								+ currentQuestion + "-" + selectedVote
								+ "_concat" + "_" + Globals.SOUND_SPEED
								+ ".mp3";

						// Rename the answer file, copy the null file into the
						// answer file.
						File answerFile = new File(ansFilePath);
						answerFile.renameTo(new File(answerFile
								.getAbsolutePath() + ".temp"));

						String nullFilePath = electionDir[0]
								+ "extras/newline_1.mp3";
						Election.copyFile(nullFilePath, ansFilePath);

						String ansTxtFilePath = electionDir[1] + "a"
								+ currentQuestion + "-" + selectedVote
								+ "_concat" + ".txt";

						// Rename the answer file, copy the null file into the
						// answer file.
						File answerTxtFile = new File(ansTxtFilePath);
						answerTxtFile.renameTo(new File(answerTxtFile
								.getAbsolutePath() + ".temp"));

						String nullTxtFilePath = electionDir[0]
								+ "extras/newline.txt";
						Election.copyFile(nullTxtFilePath, ansTxtFilePath);

					}
					if (x == Globals.SKIP_TO_BALLOT_VERIFICATION) {
						temporaryVotes[voteSubIndex] = -1;
						// skipped to the end.
					}
					if (x == Globals.TO_GO_BACK) {
						Print.debug("GOING BACK");
						// the voter wants to go back
						if (temporaryVotes[voteSubIndex] > 6) {
							File f = new File(electionDir[1] + "../"
									+ "../writeIns/recording.wav");
							if (f.exists()) {
								f.delete();
							}

							f = new File(electionDir[1] + "../../writeIns/"
									+ temporaryVotes[voteSubIndex] + ".wav");
							if (f.exists()) {
								f.delete();
							}

						}
						Print.debug("Popping a keypress!");
						voteSubIndex--;
						DRE.popKeyPress();
						Print.debug("Done popping a keypress!");
					}
					if (x != Globals.REPEAT_THIS_MESSAGE) {
						voteSubIndex++;
					}
				}
			});
			// add transitions from various vote states.
			for (int i = 0; i < a.length - 1; i++)
				g.addTransition(g.size() - 4, g.size() - 1, (i + 1));
			for (int l = originalSize - 1; l < g.size(); l++)
				g.addTransition(l, g.size(), Globals.SKIP_CONTEST);
			g.addTransition(g.size() - 1, g.size() - 4, Globals.TO_GO_BACK);

			// from the write-in.
			g.addTransition(g.size() - 2, g.size() - 1, Globals.TO_CONTINUE);
		}

		// adding the confirmation state now
		inputs = new int[3];

		// wavs = new String[3 + (qu.getMax() * 6)];
		inputs[0] = Globals.TO_CONTINUE;
		inputs[1] = Globals.TO_GO_BACK;
		inputs[2] = Globals.REPEAT_THIS_MESSAGE;

		// wavs[0] = "extras/YouHaveSelected.mp3";
		Vector<String> wavsVector3 = new Vector<String>();

		for (int j = 0; j < qu.getMax(); j++) {
			wavsVector3.add("extras/YouHaveSelected.mp3");
			wavsVector3.add(wardRelativePath + "a" + qu.getId() + "+" + (j)
					+ ".mp3");
			wavsVector3.add("extras/AsYour.mp3");
			wavsVector3.add("extras/" + (j + 1) + "th.mp3");
			wavsVector3.add("extras/choice.mp3");
			wavsVector3.add("extras/q" + qu.getId() + "confirmation.mp3");
			if (Integer.parseInt(qId) > 0) {
				String ward = Globals.PROPERTIES.getProperty("Ward");
				ward = ward.substring(ward.length() - 1, ward.length());
				Print.debug("Ward number: " + ward);
				wavsVector3.add("extras/" + ward + ".mp3");
			}
			wavsVector3.add("extras/newline.mp3");
			wavsVector3.add("extras/newline.mp3");
		}

		wavsVector3.add("extras/ReRankAll.mp3");
		wavsVector3.add("extras/newline.mp3");
		wavsVector3.add("extras/ToContinue.mp3");
		wavsVector3.add("extras/newline.mp3");
		wavsVector3.add("extras/ToRepeatThisMessage.mp3");

		wavs = new String[wavsVector3.size()];
		wavs = wavsVector3.toArray(wavs);
		Print.debug(Arrays.toString(wavs));

		g.addState(inputs, electionDir, wavs, true, new StateAction() {
			public void doAction(int x) {
				try {
					if (x != Globals.REPEAT_THIS_MESSAGE) {
						DRE.clearKeyPresses();
						int curQuestion = currentQuestion;
						if (x == Globals.TO_CONTINUE) {
							votes[currentQuestion] = new int[temporaryVotes.length];
							for (int i = 0; i < temporaryVotes.length; i++) {
								votes[currentQuestion][i] = temporaryVotes[i];
								Print.debug("votes: "
										+ Arrays.toString(votes[currentQuestion]));
							}

							if (qToGoBackTo != 0) {
								currentQuestion = numQuestions - 1;
							}

							currentQuestion++;
						} else if (x == Globals.TO_GO_BACK) {
							for (int i = 0; i < temporaryVotes.length; i++) {
								if (temporaryVotes[i] > 6) {
									File f = new File(electionDir[1]
											+ "../../writeIns/"
											+ temporaryVotes[i] + ".wav");
									if (f.exists()) {
										f.delete();
									}
								}
							}
						}

						// Clean up the files that we've changed over the course
						// of
						// the
						// question.
						// Set everything back to normal.
						for (int i = 0; i < temporaryVotes.length; i++) {
							if (x == Globals.TO_CONTINUE)
								votes[currentQuestion - 1][i] = temporaryVotes[i];
							else
								votes[currentQuestion][i] = temporaryVotes[i];
						}

						resetAnswerFiles(curQuestion, temporaryVotes.length);

						File recordingWav = new File(electionDir[1]
								+ "../../writeIns/recording.wav");
						if (recordingWav.exists()) {
							recordingWav.delete();
						}
						voteSubIndex = 0;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		questionIndicies[Integer.parseInt(qu.getId())][1] = g.size() - 1;

		for (int i = 0; i < questionIndicies.length; i++) {
			Print.debug("questionIndicies[" + i + "]: "
					+ Arrays.toString(questionIndicies[i]));
		}

		// // add transitions from last state, leaving room for write-ins
		// for (int i = 1; i < 9; i++) {
		// if (i != 7) {
		// g.addTransition(g.size() - 4, g.size() - 1, i);
		// }
		// }

		g.addTransition(g.size() - 2, g.size() - 1, Globals.TO_CONTINUE);

		g.addTransition(g.size() - 1, firstVoteForThisQuestion,
				Globals.TO_GO_BACK);

		// need to add "skip" transition from all previous questions
		// TODO This shouldn't be hardcoded?
		for (int j = 2; j <= m + 2; j++)
			g.addTransition(g.size() - j, g.size() - 1, Globals.SKIP_CONTEST);
		g.addTransition(firstVoteForThisQuestion, g.size() - 1,
				Globals.SKIP_CONTEST);
	}

	private void resetAnswerFiles(int question, int numAnswers) {
		for (int i = 0; i < numAnswers; i++) {
			// votes[currentQuestion - 1][i] = temporaryVotes[i];

			String oldAnsFilePath = electionDir[1] + "a" + (question) + "-" + i
					+ "_concat" + "_" + Globals.SOUND_SPEED + ".mp3";

			String ansFilePath = oldAnsFilePath + ".temp";

			// Rename the old answer file.
			File answerFile = new File(ansFilePath);
			File oldAnswerFile = new File(oldAnsFilePath);

			if (answerFile.exists() && oldAnswerFile.exists()) {
				boolean deleteSuccess = oldAnswerFile.delete();
				if (!deleteSuccess) {
					Print.debug("Could not delete file: " + ansFilePath);
				}
				copyFile(ansFilePath, oldAnsFilePath);
				// answerTxtFile.renameTo(new
				// File(oldAnsTxtFilePath));
				deleteSuccess = answerFile.delete();
				if (!deleteSuccess) {
					Print.debug("Could not delete file: " + ansFilePath);
				}
			}

			String oldAnsTxtFilePath = electionDir[1] + "a" + (question) + "-"
					+ i + "_concat" + ".txt";

			String ansTxtFilePath = oldAnsTxtFilePath + ".temp";

			// Rename the old answer file.
			File answerTxtFile = new File(ansTxtFilePath);
			File oldAnswerTxtFile = new File(oldAnsTxtFilePath);

			if (answerTxtFile.exists() && oldAnswerTxtFile.exists()) {
				boolean deleteSuccess = oldAnswerTxtFile.delete();
				if (!deleteSuccess) {
					Print.debug("Could not delete file: " + ansFilePath);
				}

				copyFile(ansTxtFilePath, oldAnsTxtFilePath);
				// answerTxtFile.renameTo(new
				// File(oldAnsTxtFilePath));
				deleteSuccess = answerTxtFile.delete();
				if (!deleteSuccess) {
					Print.debug("Could not delete file: " + ansFilePath);
				}
			}

		}
	}

	/**
	 * Adds a ranked question to the FMS
	 * 
	 * @param g
	 * @param qu
	 */
	public void addRankedQuestion(FiniteStateMachine g, Question qu) {
		// System.out.println("***RANKED question NOT IMPL YET***");
		addMultipleQuestion(g, qu);
	}

	/**
	 * Adds a simple question to the FSM
	 * 
	 * @param g
	 * @param qu
	 */
	public void addSimpleQuestion(FiniteStateMachine g, Question qu) {
		Print.debug("Adding simple question");
		String[] wavs;
		int[] inputs;
		Answer[] a = qu.getOrderedAnswers();
		wavs = new String[4 * a.length + 2];
		inputs = new int[a.length + 4];

		String qId = qu.getId();
		wavs[0] = new String("q" + qId + ".mp3");

		for (int i = 0; i < a.length; i++) {
			wavs[4 * i + 1] = new String("extras/ToVoteFor.mp3");
			wavs[4 * i + 2] = new String("a" + qId + "-" + a[i].getId()
					+ ".mp3");
			wavs[4 * i + 3] = new String("extras/press.mp3");
			wavs[4 * i + 4] = new String("extras/" + (i + 1) + ".mp3");
			inputs[i] = i + 1;
		}

		wavs[wavs.length - 1] = "extras/vote-instructions.mp3";

		inputs[a.length] = 7;
		inputs[a.length + 1] = 8;
		inputs[a.length + 2] = 0;
		inputs[a.length + 3] = 10;

		g.addState(inputs, electionDir, wavs, true, new StateAction() {
			public void doAction(int x) {
				selectedVote = x - 1;

				if (selectedVote != 9 && selectedVote != 6) { // changed to
																// allow
																// pressing 7
																// for write-in
					// making the answer wav file
					String inputFile;

					if (selectedVote == 7 || selectedVote == -1) {
						inputFile = new String(electionDir[0] + "extras/NoOne"
								+ "_" + Globals.SOUND_SPEED + ".mp3");
						selectedVote = -1;
					} else
						inputFile = new String(electionDir[1] + "a"
								+ currentQuestion + "-" + selectedVote + ".mp3");

					Election.copyFile(inputFile, electionDir[1] + "a"
							+ currentQuestion + ".mp3");
					Election.copyFile(
							inputFile.substring(0, inputFile.length() - 3)
									+ "txt", electionDir[1] + "a"
									+ currentQuestion + ".txt");
				} // else ignore
			}
		});

		int beginQIndex = g.size() - 1;

		addTransitionsFromPrevQ(g);

		// ADDING WRITE-IN OPTION HERE
		addWriteInOption(g, beginQIndex, qu);

		// adding the second state now (the confirmation one)
		inputs = new int[3];
		wavs = new String[3];
		inputs[0] = 1;
		inputs[1] = 3;
		inputs[2] = 10;
		wavs[0] = "extras/YouHaveSelected.mp3";
		wavs[1] = "a" + qId + ".mp3";
		wavs[2] = "extras/VoteConfirmation.mp3";

		g.addState(inputs, electionDir, wavs, true, new StateAction() {
			public void doAction(int x) {
				if (x == 1)
					votes[currentQuestion++][0] = selectedVote;
				if (x == 8)
					votes[currentQuestion++][0] = -1;
			}
		});
		for (int i = 0; i < a.length; i++)
			g.addTransition(g.size() - 4, g.size() - 1, (i + 1));
		g.addTransition(g.size() - 4, g.size() - 1, 8);
		g.addTransition(g.size() - 1, g.size() - 4, 3);
	}

	public void addWriteInOption(FiniteStateMachine g, int beginQIndex,
			Question qu) {
		getProperties();
		Print.debug("Properties: " + properties);
		final int captureTime = Integer.parseInt(properties
				.getProperty("CaptureLength"));

		Print.debug("Adding write-in option.");
		int[] input = new int[3];
		input[0] = Globals.TO_CONTINUE;
		input[1] = Globals.TO_GO_BACK;
		input[2] = Globals.REPEAT_THIS_MESSAGE;
		String[] wavs = new String[4];
		wavs[0] = "extras/Remember.mp3";
		wavs[1] = "extras/ToChangeSelection.mp3";
		wavs[2] = "extras/ToRecord.mp3";
		wavs[3] = "extras/ToRepeatThisMessage.mp3";

		g.addState(input, electionDir, wavs, true, new StateAction() {
			public void doAction(int x) {
				if (x == Globals.TO_CONTINUE) {
					File recording = new File(electionDir[1]
							+ "writeIns/recording.wav");
					if (recording.exists()) {
						// Tell them they can't record another one.
						Print.file(electionDir[0]
								+ "extras/AlreadyHaveRecording.txt");
						AudioPlayer ap = AudioPlayerFactory.getAudioPlayer(
								electionDir[0] + "extras/AlreadyHaveRecording"
										+ "_" + Globals.SOUND_SPEED + ".mp3",
								false);
						Long time = ap.getDuration();

						ap.startPlaying();
						try {
							Thread.sleep(time);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}

					else {
						// they can go right ahead and record a write-in vote.
						// Print.debug("Start recording! Beep!");
						Print.clearTextArea();
						Print.file(electionDir[0] + "extras/NowRecord.txt");
						AudioPlayer ap = AudioPlayerFactory.getAudioPlayer(
								electionDir[0] + "extras/NowRecord" + "_"
										+ Globals.SOUND_SPEED + ".mp3", false);
						Long time = ap.getDuration();
						Print.debug("Recording sleeptime: " + time);
						ap.startPlaying();
						try {
							Thread.sleep(time);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}

						String beepFileName = electionDir[0]
								+ "extras/beep.mp3";
						AudioPlayer ap2 = AudioPlayerFactory.getAudioPlayer(
								beepFileName, false);
						Long time2 = ap2.getDuration();
						Print.debug("Beep sleeptime: " + time2);
						ap2.startPlaying();

						try {
							Thread.sleep(time2);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}

						try {
							AudioCapture.captureAndSaveToFile(captureTime,
									electionDir[1]
											+ "../../writeIns/recording.wav");
						} catch (LineUnavailableException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO update this.
							e.printStackTrace();
						}
						try {
							Thread.sleep((captureTime + 1) * 1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						ap.openFileNoSpeedOption(beepFileName);
						time = ap.getDuration();
						ap.startPlaying();
						try {
							Thread.sleep(time);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						Print.debug("Stop recording! Beep!");
						Print.clearTextArea();
					}
					// Will eventually play an audio file here. This is just for
					// testing.
					// Print.debug("Press 1 to continue, press 3 to go back!");
				} else if (x == Globals.TO_GO_BACK) {
					DRE.popKeyPress();
				}
			}
		});

		// transition from pressing the last candidate key in the vote state
		g.addTransition(g.size() - 2, g.size() - 1, qu.getMax()); // TODO

		// transition if you do not want to record, and instead go back.
		g.addTransition(g.size() - 1, g.size() - 2, Globals.TO_GO_BACK);

		input = new int[3];
		input[0] = Globals.TO_CONTINUE;
		input[1] = Globals.TO_GO_BACK;
		input[2] = Globals.REPEAT_THIS_MESSAGE;
		// input[3] = 10;
		// input[4] = 2;
		wavs = new String[5];

		// wavs[0] = "extras/WaitTillEnd.mp3";
		wavs[0] = "extras/Replaying.mp3";
		wavs[1] = "../writeIns/recording.wav";

		wavs[2] = "extras/ToRerecord.mp3";
		wavs[3] = "extras/HowWriteInProcessed.mp3";
		wavs[4] = "extras/ToContinue.mp3";
		// wavs[4] = "extras/GoAhead.mp3";

		g.addState(input, electionDir, wavs, true, new StateAction() {
			public void doAction(int x) {
				if (x == Globals.TO_CONTINUE) {
					String oldAnswerFilePath = electionDir[1] + "a"
							+ currentQuestion + "+" + voteSubIndex + ".mp3";
					File oldAnswerFile = new File(oldAnswerFilePath);
					if (oldAnswerFile.exists()) {
						oldAnswerFile.delete();
					}

					String oldAnswerFilePathWav = electionDir[1] + "a"
							+ currentQuestion + "+" + voteSubIndex + ".wav";
					File oldAnswerFileWav = new File(oldAnswerFilePathWav);
					if (oldAnswerFile.exists()) {
						oldAnswerFile.delete();
					}

					String currentAnswerFile = electionDir[1] + "a"
							+ currentQuestion + "+" + voteSubIndex + ".wav";
					copyFile(electionDir[1] + "../../writeIns/recording.wav",
							currentAnswerFile);
					// if they've already recorded a WIV for this contest, but
					// want to enter it again.
					for (int i = 0; i < temporaryVotes.length; i++) {
						if (temporaryVotes[i] > 6) {
							temporaryVotes[voteSubIndex] = temporaryVotes[i];

							return;

						}
					}

					Random r = new Random();
					File f;
					int random = 0;
					do {
						random = r.nextInt();
						f = new File(electionDir[1] + "../../writeIns/"
								+ Integer.toString(random) + ".wav");
					} while (random < 6 || f == null || f.exists());

					// int hashOfTime = Math.abs(new Long(System
					// .currentTimeMillis()).hashCode()) + 5;
					// if (hashOfTime < 0)
					// hashOfTime = Integer.MAX_VALUE;

					// String newFileName = Integer.toString(hashOfTime);
					String newFileName = electionDir[1] + "../../writeIns/"
							+ Integer.toString(random) + ".wav";
					copyFile(electionDir[1] + "../../writeIns/recording.wav",
							newFileName);

					temporaryVotes[voteSubIndex] = random; // changed to
															// allow
															// write-ins
															// to
															// actually
															// be
															// candidates.
					// this variable is incremented in the state below, so it
					// must be one less than the current value.
				} else if (x == Globals.TO_GO_BACK) {
					boolean alreadyWrittenIn = false;
					for (int i = 0; i < temporaryVotes.length; i++) {
						if (temporaryVotes[i] > 6) {
							alreadyWrittenIn = true;
						}
					}
					if (!alreadyWrittenIn) {

						// make sure to delete the old recording, but only if
						// there hasn't already been a WI-vote.
						File originalRecording = new File(electionDir[1]
								+ "../writeIns/recording.wav");
						if (originalRecording.exists()) {
							originalRecording.delete();
						}
					}
				} 
				//else if (x == 9) {
//					for (int i = 0; i < temporaryVotes.length; i++) {
//						if (temporaryVotes[i] > 6) {
//							File f = new File(electionDir[1]
//									+ "../../writeIns/" + temporaryVotes[i]
//									+ ".wav");
//							if (f.exists()) {
//								f.delete();
//							}
//						}
//					}
//
//					// make sure to delete the old recording
//					File originalRecording = new File(electionDir[1]
//							+ "../../writeIns/recording.wav");
//					if (originalRecording.exists()) {
//						originalRecording.delete();
//
//					}
				//}

			}
		});

		// transition back to the record state if you don't like your recording.
		g.addTransition(g.size() - 1, g.size() - 2, Globals.TO_GO_BACK);

		// //for replay
		// g.addTransition(g.size() - 1, g.size() - 1, 2);

		// g.addTransition(g.size() - 1, beginQIndex, );

		// transition for going to the confirm state.
		g.addTransition(g.size() - 2, g.size() - 1, Globals.TO_CONTINUE);
	}

	private void addTransitionsFromPrevQ(FiniteStateMachine g) {
		// adding in the transition from the previous confirmation state to the
		// current
		// question
		g.addTransition(g.size() - 2, g.size() - 1, Globals.TO_CONTINUE);

		// add from the write-in state
		if (g.size() > 4) {
			g.addTransition(g.size() - 3, g.size() - 1, Globals.TO_CONTINUE);
		}
	}

	/**
	 * Static method that just clones a file
	 * 
	 * @param input
	 *            input filename
	 * @param output
	 *            output filename
	 */
	public static void copyFile(String inputPath, String outputPath) {
		Print.debug("Copying file from " + inputPath + " to " + outputPath);

		File output = new File(outputPath);
		File input = new File(inputPath);
		if (output.exists()) {
			output.delete();
		}
		if (!output.exists()) {
			try {
				output.createNewFile();
			} catch (IOException e) {
				Print.debug("Could not create file: " + outputPath);
				e.printStackTrace();
			}
		}

		FileChannel source = null;
		FileChannel destination = null;

		try {
			source = new FileInputStream(input).getChannel();
			destination = new FileOutputStream(output).getChannel();
			destination.transferFrom(source, 0, source.size());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (source != null) {
					source.close();
				}
				if (destination != null) {
					destination.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void copyFile(File input, File output) {
		copyFile(input.getAbsolutePath(), output.getAbsolutePath());
	}

	/**
	 * Get the votes
	 * 
	 * @return votes array
	 */
	public int[][] getVotes() {
		return votes;
	}

	/**
	 * Initializez and starts the finite state machine for this election
	 * 
	 */
	public void startElection() {
		Print.debug(fsm.printTransitionsToString());
		fsm.setInputStream(System.in);
		fsm.start();
	}
}
