/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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

<f:verbatim>
    <script type="text/javascript">
        updateClusterDiscoveryMode("#{pageSession.valueMap['clusterMode']}");
        
        function updateClusterDiscoveryMode(selected) {
            var multicastPortComponent = document.getElementById("#{pageSession.multicastPortTextId}");
            var multicastGroupComponent = document.getElementById("#{pageSession.multicastGroupTextId}");
            var tcpipMembersComponent = document.getElementById("#{pageSession.tcpipMembersTextId}");
            var dnsMembersComponent = document.getElementById("#{pageSession.dnsMembersTextId}");
            var kubernetesNamespaceComponent = document.getElementById("#{pageSession.kubernetesNamespaceTextId}");
            var kubernetesServiceNameComponent = document.getElementById("#{pageSession.kubernetesServiceNameTextId}");
            
            var multicastPortProperty = document.getElementById("propertyForm:propertySheet:discovery:multicastPortProp");
            var multicastGroupProperty = document.getElementById("propertyForm:propertySheet:discovery:multicastGroupProp");
            var tcpipMembersProperty = document.getElementById("propertyForm:propertySheet:discovery:tcpipMembersProp");
            var dnsMembersProperty = document.getElementById("propertyForm:propertySheet:discovery:dnsMembersProp");
            var kubernetesNamespaceProperty = document.getElementById("propertyForm:propertySheet:discovery:kubernetesNamespaceProp");
            var kubernetesServiceNameProperty = document.getElementById("propertyForm:propertySheet:discovery:kubernetesServiceNameProp");
          
            if (selected === "tcpip") {
                multicastPortProperty.style.display = "none";
                multicastGroupProperty.style.display = "none";
                dnsMembersProperty.style.display = "none";
                tcpipMembersProperty.style.display = "table-row";
                kubernetesNamespaceProperty.style.display = "none"
                kubernetesServiceNameProperty.style.display = "none"
                multicastPortComponent.value = "";
                multicastGroupComponent.value = "";
                dnsMembersComponent.value = "";
                tcpipMembersComponent.value = "#{pageSession.valueMap['tcpipMembers']}";
                kubernetesNamespaceComponent.value = ""
                kubernetesServiceNameComponent.value = ""
            } else if (selected === "domain") {
                multicastPortProperty.style.display = "none";
                multicastGroupProperty.style.display = "none";
                tcpipMembersProperty.style.display = "none";
                dnsMembersProperty.style.display = "none";
                kubernetesNamespaceProperty.style.display = "none"
                kubernetesServiceNameProperty.style.display = "none"
                multicastPortComponent.value = "";
                multicastGroupComponent.value = "";
                tcpipMembersComponent.value = "";
                dnsMembersComponent.value = "";
                kubernetesNamespaceComponent.value = ""
                kubernetesServiceNameComponent.value = ""
            } else if (selected === "dns") {
                multicastPortProperty.style.display = "none";
                multicastGroupProperty.style.display = "none";
                tcpipMembersProperty.style.display = "none";
                dnsMembersProperty.style.display = "table-row";
                kubernetesNamespaceProperty.style.display = "none"
                kubernetesServiceNameProperty.style.display = "none"
                multicastPortComponent.value = "";
                multicastGroupComponent.value = "";
                tcpipMembersComponent.value = "";
                dnsMembersComponent.value = "#{pageSession.valueMap['dnsMembers']}";
                kubernetesNamespaceComponent.value = ""
                kubernetesServiceNameComponent.value = ""
            } else if (selected === "kubernetes") {
                multicastPortProperty.style.display = "none";
                multicastGroupProperty.style.display = "none";
                tcpipMembersProperty.style.display = "none";
                dnsMembersProperty.style.display = "none";
                kubernetesNamespaceProperty.style.display = "table-row"
                kubernetesServiceNameProperty.style.display = "table-row"
                multicastPortComponent.value = "";
                multicastGroupComponent.value = "";
                tcpipMembersComponent.value = "";
                dnsMembersComponent.value = "";
                kubernetesNamespaceComponent.value = "#{pageSession.valueMap['kubernetesNamespace']}"
                kubernetesServiceNameComponent.value = "#{pageSession.valueMap['kubernetesServiceName']}"
            } else {
                multicastPortProperty.style.display = "table-row";
                multicastGroupProperty.style.display = "table-row";
                tcpipMembersProperty.style.display = "none";
                dnsMembersProperty.style.display = "none";
                kubernetesNamespaceProperty.style.display = "none"
                kubernetesServiceNameProperty.style.display = "none"
                multicastPortComponent.value = "#{pageSession.valueMap['multicastPort']}";
                multicastGroupComponent.value = "#{pageSession.valueMap['multicastGroup']}";
                tcpipMembersComponent.value = "";
                dnsMembersComponent.value = "";
                kubernetesNamespaceComponent.value = ""
                kubernetesServiceNameComponent.value = ""
            }
        }
    </script>
</f:verbatim>

