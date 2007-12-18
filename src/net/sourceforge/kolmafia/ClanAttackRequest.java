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

import net.java.dev.spellcast.utilities.SortedListModel;

public class ClanAttackRequest
	extends KoLRequest
	implements Comparable
{
	private static final Pattern CLANID_PATTERN =
		Pattern.compile( "name=whichclan value=(\\d+)></td><td><b>([^<]+)</td><td>([\\d]+)</td>" );
	private static final Pattern WAIT_PATTERN = Pattern.compile( "<br>Your clan can attack again in (.*?)<p>" );

	private static final SortedListModel enemyClans = new SortedListModel();
	private static String nextAttack = null;

	private final String name;

	public ClanAttackRequest()
	{
		super( "clan_attack.php" );
		this.name = null;
	}

	private ClanAttackRequest( final String id, final String name )
	{
		super( "clan_attack.php" );
		this.addFormField( "whichclan", id );

		this.name = name;
	}

	public void run()
	{
		if ( this.getPath().equals( "clan_attack.php" ) )
		{
			if ( this.name == null )
			{
				KoLmafia.updateDisplay( "Retrieving clan attack state..." );
			}
			else
			{
				KoLmafia.updateDisplay( "Attacking " + this.name + "..." );
			}
		}

		super.run();
	}

	public static final String getNextAttack()
	{
		return ClanAttackRequest.nextAttack == null ? "You may attack right now." : ClanAttackRequest.nextAttack;
	}

	public static final SortedListModel getEnemyClans()
	{
		return ClanAttackRequest.enemyClans;
	}

	public void processResults()
	{
		if ( this.name != null )
		{
			return;
		}

		ClanAttackRequest.nextAttack = null;

		if ( this.getPath().equals( "clan_attack.php" ) )
		{
			ClanAttackRequest.enemyClans.clear();

			int bagCount = 0;
			Matcher clanMatcher = ClanAttackRequest.CLANID_PATTERN.matcher( this.responseText );

			while ( clanMatcher.find() )
			{
				bagCount = Integer.parseInt( clanMatcher.group( 3 ) );
				if ( bagCount == 1 )
				{
					ClanAttackRequest.enemyClans.add( new ClanAttackRequest(
						clanMatcher.group( 1 ), clanMatcher.group( 2 ) ) );
				}
			}

			if ( ClanAttackRequest.enemyClans.isEmpty() )
			{
				this.constructURLString( "clan_war.php" ).run();
				return;
			}

			KoLSettings.setUserProperty( "clanAttacksEnabled", "true" );

			if ( ClanAttackRequest.enemyClans.getSize() > 0 )
			{
				ClanAttackRequest.enemyClans.setSelectedIndex( KoLConstants.RNG.nextInt( ClanAttackRequest.enemyClans.getSize() ) );
			}
		}
		else
		{
			Matcher nextMatcher = ClanAttackRequest.WAIT_PATTERN.matcher( this.responseText );
			if ( nextMatcher.find() )
			{
				ClanAttackRequest.nextAttack = "You may attack again in " + nextMatcher.group( 1 );
				KoLSettings.setUserProperty( "clanAttacksEnabled", "true" );
			}
			else
			{
				KoLSettings.setUserProperty( "clanAttacksEnabled", "false" );
				ClanAttackRequest.nextAttack = "You do not have the ability to attack.";
			}

			KoLmafia.updateDisplay( ClanAttackRequest.nextAttack );
		}
	}

	public String toString()
	{
		return this.name;
	}

	public int compareTo( final Object o )
	{
		return o == null || !( o instanceof ClanAttackRequest ) ? -1 : this.compareTo( (ClanAttackRequest) o );
	}

	public int compareTo( final ClanAttackRequest car )
	{
		return this.name.compareToIgnoreCase( car.name );
	}
}
