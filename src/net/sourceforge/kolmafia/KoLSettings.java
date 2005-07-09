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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Properties;
import net.java.dev.spellcast.utilities.UtilityConstants;

/**
 * An extension of {@link java.util.Properties} which handles all the
 * user settings of <code>KoLmafia</code>.  In order to maintain issues
 * involving compatibility (J2SE 1.4 does not support XML output directly),
 * all data is written using {@link java.util.Properties#store(OutputStream,String)}.
 * Files are named according to the following convention: a tilde (<code>~</code>)
 * preceeds the name of the character whose settings this object represents,
 * with the 'kcs' extension (KoLmafia Character Settings).  All global settings
 * are stored in <code>~.kcs</code>.
 */

public class KoLSettings extends Properties implements UtilityConstants
{
	private File settingsFile;
	private String characterName;

	/**
	 * Constructs a global settings file.
	 */

	public KoLSettings()
	{	this( "" );
	}

	/**
	 * Constructs a settings file for a character with the specified name.
	 * Note that in the data file created, all spaces in the character name
	 * will be replaced with an underscore, and all other punctuation will
	 * be removed.
	 *
	 * @param	characterName	The name of the character this settings file represents
	 */

	public KoLSettings( String characterName )
	{
		this.characterName = characterName;
		String noExtensionName = characterName.replaceAll( "\\/q", "" ).replaceAll( " ", "_" ).toLowerCase();
		this.settingsFile = new File( DATA_DIRECTORY + "~" + noExtensionName + ".kcs" );

		// Make sure that any settings that were applied in the default
		// that did not exist before are applied universally.

		if ( !characterName.equals( "" ) )
			loadSettings( new File( DATA_DIRECTORY + "~.kcs" ) );

		loadSettings( this.settingsFile );
		ensureDefaults();
		saveSettings();
	}

	/**
	 * Stores the settings maintained in this <code>KoLSettings</code>
	 * object to disk for later retrieval.
	 */

	public void saveSettings()
	{	storeSettings( settingsFile );
	}

	/**
	 * Loads the settings located in the given file into this object.
	 * Note that all settings are overridden; if the given file does
	 * not exist, the current global settings will also be rewritten
	 * into the appropriate file.
	 *
	 * @param	source	The file that contains (or will contain) the character data
	 */

	private void loadSettings( File source )
	{
		try
		{
			// First guarantee that a settings file exists with
			// the appropriate Properties data.

			if ( !source.exists() )
			{
				source.getParentFile().mkdirs();
				source.createNewFile();

				// Then, store the results into the designated
				// file by calling the appropriate subroutine.

				if ( source != settingsFile )
					storeSettings( source );
			}

			// Now that it is guaranteed that an XML file exists
			// with the appropriate properties, load the file.

			FileInputStream istream = new FileInputStream( source );
			load( istream );
			istream.close();
			istream = null;
		}
		catch ( IOException e1 )
		{
			// This should not happen, because it should
			// always be possible.  Therefore, no handling
			// will take place at the current time unless a
			// pressing need arises for it.
		}
		catch ( Exception e2 )
		{
			// Somehow, the settings were corrupted; this
			// means that they will have to be created after
			// the current file is deleted.

			source.delete();
			loadSettings( source );
		}
	}

	/**
	 * Ensures that all the default keys are non-null.  This is
	 * used so that there aren't lots of null checks whenever a
	 * key is loaded.
	 */

	private void ensureDefaults()
	{
		ensureProperty( "aggregatePrices", "true" );
		ensureProperty( "autoLogin", "" );
		ensureProperty( "autoRepairBoxes", "false" );
		ensureProperty( "battleAction", "attack" );
		ensureProperty( "browserBookmarks", "" );
		ensureProperty( "buffBotCasting", "" );
		ensureProperty( "buffBotMessageDisposal", "0" );
		ensureProperty( "buffBotMPRestore", "tiny house" );
		ensureProperty( "channelColors", "#000000,#000000,#000000,#000000,#000000,#000000,#000000,#000000,#000000,#000000,#000000" );
		ensureProperty( "chatStyle", "0" );
		ensureProperty( "choiceAdventure9", "3" );
		ensureProperty( "clanRosterHeader", ClanSnapshotTable.getDefaultHeader() );
		ensureProperty( "closeSending", "false" );
		ensureProperty( "createWithoutBoxServants", "false" );
		ensureProperty( "defaultLimit", "13" );
		ensureProperty( "fontSize", "3" );
		ensureProperty( "forceReconnect", "false" );
		ensureProperty( "forceSorting", "false" );
		ensureProperty( "hpAutoFlee", "0.0" );
		ensureProperty( "hpAutoRecover", "-0.1" );
		ensureProperty( "hpRecoveryScript", "" );
		ensureProperty( "http.proxyHost", "" );
		ensureProperty( "http.proxyPort", "" );
		ensureProperty( "http.proxyUser", "" );
		ensureProperty( "http.proxyPassword", "" );
		ensureProperty( "includeAscensionRecipes", "false" );
		ensureProperty( "invalidBuffMessage", "You sent an amount which was not a valid buff amount." );
		ensureProperty( "lastAdventure", "" );
		ensureProperty( "lastOtoriRequest", "19700101" );
		ensureProperty( "loginServer", "0" );
		ensureProperty( "luckySewer", "2,8,13" );
		ensureProperty( "maxPhilanthropy", "1" );
		ensureProperty( "mpAutoRecover", "-0.1" );
		ensureProperty( "mpRecoveryScript", "" );
		ensureProperty( "nameClickOpens", "0" );
		ensureProperty( "proxySet", "false" );
		ensureProperty( "reloadFrames", "" );
		ensureProperty( "savePositions", "false" );
		ensureProperty( "saveOutgoing", "false" );
		ensureProperty( "saveState", "" );
		ensureProperty( "skipFamiliars", "false" );
		ensureProperty( "skipInventory", "false" );
		ensureProperty( "sortAdventures", "false" );
		ensureProperty( "thanksMessage", "Thank you for the donation.  It is greatly appreciated." );
		ensureProperty( "useChatBasedBuffBot", "false" );
		ensureProperty( "useClosetForCreation", "false" );
		ensureProperty( "useTabbedChat", "1" );
		ensureProperty( "whiteList", "" );
	}

	/**
	 * Ensures that the given property exists, and if it does not exist,
	 * initializes it to the given value.
	 */

	private void ensureProperty( String key, String defaultValue )
	{
		if ( !containsKey( key ) )
			setProperty( key, defaultValue );
	}

	/**
	 * Stores the settings maintained in this <code>KoLSettings</code>
	 * to the noted file.  Note that this method ALWAYS overwrites
	 * the given file.
	 *
	 * @param	destination	The file to which the settings will be stored.
	 */

	private void storeSettings( File destination )
	{
		try
		{
			FileOutputStream ostream = new FileOutputStream( destination );
			store( ostream, "KoLmafia Settings" );
			ostream.close();
			ostream = null;
		}
		catch ( IOException e )
		{
			// This should not happen, because it should
			// always be possible.  Therefore, no handling
			// will take place at the current time unless a
			// pressing need arises for it.
		}
	}

}
