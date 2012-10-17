/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010,2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.flashlight.impl.provider;

import com.sun.enterprise.util.LocalStringManagerImpl;

import java.lang.reflect.Method;

import org.glassfish.flashlight.FlashlightUtils;
import org.glassfish.flashlight.provider.FlashlightProbe;

/**
 *
 * @author bnevins
 */
class DTraceMethodFinder {

    public final static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(DTraceMethodFinder.class);

    DTraceMethodFinder(FlashlightProbe p, Object t) {
        probe = p;
        targetObject = t;
        targetClass = targetObject.getClass();
        probeParamTypes = probe.getParamTypes();

        if (probeParamTypes == null)
            probeParamTypes = new Class[0];

        numProbeParams = probeParamTypes.length;
    }

    Method matchMethod() {
        String metname = probe.getProviderJavaMethodName();

        // bnevins - the contract is that you do not call me with a hidden probe
        // If you do anyway -- I'll throw an unchecked exception as punishment.
        if (probe.isHidden())
            throw new RuntimeException(localStrings.getLocalString("dtrace_cantfind",
                    "The probe is  hidden.  DTrace will ignore it.", metname));

        for (Method m : targetClass.getMethods()) {
            if (!m.getName().equals(metname))
                continue;

            // we have a name match!!
            Class[] paramTypes = m.getParameterTypes(); // guaranteed non null

            if (paramTypes.length != numProbeParams)
                continue; // overloaded method

            if (!compareParams(probeParamTypes, paramTypes))
                continue; // overloaded method

            // we have a match!!!
            return m;
        }
        String errStr = localStrings.getLocalString("dtrace_cantfind",
                "Can not match the Probe method ({0}) with any method in the DTrace object.", metname);
        throw new RuntimeException(errStr);
    }

    private boolean compareParams(Class[] probep, Class[] dtracep) {
        // the lengths are guaranteed to be the same!
        for (int i = 0; i < probep.length; i++) {
            Class probeClass = probep[i];
            Class dtraceClass = dtracep[i];

            if (probeClass.equals(dtraceClass))
                continue;
            // something that can be coverted to String...
            else if (dtraceClass.equals(String.class) && !FlashlightUtils.isIntegral(probeClass))
                continue;
            // check for something like Short.class versus short.class
            // JDK will handle the boxing/unboxing
            else if (FlashlightUtils.compareIntegral(dtraceClass, probeClass))
                continue;
            else
                return false;
        }

        return true;
    }
    private final FlashlightProbe probe;
    private final Object targetObject;
    private final Class targetClass;
    private final int numProbeParams;
    private Method method;
    private Class[] probeParamTypes;
}
