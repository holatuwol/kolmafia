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

// layout
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;

// event listeners
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.swing.SwingUtilities;

// containers
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JEditorPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import java.util.ArrayList;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class CommandDisplayFrame extends KoLFrame
{
	private KoLmafiaCLI instance;
	private LimitedSizeChatBuffer commandBuffer;

	private static int lastCommandIndex = 0;
	private static ArrayList recentCommands = new ArrayList();

	public CommandDisplayFrame( KoLmafia client )
	{
		super( client, "Graphical CLI" );

		try
		{
			commandBuffer = new LimitedSizeChatBuffer( "KoLmafia: Graphical CLI", false );
			if ( client != null )
				instance = new KoLmafiaCLI( client, commandBuffer );
		}
		catch ( Exception e )
		{
			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();
		}

		addCompactPane();
		framePanel.add( new CommandDisplayPanel(), BorderLayout.CENTER );
	}

	private class CommandDisplayPanel extends JPanel
	{
		private JTextField entryField;
		private JButton entryButton;

		public CommandDisplayPanel()
		{
			JEditorPane outputDisplay = new JEditorPane();
			outputDisplay.setEditable( false );

			commandBuffer.setChatDisplay( outputDisplay );

			JScrollPane scrollPane = new JScrollPane( outputDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

			scrollPane.setVerticalScrollBar( new CommandScrollBar() );
			commandBuffer.setScrollPane( scrollPane );

			JComponentUtilities.setComponentSize( scrollPane, 400, 300 );

			JPanel entryPanel = new JPanel();
			entryField = new JTextField();
			entryField.addKeyListener( new CommandEntryListener() );

			entryButton = new JButton( "exec" );
			entryButton.addActionListener( new CommandEntryListener() );
			entryPanel.setLayout( new BoxLayout( entryPanel, BoxLayout.X_AXIS ) );
			entryPanel.add( entryField, BorderLayout.CENTER );
			entryPanel.add( entryButton, BorderLayout.EAST );

			setLayout( new BorderLayout( 1, 1 ) );
			add( scrollPane, BorderLayout.CENTER );
			add( entryPanel, BorderLayout.SOUTH );
		}

		private class CommandScrollBar extends JScrollBar
		{
			private boolean autoscroll;

			public CommandScrollBar()
			{
				super( VERTICAL );
				this.autoscroll = true;
			}

			public void setValue( int value )
			{
				if ( getValueIsAdjusting() )
					autoscroll = getMaximum() - getVisibleAmount() - getValue() < 10;

				if ( autoscroll || getValueIsAdjusting() )
					super.setValue( value );
			}

			protected void fireAdjustmentValueChanged( int id, int type, int value )
			{
				if ( autoscroll || getValueIsAdjusting() )
					super.fireAdjustmentValueChanged( id, type, value );
			}

			public void setValues( int newValue, int newExtent, int newMin, int newMax )
			{
				if ( autoscroll || getValueIsAdjusting() )
					super.setValues( newValue, newExtent, newMin, newMax );
				else
					super.setValues( getValue(), newExtent, newMin, newMax );
			}
		}

		/**
		 * An action listener responsible for sending the text
		 * contained within the entry panel to the KoL chat
		 * server for processing.  This listener spawns a new
		 * request to the server which then handles everything
		 * that's needed.
		 */

		private class CommandEntryListener extends KeyAdapter implements ActionListener, Runnable
		{
			private String command;

			public void actionPerformed( ActionEvent e )
			{	submitCommand();
			}

			public void keyReleased( KeyEvent e )
			{
				if ( e.getKeyCode() == KeyEvent.VK_UP )
				{
					if ( lastCommandIndex <= 0 )
						return;

					entryField.setText( (String) recentCommands.get( --lastCommandIndex ) );
				}
				else if ( e.getKeyCode() == KeyEvent.VK_DOWN )
				{
					if ( lastCommandIndex + 1 >= recentCommands.size() )
						return;

					entryField.setText( (String) recentCommands.get( ++lastCommandIndex ) );
				}
				else if ( e.getKeyCode() == KeyEvent.VK_ENTER )
					submitCommand();
			}

			private void submitCommand()
			{
				this.command = entryField.getText().trim();

				recentCommands.add( command );
				lastCommandIndex = recentCommands.size();

				(new DaemonThread( this )).start();
				entryField.setText( "" );
			}

			public void run()
			{
				commandBuffer.append( "<font color=olive>&nbsp;&gt;&nbsp;" + command + "</font><br>" );
				instance.executeLine( command );
				commandBuffer.append( "<br>" );
				client.enableDisplay();
			}
		}
	}

	/**
	 * The main method used in the event of testing the way the
	 * user interface looks.  This allows the UI to be tested
	 * without having to constantly log in and out of KoL.
	 */

	public static void main( String [] args )
	{	(new CreateFrameRunnable( CommandDisplayFrame.class )).run();
	}
}
