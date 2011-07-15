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

package javax.ejb.spi;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

import javax.ejb.EJBObject;
import javax.ejb.EJBHome;


/**
 * The <code>HandleDelegate</code> interface is implemented by the EJB container. 
 * It is used by portable implementations of <code>javax.ejb.Handle</code> and
 * <code>javax.ejb.HomeHandle</code>.
 * It is not used by EJB components or by client components.
 * It provides methods to serialize and deserialize EJBObject and
 * EJBHome references to streams.
 *
 * <p> The <code>HandleDelegate</code> object is obtained by JNDI lookup at the
 * reserved name <code>"java:comp/HandleDelegate"</code>.
 *
 * @since EJB 2.0
 */

public interface HandleDelegate {
    /**
     * Serialize the EJBObject reference corresponding to a Handle.
     *
     * <p> This method is called from the <code>writeObject</code> method of 
     * portable Handle implementation classes. The <code>ostream</code> object is the
     * same object that was passed in to the Handle class's <code>writeObject</code>
     * method.
     *
     * @param ejbObject The EJBObject reference to be serialized.
     *
     * @param ostream The output stream.
     *
     * @exception IOException The EJBObject could not be serialized
     *    because of a system-level failure.
     */
    public void writeEJBObject(EJBObject ejbObject, ObjectOutputStream ostream)
	throws IOException;


    /**
     * Deserialize the EJBObject reference corresponding to a Handle.
     *
     * <p> The <code>readEJBObject</code> method is called from the
     * <code>readObject</code> method of portable <code>Handle</code>
     * implementation classes. The <code>istream</code> object is the
     * same object that was passed in to the Handle class's
     * <code>readObject</code> method.  When<code>readEJBObject</code> is called,
     * <code>istream</code> must point to the location in the stream at which the
     * EJBObject reference can be read.  The container must ensure
     * that the EJBObject reference is capable of performing
     * invocations immediately after deserialization.
     *
     * @param istream The input stream.
     *
     * @return The deserialized EJBObject reference.
     *
     * @exception IOException The EJBObject could not be deserialized
     *    because of a system-level failure.
     * @exception ClassNotFoundException The EJBObject could not be deserialized
     *    because some class could not be found.
     */
    public javax.ejb.EJBObject readEJBObject(ObjectInputStream istream)
	throws IOException, ClassNotFoundException;

    /**
     * Serialize the EJBHome reference corresponding to a HomeHandle.
     *
     * <p> This method is called from the <code>writeObject</code> method of 
     * portable <code>HomeHandle</code> implementation classes. The <code>ostream</code>
     * object is the same object that was passed in to the <code>Handle</code>
     * class's <code>writeObject</code> method.
     *
     * @param ejbHome The EJBHome reference to be serialized.
     *
     * @param ostream The output stream.
     *
     * @exception IOException The EJBObject could not be serialized
     *    because of a system-level failure.
     */
    public void writeEJBHome(EJBHome ejbHome, ObjectOutputStream ostream)
	throws IOException;

    /**
     * Deserialize the EJBHome reference corresponding to a HomeHandle.
     *
     * <p> The <code>readEJBHome</code> method is called from the 
     * <code>readObject</code> method of  portable <code>HomeHandle</code>
     * implementation classes. The <code>istream</code> object is the
     * same object that was passed in to the <code>HomeHandle</code> class's
     * <code>readObject</code> method.  When <code>readEJBHome</code> is called, 
     * <code>istream</code> must point to the location
     * in the stream at which the EJBHome reference can be read.
     * The container must ensure that the EJBHome reference is 
     * capable of performing invocations immediately after deserialization.
     *
     * @param istream The input stream.
     *
     * @return The deserialized EJBHome reference.
     *
     * @exception IOException The EJBHome could not be deserialized
     *    because of a system-level failure.
     * @exception ClassNotFoundException The EJBHome could not be deserialized
     *    because some class could not be found.
     */
    public javax.ejb.EJBHome readEJBHome(ObjectInputStream istream)
	throws IOException, ClassNotFoundException;
}
