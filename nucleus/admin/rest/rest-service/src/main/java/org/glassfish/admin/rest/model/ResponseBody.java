/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class ResponseBody {
    public static final String EVENT_NAME="response/body";
    private List<Message> messages = new ArrayList<Message>();
    private boolean includeResourceLinks = true;
    private List<ResourceLink> links = new ArrayList<ResourceLink>();

    public ResponseBody() {
    }

    public ResponseBody(boolean includeResourceLinks) {
        setIncludeResourceLinks(includeResourceLinks);
    }

    public ResponseBody(URI parentUri) {
        addParentResourceLink(parentUri);
    }

    public ResponseBody(boolean includeResourceLinks, URI parentUri) {
        setIncludeResourceLinks(includeResourceLinks);
        addParentResourceLink(parentUri);
    }

    public void setIncludeResourceLinks(boolean includeResourceLinks) {
        this.includeResourceLinks = includeResourceLinks;
    }

    public List<Message> getMessages() {
        return this.messages;
    }

    public void setMessages(List<Message> val) {
        this.messages = val;
    }

    public ResponseBody addSuccess(String message) {
        return addMessage(Message.Severity.SUCCESS, message);
    }

    public ResponseBody addWarning(String message) {
        return addMessage(Message.Severity.WARNING, message);
    }

    public ResponseBody addFailure(Throwable t) {
        for (; t != null; t = t.getCause()) {
            addFailure(t.getLocalizedMessage());
        }
        return this;
    }

    public ResponseBody addFailure(String message) {
        return addMessage(Message.Severity.FAILURE, message);
    }

    public ResponseBody addFailure(String field, String message) {
        return addMessage(Message.Severity.FAILURE, field, message);
    }

    public ResponseBody addMessage(Message.Severity severity, String field, String message) {
        return add(new Message(severity, field, message));
    }

    public ResponseBody addMessage(Message.Severity severity, String message) {
        return add(new Message(severity, message));
    }

    public ResponseBody add(Message message) {
        getMessages().add(message);
        return this;
    }

    public List<ResourceLink> getResourceLinks() {
        return this.links;
    }

    public void setResourceLinks(List<ResourceLink> val) {
        this.links = val;
    }

    public ResponseBody addParentResourceLink(URI uri) {
        if (uri == null) { return this; }
        return addResourceLink("parent", uri);
    }

    public void addActionResourceLink(String action, URI uri) {
        addResourceLink("action", action, uri);
    }

    public ResponseBody addResourceLink(String rel, URI uri) {
        return add(new ResourceLink(rel, uri));
    }

    public ResponseBody addResourceLink(String rel, String title, URI uri) {
        return add(new ResourceLink(rel, title, uri));
    }

    public ResponseBody add(ResourceLink link) {
        getResourceLinks().add(link);
        return this;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        populateJson(object);
        return object;
    }

    protected void populateJson(JSONObject object) throws JSONException {
        if (!getMessages().isEmpty()) {
            JSONArray array = new JSONArray();
            for (Message message : getMessages()) {
                array.put(message.toJson());
            }
            object.put("messages", array);
        }
        if (includeResourceLinks) {
            if (!getResourceLinks().isEmpty()) {
                JSONArray array = new JSONArray();
                for (ResourceLink link : getResourceLinks()) {
                    array.put(link.toJson());
                }
                object.put("resources", array);
            }
        }
    }
}
