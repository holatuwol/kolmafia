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

import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.swing.SwingUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * A special class which displays an item's description after you float
 * click on the JList.
 */

public class ShowDescriptionList extends JList implements KoLConstants
{
	private static final Pattern PLAYERID_MATCHER = Pattern.compile( "\\(#(\\d+)\\)" );
	private JPopupMenu contextMenu;

	public ShowDescriptionList( LockableListModel model )
	{
		super( model );
		addMouseListener( new ShowDescriptionAdapter() );

		contextMenu = new JPopupMenu();

		JMenuItem dummyItem = new JMenuItem( "Win the game?" );
		dummyItem.addActionListener( new WinGameListener() );
		contextMenu.add( dummyItem );

		addMouseListener( new PopupListener() );
	}

	private class WinGameListener implements ActionListener, Runnable
	{
		public void actionPerformed( ActionEvent e )
		{	(new Thread( this )).start();
		}

		public void run()
		{
			DEFAULT_SHELL.executeLine( "win game" );
			KoLmafia.enableDisplay();
		}
	}

	protected class PopupListener extends MouseAdapter
	{
		public void mousePressed( MouseEvent e )
		{
			maybeShowPopup( e );
		}

		public void mouseReleased( MouseEvent e )
		{
			maybeShowPopup( e );
		}

		private void maybeShowPopup( MouseEvent e )
		{
			if ( e.isPopupTrigger() )
			{
				contextMenu.show( e.getComponent(), e.getX(), e.getY() );
			}
		}
    }

	private class ShowDescriptionAdapter extends MouseAdapter
	{
		public void mouseClicked( MouseEvent e )
		{
			if ( e.getClickCount() == 2 )
			{
				int index = locationToIndex( e.getPoint() );
				Object item = getModel().getElementAt( index );

				if ( item == null )
					return;

				ensureIndexIsVisible( index );

				if ( item instanceof AdventureResult )
				{
					if ( ((AdventureResult)item).isItem() )
						FightFrame.showLocation( "desc_item.php?whichitem=" + TradeableItemDatabase.getDescriptionId( ((AdventureResult)item).getItemId() ) );
					if ( ((AdventureResult)item).isStatusEffect() )
						FightFrame.showLocation( "desc_effect.php?whicheffect=" + StatusEffectDatabase.getEffectId( ((AdventureResult)item).getName() ) );
				}
				if ( item instanceof ItemCreationRequest )
				{
					FightFrame.showLocation( "desc_item.php?whichitem=" + TradeableItemDatabase.getDescriptionId( ((ItemCreationRequest)item).getItemId() ) );
				}
				if ( item instanceof String )
				{
					Matcher playerMatcher = PLAYERID_MATCHER.matcher( (String) item );
					if ( playerMatcher.find() )
					{
						Object [] parameters = new Object [] { "#" + playerMatcher.group(1) };
						SwingUtilities.invokeLater( new CreateFrameRunnable( ProfileFrame.class, parameters ) );
					}
				}
			}
		}
	}
}
