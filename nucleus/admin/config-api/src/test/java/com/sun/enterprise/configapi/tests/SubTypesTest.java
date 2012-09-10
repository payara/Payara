/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.configapi.tests;

import com.sun.enterprise.config.serverbeans.Domain;

import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.ConfigBean;


/**
 * User: Jerome Dochez
 * Date: Mar 20, 2008
 * Time: 2:44:48 PM
 */
public class SubTypesTest extends ConfigApiTest {

    // not testing all the sub types, just a few to be sure it works ok.
    String expectedClassNames[] = {
        "com.sun.enterprise.config.serverbeans.Applications",
        "com.sun.enterprise.config.serverbeans.Configs",
        "com.sun.enterprise.config.serverbeans.Clusters"
    };


    public String getFileName() {
        return "DomainTest";
    }    

    @Test
    public void testSubTypesOfDomain() {
        Domain domain = super.getHabitat().getService(Domain.class);
        try {
            Class<?>[] subTypes = ConfigSupport.getSubElementsTypes((ConfigBean) ConfigBean.unwrap(domain));
            for (Class subType : subTypes) {
                Logger.getAnonymousLogger().fine("Found class" + subType);
            }
            for (String expectedClassName : expectedClassNames) {
                boolean found=false;
                for (Class<?> subType : subTypes)  {
                    if (subType.getName().equals(expectedClassName)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    Logger.getAnonymousLogger().severe("Cannot find " + expectedClassName + " from list of subtypes");
                    for (Class subType : subTypes) {
                        Logger.getAnonymousLogger().severe("Found class" + subType);
                    }
                    throw new RuntimeException("Cannot find " + expectedClassName);
                }
            }
        } catch(ClassNotFoundException e) {
            e.printStackTrace();           
        }
    }
}
