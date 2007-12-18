/**
 * Copyright (c) 2005-2007, KoLmafia development team
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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TelescopeRequest
	extends KoLRequest
{
	public static final int HIGH = 1;
	public static final int LOW = 2;
	private static final Pattern WHERE_PATTERN = Pattern.compile( "action=telescope([^?]*)" );

	private final int where;

	/**
	 * Constructs a new <code>TelescopeRequest</code>
	 */

	public TelescopeRequest( final int where )
	{
		super( "campground.php" );

		this.where = where;
		switch ( where )
		{
		case HIGH:
			this.addFormField( "action", "telescopehigh" );
			break;
		case LOW:
			this.addFormField( "action", "telescopelow" );
			break;
		}
	}

	public void run()
	{
		if ( KoLCharacter.getTelescopeUpgrades() < 1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have a telescope" );
			return;
		}

		if ( KoLCharacter.inBadMoon() )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Your telescope is unavailable in Bad Moon" );
			return;
		}

		if ( this.where != TelescopeRequest.HIGH && this.where != TelescopeRequest.LOW )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't look there." );
			return;
		}

		KoLmafia.updateDisplay( "Looking through your telescope..." );

		super.run();
	}

	private static final Pattern[] PATTERNS = {
	// "You focus the telescope on the entrance of the cave, and
	// see a wooden gate with an elaborate carving of <description>
	// on it."
	Pattern.compile( "carving of (.*?) on it." ),

	// "You raise the telescope a little higher, and see a window
	// at the base of a tall brick tower. Through the window, you
	// <description>."
	Pattern.compile( "Through the window, you (.*?)\\." ),

	// "Further up, you see a second window. Through this one, you
	// <description>."
	Pattern.compile( "Through this one, you (.*?)\\." ),

	// "Even further up, you see a third window. Through it you
	// <description>."
	Pattern.compile( "Through it you (.*?)\\." ),

	// "Looking still higher, you see another window. Through the
	// fourth window you <description>."
	Pattern.compile( "Through the fourth window you (.*?)\\." ),

	// "Even further up, you see a fifth window. Through that one
	// you <description>."
	Pattern.compile( "Through that one you (.*?)\\." ),

	// "Near the top of the tower, you see a sixth and final
	// window. Through it you <description>."
	Pattern.compile( "final window. *Through it you (.*?)\\." ), };

	public void processResults()
	{
		if ( this.where == TelescopeRequest.HIGH )
		{
			// "You've already peered into the Heavens
			// today. You're already feeling as inspired as you can
			// be for one day."
			if ( this.responseText.indexOf( "already peered" ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You've already done that today." );
				return;
			}

			// Let regular effect parsing detect Starry-Eyed effect.
			super.processResults();
			return;
		}

		// We looked low. Deduce how many upgrades our telescope has
		// and save what we spied in the tower.

		KoLSettings.setUserProperty( "lastTelescopeReset", String.valueOf( KoLCharacter.getAscensions() ) );

		int upgrades = 0;

		for ( int i = 0; i < TelescopeRequest.PATTERNS.length; ++i )
		{
			Matcher matcher = TelescopeRequest.PATTERNS[ i ].matcher( this.responseText );
			if ( !matcher.find() )
			{
				break;
			}

			upgrades++ ;
			KoLSettings.setUserProperty( "telescope" + upgrades, matcher.group( 1 ) );
		}

		KoLCharacter.setTelescopeUpgrades( upgrades );
		KoLSettings.setUserProperty( "telescopeUpgrades", String.valueOf( upgrades ) );
	}

	public static final boolean registerRequest( final String urlString )
	{
		Matcher matcher = TelescopeRequest.WHERE_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return false;
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "telescope look " + matcher.group( 1 ) );

		return true;
	}
}
