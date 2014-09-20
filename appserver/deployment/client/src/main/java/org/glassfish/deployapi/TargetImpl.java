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

package org.glassfish.deployapi;

import javax.enterprise.deploy.spi.Target;
import org.glassfish.deployment.client.TargetOwner;

import java.io.IOException;

/**
 * Implements the Target interface as specified by JSR-88.
 * <p>
 * This implementation is independent of the concrete type of its owner.
 * 
 * @author tjquinn
 */
public class TargetImpl implements Target {

    private TargetOwner owner;
    
    private String name;
    
    private String description;
    
    /**
     * Creates a new TargetImpl object.
     * <p>
     * Note that this constructor should normally be used only by a TargetOwner.
     * Logic that needs to create {@link Target} instances should invoke {@link TargetOwner#createTarget} or 
     * {@link TargetOwner#createTargets} on the TargetOwner.
     * 
     * @param owner
     * @param name
     * @param description
     */ // XXX It would be nice to move classes around so this could be package-visible and not public
    public TargetImpl(TargetOwner owner, String name, String description) {
        this.owner = owner;
        this.name = name;
        this.description = description;
    }

    /**
     * Returns the name of the Target.
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the description of the Target.
     * @return
     */
    public String getDescription() {
        return description;
    }
    
    public TargetOwner getOwner() {
        return owner;
    }

    /**
     *  Exports the Client stub jars to the given location.
     *  @param appName The name of the application or module.
     *  @param destDir The directory into which the stub jar file
     *  should be exported.
     *  @return the absolute location to the main jar file.
     */
    public String exportClientStubs(String appName, String destDir) 
        throws IOException {
        return owner.exportClientStubs(appName, destDir);
    }
}
