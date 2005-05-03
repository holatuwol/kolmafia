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

import java.util.regex.Pattern;

public class UseSkillRequest extends KoLRequest
{
	private int buffCount;
	private int consumedMP;
	private String target;
	private String skillName;

	/**
	 * Constructs a new <code>UseSkillRequest</code>.
	 * @param	client	The client to be notified of completion
	 * @param	skillName	The name of the skill to be used
	 * @param	target	The name of the target of the skill
	 * @param	buffCount	The number of times the target is affected by this skill
	 */

	public UseSkillRequest( KoLmafia client, String skillName, String target, int buffCount )
	{
		super( client, "skills.php" );
		addFormField( "action", "Skillz." );
		addFormField( "pwd", client.getPasswordHash() );

		int skillID = ClassSkillsDatabase.getSkillID( skillName );
		this.skillName = ClassSkillsDatabase.getSkillName( skillID );
		addFormField( "whichskill", String.valueOf( skillID ) );

		if ( ClassSkillsDatabase.isBuff( skillID ) )
		{
			addFormField( "bufftimes", "" + buffCount );

			if ( target == null || target.trim().length() == 0 )
			{
				if ( client.getCharacterData().getUserID() != 0 )
					addFormField( "targetplayer", "" + client.getCharacterData().getUserID() );
				else
					addFormField( "specificplayer", client.getLoginName() );
			}
			else
				addFormField( "specificplayer", target );
		}
		else
			addFormField( "quantity", "" + buffCount );

		this.target = target;
		this.buffCount = buffCount < 1 ? 1 : buffCount;
		this.consumedMP = ClassSkillsDatabase.getMPConsumptionByID( skillID ) * buffCount;
	}

	public int getBuffCount()
	{	return buffCount;
	}

	public String getSkillName()
	{	return skillName;
	}

	public String toString()
	{	return skillName + " (" + consumedMP + " mp)";
	}

	public void run()
	{
		if ( target == null || target.trim().length() == 0 )
			updateDisplay( DISABLED_STATE, "Casting " + skillName + "..." );
		else
			updateDisplay( DISABLED_STATE, "Casting " + skillName + " on " + target );

		super.run();

		// If a reply was obtained, check to see if it was a success message
		// Otherwise, try to figure out why it was unsuccessful.

		if ( replyContent == null || replyContent.trim().length() == 0 )
		{
			client.cancelRequest();
			updateDisplay( ERROR_STATE, "No response to skill request." );

			if ( client.isBuffBotActive() )
				client.getBuffBotLog().timeStampedLogEntry(
					BuffBotManager.ERRORCOLOR + "No response to skill request." + BuffBotManager.ENDCOLOR );

			return;
		}
		else if ( replyContent.indexOf( "You don't have enough" ) != -1 )
		{
			client.cancelRequest();
			updateDisplay( ERROR_STATE, "You don't have enough mana." );

			if ( client.isBuffBotActive() )
				client.getBuffBotLog().timeStampedLogEntry(
					BuffBotManager.ERRORCOLOR + "You don't have enough mana." + BuffBotManager.ENDCOLOR );

			return;
		}
		else if ( replyContent.indexOf( "You can only conjure" ) != -1 )
		{
			client.cancelRequest();
			updateDisplay( ERROR_STATE, "Summon limited exceeded." );

			if ( client.isBuffBotActive() )
				client.getBuffBotLog().timeStampedLogEntry(
					BuffBotManager.ERRORCOLOR + "Summon limit exceeded." + BuffBotManager.ENDCOLOR );

			return;
		}
		else if ( replyContent.indexOf( "too many songs" ) != -1 )
		{
			client.cancelRequest();
			updateDisplay( ERROR_STATE, target + " is overbuffed." );

			if ( client.isBuffBotActive() )
				client.getBuffBotLog().timeStampedLogEntry(
					BuffBotManager.ERRORCOLOR + target + " is overbuffed." + BuffBotManager.ENDCOLOR );

			return;
		}
		else if ( replyContent.indexOf( "Invalid target player" ) != -1 )
		{
			client.cancelRequest();
			updateDisplay( ERROR_STATE, target + " is not a valid target." );

			if ( client.isBuffBotActive() )
				client.getBuffBotLog().timeStampedLogEntry(
					BuffBotManager.ERRORCOLOR + target + " is not a valid target." + BuffBotManager.ENDCOLOR );

			return;
		}
		else if ( replyContent.indexOf( "busy fighting" ) != -1 )
		{
			client.cancelRequest();
			updateDisplay( ERROR_STATE, target + " is busy fighting." );

			if ( client.isBuffBotActive() )
				client.getBuffBotLog().timeStampedLogEntry(
					BuffBotManager.ERRORCOLOR + target + " is busy fighting." + BuffBotManager.ENDCOLOR );

			return;
		}
		else
		{
			client.processResult( new AdventureResult( AdventureResult.MP, 0 - consumedMP ) );

			processResults( replyContent.replaceFirst(
				"</b><br>\\(duration: ", " (" ).replaceAll( " Adventures", "" ) );

			client.applyRecentEffects();
			updateDisplay( ENABLED_STATE, skillName + " was successfully cast." );
		}
	}
}