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

package org.glassfish.enterprise.iiop.impl;


import com.sun.logging.LogDomains;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.Encoding;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.PortableInterceptor.ORBInitializer;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;

/**
 * This class is used to add IOR interceptors for supporting IN_ADDR_ANY
 * functionality in the ORB
 */
public class IORAddrAnyInitializer extends org.omg.CORBA.LocalObject 
                                implements ORBInitializer{
                                    
    private static final Logger _logger = LogDomains.getLogger(
        IORAddrAnyInitializer.class, LogDomains.CORBA_LOGGER);
    
    public static final String baseMsg = IORAddrAnyInitializer.class.getName();
    
    /** Creates a new instance of IORAddrAnyInitializer */
    public IORAddrAnyInitializer() {
    }
    
    /**
     * Called during ORB initialization.  If it is expected that initial
     * services registered by an interceptor will be used by other
     * interceptors, then those initial services shall be registered at
     * this point via calls to
     * <code>ORBInitInfo.register_initial_reference</code>.
     *
     * @param info provides initialization attributes and operations by
     *    which Interceptors can be registered.
     */
    @Override
    public void pre_init(org.omg.PortableInterceptor.ORBInitInfo info) {
    }
    
    /**
     * Called during ORB initialization. If a service must resolve initial
     * references as part of its initialization, it can assume that all
     * initial references will be available at this point.
     * <p>
     * Calling the <code>post_init</code> operations is not the final
     * task of ORB initialization. The final task, following the
     * <code>post_init</code> calls, is attaching the lists of registered
     * interceptors to the ORB. Therefore, the ORB does not contain the
     * interceptors during calls to <code>post_init</code>. If an
     * ORB-mediated call is made from within <code>post_init</code>, no
     * request interceptors will be invoked on that call.
     * Likewise, if an operation is performed which causes an IOR to be
     * created, no IOR interceptors will be invoked.
     *
     * @param info provides initialization attributes and
     *    operations by which Interceptors can be registered.
     */
    @Override
    public void post_init(org.omg.PortableInterceptor.ORBInitInfo info) {
        Codec codec = null;
        CodecFactory cf = info.codec_factory();
  
        byte major_version = 1;
        byte minor_version = 2;
        Encoding encoding = new Encoding(ENCODING_CDR_ENCAPS.value, 
                                         major_version, minor_version);
        try {
            codec = cf.create_codec(encoding);
        } catch (org.omg.IOP.CodecFactoryPackage.UnknownEncoding e) {
            _logger.log(Level.WARNING,"UnknownEncoding from " + baseMsg,e);
	    }
        try {
            info.add_ior_interceptor(new IORAddrAnyInterceptor(codec));
        } catch (DuplicateName ex) {
            _logger.log(Level.WARNING,"DuplicateName from " + baseMsg,ex);
        }
    }
    
}
