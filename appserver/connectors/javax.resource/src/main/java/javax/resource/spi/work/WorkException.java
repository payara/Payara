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

package javax.resource.spi.work;

/**
 * A common base class for all <code>Work</code> processing related exceptions.
 *
 * @version 1.0
 * @author  Ram Jeyaraman
 */
public class WorkException extends javax.resource.ResourceException {


    /**
     * Indicates an internal error condition.
     */
    public static final String INTERNAL = "-1";

    /**
     * Undefined error code.
     */
    public static final String UNDEFINED = "0";

    /**
     * Indicates start timeout expiration.
     */
    public static final String START_TIMED_OUT = "1";

    /**
     * Indicates that concurrent work within a transaction is
     * disallowed. That is, there is already another <code>Work</code>
     * instance associated with the specified transaction context.
     */
    public static final String TX_CONCURRENT_WORK_DISALLOWED = "2";

    /**
     * Indicates a failure in recreating the specified transaction context.
     */
    public static final String TX_RECREATE_FAILED = "3";

    /**
     * Constructs a new instance with null as its detail message.
     */
    public WorkException() { super(); }

    /**
     * Constructs a new instance with the specified detail message.
     *
     * @param message the detail message.
     */
    public WorkException(String message) {
        super(message);
    }

    /**
     * Constructs a new throwable with the specified cause.
     *
     * @param cause a chained exception of type
     * <code>Throwable</code>.
     */
    public WorkException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new throwable with the specified detail message and cause.
     *
     * @param message the detail message.
     *
     * @param cause a chained exception of type
     * <code>Throwable</code>.
     */
    public WorkException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new throwable with the specified detail message and
     * an error code.
     *
     * @param message a description of the exception.
     * @param errorCode a string specifying the vendor specific error code.
     */
    public WorkException(String message, String errorCode) {
        super(message, errorCode);
    }
}
