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

package com.sun.jts.pi;

import org.omg.IOP.Codec;
import org.omg.IOP.TaggedComponent;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Any;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.INV_POLICY;
import org.omg.CORBA.INTERNAL;

import org.omg.CosTransactions.*;

import org.omg.CosTSInteroperation.TAG_OTS_POLICY;
import org.omg.CosTSInteroperation.TAG_INV_POLICY;

import org.omg.PortableInterceptor.IORInfo;
import org.omg.PortableInterceptor.IORInterceptor;

/**
 * This class implements the IORInterceptor for JTS. When an instance of this
 * class is called by the ORB (during POA creation), it supplies appropriate
 * IOR TaggedComponents for the OTSPolicy / InvocationPolicy associated
 * with the POA, which will be used by the ORB while publishing IORs.
 *
 * @author Ram Jeyaraman 11/11/2000
 * @version 1.0
 */
public class IORInterceptorImpl extends LocalObject implements IORInterceptor {

    // class attributes

    private static final String name = "com.sun.jts.pi.IORInterceptor";

    // Instance attributes

    private Codec codec = null;

    public IORInterceptorImpl(Codec codec) {
        this.codec = codec;
    }

    // org.omg.PortableInterceptors.IORInterceptorOperations implementation

    public void establish_components (IORInfo info) {

        // get the OTSPolicy and InvocationPolicy objects

        OTSPolicy otsPolicy = null;

        try {
            otsPolicy = (OTSPolicy)
                info.get_effective_policy(OTS_POLICY_TYPE.value);
        } catch (INV_POLICY e) {
            // ignore. This implies an policy was not explicitly set.
            // A default value will be used instead.
        }

        InvocationPolicy invPolicy = null;
        try {
            invPolicy = (InvocationPolicy)
                info.get_effective_policy(INVOCATION_POLICY_TYPE.value);
        } catch (INV_POLICY e) {
            // ignore. This implies an policy was not explicitly set.
            // A default value will be used instead.
        }

        // get OTSPolicyValue and InvocationPolicyValue from policy objects.

        short otsPolicyValue = FORBIDS.value; // default value
        short invPolicyValue = EITHER.value;  // default value

        if (otsPolicy != null) {
            otsPolicyValue = otsPolicy.value();
        }

        if (invPolicy != null) {
            invPolicyValue = invPolicy.value();
        }

        // use codec to encode policy value into an CDR encapsulation.

        Any otsAny = ORB.init().create_any();
        Any invAny = ORB.init().create_any();

        otsAny.insert_short(otsPolicyValue);
        invAny.insert_short(invPolicyValue);

        byte[] otsCompValue = null;
        byte[] invCompValue = null;
        try {
            otsCompValue = this.codec.encode_value(otsAny);
            invCompValue = this.codec.encode_value(invAny);
        } catch (InvalidTypeForEncoding e) {
            throw new INTERNAL();
        }

        // create IOR TaggedComponents for OTSPolicy and InvocationPolicy.

        TaggedComponent otsComp = new TaggedComponent(TAG_OTS_POLICY.value,
                                                      otsCompValue);
        TaggedComponent invComp = new TaggedComponent(TAG_INV_POLICY.value,
                                                      invCompValue);

        // add ior components.

        info.add_ior_component(otsComp);
        info.add_ior_component(invComp);
    }

    // org.omg.PortableInterceptors.InterceptorOperations implementation

    public String name(){
        return IORInterceptorImpl.name;
    }

    public void destroy() {}
}


