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
 * ClassLoaderStrategy.java
 *
 * Created on August 29, 2003, 3:19 PM
 */

package com.sun.jdo.api.persistence.model;

/**
 *
 * @author mvatkina
 * @version %I%
 */
public class ClassLoaderStrategy
{
	/** System property key used to define the model behavior concerning
	 * multiple class loaders. 
	 * Value should be one of 
	 * {@link #MULTIPLE_CLASS_LOADERS_IGNORE},
	 * {@link #MULTIPLE_CLASS_LOADERS_RELOAD}, or 
	 * {@link #MULTIPLE_CLASS_LOADERS_ERROR}
	 */
	public static final String PROPERTY_MULTIPLE_CLASS_LOADERS =
		"com.sun.jdo.api.persistence.model.multipleClassLoaders"; //NOI18N
	
	/** Constant representing the value "ignore" of the System property
	 * com.sun.jdo.api.persistence.model.multipleClassLoaders
	 * Setting the system property to "ignore" causes the model to ignore
	 * any new class loader for the same fully qualified class name.
	 * @see RuntimeModel#findClassLoader
	 */
	public static final String MULTIPLE_CLASS_LOADERS_IGNORE = "ignore"; //NOI18N

	/** Constant representing the value "reload" of the System property
	 * com.sun.jdo.api.persistence.model.multipleClassLoaders 
	 * Setting the system property to "reload" causes the model to reload 
	 * the class mapping if it is specified with a new class loader.
	 * @see RuntimeModel#findClassLoader
	 */
	public static final String MULTIPLE_CLASS_LOADERS_RELOAD = "reload"; //NOI18N

	/** Constant representing the value "error" of the System property
	 * com.sun.jdo.api.persistence.model.multipleClassLoaders
	 * Setting the system property to "reload" causes the model to throw an
	 * exception if the same class is used with a diferent class loader.
	 * @see RuntimeModel#findClassLoader
	 */
	public static final String MULTIPLE_CLASS_LOADERS_ERROR = "error"; //NOI18N

	/** Value of the property used to define the model behavior concerning
	 * multiple class loaders.
	 */
	private static String _strategy = System.getProperty(
		PROPERTY_MULTIPLE_CLASS_LOADERS, MULTIPLE_CLASS_LOADERS_ERROR);

	/** Get the value of the property 
	 * {@link #PROPERTY_MULTIPLE_CLASS_LOADERS} used to define the model 
	 * behavior concerning multiple class loaders.
	 * @return the value of the property, one of
	 * {@link #MULTIPLE_CLASS_LOADERS_IGNORE},
	 * {@link #MULTIPLE_CLASS_LOADERS_RELOAD}, or
	 * {@link #MULTIPLE_CLASS_LOADERS_ERROR}
	 */
	public static String getStrategy ()
	{
		return _strategy;
	}

	/** Sets the value of the property 
	 * {@link #PROPERTY_MULTIPLE_CLASS_LOADERS} used to define the model 
	 * behavior concerning multiple class loaders.
	 * @param strategy the new value of the property. Value should be one of
	 * {@link #MULTIPLE_CLASS_LOADERS_IGNORE},
	 * {@link #MULTIPLE_CLASS_LOADERS_RELOAD}, or
	 * {@link #MULTIPLE_CLASS_LOADERS_ERROR}
	 * @see RuntimeModel#findClassLoader
	 */
	public static void setStrategy (String strategy)
	{
		_strategy = strategy;
	}

}
