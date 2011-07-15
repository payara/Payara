/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

/* CVS information
 * $Header: /cvs/glassfish/jmx-remote/rjmx-impl/src/java/com/sun/enterprise/admin/jmx/remote/internal/MBeanServerConnectionExceptionThrower.java,v 1.3 2005/12/25 04:26:33 tcfujii Exp $
 * $Revision: 1.3 $
 * $Date: 2005/12/25 04:26:33 $
 */

/** A Class
 * @author Kedar Mhaswade
 */

package com.sun.enterprise.admin.jmx.remote.internal;

import javax.management.*;
import java.io.IOException;
import java.io.NotSerializableException;

/** A purely convenience class. MBeanServerConnection interface methods throw a lot of
 * exceptions and an implementation of this interface generally ends up writing
 * large try-catch blocks.
 * <P>
 * This class has methods whose names are chosen from the
 * class MBeanServerRequestMessage {@link javax.management.remote.message.MBeanServerRequestMessage}
 * and all they do is return specific exceptions cast from the passed exception.
 * If the passed exception is none of the exceptions that a method throws, (which
 * should not be the case most of the times) then a ClassCastException results.
 * <P>
 * The principal advantage of this class is that it does the repetitive work at
 * one place and moves the exception conversion task away from actual MBeanServerConnection
 * implementation. This way some reuse of the methods throwing the same exceptions
 * is achieved.
 *
 * @author Kedar Mhaswade.
 * @since S1AS8.0
 * @version 1.0
 */

public class MBeanServerConnectionExceptionThrower {
    
    /** Creates a new instance of MBeanServerConnectionExceptionThrower */
    private MBeanServerConnectionExceptionThrower() {
    }
    
	public static void addNotificationListenerObjectName(Exception e) throws
	InstanceNotFoundException, IOException {
		if (e instanceof InstanceNotFoundException)
			throw (InstanceNotFoundException)e;
		else if (e instanceof RuntimeException)
			throw (RuntimeException)e;
		else if (e instanceof NotSerializableException)
            throw wrappedSerializationException(e);
        else
			throw wrappingIoException(e);
	}

	public static void addNotificationListeners(Exception e) throws
	InstanceNotFoundException, IOException {
		addNotificationListenerObjectName(e);
	}
	
    public static void createMBean(Exception e) throws ReflectionException,
    InstanceAlreadyExistsException, MBeanRegistrationException,
    MBeanException, NotCompliantMBeanException, IOException {
        if (e instanceof ReflectionException)
            throw (ReflectionException)e;
        else if (e instanceof InstanceAlreadyExistsException)
            throw (InstanceAlreadyExistsException)e;
        else if (e instanceof MBeanRegistrationException)
            throw (MBeanRegistrationException)e;
        else if (e instanceof MBeanException)
            throw (MBeanException)e;
        else if (e instanceof NotCompliantMBeanException)
            throw (NotCompliantMBeanException)e;
		else if (e instanceof RuntimeException)
			throw (RuntimeException)e;
        else if (e instanceof NotSerializableException)
            throw wrappedSerializationException(e);
        else
			throw wrappingIoException(e);
    }
    
    public static void createMBeanParams(Exception e) throws ReflectionException,
    InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException,
    NotCompliantMBeanException, IOException {
        createMBean(e); //throws exact same set of exceptions.
    }
    
    public static void createMBeanLoader(Exception e) throws ReflectionException,
    InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException,
    NotCompliantMBeanException, InstanceNotFoundException, IOException {
        if (e instanceof ReflectionException)
            throw (ReflectionException)e;
        else if (e instanceof InstanceAlreadyExistsException)
            throw (InstanceAlreadyExistsException)e;
        else if (e instanceof MBeanRegistrationException)
            throw (MBeanRegistrationException)e;
        else if (e instanceof MBeanException)
            throw (MBeanException)e;
        else if (e instanceof NotCompliantMBeanException)
            throw (NotCompliantMBeanException)e;
        else if (e instanceof InstanceNotFoundException)
            throw (InstanceNotFoundException)e;
		else if (e instanceof RuntimeException)
			throw (RuntimeException)e;
        else if (e instanceof NotSerializableException)
            throw wrappedSerializationException(e);
        else
			throw wrappingIoException(e);
    }
    
    public static void createMBeanLoaderParams(Exception e) throws ReflectionException,
    InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException,
    NotCompliantMBeanException, InstanceNotFoundException, IOException {
        createMBeanLoader(e); //throws exact same set of exceptions.
    }
    
    public static void getAttribute(Exception e) throws
    MBeanException, AttributeNotFoundException, InstanceNotFoundException,
    ReflectionException, IOException {
        if (e instanceof MBeanException)
            throw (MBeanException)e;
        else if (e instanceof AttributeNotFoundException)
            throw (AttributeNotFoundException)e;
        else if (e instanceof InstanceNotFoundException)
            throw (InstanceNotFoundException)e;
        else if (e instanceof ReflectionException)
            throw (ReflectionException)e;
		else if (e instanceof RuntimeException)
			throw (RuntimeException)e;
        else if (e instanceof NotSerializableException)
            throw wrappedSerializationException(e);
        else //has to be this, otherwise a ClassCastException results.
            throw wrappingIoException(e);
    }
    
    public static void getAttributes(Exception e) throws
    InstanceNotFoundException, ReflectionException, IOException {
        if (e instanceof InstanceNotFoundException)
            throw (InstanceNotFoundException)e;
        else if (e instanceof ReflectionException)
            throw (ReflectionException)e;
		else if (e instanceof RuntimeException)
			throw (RuntimeException)e;
        else if (e instanceof NotSerializableException)
            throw wrappedSerializationException(e);
        else
			throw wrappingIoException(e);
    }
    
    private static void getIOException(Exception e) throws IOException {
        throw wrappingIoException(e); // has to be this, otherwise ClassCastException results
    }
    
    public static void getDefaultDomain(Exception e) throws IOException {
        getIOException(e); //throws exact same set of exceptions
    }
    public static void getDomains(Exception e) throws IOException {
        getIOException(e); // throws exact same set of exceptions
    }
    public static void getMBeanCount(Exception e) throws IOException {
        getIOException(e); // throws exact same set of exceptions
    }
    public static void isRegistered(Exception e) throws IOException {
        getIOException(e); // throws exact same set of exceptions
    }
    
    public static void getMBeanInfo(Exception e) throws
    InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
        if (e instanceof InstanceNotFoundException)
            throw (InstanceNotFoundException)e;
        else if (e instanceof IntrospectionException)
            throw (IntrospectionException)e;
        else if (e instanceof ReflectionException)
            throw (ReflectionException)e;
		else if (e instanceof RuntimeException)
			throw (RuntimeException)e;
        else if (e instanceof NotSerializableException)
            throw wrappedSerializationException(e);
        else
			throw wrappingIoException(e);
    }
    
    public static void getObjectInstance(Exception e) throws
    InstanceNotFoundException, IOException {
        if (e instanceof InstanceNotFoundException)
            throw (InstanceNotFoundException)e;
		else if (e instanceof RuntimeException)
			throw (RuntimeException)e;
        else if (e instanceof NotSerializableException)
            throw wrappedSerializationException(e);
        else
			throw wrappingIoException(e);
    }
    
    public static void invoke(Exception e) throws InstanceNotFoundException,
    MBeanException, ReflectionException, IOException {
        if (e instanceof InstanceNotFoundException)
            throw (InstanceNotFoundException)e;
        else if (e instanceof MBeanException)
            throw (MBeanException)e;
        else if (e instanceof ReflectionException)
            throw (ReflectionException)e;
		else if (e instanceof RuntimeException)
			throw (RuntimeException)e;
        else if (e instanceof NotSerializableException)
            throw wrappedSerializationException(e);
        else //CCE Results?
			throw wrappingIoException(e);
    }
    
    public static void isInstanceOf(Exception e) throws
    InstanceNotFoundException, IOException {
        if (e instanceof InstanceNotFoundException)
            throw (InstanceNotFoundException)e;
		else if (e instanceof RuntimeException)
			throw (RuntimeException)e;
        else if (e instanceof NotSerializableException)
            throw wrappedSerializationException(e);
        else
			throw wrappingIoException(e);
    }
    
    public static void queryMBeans(Exception e) throws IOException {
        if (e instanceof NotSerializableException)
            throw wrappedSerializationException(e);
        getIOException(e); // throws exact same set of exceptions
    }
    
    public static void queryNames(Exception e) throws IOException {
        if (e instanceof NotSerializableException)
            throw wrappedSerializationException(e);
        getIOException(e); // throws exact same set of exceptions
    }
    
	public static void removeNotificationListener(Exception e)  throws 
	InstanceNotFoundException, ListenerNotFoundException, IOException {
		if (e instanceof InstanceNotFoundException)
			throw (InstanceNotFoundException)e;
		else if (e instanceof ListenerNotFoundException)
			throw (ListenerNotFoundException)e;
		else if (e instanceof RuntimeException)
			throw (RuntimeException)e;
		else if (e instanceof NotSerializableException)
            throw wrappedSerializationException(e);
        else
			throw wrappingIoException(e);
	}
	
	public static void removeNotificationListenerFilterHandback(Exception e) throws
	InstanceNotFoundException, ListenerNotFoundException, IOException {
		removeNotificationListener(e);
	}
	
	public static void removeNotificationListenerObjectName(Exception e) throws
	InstanceNotFoundException, ListenerNotFoundException, IOException {
		removeNotificationListener(e);
	}

	public static void removeNotificationListenerObjectNameFilterHandback(Exception e) throws
	InstanceNotFoundException, ListenerNotFoundException, IOException {
		removeNotificationListener(e);
	}
	
    public static void setAttribute(Exception e) throws InstanceNotFoundException,
    AttributeNotFoundException, InvalidAttributeValueException, MBeanException,
    ReflectionException, IOException {
        if (e instanceof AttributeNotFoundException)
            throw (AttributeNotFoundException)e;
        else if (e instanceof InstanceNotFoundException)
            throw (InstanceNotFoundException)e;
        else if (e instanceof InvalidAttributeValueException)
            throw (InvalidAttributeValueException)e;
        else if (e instanceof MBeanException)
            throw (MBeanException)e;
		else if (e instanceof RuntimeException)
			throw (RuntimeException)e;
        else if (e instanceof NotSerializableException)
            throw wrappedSerializationException(e);
        else
			throw wrappingIoException(e);
    }
    
    public static void setAttributes(Exception e) throws InstanceNotFoundException,
    ReflectionException, IOException {
        if (e instanceof InstanceNotFoundException)
            throw (InstanceNotFoundException)e;
        else if (e instanceof ReflectionException)
            throw (ReflectionException)e;
		else if (e instanceof RuntimeException)
			throw (RuntimeException)e;
        else if (e instanceof NotSerializableException)
            throw wrappedSerializationException(e);
        else
			throw wrappingIoException(e);
    }
    
    public static void unregisterMBean(Exception e) throws
    InstanceNotFoundException, MBeanRegistrationException, IOException {
        if (e instanceof InstanceNotFoundException)
            throw (InstanceNotFoundException)e;
        else if (e instanceof MBeanRegistrationException)
            throw (MBeanRegistrationException)e;
		else if (e instanceof RuntimeException)
			throw (RuntimeException)e;
        else if (e instanceof NotSerializableException)
            throw wrappedSerializationException(e);
        else
			throw wrappingIoException(e);
    }
	
	/**
	 * The method which acts as a "catch-all". In the distributed environment, it
	 * is obvious that a large set of exceptions is thrown under various circumstances.
	 * The ultimate exception on which this implementation falls back is {@link 
	 * java.io.IOException}. This method receives such an exception, that is
	 * not any of those thrown by MBeanServerConnection's methods, and
	 * carefully arranges its stack and initial cause. The returned instance of
	 * IOException has these properties:
	 * <li> Its stack is intact. (if not null) </li>
	 * <li> The actual exception becomes the init cause. (if not null, which is the case) <li>
	 */
	private static IOException wrappingIoException(final Throwable t) {
		final String dm = t.getMessage();
		final Throwable cause = t.getCause();
		final StackTraceElement[] st = t.getStackTrace();
		final IOException ioe = new IOException (dm);
		if (cause != null) {
			ioe.initCause(cause);
		}
		if (st != null) {
			ioe.setStackTrace(st);
		}
		return ( ioe );
	}
    
    /** A method to create a {@link java.io.NotSerializableException} if that
     * is what gets thrown by the server. It may happen that the server while
     * invoking a specific MBeanServerConnection method throws a NotSerializableException
     * especially if an MBean attribute is not serialiable, invocation result of
     * invoke method is not serializable etc. There is an argument around what to
     * do in this case because {@link java.io.IOException} is kind of reserved
     * for any communication problems and {@link java.io.NotSerializableException}
     * extends {@link java.io.IOException} :). This means that I can't throw only
     * IOException. Hence this method.
     * @return an instance of {@link java.io.NotSerializableException} that is wrapped
     * inside a {@link RuntimeException}
     * @param id indicating the integer in @link javax.management.remote.message.MBeanServerRequestMessage}
     * that gives information about the method to invoke
     */
    
    private static RuntimeException wrappedSerializationException(Exception e) {
		final String dm = e.getMessage();
		final Throwable cause = e.getCause();
		final StackTraceElement[] st = e.getStackTrace();
		final RuntimeException re = new RuntimeException(dm);
		if (cause != null) {
			re.initCause(cause);
		}
		if (st != null) {
			re.setStackTrace(st);
		}
		return ( re );
    }
}
