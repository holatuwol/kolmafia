/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.parsetree;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Iterator;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import net.sourceforge.kolmafia.textui.ScriptException;

public class LibraryFunction
	extends Function
{
	private Method method;
	private final String description;
	private final Value[] values;
	public static Interpreter interpreter;

	public LibraryFunction( final String name, final Type type, final Type[] params )
	{
		this( name, type, params, null );
	}

	public LibraryFunction( final String name, final Type type, final Type[] params,
		final String description )
	{
		super( name.toLowerCase(), type );
		this.description = description;

		this.values = new Value[ params.length ];
		Class[] args = new Class[ params.length ];

		for ( int i = 0; i < params.length; ++i )
		{
			Variable variable = new Variable( params[ i ] );
			this.variableReferences.add( new VariableReference( variable ) );
			args[ i ] = Value.class;
		}

		try
		{
			this.method = RuntimeLibrary.findMethod( name, args );
		}
		catch ( Exception e )
		{
			// This should not happen; it denotes a coding
			// error that must be fixed before release.

			StaticEntity.printStackTrace( e, "No method found for built-in function: " + name );
		}
	}

	public String getDescription()
	{
		return this.description;
	}

	public Value execute( final Interpreter interpreter )
	{
		if ( !KoLmafia.permitsContinue() )
		{
			interpreter.setState( Interpreter.STATE_EXIT );
			return null;
		}

		if ( StaticEntity.isDisabled( this.getName() ) )
		{
			this.printDisabledMessage( interpreter );
			return this.getType().initialValue();
		}

		if ( this.method == null )
		{
			throw interpreter.runtimeException( "Internal error: no method for " + this.getName() );
		}

		// Dereference variables and pass ScriptValues to function

		Iterator it = this.variableReferences.iterator();
		for ( int i = 0; it.hasNext(); ++i )
		{
			VariableReference current = (VariableReference) it.next();
			this.values[i] = current.getValue( interpreter );
		}

		try
		{
			// Invoke the method
			LibraryFunction.interpreter = interpreter;
			return (Value) this.method.invoke( this, (Object []) this.values );
		}
		catch ( InvocationTargetException e )
		{
			// This is an error in the called method. Pass
			// it on up so that we'll print a stack trace.

			Throwable cause = e.getCause();
			if ( cause instanceof ScriptException )
			{
				// Pass up exceptions intentionally generated by library
				throw (ScriptException) cause;
			}
			throw new RuntimeException( cause );
		}
		catch ( IllegalAccessException e )
		{
			// This is not expected, but is an internal error in ASH
			throw new ScriptException( e );
		}
		finally
		{
			LibraryFunction.interpreter = null;
		}
	}
}
