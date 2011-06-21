/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.swingui.CoinmastersFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MrStoreRequest
	extends CoinMasterRequest
{
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You have (\\w+) Mr. Accessor(?:y|ies) to trade." );
	public static final AdventureResult MR_A = ItemPool.get( ItemPool.MR_ACCESSORY, 1 );
	public static final CoinmasterData MR_STORE =
		new CoinmasterData(
			"Mr. Store",
			"mrstore.php",
			"Mr. A",
			"You have no Mr. Accessories to trade",
			false,
			MrStoreRequest.TOKEN_PATTERN,
			MrStoreRequest.MR_A,
			"availableMrAccessories",
			"whichitem",
			CoinMasterRequest.ITEMID_PATTERN,
			null,
			null,
			"buy",
			CoinmastersDatabase.getMrAItems(),
			CoinmastersDatabase.MrABuyPrices(),
			null,
			null
			);

	public MrStoreRequest()
	{
		super( MrStoreRequest.MR_STORE );
	}

	public MrStoreRequest( final String action )
	{
		super( MrStoreRequest.MR_STORE, action );
	}

	public MrStoreRequest( final String action, final int itemId, final int quantity )
	{
		super( MrStoreRequest.MR_STORE, action, itemId, quantity );
	}

	public MrStoreRequest( final String action, final int itemId )
	{
		this( action, itemId, 1 );
	}

	public MrStoreRequest( final String action, final AdventureResult ar )
	{
		this( action, ar.getItemId(), ar.getCount() );
	}

	public void processResults()
	{
		String responseText = this.responseText;
		if ( this.action.equals( "pullmras" ) )
		{
			// You can't pull any more items out of storage today.
			if ( responseText.indexOf( "You can't pull any more items out of storage today" ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't pull any more items out of storage today." );
			}
		}

		MrStoreRequest.parseResponse( this.getURLString(), responseText );
	}

	private static final Pattern ITEM_PATTERN = Pattern.compile( "name=whichitem value=([\\d]+)>.*?desc_?item.*?([\\d]+).*?<b>([^<]*)</b>.*?([\\d]+)&nbsp;Mr\\.", Pattern.DOTALL );
	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "mrstore.php" ) )
		{
			return;
		}

		// Learn new Mr. Items by simply visiting Mr. Store
		// Refresh the Coin Master inventory every time we visit.

		CoinmasterData data = MrStoreRequest.MR_STORE;
		LockableListModel items = data.getBuyItems();
		Map prices = data.getBuyPrices();
		items.clear();
		prices.clear();

		Matcher matcher = ITEM_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			int itemId = StringUtilities.parseInt( matcher.group(1) );
			String descId = matcher.group(2);
			String itemName = matcher.group(3);
			int price = StringUtilities.parseInt( matcher.group(4) );

			String match = ItemDatabase.getItemDataName( itemId );
			if ( match == null || !match.equals( itemName ) )
			{
				ItemDatabase.registerItem( itemId, itemName, descId );
			}

			// Add it to the Mr. Store inventory
			AdventureResult item = ItemPool.get( itemId, 0 );
			String name = StringUtilities.getCanonicalName( itemName );
			Integer iprice = new Integer( price );
			items.add( item );
			prices.put( name, iprice );
		}

		String action = GenericRequest.getAction( urlString );
		if ( action != null && action.equals( "pullmras" ) )
		{
			if ( responseText.indexOf( "You acquire" ) != -1 )
			{
				// We pulled a Mr. A from storage.
				AdventureResult remove = MrStoreRequest.MR_A.getInstance( -1 );
				AdventureResult.addResultToList( KoLConstants.storage, remove );
				CoinMasterRequest.parseBalance( data, responseText );
				CoinmastersFrame.externalUpdate();
				if ( !KoLCharacter.isHardcore() && !KoLCharacter.canInteract() )
				{
					int pulls = ConcoctionDatabase.getPullsRemaining();
					ConcoctionDatabase.setPullsRemaining( pulls - 1 );
				}
			}
			return;
		}

		CoinMasterRequest.parseResponse( data, urlString, responseText );
	}

	public static boolean registerRequest( final String urlString )
	{
		String action = GenericRequest.getAction( urlString );
		String message = null;

		if ( action == null )
		{
			message = "Visiting Mr. Store";
		}
		else if ( action.equals( "pullmras" ) )
		{
			message = "Pulling a Mr. Accessory from storage";
		}
		else if ( action.equals( "buy" ) )
		{
			CoinmasterData data = MrStoreRequest.MR_STORE;
			CoinMasterRequest.buyStuff( data, urlString );
			return true;
		}

		if ( message == null )
		{
			return false;
		}

		RequestLogger.printLine( "" );
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
