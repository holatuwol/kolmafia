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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.text.View;
import javax.swing.text.Element;
import javax.swing.text.ViewFactory;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.FormView;
import javax.swing.text.html.ImageView;
import javax.swing.text.html.HTMLEditorKit;

import java.net.URL;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;

import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extension of a standard <code>HTMLEditorKit</code> which overrides the
 * <code>getViewFactory()</code> method in order to return a different factory
 * instance which properly handles data submission requests.
 */

public class RequestEditorKit extends HTMLEditorKit implements KoLConstants
{
	private static KoLmafia client;
	private static RequestFrame frame;

	private static final RequestViewFactory DEFAULT_FACTORY = new RequestViewFactory();

	/**
	 * Returns an extension of the standard <code>HTMLFacotry</code> which intercepts
	 * some of the form handling to ensure that <code>KoLRequest</code> objects
	 * are instantiated on form submission rather than the <code>HttpRequest</code>
	 * objects created by the default HTML editor kit.
	 */

	public ViewFactory getViewFactory()
	{	return DEFAULT_FACTORY;
	}

	/**
	 * Registers the client that is supposed to be used for handling data submission
	 * to the Kingdom of Loathing server.
	 */

	private static class RequestViewFactory extends HTMLFactory
	{
		public View create( Element elem )
		{
			if ( elem.getAttributes().getAttribute( StyleConstants.NameAttribute ) == HTML.Tag.INPUT )
				return new KoLSubmitView( elem );

			if ( elem.getAttributes().getAttribute( StyleConstants.NameAttribute ) == HTML.Tag.IMG )
				return new KoLImageView( elem );

			return super.create( elem );
		}
	}

	private static class KoLImageView extends ImageView
	{
		public KoLImageView( Element elem )
		{	super( elem );
		}

	    public URL getImageURL()
		{
			String src = (String) getElement().getAttributes().getAttribute( HTML.Attribute.SRC );

			if ( src == null )
				return null;

			File localfile = new File( "images" + File.separator + src.replaceAll( "http://images.kingdomofloathing.com/", "" ) );

			try
			{
				if ( localfile.exists() )
					return localfile.toURL();
			}
			catch ( Exception e )
			{
				// Because you're loading the image from a
				// file which exists, no exception should
				// occur at this point.  Should it happen,
				// however, because the file already exists,
				// it cannot be redownloaded, so return null.

				e.printStackTrace();
				return null;
			}

			// Because the image icon could not be retrieved locally,
			// download the image from the KoL web server.  Then
			// save it to a local file, and then return the reference
			// to the local file so that the image viewer does not
			// attempt to redownload it.

			try
			{
				BufferedInputStream in = new BufferedInputStream( (new URL( src )).openConnection().getInputStream() );
				localfile.getParentFile().mkdirs();
				FileOutputStream out = new FileOutputStream( localfile );

				int offset;
				byte [] buffer = new byte[1024];

				while ( (offset = in.read( buffer )) > 0 )
					out.write( buffer, 0, offset );

				in.close();
				out.flush();
				out.close();

				return localfile.toURL();
			}
			catch ( Exception e )
			{
				// If an IOException occurs at any time during the
				// attempt to retrieve the image, return null.

				e.printStackTrace();
				return null;
			}
		}
	}

	private static class KoLSubmitView extends FormView
	{
		public KoLSubmitView( Element elem )
		{	super( elem );
		}

		protected Component createComponent()
		{
			Component c = super.createComponent();

			if ( c != null && (c instanceof JButton || c instanceof JRadioButton || c instanceof JCheckBox) )
				c.setBackground( Color.white );

			return c;
		}

		protected void submitData( String data )
		{
			String [] elements = data.split( "&" );
			String [] fields = new String[ elements.length ];

			int valueIndex = 0;

			if ( elements[0].length() > 0 )
			{
				for ( int i = 0; i < elements.length; ++i )
					fields[i] = elements[i].substring( 0, elements[i].indexOf( "=" ) );
			}
			else
				fields[0] = "";

			// First, attempt to retrieve the frame which
			// is being used by this form viewer.

			String frameText = null;
			RequestFrame frame = null;
			Object [] frames = existingFrames.toArray();

			for ( int i = 0; i < frames.length && frame == null; ++i )
			{
				if ( frames[i] instanceof RequestFrame )
				{
					frame = (RequestFrame) frames[i];
					frameText = frame.mainDisplay.getText();

					for ( int j = 0; j < fields.length && frame != null; ++j )
						if ( frameText.indexOf( fields[j] ) == -1 )
							frame = null;
				}
			}

			// If there is no frame, then there's nothing to
			// refresh, so return.

			if ( frame == null )
				return;

			// Next, retrieve the form element so that
			// you know where you need to submit the data.

			Element formElement = getElement();

			while ( formElement != null && formElement.getAttributes().getAttribute( StyleConstants.NameAttribute ) != HTML.Tag.FORM )
				formElement = formElement.getParentElement();

			// At this point, if the form element is null,
			// then there was no enclosing form for the
			// <INPUT> tag, so you can return, doing nothing.

			if ( formElement == null )
				return;

			// Now that you know you have a form element,
			// get the action field, attach the data, and
			// refresh the appropriate request frame.

			String action = (String) formElement.getAttributes().getAttribute( HTML.Attribute.ACTION );

			// If there is no action, how do we know which page to
			// connect to?  We assume it's the originating page.

			if ( action == null )
				action = frame.getCurrentLocation();

			// Prepare the element string -- make sure that
			// you don't have duplicate fields.

			for ( int i = 0; i < elements.length; ++i )
				for ( int j = i + 1; j < elements.length; ++j )
					if ( elements[i] != null && elements[j] != null && fields[i].equals( fields[j] ) )
						elements[j] = null;

			// Now, prepare the request string that will
			// be posted to KoL.

			KoLRequest request;

			if ( action.indexOf( "?" ) != -1 )
			{
				StringBuffer actionString = new StringBuffer();
				actionString.append( action );

				for ( int i = 0; i < elements.length; ++i )
					if ( elements[i] != null )
					{
						actionString.append( '&' );
						actionString.append( elements[i] );
					}

				// For quirky URLs where there's a question mark
				// in the middle of the URL, just string the data
				// onto the URL.  This is the way browsers work,
				// so it's the way KoL expects the data.

				request = new KoLRequest( frame.client, actionString.toString(), true );
			}
			else
			{
				// For normal URLs, the form data can be submitted
				// just like in every other request.

				request = new KoLRequest( frame.client, action, true );
				if ( elements[0].length() > 0 )
					for ( int i = 0; i < elements.length; ++i )
						if ( elements[i] != null )
							request.addFormField( elements[i] );
			}

			frame.refresh( request );
		}
	}
}
