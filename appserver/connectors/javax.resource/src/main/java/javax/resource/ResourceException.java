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

package javax.resource;

/**
 * This is the root interface of the exception hierarchy defined
 * for the Connector architecture.
 * 
 * The ResourceException provides the following information:
 * <UL>
 *   <LI> A resource adapter vendor specific string describing the error.
 *        This string is a standard Java exception message and is available
 *        through getMessage() method.
 *   <LI> resource adapter vendor specific error code.
 *   <LI> reference to another exception. Often a resource exception
 *        will be result of a lower level problem. If appropriate, this
 *        lower level exception can be linked to the ResourceException.
 *        Note, this has been deprecated in favor of J2SE release 1.4 exception
 *        chaining facility.
 * </UL>
 *
 * @version 1.0
 * @author Rahul Sharma
 * @author Ram Jeyaraman
 */

public class ResourceException extends java.lang.Exception {

    /** Vendor specific error code */
    private String errorCode;

    /** reference to another exception */
    private Exception linkedException;

    /**
     * Constructs a new instance with null as its detail message.
     */
    public ResourceException() { super(); }

    /**
     * Constructs a new instance with the specified detail message.
     *
     * @param message the detail message.
     */
    public ResourceException(String message) {
	super(message);
    }

    /**
     * Constructs a new throwable with the specified cause.
     *
     * @param cause a chained exception of type <code>Throwable</code>.
     */
    public ResourceException(Throwable cause) {
	super(cause);
    }

    /**
     * Constructs a new throwable with the specified detail message and cause.
     *
     * @param message the detail message.
     *
     * @param cause a chained exception of type <code>Throwable</code>.
     */
    public ResourceException(String message, Throwable cause) {
	super(message, cause);
    }

    /**
     * Create a new throwable with the specified message and error code.
     *
     * @param message a description of the exception.
     * @param errorCode a string specifying the vendor specific error code.
     */
    public ResourceException(String message, String errorCode) {
	super(message);
	this.errorCode = errorCode;
    }    

    /**
     * Set the error code.
     *
     * @param errorCode the error code.
     */
    public void setErrorCode(String errorCode) {
	this.errorCode = errorCode;
    }

    /**
     * Get the error code.
     *
     * @return the error code.
     */
    public String getErrorCode() {
	return this.errorCode;
    }

    /**
     * Get the exception linked to this ResourceException
     *
     * @return         linked Exception, null if none
     *
     * @deprecated J2SE release 1.4 supports a chained exception facility 
     * that allows any throwable to know about another throwable, if any,
     * that caused it to get thrown. Refer to <code>getCause</code> and 
     * <code>initCause</code> methods of the 
     * <code>java.lang.Throwable</code> class..
     */
    public Exception getLinkedException() {
	return (linkedException);
    }

    /**
     * Add a linked Exception to this ResourceException.
     *
     * @param ex       linked Exception
     *
     * @deprecated J2SE release 1.4 supports a chained exception facility 
     * that allows any throwable to know about another throwable, if any,
     * that caused it to get thrown. Refer to <code>getCause</code> and 
     * <code>initCause</code> methods of the 
     * <code>java.lang.Throwable</code> class.
     */
    public void setLinkedException(Exception ex) {
	linkedException = ex;
    }

    /**
     * Returns a detailed message string describing this exception.
     *
     * @return a detailed message string.
     */
    public String getMessage() {
	String msg = super.getMessage();
	String ec = getErrorCode();
	if ((msg == null) && (ec == null)) {
	    return null;
	}
	if ((msg != null) && (ec != null)) {
	    return (msg + ", error code: " + ec);
	}
	return ((msg != null) ? msg : ("error code: " + ec));
    }
}
