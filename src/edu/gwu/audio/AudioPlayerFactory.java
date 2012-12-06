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

import edu.gwu.election.Print;

public class AudioPlayerFactory {
	public AudioPlayerFactory(){}
	
	/**
	 * @return the appropriate AudioPlayer Object based on the suffix of the filename given. 
	 * Returns null if the filetype is invalid. This method does not look at the file header or anything beyond the last few letters of the file name. 
	 * This method and class can be updated if and when we add support for more audio file types. 
	 */
	public static AudioPlayer getAudioPlayer(String file, boolean speedOption) {
		AudioPlayer player=null;
		
		String[] fileName = file.split("\\.");
		String suffix = fileName[fileName.length - 1]; //the last part of the filename, should be the file extension. 
		
		
		//TODO: in case of audioplayerexception, try automatically creating a different player
		if(suffix.compareToIgnoreCase("wav") == 0){
			player = new AudioPlayerWav(file, speedOption);
		}		
		else if(suffix.compareToIgnoreCase("mp3") == 0){
			player = new AudioPlayerJlGUI(file, speedOption);
		}
		else {
			Print.debug("NO COMPATIBLE AUDIO PLAYER: " + file);
			throw new NoCompatibleAudioPlayerException();
		}
		return player;
	}
}
