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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

/**
 * Internal class used to handle everything related to creating items; this allows creating of items, which usually
 * get resold in malls.
 */

public class CreateItemPanel
	extends InventoryPanel
{
	public CreateItemPanel( final boolean food, final boolean booze, final boolean equip, boolean other )
	{
		super( "create item", "create & use", ConcoctionDatabase.getCreatables(), equip && !other );

		if ( this.isEquipmentOnly )
		{
			super.addFilters();
		}
		else
		{
			JPanel filterPanel = new JPanel();
			filterPanel.add( new CreationSettingCheckBox(
				"require in-a-boxes", "requireBoxServants", "Do not cook/mix without chef/bartender" ) );
			filterPanel.add( new CreationSettingCheckBox(
				"repair on explosion", "autoRepairBoxServants",
				"Automatically repair chefs and bartenders on explosion" ) );

			this.northPanel.add( filterPanel, BorderLayout.NORTH );
			this.setFixedFilter( food, booze, equip, other, true );
		}

		ConcoctionDatabase.getCreatables().updateFilter( false );
	}

	public void addFilters()
	{
	}

	public void actionConfirmed()
	{
		Object selected = this.elementList.getSelectedValue();

		if ( selected == null )
		{
			return;
		}

		CreateItemRequest selection = (CreateItemRequest) selected;
		int quantityDesired =
			InputFieldUtilities.getQuantity(
				"Creating multiple " + selection.getName() + "...", selection.getQuantityPossible() );
		if ( quantityDesired < 1 )
		{
			return;
		}

		KoLmafia.updateDisplay( "Verifying ingredients..." );
		selection.setQuantityNeeded( quantityDesired );

		RequestThread.openRequestSequence();

		SpecialOutfit.createImplicitCheckpoint();
		RequestThread.postRequest( selection );
		SpecialOutfit.restoreImplicitCheckpoint();

		RequestThread.closeRequestSequence();
	}

	public void actionCancelled()
	{
		Object selected = this.elementList.getSelectedValue();

		if ( selected == null )
		{
			return;
		}

		CreateItemRequest selection = (CreateItemRequest) selected;

		int maximum = UseItemRequest.maximumUses( selection.getItemId() );
		int quantityDesired =
			maximum < 2 ? maximum : InputFieldUtilities.getQuantity(
				"Creating multiple " + selection.getName() + "...", Math.min(
					maximum, selection.getQuantityPossible() ) );

		if ( quantityDesired < 1 )
		{
			return;
		}

		KoLmafia.updateDisplay( "Verifying ingredients..." );
		selection.setQuantityNeeded( quantityDesired );

		RequestThread.openRequestSequence();

		SpecialOutfit.createImplicitCheckpoint();
		RequestThread.postRequest( selection );
		SpecialOutfit.restoreImplicitCheckpoint();

		RequestThread.postRequest( new UseItemRequest( new AdventureResult(
			selection.getItemId(), quantityDesired ) ) );
		RequestThread.closeRequestSequence();
	}

	private class CreationSettingCheckBox
		extends JCheckBox
		implements ActionListener
	{
		private final String property;

		public CreationSettingCheckBox( final String label, final String property, final String tooltip )
		{
			super( label );

			this.setToolTipText( tooltip );
			this.setSelected( Preferences.getBoolean( property ) );

			this.addActionListener( this );

			this.property = property;
			Preferences.registerCheckbox( property, this );
		}

		public void actionPerformed( final ActionEvent e )
		{
			if ( Preferences.getBoolean( this.property ) == this.isSelected() )
			{
				return;
			}

			Preferences.setBoolean( this.property, this.isSelected() );
			ConcoctionDatabase.refreshConcoctions();
		}
	}

}