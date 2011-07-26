/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.jauth;

import java.util.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.AppConfigurationEntry;

/**
 * Shared logic from Client and ServerAuthContext reside here.
 */
final class AuthContext {

    static final String INIT			= "initialize";
    static final String DISPOSE_SUBJECT		= "disposeSubject";

    static final String SECURE_REQUEST		= "secureRequest";
    static final String VALIDATE_RESPONSE	= "validateResponse";

    static final String VALIDATE_REQUEST	= "validateRequest";
    static final String SECURE_RESPONSE		= "secureResponse";

    // managesSessions method is implemented by looking for 
    // corresponding option value in module configuration
    static final String MANAGES_SESSIONS	= "managesSessions";
    static final String MANAGES_SESSIONS_OPTION	= "managessessions";

    private ConfigFile.Entry[] entries;
    private Logger logger;

    AuthContext(ConfigFile.Entry[] entries,
		Logger logger) throws AuthException {

	this.entries = entries;
	this.logger = logger;
    }

    /**
     * Invoke modules according to configuration
     */
    Object[] invoke(final String methodName, final Object[] args)
		throws AuthException {

    // invoke modules in a doPrivileged
	final Object rValues[] = new Object[entries.length];

	try {
	    java.security.AccessController.doPrivileged
		(new java.security.PrivilegedExceptionAction() {
		public Object run() throws AuthException {
		    invokePriv(methodName, args, rValues);
		    return null;
		}
	    });
	} catch (java.security.PrivilegedActionException pae) {
	    if (pae.getException() instanceof AuthException) {
		throw (AuthException)pae.getException();
	    } else {
		AuthException ae = new AuthException();
		ae.initCause(pae.getException());
		throw ae;
	    }
	}
	return rValues;
    }

    void invokePriv(String methodName, Object[] args, Object[] rValues)
	throws AuthException {

	// special treatment for managesSessions until the module
	// interface can be extended.
	if (methodName.equals(AuthContext.MANAGES_SESSIONS)) {
	    for (int i = 0; i < entries.length; i++) {
		Map options = entries[i].getOptions();
		String mS = (String) options.get(AuthContext.MANAGES_SESSIONS_OPTION); 
		rValues[i] = Boolean.valueOf(mS);
	    }
	    return;
	}

	boolean success = false;
	AuthException firstRequiredError = null;
	AuthException firstError = null;

	// XXX no way to reverse module invocation

	for (int i = 0; i < entries.length; i++) {

	    // get initialized module instance
	
	    Object module = entries[i].module;

	    // invoke the module

	    try {
		Method[] mArray = module.getClass().getMethods();
		for (int j = 0; j < mArray.length; j++) {
		    if (mArray[j].getName().equals(methodName)) {

			// invoke module
			rValues[i] = mArray[j].invoke(module, args);

			// success -
			// return if SUFFICIENT and no previous REQUIRED errors
			
			if (firstRequiredError == null &&
			    entries[i].getControlFlag() ==
		      AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT) {

			    if (logger != null && logger.isLoggable(Level.FINE)) {
                                logger.fine(entries[i].getLoginModuleName() +
					"." +
					methodName +
					" SUFFICIENT success");
			    }

			    return;
			}

			if (logger != null && logger.isLoggable(Level.FINE)) {
                                logger.fine(entries[i].getLoginModuleName() +
					"." +
					methodName +
					" success");
			}

			success = true;
			break;
		    }
		}

		if (!success) {
		    // PLEASE NOTE:
		    // this exception will be thrown if any module 
		    // in the context does not support the method.
		    NoSuchMethodException nsme = 
			new NoSuchMethodException("module " +
				module.getClass().getName() +
				" does not implement " +
				methodName);
		    AuthException ae = new AuthException();
		    ae.initCause(nsme);
		    throw ae;
		}
	    } catch (IllegalAccessException iae) {
		AuthException ae = new AuthException();
		ae.initCause(iae);
		throw ae;
	    } catch (InvocationTargetException ite) {

		// failure cases

		AuthException ae;

		if (ite.getCause() instanceof AuthException) {
		    ae = (AuthException)ite.getCause();
		} else {
		    ae = new AuthException();
		    ae.initCause(ite.getCause());
		}

		if (entries[i].getControlFlag() ==
		    AppConfigurationEntry.LoginModuleControlFlag.REQUISITE) {

		    if (logger != null && logger.isLoggable(Level.FINE)) {
                                logger.fine(entries[i].getLoginModuleName() +
					"." +
					methodName +
					" REQUISITE failure");
		    }

		    // immediately throw exception

		    if (firstRequiredError != null) {
			throw firstRequiredError;
		    } else {
			throw ae;
		    }

		} else if (entries[i].getControlFlag() ==
			AppConfigurationEntry.LoginModuleControlFlag.REQUIRED) {

		    if (logger != null && logger.isLoggable(Level.FINE)) {
                            logger.fine(entries[i].getLoginModuleName() +
                                    "." +
                                    methodName +
                                    " REQUIRED failure");
		    }

		    // save exception and continue

		    if (firstRequiredError == null) {
			firstRequiredError = ae;
		    }

		} else {

		    if (logger != null && logger.isLoggable(Level.FINE)) {
                        logger.fine(entries[i].getLoginModuleName() +
                                "." +
                                methodName +
                                " OPTIONAL failure");
		    }

		    // save exception and continue

		    if (firstError == null) {
			firstError = ae;
		    }
		}
	    }
	}

	// done invoking entire stack of modules

	if (firstRequiredError != null) {
	    throw firstRequiredError;
	} else if (firstError != null && !success) {
	    throw firstError;
	}

	// if no errors, return gracefully
	if (logger != null && logger.isLoggable(Level.FINE)) {
            logger.fine("overall " + methodName + " success");
	}
    }
}
