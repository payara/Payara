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

package com.sun.enterprise.deployment;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.deployment.common.Descriptor;

/**
 * Created by IntelliJ IDEA.
 * User: naman
 * Date: 24/5/12
 * Time: 11:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class CommonResourceFunctionality implements Serializable {

    private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(CommonResourceFunctionality.class);

    private Set<MailSessionDescriptor> mailSessionDescriptors =
            new HashSet<MailSessionDescriptor>();

    private Set<DataSourceDefinitionDescriptor> datasourceDefinitionDescs =
            new HashSet<DataSourceDefinitionDescriptor>();

    private Set<Descriptor> allResourceDescriptors = new HashSet<Descriptor>();

    public Set<Descriptor> getAllResourcesDescriptors() {
        return allResourceDescriptors;
    }

    protected Descriptor getAllResourcesDescriptors(String name) {
        Descriptor descriptor = null;
        for (Descriptor thiDescriptor : this.getAllResourcesDescriptors()) {
            if (thiDescriptor.getName().equals(name)) {
                descriptor = thiDescriptor;
                break;
            }
        }
        return descriptor;
    }

    private boolean foundDescriptor(Descriptor reference) {

        Descriptor descriptor = getAllResourcesDescriptors(reference.getName());
        if (descriptor != null) {
            return true;
        }
        return false;
    }

    public Set<MailSessionDescriptor> getMailSessionDescriptors() {
        return mailSessionDescriptors;
    }

    protected MailSessionDescriptor getMailSessionDescriptor(String name) {
        MailSessionDescriptor ddDesc = null;
        for (MailSessionDescriptor ddd : this.getMailSessionDescriptors()) {
            if (ddd.getName().equals(name)) {
                ddDesc = ddd;
                break;
            }
        }

        return ddDesc;
    }

    public void addMailSessionDescriptor(MailSessionDescriptor reference) {
        if (foundDescriptor(reference)) {
            throw new IllegalStateException(
                    localStrings.getLocalString("exceptionwebduplicatedescriptor",
                            "This app cannot have descriptor definitions of same name : [{0}]",
                            reference.getName()));
        } else {
            getMailSessionDescriptors().add(reference);
            allResourceDescriptors.add(reference);
        }
    }

    public void removeMailSessionDescriptor(MailSessionDescriptor reference) {
        this.getMailSessionDescriptors().remove(reference);
        allResourceDescriptors.remove(reference);
    }

    public Set<DataSourceDefinitionDescriptor> getDataSourceDefinitionDescriptors() {
        return datasourceDefinitionDescs;
    }

    protected DataSourceDefinitionDescriptor getDataSourceDefinitionDescriptor(String name) {
        DataSourceDefinitionDescriptor ddDesc = null;
        for (DataSourceDefinitionDescriptor ddd : this.getDataSourceDefinitionDescriptors()) {
            if (ddd.getName().equals(name)) {
                ddDesc = ddd;
                break;
            }
        }

        return ddDesc;
    }

    public void addDataSourceDefinitionDescriptor(DataSourceDefinitionDescriptor reference) {
        if (foundDescriptor(reference)) {
            throw new IllegalStateException(
                    localStrings.getLocalString("exceptionwebduplicatedescriptor",
                            "This app cannot have descriptor definitions of same name : [{0}]",
                            reference.getName()));
        } else {
            getDataSourceDefinitionDescriptors().add(reference);
            allResourceDescriptors.add(reference);
        }
    }

    public void removeDataSourceDefinitionDescriptor(DataSourceDefinitionDescriptor reference) {
        this.getDataSourceDefinitionDescriptors().remove(reference);
        allResourceDescriptors.remove(reference);
    }

}
