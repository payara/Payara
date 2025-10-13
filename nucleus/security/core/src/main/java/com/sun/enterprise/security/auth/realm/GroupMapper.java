/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018-2019] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.auth.realm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 *
 * @author Kumar
 */
public class GroupMapper {

    private Map<String, List<String>> groupMappingTable = new HashMap<>();

    public void parse(String mappingStr) {
        StringTokenizer tokenizer = new StringTokenizer(mappingStr, ";");
        while (tokenizer.hasMoreElements()) {
            String mapping = tokenizer.nextToken();
            String[] mappingGroups = mapping.split(",");
            String mappedGroup = null;
            int indexOfArrow = mapping.indexOf("->");
            if (indexOfArrow > 0 && mappingGroups != null && (mappingGroups.length > 0)) {
                String tmpGroup = mapping.substring(indexOfArrow + 2);
                mappedGroup = tmpGroup.trim();
            }
            validate(mappedGroup, mappingGroups);
            for (String grp : mappingGroups) {
                int aIndex = grp.indexOf("->");
                String theGroup = (aIndex > 0) ? grp.substring(0, aIndex).trim() : grp.trim();
                List<String> mappedGroupList = groupMappingTable.get(theGroup);
                if (mappedGroupList == null) {
                    mappedGroupList = new ArrayList<>();
                }
                mappedGroupList.add(mappedGroup);
                groupMappingTable.put(theGroup, mappedGroupList);
            }
        }
    }

    public void getMappedGroups(String group, List<String> result) {
        if (result == null) {
            throw new RuntimeException("result argument cannot be NULL");
        }
        
        List<String> mappedGrps = groupMappingTable.get(group);
        if (mappedGrps == null || mappedGrps.isEmpty()) {
            return;
        }
        
        addUnique(result, mappedGrps);
        
        // Look for transitive closure
        List<String> result1 = new ArrayList<>();
        for (String str : mappedGrps) {
            getMappedGroups(group, str, result1);
        }
        
        addUnique(result, result1);
    }

    private void addUnique(List<String> dest, List<String> src) {
        for (String str : src) {
            if (!dest.contains(str)) {
                dest.add(str);
            }
        }
    }

    private void getMappedGroups(String group, String str, List<String> result) {
        List<String> mappedGrps = groupMappingTable.get(str);
        if (mappedGrps == null || mappedGrps.isEmpty()) {
            return;
        }
        
        if (mappedGrps.contains(group)) {
            throw new RuntimeException("Illegal Mapping: cycle detected with group'" + group);
        }
        
        addUnique(result, mappedGrps);
        
        for (String str1 : mappedGrps) {
            getMappedGroups(group, str1, result);
        }
    }

    private void validate(String mappedGroup, String[] mappingGroups) {
        for (String str : mappingGroups) {
            int aIndex = str.indexOf("->");
            String theGroup = (aIndex > 0) ? str.substring(0, aIndex) : str;
            if (theGroup.equals(mappedGroup)) {
                throw new RuntimeException("Illegal Mapping: Identity Mapping of group '" + theGroup + "' to '" + theGroup + "'");
            }
        }
    }
}
