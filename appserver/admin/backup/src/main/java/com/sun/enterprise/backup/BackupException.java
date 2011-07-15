/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

/*
 * BackupException.java
 *
 * Created on January 21, 2004, 3:51 PM
 */


package com.sun.enterprise.backup;


/** 
 * Backup-Restore <strong>guarantees</strong> that this will be the one and only one kind of
 * Exception that will ever be thrown.  All fatal errors will
 * result in this Exception being thrown.  This is a checked Exception so callers
 * will be forced to deal with it.  <p>
 * the class features built-in i18n.  I.e. any String passed to a BackupException
 * constructor will first be used as a key into the i18n Strings.  If it is not
 * found, the String itself will be used as the messsage.
 *
 * @author bnevins
 */

public class BackupException extends Exception
{
	/**
	 * Constructs a BackupException with a possibly i18n'd detail message.
	 * @param s the detail message which is first checked for as a key for an i18n string.  
	 * If not found it will be used as the message itself.
	 */	
	public BackupException(String s)
	{
		super(StringHelper.get(s));
	}
	
	/**
	 * @param s the detail message which is first checked for as a key for an i18n string.  
	 * If not found it will be used as the message itself.
	 * @param o the parameter for the recovered i18n string. I.e. "{0}" will be
	 * replaced with o.toString().  If there is no i18n string located
	 * o will be ignored.
	 */	
	public BackupException(String s, Object o)
	{
		super(StringHelper.get(s, o));
	}

	/**
	 * @param s the detail message which is first checked for as a key for an i18n string.  
	 * If not found it will be used as the message itself.
	 * @param t the cause.
	 */	
	public BackupException(String s, Throwable t)
	{
		super(StringHelper.get(s), t);
	}

	/**
	 * @param s the detail message which is first checked for as a key for an i18n string.  
	 * If not found it will be used as the message itself.
	 * @param t the cause.
	 * @param o the parameter for the recovered i18n string. I.e. "{0}" will be
	 * replaced with o.toString().  If there is no i18n string located
	 * o will be ignored.
	 */	
	public BackupException(String s, Throwable t, Object o)
	{
		super(StringHelper.get(s, o), t);
	}
}
