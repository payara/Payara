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

import org.omg.IOP.*;
import org.omg.IOP.CodecFactoryPackage.UnknownEncoding;

import org.omg.CORBA.*;

import org.omg.CosTransactions.OTS_POLICY_TYPE;
import org.omg.CosTransactions.INVOCATION_POLICY_TYPE;

import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.*;
//import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableInterceptor.ORBInitInfoPackage.InvalidName;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;

import com.sun.jts.CosTransactions.MinorCode;

/**
 * This class implements the ORBInitializer for JTS. When an instance of this
 * class is called during ORB initialization, it registers the IORInterceptors
 * and the JTS request/reply interceptors with the ORB.
 *
 * @author Ram Jeyaraman 11/11/2000
 * @version 1.0
 */
public class ORBInitializerImpl extends LocalObject implements ORBInitializer {

    public ORBInitializerImpl() {}

    public void pre_init(ORBInitInfo info) {}

    public void post_init(ORBInitInfo info) {

        // get hold of the codec instance to pass onto interceptors.

        CodecFactory codecFactory = info.codec_factory();
        Encoding enc = new Encoding(
                            ENCODING_CDR_ENCAPS.value, (byte) 1, (byte) 2);
        Codec codec = null;
        try {
            codec = codecFactory.create_codec(enc);
        } catch (UnknownEncoding e) {
            throw new INTERNAL(MinorCode.TSCreateFailed,
                               CompletionStatus.COMPLETED_NO);
        }

        // get hold of PICurrent to allocate a slot for JTS service.

        Current pic = null;
        try {
            pic = (Current) info.resolve_initial_references("PICurrent");
        } catch (InvalidName e) {
            throw new INTERNAL(MinorCode.TSCreateFailed,
                               CompletionStatus.COMPLETED_NO);
        }

        // allocate a PICurrent slotId for the transaction service.

        int[] slotIds = new int[2];
        try {
            slotIds[0] = info.allocate_slot_id();
            slotIds[1] = info.allocate_slot_id();
        } catch (BAD_INV_ORDER e) {
            throw new INTERNAL(MinorCode.TSCreateFailed,
                               CompletionStatus.COMPLETED_NO);
        }

        // get hold of the TSIdentification instance to pass onto interceptors.

        TSIdentification tsi = null;
        try {
            tsi = (TSIdentification)
                    info.resolve_initial_references("TSIdentification");
        } catch (InvalidName e) {
            // the TransactionService is unavailable.
        }

        // register the policy factories.

        try {
            info.register_policy_factory(OTS_POLICY_TYPE.value,
                                         new OTSPolicyFactory());
        } catch (BAD_INV_ORDER e) {
            // ignore, policy factory already exists.
        }

        try {
            info.register_policy_factory(INVOCATION_POLICY_TYPE.value,
                                         new InvocationPolicyFactory());
        } catch (BAD_INV_ORDER e) {
            // ignore, policy factory already exists.
        }

        // register the interceptors.

        try {
            info.add_ior_interceptor(new IORInterceptorImpl(codec));
            InterceptorImpl interceptor =
                new InterceptorImpl(pic, codec, slotIds, tsi);
            info.add_client_request_interceptor(interceptor);
            info.add_server_request_interceptor(interceptor);
        } catch (DuplicateName e) {
            throw new INTERNAL(MinorCode.TSCreateFailed,
                               CompletionStatus.COMPLETED_NO);
        }
    }
}
