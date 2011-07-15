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
 * LoggerJDK13.java
 *
 * Created on May 15, 2002, 2:00 PM
 */

package com.sun.jdo.spi.persistence.utility.logging;

import java.io.PrintStream;

/** 
 * This class provides an implementation of the 
 * com.sun.jdo.spi.persistence.utility.Logger interface which 
 * subclasses the AbstractLogger and provides an implementation of 
 * its abstract methods which logs to a PrintStream (System.out).
 * Note that this logger doesn't explicitly flush the PrintStream and 
 * depends on the JVM for flushing.
 *
 * @author Rochelle Raccah
 * @version %I%
 */
public class LoggerJDK13 extends AbstractLogger
{
	private static final PrintStream _printStream = System.out;

	/** Creates a new LoggerJDK13.  The supplied class loader or the
	 * loader which loaded this class must be able to load the bundle.
	 * @param loggerName the full domain name of this logger
	 * @param bundleName the bundle name for message translation
	 * @param loader the loader used for looking up the bundle file
	 * and possibly the logging.properties or alternative file
	 */
	public LoggerJDK13 (String loggerName, String bundleName, 
		ClassLoader loader)
	{
		super(loggerName, bundleName, loader);
	}

	private static PrintStream getPrintStream () { return _printStream; }

	/**
	 * Log a message.
	 * <p>
	 * If the logger is currently enabled for the message 
	 * level then the given message, and the exception dump, 
	 * is forwarded to all the
	 * registered output Handler objects.
	 * <p>
	 * @param level The level for this message
	 * @param msg The string message (or a key in the message catalog)
	 * @param thrown The exception to log
	 */
	public synchronized void log (int level, String msg, Throwable thrown)
	{
		if (isLoggable(level))
		{
			logInternal(level, getMessage(msg));
			thrown.printStackTrace(getPrintStream());
		}
	}

	/**
	 * This method does the actual logging.  It is expected that if a 
	 * check for isLoggable is desired for performance reasons, it has 
	 * already been done, as it should not be done here.  This 
	 * implementation uses a print stream for logging.
	 * @param level the level to print
	 * @param message the message to print
	 */
	protected synchronized void logInternal (int level, String message)
	{
		getPrintStream().println(getMessageWithPrefix(level, message));
	}
}
