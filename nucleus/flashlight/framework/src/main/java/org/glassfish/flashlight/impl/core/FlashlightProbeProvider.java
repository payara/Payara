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

package org.glassfish.flashlight.impl.core;

import com.sun.enterprise.util.ObjectAnalyzer;
import org.glassfish.api.monitoring.ProbeInfo;
import org.glassfish.api.monitoring.ProbeProviderInfo;
import org.glassfish.flashlight.provider.FlashlightProbe;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Byron Nevins, October 2009
 * This class implements a very public interface.
 * I changed it to do some minimal error checking.
 * It throws RuntimeException because it is too late to change the signature
 * for the interface.
 *
 *
 * @author Mahesh Kannan
 * @author Byron Nevins
 */
public class FlashlightProbeProvider implements ProbeProviderInfo{
    /**
     * GUARANTEED to have all 3 names valid -- or at least not null and not empty
     * @param moduleProviderName
     * @param moduleName
     * @param probeProviderName
     * @param providerClazz
     * @throws RuntimeException if parameters are null or empty
     */
     public FlashlightProbeProvider(String moduleProviderName, String moduleName,
                                    String probeProviderName, Class providerClazz) {

        if(!ok(moduleProviderName) || !ok(moduleName) || !ok(providerClazz))
            throw new RuntimeException(CTOR_ERROR);

        this.moduleProviderName = moduleProviderName;
        this.moduleName = moduleName;
        this.providerClazz = providerClazz;

        if(probeProviderName == null)
            this.probeProviderName = providerClazz.getName();
        else
            this.probeProviderName = probeProviderName;

    }

	public String toString() {
		return ObjectAnalyzer.toString(this);
	}
    public Class getProviderClass() {
        return providerClazz;
    }
    public String getModuleProviderName() {
        return moduleProviderName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getProbeProviderName() {
        return probeProviderName;
    }

    public void addProbe(FlashlightProbe probe) {
        probes.put(probe.getProbeDesc(), probe);
    }

    public FlashlightProbe getProbe(String probeDescriptor) {
        return (probes.get(probeDescriptor));
    }

    public Collection<FlashlightProbe> getProbes() {
        return probes.values();
    }

    public ProbeInfo[] getProbesInfo() {
        // confusing -- the *map* of the probes is named "probes"
        Collection<FlashlightProbe> fprobes = getProbes();
        ProbeInfo[] infos = new ProbeInfo[fprobes.size()];

        int i = 0;
        for(FlashlightProbe fprobe : fprobes) {
            infos[i++] = (ProbeInfo) fprobe;
        }
        return infos;
    }

    public boolean isDTraceInstrumented() {
        return dtraceIsInstrumented;
    }
    public void setDTraceInstrumented(boolean b) {
        dtraceIsInstrumented = b;
    }
    
    // note that it is IMPOSSIBLE for an object instance to have null variables --
    // they are final and checked at instantiation time...
    // we are NOT checking the probes --
    public boolean namesEqual(Object o) {
        if(o == null)
            return false;

        if( ! (o instanceof FlashlightProbeProvider))
            return false;

        FlashlightProbeProvider fpp = (FlashlightProbeProvider) o;

        return
            fpp.moduleName.equals(moduleName) &&
            fpp.moduleProviderName.equals(moduleProviderName)  &&
            fpp.probeProviderName.equals(probeProviderName) &&
            fpp.providerClazz == providerClazz;
    }
    
    private static boolean ok(String s) {
        return s != null && s.length() > 0;
    }

    private static boolean ok(Class clazz) {
        return clazz != null;
    }

    private boolean dtraceIsInstrumented;
    private final String moduleProviderName;
    private final String moduleName;
    private final String probeProviderName;
    private final Class providerClazz;
    private ConcurrentHashMap<String, FlashlightProbe> probes = new ConcurrentHashMap<String, FlashlightProbe>();
    private static final String CTOR_ERROR = "ProbeProviderInfo constructor -- you must supply valid arguments";
}
