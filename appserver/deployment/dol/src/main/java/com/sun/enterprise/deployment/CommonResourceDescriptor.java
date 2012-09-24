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

import java.util.Set;
import org.glassfish.deployment.common.Descriptor;

/**
 * This class is used to defined common descriptor elements which is shared by classes which implements BundleDescriptor.
 * User: naman mehta
 * Date: 22/5/12
 * Time: 3:58 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class CommonResourceDescriptor extends Descriptor {

    CommonResourceFunctionality commonResourceFunctionality = new CommonResourceFunctionality();

    protected CommonResourceDescriptor() {

    }

    protected CommonResourceDescriptor(Descriptor other) {
        super(other);
    }

    protected MailSessionDescriptor getMailSessionDescriptor(String name) {
        return commonResourceFunctionality.getMailSessionDescriptor(name);
    }

    public void addMailSessionDescriptor(MailSessionDescriptor reference) {
        commonResourceFunctionality.addMailSessionDescriptor(reference);
    }

    public void removeMailSessionDescriptor(MailSessionDescriptor reference) {
        commonResourceFunctionality.removeMailSessionDescriptor(reference);
    }

    public Set<MailSessionDescriptor> getMailSessionDescriptors() {
        return commonResourceFunctionality.getMailSessionDescriptors();
    }

    public Set<DataSourceDefinitionDescriptor> getDataSourceDefinitionDescriptors() {
        return commonResourceFunctionality.getDataSourceDefinitionDescriptors();
    }

    protected DataSourceDefinitionDescriptor getDataSourceDefinitionDescriptor(String name) {
        return commonResourceFunctionality.getDataSourceDefinitionDescriptor(name);
    }

    public void addDataSourceDefinitionDescriptor(DataSourceDefinitionDescriptor reference) {
        commonResourceFunctionality.addDataSourceDefinitionDescriptor(reference);
    }

    public void removeDataSourceDefinitionDescriptor(DataSourceDefinitionDescriptor reference) {
        commonResourceFunctionality.removeDataSourceDefinitionDescriptor(reference);
    }
    
    // for connector-resource-definition
    public Set<ConnectorResourceDefinitionDescriptor> getConnectorResourceDefinitionDescriptors() {
        return commonResourceFunctionality.getConnectorResourceDefinitionDescriptors();
    }

    protected ConnectorResourceDefinitionDescriptor getConnectorResourceDefinitionDescriptor(String name) {
        return commonResourceFunctionality.getConnectorResourceDefinitionDescriptor(name);
    }

    public void addConnectorResourceDefinitionDescriptor(ConnectorResourceDefinitionDescriptor reference){
        commonResourceFunctionality.addConnectorResourceDefinitionDescriptor(reference);
    }

    public void removeConnectorResourceDefinitionDescriptor(ConnectorResourceDefinitionDescriptor reference){
        commonResourceFunctionality.removeConnectorResourceDefinitionDescriptor(reference);
    }

    // for admin-object-definition
    public Set<AdministeredObjectDefinitionDescriptor> getAdministeredObjectDefinitionDescriptors() {
        return commonResourceFunctionality.getAdministeredObjectDefinitionDescriptors();
    }

    protected AdministeredObjectDefinitionDescriptor getAdministeredObjectDefinitionDescriptor(String name) {
        return commonResourceFunctionality.getAdministeredObjectDefinitionDescriptor(name);
    }

    public void addAdministeredObjectDefinitionDescriptor(AdministeredObjectDefinitionDescriptor reference){
        commonResourceFunctionality.addAdministeredObjectDefinitionDescriptor(reference);
    }

    public void removeAdministeredObjectDefinitionDescriptor(AdministeredObjectDefinitionDescriptor reference){
        commonResourceFunctionality.removeAdministeredObjectDefinitionDescriptor(reference);
    }

    public Set<JMSConnectionFactoryDefinitionDescriptor> getJMSConnectionFactoryDefinitionDescriptors() {
        return commonResourceFunctionality.getJMSConnectionFactoryDefinitionDescriptors();
    }

    protected JMSConnectionFactoryDefinitionDescriptor getJMSConnectionFactoryDefinitionDescriptor(String name) {
        return commonResourceFunctionality.getJMSConnectionFactoryDefinitionDescriptor(name);
    }

    public void addJMSConnectionFactoryDefinitionDescriptor(JMSConnectionFactoryDefinitionDescriptor reference) {
        commonResourceFunctionality.addJMSConnectionFactoryDefinitionDescriptor(reference);
    }

    public void removeJMSConnectionFactoryDefinitionDescriptor(JMSConnectionFactoryDefinitionDescriptor reference) {
        commonResourceFunctionality.removeJMSConnectionFactoryDefinitionDescriptor(reference);
    }

    public Set<JMSDestinationDefinitionDescriptor> getJMSDestinationDefinitionDescriptors() {
        return commonResourceFunctionality.getJMSDestinationDefinitionDescriptors();
    }

    protected JMSDestinationDefinitionDescriptor getJMSDestinationDefinitionDescriptor(String name) {
        return commonResourceFunctionality.getJMSDestinationDefinitionDescriptor(name);
    }

    public void addJMSDestinationDefinitionDescriptor(JMSDestinationDefinitionDescriptor reference) {
        commonResourceFunctionality.addJMSDestinationDefinitionDescriptor(reference);
    }

    public void removeJMSDestinationDefinitionDescriptor(JMSDestinationDefinitionDescriptor reference) {
        commonResourceFunctionality.removeJMSDestinationDefinitionDescriptor(reference);
    }

}
