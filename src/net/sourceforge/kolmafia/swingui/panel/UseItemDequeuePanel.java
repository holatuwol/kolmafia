/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.swingui.button.ThreadedButton;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;

public class UseItemDequeuePanel
	extends ItemManagePanel
{
	private final JTabbedPane queueTabs;
	private final boolean food, booze, spleen;

	public UseItemDequeuePanel( final boolean food, final boolean booze, final boolean spleen )
	{
		super( ConcoctionDatabase.getUsables(), false, false );

		this.food = food;
		this.booze = booze;
		this.spleen = spleen;

		this.queueTabs = KoLmafiaGUI.getTabbedPane();

		if ( this.food )
		{
			this.queueTabs.addTab( "0 Full Queued", this.centerPanel );
		}
		else if ( this.booze )
		{
			this.queueTabs.addTab( "0 Drunk Queued", this.centerPanel );
		}
		else if ( this.spleen )
		{
			this.queueTabs.addTab( "0 Spleen Queued", this.centerPanel );
		}

		this.queueTabs.addTab( "Ingredients Used", new GenericScrollPane( ConcoctionDatabase.getQueuedIngredients( this.food, this.booze, this.spleen ), 7 ) );

		JLabel test = new JLabel( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );

		this.elementList.setCellRenderer( ListCellRendererFactory.getCreationQueueRenderer() );
		this.elementList.setFixedCellHeight( (int) ( test.getPreferredSize().getHeight() * 2.5f ) );

		this.elementList.setVisibleRowCount( 3 );
		this.elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

		this.actualPanel.add( this.queueTabs, BorderLayout.CENTER );

		this.setButtons( false, new ActionListener[] { new ConsumeListener(), new CreateListener() } );

		this.eastPanel.add( new UndoQueueButton(), BorderLayout.SOUTH );

		this.setEnabled( true );
		this.filterItems();
	}

	public JTabbedPane getQueueTabs()
	{
		return this.queueTabs;
	}

	public AutoFilterTextField getWordFilter()
	{
		return new ConsumableFilterField();
	}

	private class ConsumeListener
		extends ThreadedListener
	{
		public void run()
		{
			ConcoctionDatabase.handleQueue( UseItemDequeuePanel.this.food, UseItemDequeuePanel.this.booze, UseItemDequeuePanel.this.spleen, KoLConstants.CONSUME_USE );

			if ( UseItemDequeuePanel.this.food )
			{
				UseItemDequeuePanel.this.queueTabs.setTitleAt( 0, ConcoctionDatabase.getQueuedFullness() + " Full Queued" );
			}
			if ( UseItemDequeuePanel.this.booze )
			{
				UseItemDequeuePanel.this.queueTabs.setTitleAt( 0, ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued" );
			}
			if ( UseItemDequeuePanel.this.spleen )
			{
				UseItemDequeuePanel.this.queueTabs.setTitleAt( 0, ConcoctionDatabase.getQueuedSpleenHit() + " Spleen Queued" );
			}
			ConcoctionDatabase.getUsables().sort();
		}

		public String toString()
		{
			return "consume";
		}
	}

	private class CreateListener
		extends ThreadedListener
	{
		public void run()
		{
			ConcoctionDatabase.handleQueue( UseItemDequeuePanel.this.food, UseItemDequeuePanel.this.booze, UseItemDequeuePanel.this.spleen, KoLConstants.NO_CONSUME );

			if ( UseItemDequeuePanel.this.food )
			{
				UseItemDequeuePanel.this.queueTabs.setTitleAt( 0, ConcoctionDatabase.getQueuedFullness() + " Full Queued" );
			}
			if ( UseItemDequeuePanel.this.booze )
			{
				UseItemDequeuePanel.this.queueTabs.setTitleAt( 0, ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued" );
			}
			if ( UseItemDequeuePanel.this.spleen )
			{
				UseItemDequeuePanel.this.queueTabs.setTitleAt( 0, ConcoctionDatabase.getQueuedSpleenHit() + " Spleen Queued" );
			}
			ConcoctionDatabase.getUsables().sort();
		}

		public String toString()
		{
			return "create";
		}
	}

	private class UndoQueueButton
		extends ThreadedButton
	{
		public UndoQueueButton()
		{
			super( "undo" );
		}

		public void run()
		{
			ConcoctionDatabase.pop( UseItemDequeuePanel.this.food, UseItemDequeuePanel.this.booze, UseItemDequeuePanel.this.spleen );
			ConcoctionDatabase.refreshConcoctions();

			if ( UseItemDequeuePanel.this.food )
			{
				UseItemDequeuePanel.this.queueTabs.setTitleAt(
					0, ConcoctionDatabase.getQueuedFullness() + " Full Queued" );
			}
			if ( UseItemDequeuePanel.this.booze )
			{
				UseItemDequeuePanel.this.queueTabs.setTitleAt(
					0, ConcoctionDatabase.getQueuedInebriety() + " Drunk Queued" );
			}
			if ( UseItemDequeuePanel.this.spleen )
			{
				UseItemDequeuePanel.this.queueTabs.setTitleAt(
					0, ConcoctionDatabase.getQueuedSpleenHit() + " Spleen Queued" );
			}
			ConcoctionDatabase.getUsables().sort();
		}
	}

	private class ConsumableFilterField
		extends FilterItemField
	{
		public boolean isVisible( final Object element )
		{
			Concoction creation = (Concoction) element;

			if ( creation.getQueued() == 0 )
			{
				return false;
			}

			if ( ItemDatabase.getFullness( creation.getName() ) > 0 )
			{
				return UseItemDequeuePanel.this.food && super.isVisible( element );
			}

			if ( ItemDatabase.getInebriety( creation.getName() ) > 0 )
			{
				return UseItemDequeuePanel.this.booze && super.isVisible( element );
			}

			if ( ItemDatabase.getSpleenHit( creation.getName() ) > 0 )
			{
				return UseItemDequeuePanel.this.spleen && super.isVisible( element );
			}

			return false;
		}
	}
}