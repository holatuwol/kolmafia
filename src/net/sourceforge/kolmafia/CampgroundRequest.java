/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

/**
 * A lot of information about the character can only be found out
 * by requesting the campground.  Things like bartender-in-the-boxes
 * and the toaster can only be discovered here.
 */

public class CampgroundRequest extends KoLRequest
{
	private String action;

	/**
	 * Constructs a new <code>CampgroundRequest</code>.
	 * @param	client	The client to be notified of all the information parsed
	 */

	public CampgroundRequest( KoLmafia client )
	{
		super( client, "campground.php" );
		this.action = "";
	}

	/**
	 * Constructs a new <code>CampgroundRequest</code> with the
	 * specified action in mind.
	 */

	public CampgroundRequest( KoLmafia client, String action )
	{
		super( client, "campground.php" );
		addFormField( "action", action );
		this.action = action;
	}

	/**
	 * Runs the campground request, updating the client as appropriate.
	 */

	public void run()
	{
		super.run();

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( isErrorState || responseCode != 200 )
			return;

		// For now, just set whether or not you have a
		// bartender and chef.

		client.getCharacterData().setChef( replyContent.indexOf( "cook.php" ) != -1 );
		client.getCharacterData().setBartender( replyContent.indexOf( "cocktail.php" ) != -1 );

		processResults( replyContent.substring( 0, replyContent.indexOf( "Your Campsite" ) ) );

		// Update adventure tally for resting and relaxing
		// at the campground.

		if ( action.equals( "rest" ) )
		{
			if ( replyContent.indexOf( "You sleep" ) != -1 )
				client.addToResultTally( new AdventureResult( AdventureResult.ADV, -1 ) );
			else
			{
				updateDisplay( KoLFrame.ENABLED_STATE, "Could not rest." );
				client.cancelRequest();
			}
		}
		else if ( action.equals( "relax" ) )
		{
			if ( replyContent.indexOf( "You relax" ) != -1 )
				client.addToResultTally( new AdventureResult( AdventureResult.ADV, -1 ) );
			else
			{
				updateDisplay( KoLFrame.ENABLED_STATE, "Could not relax." );
				client.cancelRequest();
			}
		}
	}
}