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
 * AbstractLoggerFactory.java
 *
 * Created on May 13, 2002, 10:15 PM
 */

package com.sun.jdo.spi.persistence.utility.logging;

import java.util.Map;
import java.util.HashMap;

/** 
 *
 * @author Rochelle Raccah
 * @version %I%
 */
abstract public class AbstractLoggerFactory implements LoggerFactory
{
	private final static String _domainPrefix = "com.sun.jdo."; //NOI18N

	private final static Map _loggerCache = new HashMap();

	private static final String _bundleName =
		"com.sun.jdo.spi.persistence.utility.logging.Bundle"; // NOI18N


	/** Get the error logger which is used to log things during creation of 
	 * loggers.
	 */
	protected static Logger getErrorLogger () 
	{
		return LogHelper.getLogger("", _bundleName,  // NOI18N
			AbstractLoggerFactory.class.getClassLoader());
	}

	/** Get a Logger.  The class that implements this interface is responsible
	 * for creating a logger for the named component.
	 * The bundle name and class loader are passed to allow the implementation
	 * to properly find and construct the internationalization bundle.
	 * @param relativeLoggerName the relative name of this logger
	 * @param bundleName the fully qualified name of the resource bundle
	 * @param loader the class loader used to load the resource bundle, or null
	 * @return the logger
	 */
	public synchronized Logger getLogger (String relativeLoggerName, 
		String bundleName, ClassLoader loader)
	{
		String absoluteLoggerName = getAbsoluteLoggerName(relativeLoggerName);
		Logger value = (Logger)_loggerCache.get(absoluteLoggerName);

		if (value == null)
		{
			value = createLogger(absoluteLoggerName, bundleName, loader);

			if (value != null)
				_loggerCache.put(absoluteLoggerName, value);
		}

		return value;
	}

	/** Create a new Logger.  Subclasses are responsible for creating a 
	 * logger for the named component.  The bundle name and class loader 
	 * are passed to allow the implementation to properly find and 
	 * construct the internationalization bundle.
	 * @param absoluteLoggerName the absolute name of this logger
	 * @param bundleName the fully qualified name of the resource bundle
	 * @param loader the class loader used to load the resource bundle, or null
	 * @return the logger
	 */
	abstract protected Logger createLogger (String absoluteLoggerName, 
		String bundleName, ClassLoader loader);

	protected String getDomainRoot () { return _domainPrefix; }

	protected String getAbsoluteLoggerName (String relativeLoggerName)
	{
		return (relativeLoggerName.startsWith("java") ?			//NOI18N
			relativeLoggerName : (getDomainRoot() + relativeLoggerName));
	}
}
