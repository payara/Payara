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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.flashlight.xml;

import java.util.*;

/**
 *
 * @author Mahesh Meswani
 */
public class XmlProbe {
    String probeName = null;
    String probeMethod = null;
    List<XmlProbeParam> probeParams = null;
    boolean hasSelf = false;
    boolean isHidden = false;
    boolean stateful = false;
    boolean statefulReturn = false;
    boolean statefulException = false;
    String profileNames = null;

    public String getProbeName() {
        return probeName;
    }

    public String getProbeMethod() {
        return probeMethod;
    }

    public List<XmlProbeParam> getProbeParams() {
        return probeParams;
    }

    public boolean hasSelf() {
        return hasSelf;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public boolean getStateful() { return stateful; }
    public boolean getStatefulReturn() { return statefulReturn; }
    public boolean getStatefulException() { return statefulException; }
    public String getProfileNames() { return profileNames; } 

    public XmlProbe(String probeName, String method, List<XmlProbeParam> params, boolean hasSelf, boolean isHidden) {
        this.probeName = probeName;
        probeMethod = method;
        probeParams = params;
        this.hasSelf = hasSelf;
        this.isHidden = isHidden;
    }

    public XmlProbe(String probeName, String method, List<XmlProbeParam> params, boolean hasSelf, boolean isHidden,
            boolean isStateful, boolean statefulReturn, boolean statefulException, String profileNames) {
        this.probeName = probeName;
        probeMethod = method;
        probeParams = params;
        this.hasSelf = hasSelf;
        this.isHidden = isHidden;
        this.stateful = isStateful;
        this.statefulReturn = statefulReturn;
        this.statefulException = statefulException;
        this.profileNames = profileNames;
    }

    @Override
    public String toString() {
        final StringBuilder paramsStr = new StringBuilder("     \n");
        for (XmlProbeParam param : probeParams) {
            paramsStr.append("         , Param ").append(param.toString());
        }
        return (" Probe name = " + probeName +
                " , method = " + probeMethod + paramsStr.toString());
    }
}
