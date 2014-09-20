/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployment.client;

import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.status.ClientConfiguration;

import java.io.IOException;

/**
 * Represents any type of owner of a Target.
 * <p>
 * Each Target object needs to know what object created it so it can
 * delegate certain task to that object.  Different classes that connect to the
 * admin back-end in different ways can create Target objects, so this interface
 * prescribes the behavior that each such "owner" of Targets must provide.
 * <p>
 * Fully-formed Target objects will have links back to their respective TargetOwner
 * objects.
 * 
 * @author tjquinn
 */
public interface TargetOwner {

    /**
     * Creates a single {@link Target} with the given name.
     * @param name the name of the Target to be returned
     * @return a new Target
     */
    public Target createTarget(String name);
    
    /**
     * Creates several {@link Target} objects with the specified names.
     * @param names the names of the targets to be returned
     * @return new Targets, one for each name specified
     */
    public Target[] createTargets(String[] names);
    
    /**
     * Returns the Web URL for the specified module on the {@link Target}
     * implied by the TargetModuleID.
     * @param tmid
     * @return web url
     */
    public String getWebURL(TargetModuleID tmid);

    /**
     * Sets the Web URL for the specified module on the {@link Target} implied
     * by the TargetModuleID.
     * represents a Web module or submodule on a Target.
     * @param tmid
     * @param the URL
     */
    public void setWebURL(TargetModuleID tmid, String webURL);

    /**
     *  Exports the Client stub jars to the given location.
     *  @param appName The name of the application or module.
     *  @param destDir The directory into which the stub jar file
     *  should be exported.
     *  @return the absolute location to the main jar file.
     */
    public String exportClientStubs(String appName, String destDir) 
        throws IOException;
}
