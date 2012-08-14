/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.module.maven.commandsecurityplugin;

import java.util.ArrayList;
import java.util.List;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Verifies that all inhabitants in the module that are commands also take care
 * of authorization, issuing warnings or failing the build (configurable) if
 * any do not.
 * <p>
 * The mojo has to analyze not only the inhabitants but potentially also 
 * their ancestor classes.  To improve performance across multiple modules in 
 * the same build the mojo stores information about classes known to be commands and
 * classes known not to be commands in the maven session. 
 * 
 * @author tjquinn
 * @goal check
 * @threadSafe
 * @phase process-classes
 * @requiresProject
 * @requiresDependencyResolution compile+runtime
 */
public class CheckMojo extends AbstractMojo {
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;
    
    /** 
     * The Maven Session Object 
     * 
     * @parameter expression="${session}" 
     * @required 
     * @readonly 
     */ 
    protected MavenSession session; 
    
    /**
     * Whether failures are fatal to the build.
     * @parameter
     *   expression="${command-security-plugin.isFailureFatal}"
     *   default-value="true"
     */
    private String isFailureFatal;
    
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final TypeProcessor typeProcessor = new TypeProcessorImpl(this, session, project, isFailureFatal);
        typeProcessor.execute();
        
        final StringBuilder trace = typeProcessor.trace();
        
        if (trace != null) {
            getLog().debug(trace.toString());
        }
        if (typeProcessor.okClassNames() != null) {
            getLog().debug("Command classes with authorization: " + typeProcessor.okClassNames().toString());
        }
        final List<String> offendingClassNames = typeProcessor.offendingClassNames();
        if ( ! offendingClassNames.isEmpty()) {
            if (typeProcessor.isFailureFatal()) {
                getLog().error("Following command classes neither provide nor inherit authorization: " + offendingClassNames.toString());
                throw new MojoFailureException("Command class(es) with no authorization");
            } else {
                getLog().warn("Following command classes neither provide nor inherit authorization: " + offendingClassNames.toString());
            }
        }
        
    }
}
