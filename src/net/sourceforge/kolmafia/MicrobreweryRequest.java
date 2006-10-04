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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

public class MicrobreweryRequest extends KoLRequest
{
	private static final Pattern COST_PATTERN = Pattern.compile( "(.*?) \\((\\d*) Meat\\)" );
	private static final Pattern AVAILABLE_PATTERN = Pattern.compile( "<td>([\\w -]*?\\(.*? Meat\\))</td>" );

	private int price;
	private int itemID;
	private boolean isPurchase;

	public MicrobreweryRequest()
	{
		super( "brewery.php" );
		this.isPurchase = false;
	}

	public MicrobreweryRequest( String name )
	{
		super( "brewery.php" );
		addFormField( "action", "Yep." );

		this.isPurchase = true;
		this.price = 0;

		// Parse item name and price
		Matcher itemMatcher = COST_PATTERN.matcher( name );
		int itemID = 0;

		if ( itemMatcher.find( 0 ) )
		{
			String itemName = itemMatcher.group(1);
			this.price = Integer.parseInt ( itemMatcher.group(2) );

			// Get the menu the microbrewery offers today
			if ( microbreweryItems.isEmpty() )
				(new MicrobreweryRequest()).run();

			// Find the item in the menu
			for ( int i = 0; i < 3; i++ )
				if ( ((String)microbreweryItems.get(i)).equals( name ) )
				{
					itemID = -1 - i;
					break;
				}

			if ( itemID == 0 )
				itemID = TradeableItemDatabase.getItemID( itemName );
		}

		addFormField( "whichitem", String.valueOf( itemID ) );
	}

	public void run()
	{
		if ( !KoLCharacter.inMoxieSign() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You can't find the micromicrobrewery." );
			return;
		}

		if ( isPurchase )
		{
			if ( price == 0 )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "The micromicromicrobrewery doesn't sell that." );
				return;
			}

			if ( price > KoLCharacter.getAvailableMeat() )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Insufficient funds." );
				return;
			}

			if ( !KoLCharacter.canDrink() )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "You can't drink. Why are you here?" );
				return;
			}
		}

		KoLmafia.updateDisplay( "Visiting the micromicrobrewery..." );
		super.run();
	}

	public void processResults()
	{
		if ( isPurchase )
		{
			if ( responseText.indexOf( "You're way too drunk already." ) != -1 )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Consumption limit reached." );
				return;
			}

			if ( responseText.indexOf( "You can't afford that item.") != -1 )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Insufficient funds." );
				return;
			}

			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, 0 - price ) );
			KoLmafia.updateDisplay( "Drink purchased." );
			return;
		}

		Matcher purchaseMatcher = AVAILABLE_PATTERN.matcher( responseText );
		while ( purchaseMatcher.find() )
			microbreweryItems.add( purchaseMatcher.group(1) );

		KoLmafia.updateDisplay( "Menu retrieved." );
	}

	protected boolean mayChangeCreatables()
	{	return false;
	}
}
