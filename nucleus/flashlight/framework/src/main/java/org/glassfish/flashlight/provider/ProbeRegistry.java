/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.flashlight.provider;

import java.util.Collection;
import java.util.ArrayList;

import org.glassfish.flashlight.provider.FlashlightProbe;
import org.glassfish.flashlight.impl.core.*;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mahesh Kannan
 *         Date: Jul 20, 2008
 */
@Service
@Singleton
public class ProbeRegistry {

    private static volatile ProbeRegistry _me = new ProbeRegistry();

    private static ConcurrentHashMap<Integer, FlashlightProbe> probeMap =
                new ConcurrentHashMap<Integer, FlashlightProbe>();
    private static ConcurrentHashMap<String, FlashlightProbe> probeDesc2ProbeMap =
                new ConcurrentHashMap<String, FlashlightProbe>();

    public static ProbeRegistry getInstance() {
        return _me;
    }

    // bnevins -- todo this is a huge concurrency bug!
    // why is it even here?!?
    // @deprecated

    @Deprecated
    public static ProbeRegistry createInstance() {
    	if (_me == null) {
    		_me = new ProbeRegistry();
    	}

    	return _me;
    }

    public static void cleanup() {
        if (_me != null) {
            _me = new ProbeRegistry();
        }
        ProbeProviderRegistry.cleanup();
    }

    public void registerProbe(FlashlightProbe probe) {
        probeMap.put(probe.getId(), probe);
        probeDesc2ProbeMap.put(probe.getProbeDesc(), probe);
        //System.out.println("[FL]Registered probe : " + probe.getProbeStr());
    }

    public void unregisterProbe(FlashlightProbe probe) {
        probeDesc2ProbeMap.remove(probe.getProbeDesc());
        probeMap.remove(probe.getId());
    }

    public void unregisterProbe(int id) {
        probeMap.remove(id);
    }

    public FlashlightProbe getProbe(int id) {
        return probeMap.get(id);
    }

    public FlashlightProbe getProbe(String probeStr) {
        //System.out.println("[FL]Get probe : " + probeStr);
        return probeDesc2ProbeMap.get(probeStr);
    }

    public static FlashlightProbe getProbeById(int id) {
        return _me.getProbe(id);
    }

    public Collection<FlashlightProbe> getAllProbes() {
       Collection<FlashlightProbe> allProbes = probeMap.values();
       Collection<FlashlightProbe> visibleProbes = new ArrayList<FlashlightProbe>();
       for (FlashlightProbe probe : allProbes) {
           if (!probe.isHidden())
               visibleProbes.add(probe);
       }
       return visibleProbes;
    }

   public static void invokeProbe(int id, Object[] args) {
    	FlashlightProbe probe = probeMap.get(id);
    	if (probe != null) {
    		probe.fireProbe(args);
    	}
    }

   public static Object invokeProbeBefore(int id, Object[] args) {
       FlashlightProbe probe = probeMap.get(id);
       if (probe != null) {
           return probe.fireProbeBefore(args);
       }
       return null;
   }

   public static void invokeProbeAfter(Object returnValue, int id,
                        Object states) {
       FlashlightProbe probe = probeMap.get(id);
       if (probe != null) {
           try {
               probe.fireProbeAfter(returnValue, (ArrayList<FlashlightProbe.ProbeInvokeState>)states);
           } catch (ClassCastException e) {
               // Make sure the state we got was really ok, internal error if that happens
           }
       }
   }

   public static void invokeProbeOnException(Object exceptionValue, int id,
                        Object states) {
       FlashlightProbe probe = probeMap.get(id);
       if (probe != null) {
           try {
               probe.fireProbeOnException(exceptionValue, (ArrayList<FlashlightProbe.ProbeInvokeState>)states);
           } catch (ClassCastException e) {
               // Make sure the state we got was really ok, internal error if that happens
           }
       }
   }
}
