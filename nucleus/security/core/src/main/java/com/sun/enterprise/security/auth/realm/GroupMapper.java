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

package com.sun.enterprise.security.auth.realm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 *
 * @author Kumar
 */
public class GroupMapper {

    private Map<String, ArrayList<String>> groupMappingTable = new HashMap<String, ArrayList<String>>();
    
    public void parse(String mappingStr) {
        StringTokenizer tokenizer = new StringTokenizer(mappingStr, ";");
        while(tokenizer.hasMoreElements()) {
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
                String theGroup = null;
                if (aIndex > 0) {
                    String tGrp = grp.substring(0, aIndex);
                    theGroup = tGrp.trim();
                } else {
                    theGroup = grp.trim();
                }
                ArrayList<String> mappedGroupList = groupMappingTable.get(theGroup);
                if (mappedGroupList == null) {
                    mappedGroupList = new ArrayList<String>();
                }
                mappedGroupList.add(mappedGroup);
                groupMappingTable.put(theGroup, mappedGroupList);
            }
        }
    }
    
    public void getMappedGroups(String group, ArrayList<String> result) {
        if (result == null) {
            throw new RuntimeException("result argument cannot be NULL");
        }
        ArrayList<String> mappedGrps = groupMappingTable.get(group);
        if (mappedGrps == null || mappedGrps.isEmpty()) {
            return;
        }
        addUnique(result,mappedGrps);
        //look for transitive closure
        ArrayList<String> result1 = new ArrayList<String>();
        for (String str : mappedGrps) {
            getMappedGroups(group, str,result1);
        }
        addUnique(result, result1);
    }
    
    private void addUnique(ArrayList<String> dest, ArrayList<String> src) {
        for (String str : src) {
            if (!dest.contains(str)) {
                dest.add(str);
            }
        }
    }
    /*
    public void traverse() {
        Iterator<String> it = groupMappingTable.keySet().iterator();
        while(it.hasNext()) {
            String key = it.next();
            System.out.println();
            System.out.print( key + "<<<Is Mapped to>>>");
            ArrayList<String> list = new ArrayList<String>();
            getMappedGroups(key, list);
            if (list != null) {
                for (String str : list) {
                    System.out.print(str + ", ");
                }
            }
            System.out.println();
        }
    }*/
    /**
     * @param args the command line arguments
     
    public static void main(String[] args) {
        // TODO code application logic here
        GroupMapper mapper = new GroupMapper();
        mapper.parse(mappingStr);
        mapper.traverse();
    }*/

    private void getMappedGroups(String group, String str, ArrayList<String> result) {
       
        ArrayList<String> mappedGrps = groupMappingTable.get(str);
        if (mappedGrps == null || mappedGrps.isEmpty()) {
            return;
        }
        if (mappedGrps.contains(group)) {
            throw new RuntimeException("Illegal Mapping: cycle detected with group'" + group);
        }
        addUnique(result,mappedGrps);
        for (String str1 : mappedGrps) {
            getMappedGroups(group, str1,result);
        }
    }

    private void validate(String mappedGroup, String[] mappingGroups) {
        for (String str : mappingGroups) {
            int aIndex = str.indexOf("->");
            String theGroup = null;
            if (aIndex > 0) {
                theGroup = str.substring(0, aIndex);
            } else {
                theGroup = str;        
            }
            if (theGroup.equals(mappedGroup)) {
                throw new RuntimeException("Illegal Mapping: Identity Mapping of group '" + theGroup + "' to '" + theGroup + "'");
            }
        }
    }
}
