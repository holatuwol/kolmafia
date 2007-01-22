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

public class ProposeTradeRequest extends SendMessageRequest
{
	public static final Pattern ITEMID_PATTERN = Pattern.compile( "item\\d+=(\\d+)" );
	public static final Pattern QUANTITY_PATTERN = Pattern.compile( "howmany\\d+=(\\d+)" );

	private int offerId;
	private boolean isCounterOffer;
	private String recipient, message;

	public ProposeTradeRequest()
	{	super( "makeoffer.php", new Object[0] );
	}

	public ProposeTradeRequest( int offerId, String message, Object [] attachments )
	{
		super( "counteroffer.php", attachments );
		addFormField( "action", "counter" );
		addFormField( "whichoffer", String.valueOf( offerId ) );
		addFormField( "memo", message.replaceAll( "Meat:", "Please respond with " ) );

		this.offerId = offerId;
		this.message = message;
		this.isCounterOffer = true;
		this.recipient = KoLmafia.getPlayerId( recipient );
	}

	public ProposeTradeRequest( String recipient, String message, Object [] attachments )
	{
		super( "makeoffer.php", attachments );
		addFormField( "action", "proposeoffer" );
		addFormField( "towho", recipient );
		addFormField( "memo", message.replaceAll( "Meat:", "Please respond with " ) );

		this.offerId = 0;
		this.message = message;
		this.isCounterOffer = false;
		this.recipient = KoLmafia.getPlayerId( recipient );
	}

	public int getCapacity()
	{	return 11;
	}

	public SendMessageRequest getSubInstance( Object [] attachments )
	{
		return isCounterOffer ? new ProposeTradeRequest( offerId, message, attachments ) :
			new ProposeTradeRequest( recipient, message, attachments );
	}

	public String getSuccessMessage()
	{	return "";
	}

	public boolean allowUntradeableTransfer()
	{	return false;
	}

	public void processResults()
	{	responseText = responseText.substring( 0, responseText.lastIndexOf( "<b>Propose" ) ).replaceAll( "[Mm]eat:", "Please respond with " );
	}

	public String getStatusMessage()
	{	return "Placing items in KoL escrow";
	}

	public String getItemField()
	{	return "whichitem";
	}

	public String getQuantityField()
	{	return "howmany";
	}

	public String getMeatField()
	{	return "offermeat";
	}

	public static boolean registerRequest( String urlString )
	{
		if ( !urlString.startsWith( "makeoffer.php" ) && !urlString.startsWith( "counteroffer.php" ) )
			return false;

		return registerRequest( "trade", urlString, inventory, 0 );
	}
}
