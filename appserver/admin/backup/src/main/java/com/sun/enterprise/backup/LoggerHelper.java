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

package com.sun.enterprise.backup;

import java.util.logging.*;

// Resource Bundle:
// com/sun/logging/enterprise/system/tools/deployment/backend/LogStrings.properties

class LoggerHelper
{
	private LoggerHelper() 
	{
	}
	
	///////////////////////////////////////////////////////////////////////////

	final static Logger get()
	{
		// the final should cause this to be inlined...
		return logger;
	}
	
	///////////////////////////////////////////////////////////////////////////

	final static void setLevel(BackupRequest req)
	{
		// the final should cause this to be inlined...
		if(req.terse)
			logger.setLevel(Level.WARNING);
		else
			logger.setLevel(Level.INFO);
		
		/* test logging messages
		 String me = System.getProperty("user.name");
		if(me != null && me.equals("bnevins"))
		{
			logger.finest("finest");
			logger.finer("finer");
			logger.fine("fine");
			logger.info("info");
			logger.warning("warning");
			logger.severe("severe");
		}
		 **/
	}

	///////////////////////////////////////////////////////////////////////////
	////////         Convenience methods        ///////////////////////////////
	///////////////////////////////////////////////////////////////////////////

	final static void finest(String s) { logger.finest(s); }
	final static void finest(String s, Object o) { logger.log(Level.FINEST, s, new Object[] { o }); }
	final static void finer(String s) { logger.finer(s); }
	final static void finer(String s, Object o) { logger.log(Level.FINER, s, new Object[] { o }); }
	final static void fine(String s) { logger.fine(s); }
	final static void fine(String s, Object o) { logger.log(Level.FINE, s, new Object[] { o }); }
	final static void info(String s) { logger.info(s); }
	final static void info(String s, Object o) { logger.log(Level.INFO, s, new Object[] { o }); }
	final static void warning(String s) { logger.warning(s); }
	final static void warning(String s, Object o) { logger.log(Level.WARNING, s, new Object[] { o }); }
	final static void severe(String s) { logger.severe(s); }
	final static void severe(String s, Object o) { logger.log(Level.SEVERE, s, new Object[] { o }); }
	
	///////////////////////////////////////////////////////////////////////////

	private static Logger	logger = null;

	static
	{
		try
		{
			//System.setProperty("java.util.logging.ConsoleHandler.level", Constants.logLevel);
			logger = Logger.getLogger("backup", Constants.loggingResourceBundle);
			
			// attach a handler that will at least be capable of spitting out FINEST messages
			// the Level of the Logger itself will determine what the handler actually gets...
			Handler h = new ConsoleHandler();
			h.setLevel(Level.FINEST);
			logger.addHandler(h);
		}
		catch(Throwable t)
		{
			try
			{
				logger = Logger.getLogger("backup");
				logger.warning("Couldn't create Backup Logger with a resource bundle.  Created a Logger without a Resource Bundle.");
			}
			catch(Throwable t2)
			{
				// now what?
			}
		}
	}
}

