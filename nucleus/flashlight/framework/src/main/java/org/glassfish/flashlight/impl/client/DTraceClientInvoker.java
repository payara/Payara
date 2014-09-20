/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.flashlight.impl.client;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.*;
import org.glassfish.flashlight.FlashlightUtils;
import org.glassfish.flashlight.client.ProbeClientInvoker;
import org.glassfish.flashlight.provider.FlashlightProbe;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.flashlight.FlashlightLoggerInfo;

/**
 * bnevins Aug 15, 2009
 * DTraceClientInvoker is only public because an internal class is using it from a
 * different package.  If this were C++ we would have a "friend" relationship between
 * the 2 classes.  In Java we are stuck with making it public.
 * Notes:
 * DTrace has a fairly serious limitation.  It only allows parameters that are
 * integral primitives (all primitives except float and double) and java.lang.String
 * So what we do is automagically convert other class objects to String via Object.toString()
 * This brings in a new rub with overloaded methods.  E.g. we can't tell apart these 2 methods:
 * foo(Date) and foo(String)
 *
 * TODO:I believe we should disallow such overloads when the DTrace object is being produced rather than
 * worrying about it in this class.
 *
 * @author bnevins
 */

public class DTraceClientInvoker implements ProbeClientInvoker{
    private static final Logger logger = FlashlightLoggerInfo.getLogger();

    public DTraceClientInvoker(int ID, FlashlightProbe p) {
        id          = ID;
        method      = p.getDTraceMethod();
        targetObj   = p.getDTraceProviderImpl();
    }

    public void invoke(Object[] args) {
        if(FlashlightUtils.isDtraceAvailable()) {
            try {
                method.invoke(targetObj, fixArgs(args));
            }
            catch(Exception e) {
                logger.log(Level.WARNING, FlashlightLoggerInfo.DTRACE_UNEXPECTED_EXCEPTION, e.getMessage());
            }
        }
    }

    public int getId() {
        return id;
    }

    private Object[] fixArgs(Object[] args) {
        // Logic:  DTrace only allows integral primitives and java.lang.String.
        // convert anything else to String.
        // be careful to not unbox!!
        // Very important to send back a COPY -- other listeners are sharing these args!!

        Object[] fixedArgs = new Object[args.length];

        for(int i = 0; i < args.length; i++) {
            if(args[i] == null) {
                fixedArgs[i] = null;
                continue;
            }

            Class clazz = args[i].getClass();

            if(!FlashlightUtils.isLegalDtraceParam(clazz))
                fixedArgs[i] = args[i].toString();
            else
                fixedArgs[i] = args[i];
        }
        
        return fixedArgs;
    }

    private final   int             id;
    private final   Method          method;
    private final   Object          targetObj;
}

