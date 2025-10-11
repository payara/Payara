/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 * Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.

 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.

 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.

 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.

 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"

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
 **/

package org.glassfish.admingui.common.handlers;

import org.glassfish.admingui.common.util.GuiUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Type representing properties as List of Maps.
 */
class PropertyList implements Iterable<Map.Entry<String, String>> {
    private Map<String, String> properties = new LinkedHashMap<>();

    private PropertyList() {

    }

    void put(String key, String value) {
        if (GuiUtil.isEmpty(key)) {
            return;
        }
        if (!GuiUtil.isEmpty(value)) {
            properties.put(key, value);
        }
    }

    void put(String key, Map<String, String> sourceMap, String sourceKey) {
        if (sourceMap != null) {
            put(key, sourceMap.get(sourceKey));
        }
    }

    public int size() {
        return properties.size();
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }

    public boolean containsKey(Object key) {
        return properties.containsKey(key);
    }

    public String get(Object key) {
        return properties.get(key);
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return properties.entrySet().iterator();
    }

    public List<Map<String, String>> toList() {
        return properties.entrySet().stream()
                .map(e -> toPropertyEntry(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public String toParamValue() {
        StringBuilder paramValue = new StringBuilder();
        for (Map.Entry<String, String> oneProp : this) {
            paramValue.append(oneProp.getKey()).append("=");
            String value = oneProp.getValue().replaceAll("\\\\", "\\\\\\\\");
            value = UtilHandlers.escapePropertyValue(value);
            paramValue.append(value).append(":");
        }
        return paramValue.toString();
    }

    static Map<String, String> toPropertyEntry(String name, String value) {
        Map<String, String> result = new HashMap<>();
        result.put("name", name);
        result.put("value", value);
        return result;
    }

    static PropertyList fromList(List<Map<String, String>> propertyList) {
        PropertyList result = new PropertyList();
        for (Map<String, String> entry : propertyList) {
            result.put(entry.get("name"), entry.get("value"));
        }
        return result;
    }

    static PropertyList fromMap(Map<String, String> map) {
        PropertyList result = new PropertyList();
        result.properties.putAll(map);
        return result;
    }
}
