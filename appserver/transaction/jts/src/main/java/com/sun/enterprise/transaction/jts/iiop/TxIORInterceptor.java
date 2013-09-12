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

package com.sun.enterprise.transaction.jts.iiop;

import java.util.logging.Logger;
import java.util.logging.Level;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.INV_POLICY;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.LocalObject;
import org.omg.CosTransactions.ADAPTS;
import org.omg.CosTransactions.SHARED;
import org.omg.CosTransactions.OTSPolicy;
import org.omg.CosTSInteroperation.TAG_OTS_POLICY;
import org.omg.CosTSInteroperation.TAG_INV_POLICY;
import org.omg.IOP.Codec;
import org.omg.IOP.TaggedComponent;
import org.omg.PortableInterceptor.IORInfo;
import org.omg.PortableInterceptor.IORInterceptor;

import com.sun.logging.LogDomains;
import org.glassfish.enterprise.iiop.api.GlassFishORBHelper;
import org.glassfish.hk2.api.ServiceLocator;

public class TxIORInterceptor extends LocalObject implements IORInterceptor {
    

    private static Logger _logger = 
           LogDomains.getLogger(com.sun.jts.pi.InterceptorImpl.class, LogDomains.TRANSACTION_LOGGER);

    private Codec codec;
    
    private ServiceLocator habitat;
   
    public TxIORInterceptor(Codec c, ServiceLocator h) {
        codec = c;
        habitat = h;
    }
    
    public void destroy() {
    }
    
    public String name() {
        return "TxIORInterceptor";
    }
    
    // Note: this is called for all remote refs created from this ORB,
    // including EJBs and COSNaming objects.
    public void establish_components(IORInfo iorInfo) {
        try {
            _logger.log(Level.FINE, "TxIORInterceptor.establish_components->:");

            // Add OTS tagged components. These are always the same for all EJBs
            OTSPolicy otsPolicy = null;
            try {
                otsPolicy = (OTSPolicy)iorInfo.get_effective_policy(
			 habitat.getService(GlassFishORBHelper.class).getOTSPolicyType());
            } catch ( INV_POLICY ex ) {
                _logger.log(Level.FINE, 
                        "TxIORInterceptor.establish_components: OTSPolicy not present");
            }
	    addOTSComponents(iorInfo, otsPolicy);

        } catch (Exception e) {
            _logger.log(Level.WARNING,"Exception in establish_components", e);
        } finally {
            _logger.log(Level.FINE, "TxIORInterceptor.establish_components<-:");
        }
    }

    private void addOTSComponents(IORInfo iorInfo, OTSPolicy otsPolicy) {       
        short invPolicyValue = SHARED.value;
        short otsPolicyValue = ADAPTS.value;            

        if (otsPolicy != null) {
	    otsPolicyValue = otsPolicy.value();
	}
        
        Any otsAny = ORB.init().create_any();
        Any invAny = ORB.init().create_any();
        
        otsAny.insert_short(otsPolicyValue);
        invAny.insert_short(invPolicyValue);
        
        byte[] otsCompValue = null;
        byte[] invCompValue = null;                 
        try {
            otsCompValue = codec.encode_value(otsAny);
            invCompValue = codec.encode_value(invAny);
        } catch (org.omg.IOP.CodecPackage.InvalidTypeForEncoding e) {
            throw new INTERNAL("InvalidTypeForEncoding "+e.getMessage());
        }
    
        TaggedComponent otsComp = new TaggedComponent(TAG_OTS_POLICY.value,
                                                      otsCompValue);
        iorInfo.add_ior_component(otsComp);

        TaggedComponent invComp = new TaggedComponent(TAG_INV_POLICY.value,
                                                      invCompValue);
        iorInfo.add_ior_component(invComp);
    }

}

// End of file.
