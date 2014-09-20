/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
 * MethodHelper.java
 *
 * Created on December 20, 2001, 5:30 PM
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ResourceBundle;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.jdo.spi.persistence.utility.generator.JavaClassWriterHelper;
import org.glassfish.ejb.deployment.descriptor.IASEjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.QueryDescriptor;
import org.glassfish.ejb.deployment.descriptor.runtime.IASEjbCMPFinder;
import org.glassfish.ejb.deployment.descriptor.runtime.PrefetchDisabledDescriptor;
import org.glassfish.persistence.common.I18NHelper;

/** This is a subclass of {@link AbstractMethodHelper} 
 * which provides overridden method implementations based on an SunONE
 * implementation.
 *
 * @author Rochelle Raccah
 */
public class MethodHelper extends AbstractMethodHelper
{
	/** I18N message handler */
	private final static ResourceBundle messages = I18NHelper.loadBundle(
        	MethodHelper.class);

	/** Creates a new instance of MethodHelper
	 * @param descriptor the IASEjbCMPEntityDescriptor which defines the 
	 * information for this bean.
	 */
	public MethodHelper (IASEjbCMPEntityDescriptor descriptor)
	{
		super(descriptor);
	}

	/**
	 * Reads all known methods and sorts them by name into specific
	 * Collections for further processing.
	 */
	protected void categorizeMethods ()
	{
		IASEjbCMPEntityDescriptor cmpDescriptor = 
			(IASEjbCMPEntityDescriptor)getDescriptor();

		super.categorizeMethods();

		// replace the finders and selectors with ias specific info
		setFinders(getListForCollection(cmpDescriptor.getFinders()));
		setSelectors(getListForCollection(cmpDescriptor.getSelectors()));
	}

	// returns a non-null list for the supplied collection
	private static ArrayList getListForCollection (Collection aCollection)
	{
		return ((aCollection != null) ? 
			new ArrayList(aCollection) : new ArrayList());
	}

	/** Returns <code>true</code> if prefetch is enabled for the specified
	 * method, <code>false</code> otherwise. Prefetch is enabled by default.
	 * @param method the java.lang.reflect.Method object used to find the
	 * prefetch setting.
	 * @return a boolean representing the prefetch setting
	 */
	public boolean isQueryPrefetchEnabled (Method method) 
	{
		boolean enabled = true;
		QueryDescriptor queryDescriptor = getQueryDescriptor(method);

		if (queryDescriptor != null) 
		{
			IASEjbCMPEntityDescriptor cmpDescriptor = 
				(IASEjbCMPEntityDescriptor)getDescriptor();
			PrefetchDisabledDescriptor pdDescriptor = 
				cmpDescriptor.getPrefetchDisabledDescriptor();

			if (pdDescriptor != null)
			{
				MethodDescriptor methodDescriptor =
					queryDescriptor.getQueryMethodDescriptor();

				enabled = !pdDescriptor.isPrefetchDisabledFor(
					methodDescriptor);
			}
		}

		return enabled;
	}

	/** Gets the jdo filter expression associated with the specified method 
	 * if it exists.  Note that this method should only be used for CMP 1.1 - 
	 * use {@link #getQueryString} for CMP 2.0.
	 * @param method the java.lang.reflect.Method object used to find the 
	 * query filter
	 * @return the jdo filter expression
	 */
	public String getJDOFilterExpression (Method method)
	{
		IASEjbCMPFinder cmpFinder = getFinder(method);

		return ((cmpFinder != null) ? cmpFinder.getQueryFilter() : null);
	}

	/** Gets the jdo parameter declaration associated with the specified 
	 * method if it exists.  Note that this method should only be used for 
	 * CMP 1.1 - use {@link #getQueryString} for CMP 2.0.
	 * @param method the java.lang.reflect.Method object used to find the 
	 * parameter declaration
	 * @return the jdo parameter declaration
	 */
	public String getJDOParameterDeclaration (Method method)
	{
		IASEjbCMPFinder cmpFinder = getFinder(method);

		return ((cmpFinder != null) ? 
			cmpFinder.getQueryParameterDeclaration() : null);
	}

	/** Gets the jdo variables declaration associated with the specified 
	 * method if it exists.  Note that this method should only be used for 
	 * CMP 1.1 - use {@link #getQueryString} for CMP 2.0.
	 * @param method the java.lang.reflect.Method object used to find the 
	 * parameter declaration
	 * @return the jdo variables declaration
	 */
	public String getJDOVariableDeclaration (Method method)
	{
		IASEjbCMPFinder cmpFinder = getFinder(method);

		return ((cmpFinder != null) ? cmpFinder.getQueryVariables() : null);
	}

	/** Gets the jdo ordering specification associated with the specified
	 * method if it exists.  Note that this method should only be used for
	 * CMP 1.1 - use {@link #getQueryString} for CMP 2.0.
	 * @param method the java.lang.reflect.Method object used to find the
	 * parameter declaration
	 * @return the jdo ordering specification
	 */
	public String getJDOOrderingSpecification (Method method)
        {
		IASEjbCMPFinder cmpFinder = getFinder(method);

		return ((cmpFinder != null) ? cmpFinder.getQueryOrdering() : null);
        }


	private IASEjbCMPFinder getFinder (Method method)
	{
		IASEjbCMPEntityDescriptor cmpDescriptor = 
			(IASEjbCMPEntityDescriptor)getDescriptor();
		IASEjbCMPFinder finder = cmpDescriptor.getIASEjbCMPFinder(method);

		if (finder == null) {
			String methodSignature = cmpDescriptor.getName() + '.' +
				method.getName() + 
				JavaClassWriterHelper.parenleft_ +
				JavaClassWriterHelper.getParameterTypesList(method) + 
				JavaClassWriterHelper.parenright_ ;
			String msg = I18NHelper.getMessage(messages, 
				"EXC_MissingCMP11Finder", methodSignature);//NOI18N
			throw new RuntimeException(msg);
		}

		return finder;
	}
}
