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
 * AbstractMethodHelper.java
 *
 * Created on December 20, 2001, 5:30 PM
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.enterprise.deployment.MethodDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.PersistenceDescriptor;
import org.glassfish.ejb.deployment.descriptor.QueryDescriptor;

/** This is a helper class which extracts the information needed for method
 * code generation of the concrete bean class.
 *
 * @author Rochelle Raccah
 */
abstract public class AbstractMethodHelper
{
	/** Constant representing a local interface return type. */
	public static final int LOCAL_RETURN = 0;

	/** Constant representing a remote interface return type. */
	public static final int REMOTE_RETURN = 1;

	/** Constant representing no return type. */
	public static final int NO_RETURN = 2;

	private EjbCMPEntityDescriptor _cmpDescriptor;
	private List finders = new ArrayList();
	private List selectors = new ArrayList();
	//private ArrayList otherMethods = new ArrayList();
	private List createMethods = new ArrayList();
	private Map methodNames = new HashMap();

	/** Creates a new instance of AbstractMethodHelper
	 * @param descriptor the EjbCMPEntityDescriptor which defines the 
	 * information for this bean.
	 */
	public AbstractMethodHelper (EjbCMPEntityDescriptor descriptor)
	{
		_cmpDescriptor = descriptor;
		categorizeMethods();	// Separate methods into categories.
	}

	/** Gets the EjbCMPEntityDescriptor which defines the 
	 * information for this bean.
	 * @return the EjbCMPEntityDescriptor for the bean specified in the
	 * constructor.
	 */
	protected EjbCMPEntityDescriptor getDescriptor() { return _cmpDescriptor; }

	/**
	 * Reads all known methods and sorts them by name into specific
	 * Collections for further processing.
	 */
	protected void categorizeMethods ()
	{
		EjbCMPEntityDescriptor descriptor = getDescriptor();
		Iterator iterator = descriptor.getMethodDescriptors().iterator();

		while (iterator.hasNext())
		{
			MethodDescriptor methodDescriptor = 
				(MethodDescriptor)iterator.next();
			Method method = methodDescriptor.getMethod(descriptor);
			String methodName = methodDescriptor.getName();

			//if (DEBUG)
			//	System.out.println("Method: " + methodName); // NOI18N

			if (methodName.startsWith(CMPTemplateFormatter.find_))
				finders.add(method);
			else if (methodName.startsWith(CMPTemplateFormatter.ejbSelect_))
				selectors.add(method); 
			else if (methodName.startsWith(CMPTemplateFormatter.create_))
				createMethods.add(method);
			else if (methodName.startsWith(CMPTemplateFormatter.get_) || 
				methodName.startsWith(CMPTemplateFormatter.set_))
			{
				;// skip
			}
			//else
			//	otherMethods.add(method);

			// It is OK to use HashMap here as we won't use it for possible 
			// overloaded methods. 
			methodNames.put(methodName, method);
		}
	}

	/** Gets the list of finder methods for this bean.
	 * @return a list of java.lang.reflect.Method objects which represent 
	 * the finders for this bean
	 */
	public List getFinders () { return finders; }

	// give subclasses a chance to replace the list
	protected void setFinders (List finderList)
	{
		finders = finderList;
	}

	/** Gets the list of selector methods for this bean.
	 * @return a list of java.lang.reflect.Method objects which represent 
	 * the selectors for this bean
	 */
	public List getSelectors () { return selectors; }

	// give subclasses a chance to replace the list
	protected void setSelectors (List selectorList)
	{
		selectors = selectorList;
	}

	/** Gets the list of ejb create methods for this bean.
	 * @return a list of java.lang.reflect.Method objects which represent 
	 * the ejb create methods for this bean
	 */
	public List getCreateMethods () { return createMethods; }

	// might need this later
	//public List getOtherMethods () { return otherMethods; }

	/** Gets a map of the method names for this bean.  The keys are the 
	 * method names and the values are the java.lang.reflect.Method objects.
	 * These should represent all methods of this bean.
	 * @return a map of the method names to java.lang.reflect.Method objects 
	 * for this bean
	 */
	public Map getMethodNames () { return methodNames; }

	/** Gets the name of the local home which corresponds to this bean.
	 * @return the name of the local home class
	 */
	public String getLocalHome ()
	{
		return getDescriptor().getLocalHomeClassName();
	}
	
	/** Gets the name of the remote home which corresponds to this bean.
	 * @return the name of the remote home class
	 */
	public String getRemoteHome ()
	{
		return getDescriptor().getHomeClassName();
	}

	/** Gets the query descriptor associated with the specified method if it 
	 * exists.
	 * @param method the java.lang.reflect.Method object used to find the 
	 * query string
	 * @return a query descriptor for the specified method. Returns
	 * <code>null</code> for CMP 1.1 queries.
	 */
	protected QueryDescriptor getQueryDescriptor (Method method)
	{
		PersistenceDescriptor persistenceDescriptor =
			getDescriptor().getPersistenceDescriptor();
		return persistenceDescriptor.getQueryFor(method);
	}

	/** Gets the query string associated with the specified method if it 
	 * exists.
	 * @param method the java.lang.reflect.Method object used to find the 
	 * query string
	 * @return a query string for the specified method
	 */
	public String getQueryString (Method method)
	{
		QueryDescriptor queryDescriptor = getQueryDescriptor(method);

		return ((queryDescriptor != null) ? queryDescriptor.getQuery() : null);
	}

	/** Gets the return type associated with the specified method if it 
	 * exists.  If no corresponding query descriptor is found, the value
	 * <code>NO_RETURN</code> is returned.
	 * @param method the java.lang.reflect.Method object used to find the 
	 * query return type
	 * @return the return type for the specified method, one of 
	 * {@link #LOCAL_RETURN}, {@link #REMOTE_RETURN}, or {@link #NO_RETURN}
	 */
	public int getQueryReturnType (Method method)
	{
		QueryDescriptor queryDescriptor = getQueryDescriptor(method);

		if (queryDescriptor != null)
		{
			if (queryDescriptor.getHasLocalReturnTypeMapping())
				return LOCAL_RETURN;
			if (queryDescriptor.getHasRemoteReturnTypeMapping())
				return REMOTE_RETURN;
		}

		return NO_RETURN;
	}

	/** Returns <code>true</code> if prefetch is enabled for the specified 
	 * method, <code>false</code> otherwise. Prefetch is enabled by default.
	 * @param method the java.lang.reflect.Method object used to find the 
	 * prefetch setting.
	 * @return a boolean representing the prefetch setting
	 */
	abstract public boolean isQueryPrefetchEnabled (Method method);

	/** Gets the jdo filter expression associated with the specified method 
	 * if it exists.  Note that this method should only be used for CMP 1.1 - 
	 * use {@link #getQueryString} for CMP 2.0.
	 * @param method the java.lang.reflect.Method object used to find the 
	 * query filter
	 * @return the jdo filter expression
	 */
	abstract public String getJDOFilterExpression (Method method);

	/** Gets the jdo parameter declaration associated with the specified 
	 * method if it exists.  Note that this method should only be used for 
	 * CMP 1.1 - use {@link #getQueryString} for CMP 2.0.
	 * @param method the java.lang.reflect.Method object used to find the 
	 * parameter declaration
	 * @return the jdo parameter declaration
	 */
	abstract public String getJDOParameterDeclaration (Method method);

	/** Gets the jdo variables declaration associated with the specified 
	 * method if it exists.  Note that this method should only be used for 
	 * CMP 1.1 - use {@link #getQueryString} for CMP 2.0.
	 * @param method the java.lang.reflect.Method object used to find the 
	 * parameter declaration
	 * @return the jdo variables declaration
	 */
	abstract public String getJDOVariableDeclaration (Method method);

	/** Gets the jdo ordering specification associated with the specified 
	 * method if it exists.  Note that this method should only be used for
	 * CMP 1.1 - use {@link #getQueryString} for CMP 2.0.
	 * @param method the java.lang.reflect.Method object used to find the 
	 * parameter declaration
	 * @return the jdo ordering specification
	 */
	abstract public String getJDOOrderingSpecification (Method method);
}
