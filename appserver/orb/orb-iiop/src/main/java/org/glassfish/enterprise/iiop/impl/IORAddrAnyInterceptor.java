/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.enterprise.iiop.impl;

import com.sun.logging.LogDomains;
import org.omg.IOP.Codec;


import java.net.InetAddress;
import java.net.SocketException;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IORAddrAnyInterceptor extends org.omg.CORBA.LocalObject
                    implements org.omg.PortableInterceptor.IORInterceptor{
    
    public static final String baseMsg = IORAddrAnyInterceptor.class.getName();
    private static final Logger _logger = LogDomains.getLogger(
        IORAddrAnyInterceptor.class, LogDomains.CORBA_LOGGER);
    
    private Codec codec;
    
    
    /** Creates a new instance of IORAddrAnyInterceptor
     * @param c The codec
     */
    public IORAddrAnyInterceptor(Codec c) {
        codec = c;
    }

    /**
     * Provides an opportunity to destroy this interceptor.
     * The destroy method is called during <code>ORB.destroy</code>. When an
     * application calls <code>ORB.destroy</code>, the ORB:
     * <ol>
     *  <li>waits for all requests in progress to complete</li>
     *  <li>calls the <code>Interceptor.destroy</code> operation for each
     *      interceptor</li>
     *  <li>completes destruction of the ORB</li>
     * </ol>
     * Method invocations from within <code>Interceptor.destroy</code> on
     * object references for objects implemented on the ORB being destroyed
     * result in undefined behavior. However, method invocations on objects
     * implemented on an ORB other than the one being destroyed are
     * permitted. (This means that the ORB being destroyed is still capable
     * of acting as a client, but not as a server.)
     */
    @Override
    public void destroy() {
    }
    
    /**
     * A server side ORB calls the <code>establish_components</code>
     * operation on all registered <code>IORInterceptor</code> instances
     * when it is assembling the list of components that will be included
     * in the profile or profiles of an object reference. This operation
     * is not necessarily called for each individual object reference.
     * For example, the POA specifies policies at POA granularity and
     * therefore, this operation might be called once per POA rather than
     * once per object. In any case, <code>establish_components</code> is
     * guaranteed to be called at least once for each distinct set of
     * server policies.
     * <p>
     * An implementation of <code>establish_components</code> must not
     * throw exceptions. If it does, the ORB shall ignore the exception
     * and proceed to call the next IOR Interceptor's
     * <code>establish_components</code> operation.
     *
     * @param iorInfo The <code>IORInfo</code> instance used by the ORB
     *    service to query applicable policies and add components to be
     *    included in the generated IORs.
     */
    @Override
    public void establish_components(org.omg.PortableInterceptor.IORInfo iorInfo) {
        /*
        try {
            IORInfoExt iorInfoExt = (IORInfoExt) iorInfo;
            int port = iorInfoExt.getServerPort(ORBSocketFactory.IIOP_CLEAR_TEXT);

            ArrayList allInetAddress = getAllInetAddresses();
            addAddressComponents(iorInfo, allInetAddress, port);
            ORB orb = (ORB)((IORInfoImpl)iorInfo).getORB();
            Object[] userPorts = orb.getUserSpecifiedListenPorts().toArray();
            if (userPorts.length > 0) {
                for (int i = 0; i < userPorts.length; i++) {
                    com.sun.corba.ee.internal.corba.ORB.UserSpecifiedListenPort p =
                        ((com.sun.corba.ee.internal.corba.ORB.UserSpecifiedListenPort)userPorts[i]);
            //        if (p.getType().equals(ORBSocketFactory.IIOP_CLEAR_TEXT)) {
                        addAddressComponents(iorInfo, allInetAddress, p.getPort());
            //        }
                }
            }
        } catch (Exception e) {
            _logger.log(Level.WARNING,"Exception in " + baseMsg, e);
        }
        */
    }
    
    /**
     * Returns the name of the interceptor.
     * <p>
     * Each Interceptor may have a name that may be used administratively
     * to order the lists of Interceptors. Only one Interceptor of a given
     * name can be registered with the ORB for each Interceptor type. An
     * Interceptor may be anonymous, i.e., have an empty string as the name
     * attribute. Any number of anonymous Interceptors may be registered with
     * the ORB.
     *
     * @return the name of the interceptor.
     */
    @Override
    public String name() {
        return baseMsg;
    }

    protected short intToShort( int value ) 
    {
	if (value > 32767) {
            return (short) (value - 65536);
        }
	return (short)value ;
    }
    
    /*
    private void addAddressComponents(org.omg.PortableInterceptor.IORInfo iorInfo, 
                    ArrayList allInetAddress, int port) {
        try {
            for (int i = 0; i < allInetAddress.size(); i++) {
                String address = ((InetAddress)allInetAddress.get(i)).getHostAddress();
                AlternateIIOPAddressComponent iiopAddress = 
                    new AlternateIIOPAddressComponent(address, intToShort(port));
                Any any = ORB.init().create_any();
                AlternateIIOPAddressComponentHelper.insert(any, iiopAddress);
                byte[] data = codec.encode_value(any);
                TaggedComponent taggedComponent =
                    new TaggedComponent( org.omg.IOP.TAG_ALTERNATE_IIOP_ADDRESS.value,
					//AlternateIIOPAddressComponent.TAG_ALTERNATE_IIOP_ADDRESS_ID,
                            data);
                iorInfo.add_ior_component(taggedComponent);
            }
        } catch (Exception e) {
            _logger.log(Level.WARNING,"Exception in " + baseMsg, e);
        }
    }
    */
    
}
