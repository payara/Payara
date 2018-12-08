/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.microprofile.openapi.impl.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.servers.Server;

public class PathItemImpl extends ExtensibleImpl implements PathItem {

    protected String ref;
    protected String summary;
    protected String description;
    protected Operation get;
    protected Operation put;
    protected Operation post;
    protected Operation delete;
    protected Operation options;
    protected Operation head;
    protected Operation patch;
    protected Operation trace;
    protected List<Server> servers = new ArrayList<>();
    protected List<Parameter> parameters = new ArrayList<>();

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public void setRef(String ref) {
        this.ref = ref;
    }

    @Override
    public PathItem ref(String ref) {
        setRef(ref);
        return this;
    }

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public void setSummary(String summary) {
        this.summary = summary;
    }

    @Override
    public PathItem summary(String summary) {
        setSummary(summary);
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public PathItem description(String description) {
        setDescription(description);
        return this;
    }

    @Override
    public Operation getGET() {
        return get;
    }

    @Override
    public void setGET(Operation get) {
        this.get = get;
    }

    @Override
    public PathItem GET(Operation get) {
        setGET(get);
        return this;
    }

    @Override
    public Operation getPUT() {
        return put;
    }

    @Override
    public void setPUT(Operation put) {
        this.put = put;
    }

    @Override
    public PathItem PUT(Operation put) {
        setPUT(put);
        return this;
    }

    @Override
    public Operation getPOST() {
        return post;
    }

    @Override
    public void setPOST(Operation post) {
        this.post = post;
    }

    @Override
    public PathItem POST(Operation post) {
        setPOST(post);
        return this;
    }

    @Override
    public Operation getDELETE() {
        return delete;
    }

    @Override
    public void setDELETE(Operation delete) {
        this.delete = delete;
    }

    @Override
    public PathItem DELETE(Operation delete) {
        setDELETE(delete);
        return this;
    }

    @Override
    public Operation getOPTIONS() {
        return options;
    }

    @Override
    public void setOPTIONS(Operation options) {
        this.options = options;
    }

    @Override
    public PathItem OPTIONS(Operation options) {
        setOPTIONS(options);
        return this;
    }

    @Override
    public Operation getHEAD() {
        return head;
    }

    @Override
    public void setHEAD(Operation head) {
        this.head = head;
    }

    @Override
    public PathItem HEAD(Operation head) {
        setHEAD(head);
        return this;
    }

    @Override
    public Operation getPATCH() {
        return patch;
    }

    @Override
    public void setPATCH(Operation patch) {
        this.patch = patch;
    }

    @Override
    public PathItem PATCH(Operation patch) {
        setPATCH(patch);
        return this;
    }

    @Override
    public Operation getTRACE() {
        return trace;
    }

    @Override
    public void setTRACE(Operation trace) {
        this.trace = trace;
    }

    @Override
    public PathItem TRACE(Operation trace) {
        setTRACE(trace);
        return this;
    }

    @Override
    public List<Operation> readOperations() {
        List<Operation> allOperations = new ArrayList<>();
        if (this.get != null) {
            allOperations.add(this.get);
        }
        if (this.put != null) {
            allOperations.add(this.put);
        }
        if (this.head != null) {
            allOperations.add(this.head);
        }
        if (this.post != null) {
            allOperations.add(this.post);
        }
        if (this.delete != null) {
            allOperations.add(this.delete);
        }
        if (this.patch != null) {
            allOperations.add(this.patch);
        }
        if (this.options != null) {
            allOperations.add(this.options);
        }
        if (this.trace != null) {
            allOperations.add(this.trace);
        }

        return allOperations;
    }

    @Override
    public Map<HttpMethod, Operation> readOperationsMap() {
        Map<HttpMethod, Operation> result = new EnumMap<>(HttpMethod.class);

        if (this.get != null) {
            result.put(HttpMethod.GET, this.get);
        }
        if (this.put != null) {
            result.put(HttpMethod.PUT, this.put);
        }
        if (this.post != null) {
            result.put(HttpMethod.POST, this.post);
        }
        if (this.delete != null) {
            result.put(HttpMethod.DELETE, this.delete);
        }
        if (this.patch != null) {
            result.put(HttpMethod.PATCH, this.patch);
        }
        if (this.head != null) {
            result.put(HttpMethod.HEAD, this.head);
        }
        if (this.options != null) {
            result.put(HttpMethod.OPTIONS, this.options);
        }
        if (this.trace != null) {
            result.put(HttpMethod.TRACE, this.trace);
        }

        return result;
    }

    @Override
    public List<Server> getServers() {
        return servers;
    }

    @Override
    public void setServers(List<Server> servers) {
        this.servers = servers;
    }

    @Override
    public PathItem servers(List<Server> servers) {
        setServers(servers);
        return this;
    }

    @Override
    public PathItem addServer(Server server) {
        servers.add(server);
        return this;
    }

    @Override
    public List<Parameter> getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public PathItem parameters(List<Parameter> parameters) {
        setParameters(parameters);
        return this;
    }

    @Override
    public PathItem addParameter(Parameter parameter) {
        parameters.add(parameter);
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PathItemImpl other = (PathItemImpl) obj;
        if (!Objects.equals(this.ref, other.ref)) {
            return false;
        }
        if (!Objects.equals(this.summary, other.summary)) {
            return false;
        }
        if (!Objects.equals(this.description, other.description)) {
            return false;
        }
        if (!Objects.equals(this.get, other.get)) {
            return false;
        }
        if (!Objects.equals(this.put, other.put)) {
            return false;
        }
        if (!Objects.equals(this.post, other.post)) {
            return false;
        }
        if (!Objects.equals(this.delete, other.delete)) {
            return false;
        }
        if (!Objects.equals(this.options, other.options)) {
            return false;
        }
        if (!Objects.equals(this.head, other.head)) {
            return false;
        }
        if (!Objects.equals(this.patch, other.patch)) {
            return false;
        }
        if (!Objects.equals(this.trace, other.trace)) {
            return false;
        }
        if (!Objects.equals(this.servers, other.servers)) {
            return false;
        }
        if (!Objects.equals(this.parameters, other.parameters)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.ref);
        hash = 37 * hash + Objects.hashCode(this.summary);
        hash = 37 * hash + Objects.hashCode(this.description);
        hash = 37 * hash + Objects.hashCode(this.get);
        hash = 37 * hash + Objects.hashCode(this.put);
        hash = 37 * hash + Objects.hashCode(this.post);
        hash = 37 * hash + Objects.hashCode(this.delete);
        hash = 37 * hash + Objects.hashCode(this.options);
        hash = 37 * hash + Objects.hashCode(this.head);
        hash = 37 * hash + Objects.hashCode(this.patch);
        hash = 37 * hash + Objects.hashCode(this.trace);
        hash = 37 * hash + Objects.hashCode(this.servers);
        hash = 37 * hash + Objects.hashCode(this.parameters);
        return hash;
    }

}
