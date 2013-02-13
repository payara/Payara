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

package org.glassfish.web.deployment.descriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.deployment.common.Descriptor;

/*
 * Deployment object representing the absolute-ordering of web-fragment.
 * @author Shing Wai Chan
 */
public class AbsoluteOrderingDescriptor extends Descriptor {
    private static final Object OTHERS = new Object();

    private static final LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(AbsoluteOrderingDescriptor.class);

    private List<Object> absOrder = new ArrayList<Object>();

    private boolean hasOthers = false;

    public void addName(String name) {
        if (absOrder.add(name) == false) {
            throw new IllegalStateException(localStrings.getLocalString(
                    "web.deployment.exceptionalreadydefinedinabsoluteordering",
                    "[{0}] has already been defined in the absolute-ordering.",
                    new Object[] { name }));
        }
    }

    public void addOthers() {
        if (absOrder.add(OTHERS) == false) {
            throw new IllegalStateException(localStrings.getLocalString(
                    "web.deployment.exceptionalreadydefinedinabsoluteordering",
                    "[{0}] is already defined in the absolute-ordering.",
                    new Object[] { "<others/>" }));
        }

        hasOthers = true;
    }

    public List<Object> getOrdering() {
        return Collections.unmodifiableList(absOrder);
    }

    /**
     * @return true if this AbsoluteOrderingDescriptor contains an
     * others element, false otherwise
     */
    public boolean hasOthers() {
        return hasOthers;
    }

    /**
     * This method return the WebFragmentDescriptor in absolute order.
     * Note that the number of element return may be less than that of the original list.
     */
    public List<WebFragmentDescriptor> order(List<WebFragmentDescriptor> wfs) {
        List<WebFragmentDescriptor> wfList = new ArrayList<WebFragmentDescriptor>();
        if (wfs != null && wfs.size() > 0) {
            Map<String, WebFragmentDescriptor> map = new HashMap<String, WebFragmentDescriptor>();
            List<WebFragmentDescriptor> othersList = new ArrayList<WebFragmentDescriptor>();
            for (WebFragmentDescriptor wf : wfs) {
                String name = wf.getName();
                if (name != null && name.length() > 0 && absOrder.contains(name)) {
                    map.put(name, wf);
                } else {
                    othersList.add(wf);
                }
            }

            for (Object obj : absOrder) {
                if (obj instanceof String) {
                    WebFragmentDescriptor wf = map.get((String)obj);
                    if (wf != null) {
                        wfList.add(wf);
                    }
                } else { // others
                    for (WebFragmentDescriptor wf : othersList) {
                        wfList.add(wf);
                    }
                }
            }
        }
        return wfList;
    }
}
