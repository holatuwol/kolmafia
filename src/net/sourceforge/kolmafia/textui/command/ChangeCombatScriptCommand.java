/**
 * Copyright (c) 2005-2009, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import java.util.Iterator;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.session.CustomCombatManager;

public class ChangeCombatScriptCommand
	extends AbstractCommand
{
	public ChangeCombatScriptCommand()
	{
		this.usage = " [<script>] - show [or select] Custom Combat Script in use.";
	}

	public void run( final String command, String parameters )
	{
		update( parameters );
	}
	
	public static void update( String parameters )
	{
		if ( parameters.length() > 0 )
		{
			parameters = parameters.toLowerCase();

			boolean foundScript = false;
			Iterator iterator = CustomCombatManager.getAvailableScripts().iterator();

			while ( iterator.hasNext() && !foundScript )
			{
				String script = (String) iterator.next();

				if ( script.toLowerCase().indexOf( parameters ) != -1 )
				{
					foundScript = true;
					CustomCombatManager.setScript( script );
					KoLmafia.updateDisplay( "CCS set to " + CustomCombatManager.getScript() );
				}
			}

			if ( !foundScript )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "No matching CCS found!" );
				return;
			}
		}
		else
		{
			KoLmafia.updateDisplay( "CCS is " + CustomCombatManager.getScript() );
		}

		String battleAction = Preferences.getString( "battleAction" );

		if ( !battleAction.startsWith( "custom" ) )
		{
			KoLmafia.updateDisplay( "(but battle action is currently set to " + battleAction + ")" );
		}
	}
}