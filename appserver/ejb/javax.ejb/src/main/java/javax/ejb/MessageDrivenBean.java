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

package javax.ejb;

/**
 * The MessageDrivenBean interface defines methods that the EJB container uses
 * to notify a message driven bean instance of the instance's life cycle 
 * events.
 * <p>
 * As of EJB 3.0 it is no longer required that a message driven bean class
 * implement this interface.
 *
 * @since EJB 2.0
 */
public interface MessageDrivenBean extends EnterpriseBean {
    /**
     * Set the associated message-driven context. The container calls 
     * this method after the instance creation.
     *
     * <p> The message driven bean instance should store the reference to the
     * context object in an instance variable.
     *
     * <p> This method is called with no transaction context.
     *
     * @param ctx A MessageDrivenContext interface for the instance.
     *
     * @exception EJBException Thrown by the method to indicate a failure
     *    caused by a system-level error.
     *
     */
    void setMessageDrivenContext(MessageDrivenContext ctx) throws EJBException;

    /**
     * A container invokes this method before it ends the life of the 
     * message-driven object. This happens when a container decides to 
     * terminate the message-driven object.
     * 
     * <p> This method is called with no transaction context.
     *
     * @exception EJBException Thrown by the method to indicate a failure
     *    caused by a system-level error.
     *
     */
     void ejbRemove() throws EJBException;

}
