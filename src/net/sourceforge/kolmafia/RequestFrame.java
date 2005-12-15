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
import java.util.regex.Matcher;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JSplitPane;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JList;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.ListSelectionModel;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class RequestFrame extends KoLFrame
{
	private static int combatRound;

	private RequestFrame parent;
	private KoLRequest currentRequest;
	private LimitedSizeChatBuffer mainBuffer;

	private boolean showSideBar;
	private LimitedSizeChatBuffer sideBuffer;
	private CharpaneRequest sidePaneRequest;

	protected JEditorPane sideDisplay;
	protected JEditorPane mainDisplay;

	public RequestFrame( KoLmafia client, KoLRequest request )
	{	this( client, null, request );
	}

	public RequestFrame( KoLmafia client, RequestFrame parent, KoLRequest request )
	{
		this( client, null, request, request == null || request.getURLString().equals( "main.php" ) );
	}

	public RequestFrame( KoLmafia client, RequestFrame parent, KoLRequest request, boolean showSideBar )
	{
		super( client, "" );

		this.parent = parent;
		this.currentRequest = request;
		this.showSideBar = showSideBar;
		this.combatRound = 1;

		if ( request != null && request instanceof FightRequest )
			combatRound = ((FightRequest)request).getCombatRound();

		this.mainDisplay = new JEditorPane();
		this.mainDisplay.setEditable( false );

		if ( !(this instanceof PendingTradesFrame) )
			this.mainDisplay.addHyperlinkListener( new KoLHyperlinkAdapter() );

		this.mainBuffer = new LimitedSizeChatBuffer( "Mini-Browser", false );
		this.mainBuffer.setChatDisplay( this.mainDisplay );

		JScrollPane mainScroller = new JScrollPane( this.mainDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );

		// Game text descriptions and player searches should not add
		// extra requests to the server by having a side panel.

		if ( !showSideBar )
		{
			this.sideBuffer = null;

			JComponentUtilities.setComponentSize( mainScroller, 400, 300 );
			framePanel.setLayout( new GridLayout( 1, 1 ) );
			framePanel.add( mainScroller );
		}
		else
		{
			this.sideDisplay = new JEditorPane();
			this.sideDisplay.setEditable( false );
			this.sideDisplay.addHyperlinkListener( new KoLHyperlinkAdapter() );

			this.sideBuffer = new LimitedSizeChatBuffer( "", false );
			this.sideBuffer.setChatDisplay( sideDisplay );

			JScrollPane sideScroller = new JScrollPane( this.sideDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
			JComponentUtilities.setComponentSize( sideScroller, 150, 450 );

			JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true, sideScroller, mainScroller );
			splitPane.setOneTouchExpandable( true );
			JComponentUtilities.setComponentSize( splitPane, 600, 450 );

			framePanel.setLayout( new GridLayout( 1, 1 ) );
			framePanel.add( splitPane );
		}

		(new DisplayRequestThread()).start();
	}

	protected void addMenuBar()
	{
		super.addMenuBar();
		JMenuBar menuBar = getJMenuBar();

		// The function and goto menus are only available if there is a sidebar;
		// otherwise, adding them clutters the user interface.

		JMenu functionMenu = new JMenu( "Function" );
		functionMenu.add( new DisplayRequestMenuItem( "Inventory", "inventory.php" ) );
		functionMenu.add( new DisplayRequestMenuItem( "Character", "charsheet.php" ) );
		functionMenu.add( new DisplayRequestMenuItem( "Class Skills", "skills.php" ) );
		functionMenu.add( new DisplayRequestMenuItem( "Read Messages", "messages.php" ) );
		functionMenu.add( new DisplayRequestMenuItem( "Account Menu", "account.php" ) );
		functionMenu.add( new DisplayRequestMenuItem( "Documentation", "doc.php?topic=home" ) );
		functionMenu.add( new DisplayPageMenuItem( "KoL Forums", "http://forums.kingdomofloathing.com/" ) );
		functionMenu.add( new DisplayPageMenuItem( "Radio KoL", "http://radiokol24.kicks-ass.net:8015/listen.pls" ) );
		functionMenu.add( new DisplayPageMenuItem( "Radio WKoL", "http://72.35.226.80:9140/listen.pls" ) );
		functionMenu.add( new DisplayRequestMenuItem( "Report Bug", "sendmessage.php?toid=Jick" ) );
		functionMenu.add( new DisplayPageMenuItem( "Donate to KoL", "http://www.kingdomofloathing.com/donatepopup.php?pid=" + KoLCharacter.getUserID() ) );
		functionMenu.add( new DisplayRequestMenuItem( "Log Out", "logout.php" ) );

		menuBar.add( functionMenu, 0 );

		JMenu gotoMenu = new JMenu( "Goto (Maki)" );

		gotoMenu.add( new DisplayRequestMenuItem( "Main Map", "main.php" ) );
		gotoMenu.add( new DisplayRequestMenuItem( "Seaside Town", "town.php" ) );
		gotoMenu.add( new DisplayRequestMenuItem( "The Mall", "mall.php" ) );
		gotoMenu.add( new DisplayRequestMenuItem( "Clan Hall", "clan_hall.php" ) );
		gotoMenu.add( new DisplayRequestMenuItem( "Campground", "campground.php" ) );

		gotoMenu.add( new DisplayRequestMenuItem( "Big Mountains", "mountains.php" ) );
		gotoMenu.add( new DisplayRequestMenuItem( "Nearby Plains", "plains.php" ) );

		JMenu lairMenu = new JMenu( "Sorceress' Lair" );
			lairMenu.add( new DisplayRequestMenuItem( "Entrance Cavern", "lair1.php" ) );
			lairMenu.add( new DisplayRequestMenuItem( "Entryway Statues", "lair2.php" ) );
			lairMenu.add( new DisplayRequestMenuItem( "Hedge Maze Puzzle", "lair3.php" ) );
			lairMenu.add( new DisplayRequestMenuItem( "Tower: Floors 1-3", "lair4.php" ) );
			lairMenu.add( new DisplayRequestMenuItem( "Tower: Floors 4-6", "lair5.php" ) );
			lairMenu.add( new DisplayRequestMenuItem( "Outside the Chamber", "lair6.php" ) );
		gotoMenu.add( lairMenu );

		gotoMenu.add( new DisplayRequestMenuItem( "Desert Beach", "beach.php" ) );
		gotoMenu.add( new DisplayRequestMenuItem( "Distant Woods", "woods.php" ) );
		gotoMenu.add( new DisplayRequestMenuItem( "Mysterious Island", "island.php" ) );

		menuBar.add( gotoMenu, 1 );
	}

	/**
	 * Utility method which returns the current URL being pointed
	 * to by this <code>RequestFrame</code>.
	 */

	public String getCurrentLocation()
	{	return currentRequest.getURLString();
	}

	/**
	 * Utility method which refreshes the current frame with
	 * data contained in the given request.  If the request
	 * has not yet been run, it will be run before the data
	 * is display in this frame.
	 */

	public void refresh( KoLRequest request )
	{
		String location = request.getURLString();

		if ( parent == null || location.startsWith( "search" ) )
		{
			currentRequest = request;
			(new DisplayRequestThread()).start();
		}
		else
			parent.refresh( request );
	}

	/**
	 * Utility method which refreshes the side pane.  This
	 * is used whenever something occurs in the main pane
	 * which is suspected to cause some display change here.
	 */

	private void refreshSidePane()
	{
		if ( showSideBar )
		{
			sidePaneRequest.run();
			sideBuffer.clearBuffer();
			sideBuffer.append( getDisplayHTML( sidePaneRequest.responseText ) );
			sideDisplay.setCaretPosition(0);
		}
	}

	/**
	 * Utility method which converts the given text into a form which
	 * can be displayed properly in a <code>JEditorPane</code>.  This
	 * method is necessary primarily due to the bad HTML which is used
	 * but can still be properly rendered by post-3.2 browsers.
	 */

	protected String getDisplayHTML( String responseText )
	{
		// Switch all the <BR> tags that are not understood
		// by the default Java browser to an understood form,
		// and remove all <HR> tags.

		String displayHTML = responseText.replaceAll( "<[Bb][Rr]( ?/)?>", "<br>" ).replaceAll( "<[Hh][Rr].*?>", "<br>" );

		// Fix all the super-small font displays used in the
		// various KoL panes.

		displayHTML = displayHTML.replaceAll( "font-size: .8em;", "" ).replaceAll( "<font size=[12]>", "" ).replaceAll(
			" class=small", "" ).replaceAll( " class=tiny", "" );

		// This is to replace all the rows with a black background
		// because they are not properly rendered.

		displayHTML = displayHTML.replaceAll( "<tr><td([^>]*?) bgcolor=black([^>]*?)>((</td>)?)</tr>", "<tr><td$1$2></td></tr>" );

		// The default browser doesn't understand the table directive
		// style="border: 1px solid black"; turn it into a simple "border=1"

		displayHTML = displayHTML.replaceAll( "style=\"border: 1px solid black\"", "border=1" );

		// turn:  <form...><td...>...</td></form>
		// into:  <td...><form...>...</form></td>

		displayHTML = displayHTML.replaceAll( "(<form[^>]*>)((<input[^>]*>)*)(<td[^>]*>)", "$4$1$2" );
		displayHTML = displayHTML.replaceAll( "</td></form>", "</form></td>" );

		// turn:  <form...><tr...><td...>...</td></tr></form>
		// into:  <tr...><td...><form...>...</form></td></tr>

		displayHTML = displayHTML.replaceAll( "(<form[^>]*>)((<input[^>]*>)*)<tr>(<td[^>]*>)", "<tr>$4$1$2" );
		displayHTML = displayHTML.replaceAll( "</td></tr></form>", "</form></td></tr>" );

		// KoL also has really crazy nested Javascript links, and
		// since the default browser doesn't recognize these, be
		// sure to convert them to standard <A> tags linking to
		// the correct document.

		displayHTML = displayHTML.replaceAll( "<a[^>]*?\\([\'\"](.*?)[\'\"].*?>", "<a href=\"$1\">" );
		displayHTML = displayHTML.replaceAll( "<img([^>]*?) onClick=\'window.open\\(\"(.*?)\".*?\'(.*?)>", "<a href=\"$2\"><img$1 $3 border=0></a>" );

		// The search form for viewing players has an </html>
		// tag appearing right after </style>, which may confuse
		// the HTML parser.

		displayHTML = displayHTML.replaceAll( "</style></html>" , "</style>" );

		// For some reason, character entitites are not properly
		// handled by the mini browser.

		displayHTML = displayHTML.replaceAll( "&ntilde;", "�" ).replaceAll( "&trade;", " [tm]" ).replaceAll( "&infin;", "**" );
		return displayHTML;
	}

	/**
	 * A special thread class which ensures that attempts to
	 * refresh the frame with data do not long the Swing thread.
	 */

	protected class DisplayRequestThread extends DaemonThread
	{
		public void run()
		{
			if ( currentRequest == null )
				return;

			mainBuffer.clearBuffer();

			if ( getCurrentLocation().startsWith( "adventure.php" ) )
			{
				Matcher dataMatcher = Pattern.compile( "adv=(\\d+)" ).matcher( currentRequest.getDataString() );

				if ( client.isLuckyCharacter() &&
				     dataMatcher.find() &&
				     AdventureRequest.hasLuckyVersion( dataMatcher.group(1) ) )
				{

					if ( getProperty( "cloverProtectActive" ).equals( "true" ) )
					{
						client.updateDisplay( ERROR_STATE, "You have a ten-leaf clover." );
						mainBuffer.append( "<h1><font color=\"red\">You have a ten-leaf clover.	 Please deactivate clover protection in your startup options first if you are certain you want to use your clovers while adventuring.</font></h1>" );
						return;
					}

					client.processResult( SewerRequest.CLOVER );
				}
			}

			// Update the title for the RequestFrame to include the
			// current round of combat (for people who track this
			// sort of thing).

			if ( getCurrentLocation().startsWith( "fight" ) )
			{
				combatRound++;
				setTitle( "Mini-Browser: Combat Round " + combatRound );
			}
			else
			{
				combatRound = 1;
				setTitle( "Mini-Browser" );
			}

			if ( currentRequest.responseText == null )
			{
				mainBuffer.append( "Retrieving..." );
				currentRequest.run();
			}

			mainBuffer.clearBuffer();
			mainBuffer.append( getDisplayHTML( currentRequest.responseText ) );
			mainDisplay.setCaretPosition( 0 );

			// In the event that something resembling a gain event
			// is seen in the response text, or in the event that you
			// switch between compact and full mode, refresh the sidebar.

			if ( showSideBar && sidePaneRequest == null )
			{
				sidePaneRequest = new CharpaneRequest( client );
				refreshSidePane();
			}

			if ( client.processResults( currentRequest.responseText ) || getCurrentLocation().indexOf( "togglecompact" ) != -1 )
			{
				KoLCharacter.refreshCalculatedLists();
				if ( showSideBar )
					refreshSidePane();
			}

			// See if the person learned a new skill from using a
			// mini-browser frame.

			Matcher learnedMatcher = Pattern.compile( "<td>You learn a new skill: <b>(.*?)</b>" ).matcher( currentRequest.responseText );
			if ( learnedMatcher.find() )
				KoLCharacter.addAvailableSkill( new UseSkillRequest( client, learnedMatcher.group(1), "", 1 ) );

			// Update the mushroom plot, if applicable.

			if ( getCurrentLocation().indexOf( "mushroom" ) != -1 )
				MushroomPlot.parsePlot( currentRequest.responseText );
		}
	}
}
