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
 * BackupWarningException.java
 *
 * Created on April 1, 2004, 6:23 PM
 */

package com.sun.enterprise.backup;

import com.sun.enterprise.util.StringUtils;

/**
 *
 * @author  bnevins
 * Problem -- some "errors" should not be handled by CLI as "errors", but as warnings.
 * However, the Exception throwing mechanism is the non-kludgiest way to get the
 * message back to the command handler in CLI.
 * Thus this class.
 */
public class BackupWarningException extends BackupException
{
	/**
	 * Constructs a BackupWarningException with a possibly i18n'd detail message.
	 * @param s the detail message which is first checked for as a key for an i18n string.  
	 * If not found it will be used as the message itself.
	 */	
	public BackupWarningException(String s)
	{
		super(s);
	}
	
	/**
	 * @param s the detail message which is first checked for as a key for an i18n string.  
	 * If not found it will be used as the message itself.
	 * @param o the parameter for the recovered i18n string. I.e. "{0}" will be
	 * replaced with o.toString().  If there is no i18n string located
	 * o will be ignored.
	 */	
	public BackupWarningException(String s, Object o)
	{
		super(s, o);
	}
}
