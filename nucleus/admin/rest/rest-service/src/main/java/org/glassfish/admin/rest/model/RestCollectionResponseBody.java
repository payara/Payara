/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
import javax.ws.rs.core.UriInfo;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.composite.RestModel;
import org.glassfish.admin.rest.utils.JsonUtil;

/**
 * Used to return a standard REST response body that contains a collection of entities
 * @author tmoreau
 */
public class RestCollectionResponseBody<T extends RestModel> extends ResponseBody {

    private String collectionName;
    private UriInfo uriInfo;
    private List<T> items = new ArrayList<T>();

    // If you want this object to automatically compute the links to the child entities,
    // then include the collection name (which becomes the 'rel' part of the link) and the
    // URI of the collection resource, then just call 'addItem(item, name)' for each child.
    // Otherwise pass in null for both, and call the appropriate addItem method
    // to either add a child without a link, or a child where you control the link.

    public RestCollectionResponseBody(UriInfo uriInfo, String collectionName) {
        super();
        setUriInfo(uriInfo);
        setCollectionName(collectionName);
    }

    public RestCollectionResponseBody(boolean includeResourceLinks, UriInfo uriInfo, String collectionName) {
        super(includeResourceLinks);
        setUriInfo(uriInfo);
        setCollectionName(collectionName);
    }

    public String getCollectionName() {
        return this.collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public UriInfo getUriInfo() {
        return this.uriInfo;
    }

    public void setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    public List<T> getItems() {
        return this.items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public void addItem(T item, String name) {
        URI uri = (this.collectionName != null) ? this.uriInfo.getAbsolutePathBuilder().path("id").path(name).build() : null;
        addItem(item, name, uri);
    }

    public void addItem(T item, String name, URI uri) {
        addItem(item, this.collectionName, name, uri);
    }

    public void addItem(T item, String collectionName, String name, URI uri) {
        getItems().add(item);
        if (collectionName != null && uri != null) {
           addResourceLink(collectionName, name, uri);
        }
    }

    @Override
    protected void populateJson(JSONObject object) throws JSONException {
        super.populateJson(object);
        JSONArray array = new JSONArray();
        for (RestModel item : getItems()) {
            array.put(JsonUtil.getJsonObject(item));
        }
        object.put("items", array);
    }
}
