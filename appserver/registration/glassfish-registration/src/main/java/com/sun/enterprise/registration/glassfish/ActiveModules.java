/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.registration.glassfish;

import com.sun.enterprise.module.Module;
import com.sun.enterprise.module.ModuleState;
import com.sun.enterprise.module.ModulesRegistry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActiveModules {

    private static final int UNMAPPED_MODULE = 384;
    private static final int NLONGS = 6;
    private ModulesRegistry registry;
    private Logger logger;

    public ActiveModules(Logger logger, ModulesRegistry registry) {
        this.logger = logger;
        this.registry = registry;
    }

    /*
     * For all modules in the READY state map the module to a location
     * in a bit field.   The location is determined by the mapping defined
     * in the ModuleMap.   The bit to set is in one of six long values.  
     * This method calculates which bit in which long value should be set.
     * We use network-byte order.
     */
    public String generateModuleStatus() {
        long[] loadedModules = {0, 0, 0, 0, 0, 0};
        StringBuilder str = new StringBuilder(128);

        for (Module m : registry.getModules()) {
            if (m.getState() == ModuleState.READY) {
                Integer bit = ModuleMap.getMap().get(m.getName());

                /* 
                 * If the module name is not in the map set the high bit
                 * so we know we have unmapped modules in use.
                 */
                if (bit == null) {
                    bit = UNMAPPED_MODULE;
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("ActiveModules: Unmapped module: " +
                                m.getName());
                    }
                }

                int group = (bit - 1) / 64;

                if (group > 5) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("ActiveModules: id out of range: " +
                                m.getName() + " " + bit);
                    }
                    // Ignore if the module map id is out of range.
                    continue;
                }

                bit -= 64 * group;

                if (bit != null) {
                    loadedModules[group] |= ((long)1 << (bit.intValue() - 1));
                }
            }
        }

        // Convert the longs into a string suitable as a value in the UserAgent.
        // We are using the standard network byte order (big-endian)
        for (int i = NLONGS - 1; i >= 0; i--) {
            str.append(Long.toHexString(loadedModules[i]));
            if (i != 0)
                str.append(" ");
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("ActiveModules: loadModules[0]: " +
                    Long.toBinaryString(loadedModules[0]));
            logger.fine("ActiveModules: loadModules[1]: " +
                    Long.toBinaryString(loadedModules[1]));
            logger.fine("ActiveModules: loadModules[2]: " +
                    Long.toBinaryString(loadedModules[2]));
            logger.fine("ActiveModules: loadModules[3]: " +
                    Long.toBinaryString(loadedModules[3]));
            logger.fine("ActiveModules: loadModules[4]: " +
                    Long.toBinaryString(loadedModules[4]));
            logger.fine("ActiveModules: loadModules[5]: " +
                    Long.toBinaryString(loadedModules[5]));
        }

        return (str.toString());
    }
}
