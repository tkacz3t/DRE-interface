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
package edu.gwu.election.fsm;

/**
 * Interface to allow passing methods as parameters. 
 * Used for specifying the action for each state. 
 * @author Alex Florescu
 *
 */
public interface StateAction {
    void doAction(int input);
}
