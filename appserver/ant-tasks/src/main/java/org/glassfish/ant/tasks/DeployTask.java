/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.ant.tasks;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;

import java.util.*;

public class DeployTask extends AdminTask {

    private String action;
    private String file;
    private Component component;
    private List<Component> components = new ArrayList<Component>();

    public DeployTask() {
        setAction("deploy");
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void setName(String name) {
        addCommandParameter("name", name);
    }

	public void setContextroot(String contextroot) {
        addCommandParameter("contextroot", contextroot);
    }


    public void setPrecompilejsp(boolean precompilejsp) {
        addCommandParameter("precompilejsp", Boolean.toString(precompilejsp));
    }

    public void setVerify(boolean verify) {
        addCommandParameter("verify", Boolean.toString(verify));
    }

    public void setCreatetables(boolean createtables) {
        addCommandParameter("createtables", Boolean.toString(createtables));
    }

    public void setDropandcreatetables(boolean dropandcreatetables) {
        addCommandParameter("dropandcreatetables", Boolean.toString(dropandcreatetables));
    }

    public void setUniquetablenames(boolean uniquetablenames) {
        addCommandParameter("uniquetablenames", Boolean.toString(uniquetablenames));
    }

    public void setEnabled(boolean enabled) {
        addCommandParameter("enabled", Boolean.toString(enabled));
    }

    public void setAvailabilityenabled(boolean availabilityenabled) {
        addCommandParameter("availabilityenabled", Boolean.toString(availabilityenabled));
    }

    public void setVirtualservers(String virtualservers) {
        addCommandParameter("virtualservers", virtualservers);
    }

    public void setRetrievestubs(String retrieve) {
        addCommandParameter("retrieve", retrieve);
    }

    public void setdbvendorname(String dbvendorname) {
        addCommandParameter("dbvendorname", dbvendorname);
    }

    public void setLibraries(String libraries) {
        addCommandParameter("libraries", libraries);
    }

    public void setDeploymentPlan(String deploymentplan) {
        addCommandParameter("deploymentplan", deploymentplan);
    }

    public void setForce(boolean force) {
        addCommandParameter("force", Boolean.toString(force));
    }

    public void setUpload(boolean force) {
        addCommandParameter("upload", Boolean.toString(force));
    }

    public void setProperties(String properties) {
        addCommandParameter("properties", properties);
    }

    public void setType(String type) {
        addCommandParameter("type", type);
    }

    public Component createComponent() {
        component = new Component();
        components.add(component);
        return component;
    }

    public void execute() throws BuildException {
        if (components.size() == 0 && file == null ) {
            log("File attributes or component must be specified", Project.MSG_WARN);
            return;
        }
        processComponents();
        if (file != null) {
            addCommandOperand(file);
            super.execute(action + " " + getCommand());
        }
    }

    private void processComponents() throws BuildException {
        for (Component comp : components) {
            if (comp.name != null)
                comp.addCommandParameter("name", comp.name);
            if (comp.file == null) {
                log("File attribute must be specified in component to deploy", Project.MSG_WARN);
                continue;
            }
            comp.addCommandOperand(comp.file);
            super.execute(action + " " + comp.getCommand());
        }
    }
}
