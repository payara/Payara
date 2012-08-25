/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.admin;

import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Dom;
import org.glassfish.api.admin.config.Named;
import org.glassfish.api.admin.config.ReferenceContainer;

import java.util.*;

import com.sun.enterprise.config.serverbeans.*;

/**
 * Utility class for all V2 style related dotted names commands.
 *
 * User: Jerome Dochez
 * Date: Jul 9, 2008
 * Time: 11:38:50 PM
 */
public class V2DottedNameSupport {

    public Map<Dom, String> getAllDottedNodes(ConfigBeanProxy proxy) {
        return getAllDottedNodes(Dom.unwrap(proxy));
    }

    public Map<Dom, String> getAllDottedNodes(Dom root) {

        Map<Dom, String> result = new HashMap<Dom, String>();
        getAllSubDottedNames(null, root, result);
        return result;
    }

    protected void getAllSubDottedNames(String prefix, Dom parent, Map<Dom, String> result) {

        Set<String> elementNames = parent.getElementNames();

        for (String childName : elementNames) {

            // by default, it's a collection unless I can find the model for it
            // and ensure this is one or not.
            // not finding the model usually means that it was a "*" element therefore
            // a collection.
            boolean collection = true;
            if (parent.model.findIgnoreCase(childName)!=null) {
                // if this is a leaf node, we should really treat it as an attribute.
                if (parent.model.getElement(childName).isLeaf())
                    continue;
                collection = parent.model.getElement(childName).isCollection();

            }

            for (Dom child : parent.nodeElements(childName)) {

                StringBuffer newPrefix = new StringBuffer();
                if (prefix==null) {
                    newPrefix.append(childName);
                } else {
                    newPrefix.append(prefix).append(".").append(childName);
                }

                if (collection) {

                    String name = child.getKey();
                    if (name==null) {
                        name = child.attribute("name");
                    }

                    if (name!=null) {
                        newPrefix.append(".").append(name);
                    }
                    // now traverse the child
                    getAllSubDottedNames(newPrefix.toString(), child, result);
                } else {
                    getAllSubDottedNames(newPrefix.toString(), child, result);

                }
            }
        }
        if (prefix!=null) {
            result.put(parent, prefix);
        }
    }

    public Map<String, String> getNodeAttributes(Dom node, String prefix) {
        Map<String, String> result = new  HashMap<String, String>();
        for (String attrName : node.model.getAttributeNames()) {
            String value = (String) node.model.findIgnoreCase(attrName).get(node, String.class);
            if (value!=null) {
                result.put(attrName, value);
            }
        }
        for (String leafName : node.model.getLeafElementNames()) {
            List values = node.leafElements(leafName);
            Iterator i = values.iterator();
            StringBuffer value = new StringBuffer();
            while (i.hasNext()) {
                String nextValue = (String) i.next();

                if (nextValue!=null) {
                    value.append(nextValue);
                    if(i.hasNext()) {
                        value.append(",");
                    }
                }
            }
            result.put(leafName, value.toString());
        }
        return result;
    }

    public Map<Dom, String> getMatchingNodes(Map<Dom, String> nodes, String pattern) {

        Map<Dom, String> result = new HashMap<Dom, String>();
        for (Map.Entry<Dom, String> entry : nodes.entrySet()) {

            String dottedName = entry.getValue();
            if (matches(dottedName, pattern)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public boolean matches(String dottedName, String pattern) {
        StringTokenizer patternToken = new StringTokenizer(pattern, ".");
        if (patternToken.hasMoreElements()) {
            String token = (String) patternToken.nextElement();
            if (token.startsWith("*")) {
                // let's find the end delimiter...
                if (token.length()>1) {
                    String delim = token.substring(1);
                    if (dottedName.indexOf(delim)!=-1) {
                        // found the delimiter...
                        // we have to be careful, the delimiter can be at the end of the string...
                        String remaining = dottedName.substring(dottedName.indexOf(delim) + delim.length());
                        if (remaining.length()==0) {
                            // no more dotted names, better be done with the pattern
                            return !patternToken.hasMoreElements();
                        } else {
                            remaining = remaining.substring(1);
                        }
                        if (patternToken.hasMoreElements()) {
                            return matches(remaining, pattern.substring(token.length()+1));
                        } else {
                            return true;
                        }
                    } else {
                        return false;
                    }
                } else {
                    if (patternToken.hasMoreElements()) {
                        // now this can be tricky, seems like the get/set can accept something like *.config
                        // which really means *.*.*.config for the pattern matching mechanism.
                        // so instead of jumping one dotted name token, we may need to jump multiple tokens
                        // until we find the next delimiter, let's find this first.
                        String delim = (String) patternToken.nextElement();
                        if (dottedName.lastIndexOf('.') == -1) {
                            // more pattern, but no more dotted names.
                            // unless the pattern is "*", we don't have a match
                            if (delim.equals("*")) {
                                return true;
                            }  else {
                                return false;
                            }
                        }
                        // we are not going to check if the delim is a attribute, it has to be an element name.
                        // we will leave the attribute checking to someone else.
                        if (dottedName.contains("." + delim)) {
                            String remaining = dottedName.substring(dottedName.indexOf("." + delim) + 1);
                            return matches(remaining,
                                    pattern.substring(token.length()+1));
                        } else {
                            return false;
                        }
                    } else {
                        return true;
                    }
                }
            } else {
                String delim;
                if (token.lastIndexOf("*")!=-1) {
                    delim = token.substring(0, token.lastIndexOf("*"));
                } else {
                    delim = token;
                }
                if (matchName(dottedName, delim)) {
                    if (patternToken.hasMoreElements()) {
                        if (dottedName.length()<=delim.length()+1) {
                            if ((pattern.substring(token.length()+1)).equals("*")) {
                                return true;
                            }  else {
                                // end of our name and more pattern to go...
                                return false;
                            }
                        }
                        String remaining = dottedName.substring(delim.length()+1);
                        return matches(remaining, pattern.substring(token.length()+1));
                    } else {
                        if (dottedName.length()>delim.length()) {
                            String remaining = dottedName.substring(delim.length()+1);
                            // if we have more dotted names (with grandchildren elements)
                            // we don't match
                            if (remaining.indexOf('.')!=-1) {
                                return false;
                            } else {
                                return true;
                            }
                        } else {
                           // no more pattern, no more dotted name, this is matching.
                           return true;
                        }
                    }
                }
            }
            return false;
        } else {
            // patter is exhausted, only elements one level down should be returned
            return dottedName.indexOf(".")==-1;
        }
    }

    protected boolean matchName(String a, String b) {
        String nextTokenName = a;
        if (a.indexOf('.')!=-1) {
            nextTokenName = a.substring(0, a.indexOf('.'));
        }
                
        if (nextTokenName.equals(b)) {
            return true;
        }
        if (nextTokenName.replace('_', '-').equals(b.replace('_', '-'))) {
            return true;
        }
        return false;
    }
    
    final static class TreeNode {
        final Dom node;
        final String name;
        final String relativeName;
        public TreeNode(Dom node, String name, String relativeName) {
            this.node = node;
            this.name = name;
            this.relativeName = relativeName;
        }
    }

    public TreeNode[] getAliasedParent(Domain domain, String prefix) {

        // let's get the potential aliased element name
        String name;
        String newPrefix;
        if (prefix.indexOf('.') != -1) {
            name = prefix.substring(0, prefix.indexOf('.'));
            newPrefix = prefix.substring(name.length()+1);
        } else {
            name = prefix;
            newPrefix="";
        }

        // check for resources
        if (newPrefix.startsWith("resources")) {
            String relativeName = newPrefix;
            if (newPrefix.indexOf('.') != -1) {
                String str = newPrefix.substring(0, newPrefix.indexOf('.'));
                relativeName = newPrefix.substring(str.length() + 1);
                name += "." + str;
            }
            TreeNode [] result = new TreeNode[1];
            result[0] = new TreeNode(Dom.unwrap(domain.getResources()), name, relativeName);
            return result;
        }

        // server-config
         for (Config config : domain.getConfigs().getConfig()) {
             if (config.getName().equals(name)) {
                 return new TreeNode[] {
                        new  TreeNode(Dom.unwrap(config), name, newPrefix)
                 };
             }
         }

        // this is getting a bit more complicated, as the name can be the server or cluster name
        // yet, the aliasing should return both the server-config

        // server                                `
        Named[] nodes = getNamedNodes(domain.getServers().getServer(),
                domain.getConfigs().getConfig(), name);

        if (nodes==null && domain.getClusters()!=null) {
            // no luck with server, try cluster.
            nodes = getNamedNodes(domain.getClusters().getCluster(),
                    domain.getConfigs().getConfig(), name);
        }
        if (nodes!=null) {
            TreeNode[] result = new TreeNode[nodes.length];
            for (int i=0;i<nodes.length;i++) {
                result[i] = new TreeNode(Dom.unwrap((ConfigBeanProxy) nodes[i]), name, newPrefix);
            }
            return result;
        }

        return new TreeNode[] {
            //new TreeNode(Dom.unwrap(domain), name, newPrefix)
            new TreeNode(Dom.unwrap(domain), "", prefix)
        };
    }

    public Named[] getNamedNodes(List<? extends Named> target, List<? extends Named>references,  String name) {
        for (Named config : target) {
            if (config.getName().equals(name)) {
                if (config instanceof ReferenceContainer) {
                    for (Named reference : references) {
                        if (reference.getName().equals(((ReferenceContainer) config).getReference())) {
                            return new Named[] {
                                    config, reference
                            };
                        }
                    }
                } else {
                    return new Named[] { config };
                }
            }
        }
        return null;
    }

    public List<Map.Entry> applyOverrideRules(List<Map.Entry> nodes) {
        HashMap<String, Map.Entry> store = new HashMap<String, Map.Entry>();
        for (int i=0; i<nodes.size(); i++) {
            Map.Entry<Dom, String> currentNode = nodes.get(i);
            Map.Entry<Dom, String> storedNode = store.get(currentNode.getValue());
            if(storedNode == null) {
                store.put(currentNode.getValue(), currentNode);
                continue;
            }
            int storedNodePrecedenceLevel = getPrecedenceLevel(storedNode.getKey());
            int currNodePrecedenceLevel = getPrecedenceLevel(currentNode.getKey());
            if(storedNodePrecedenceLevel < currNodePrecedenceLevel)
                store.put(currentNode.getValue(), currentNode);
        }
        List<Map.Entry> finalList = new ArrayList<Map.Entry>();
        finalList.addAll(store.values());
        store.clear();
        return finalList;
    }

    private int getPrecedenceLevel(Dom entry) {
        String parent = entry.parent().getImplementation();
        int level = 4;
        if(Config.class.getCanonicalName().equals(parent))
            level = 1;
        if(Cluster.class.getCanonicalName().equals(parent))
            level = 2;
        if(Server.class.getCanonicalName().equals(parent))
            level = 3;
        return level;
    }

    public List<Map.Entry> sortNodesByDottedName(Map<Dom, String> nodes) {
        List<Map.Entry> mapEntries = new ArrayList(nodes.entrySet());
        Collections.sort(mapEntries, new Comparator() {
            public int compare(Object o1, Object o2) {
                return (((String)(((Map.Entry) (o1)).getValue())).compareTo((String)(((Map.Entry) (o2)).getValue())));
            }
        });
        return mapEntries;
    }
    
    public List<org.glassfish.flashlight.datatree.TreeNode> sortTreeNodesByCompletePathName(List<org.glassfish.flashlight.datatree.TreeNode> nodes) {
        Collections.sort(nodes, new Comparator() {
            public int compare(Object o1, Object o2) {
                return (((String)(((org.glassfish.flashlight.datatree.TreeNode) (o1)).getCompletePathName())).compareTo(
                        (String)(((org.glassfish.flashlight.datatree.TreeNode) (o2)).getCompletePathName())));
            }
        });
        return nodes;
    }
}
