/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.virtualization.config;

import org.glassfish.api.Param;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;

/**
 * Defines an emulator used by the underlying virtualization infrastructure. Certain infrastructure like kvm offers
 * more than one emulator (could be sofware, or hardware based emulators) to run virtual machines.
 *
 * @author Jerome Dochez
 */

@Configured
public interface Emulator extends ConfigBeanProxy {

    /**
     * Name of the emulator.
     * @return
     */
    @Attribute(key=true)
    String getName();
    @Param(name="name", primary = true)
    void setName(String name);

    /**
     * Defines the virtualization type
     *
     * @return the virtualization type
     */
    @Attribute String getVirtType();
    @Param(name="virt-type") void setVirtType(String virtType);

    /**
     * Defines the path to the virtualization emulator.
     *
     * @return  the emulator path
     */
    @Attribute String getEmulatorPath();
    @Param(name="emulator-path") void setEmulatorPath(String path);

    /**
     * Defines the connection string to the emulator. The connection
     * string can also embed ${user.name} and ${target.host} elements that will
     * be replaced at runtime by the target machine identifier (like its IP
     * address) and the user configured to access this machine.
     *
     * List of tags replaced at runtime :
     * ${user.name}     user name used to connect to the target machine
     * ${target.host}   target machine identifier
     * ${auth.sep}      separator character if an authentication mechanism is provied
     * ${auth.method}     authentication mechanism (optional)
     *
     * @return the connection string
     */
    @Attribute String getConnectionString();
    @Param(name="connection-string") void setConnectionString(String connectString);
}
