/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployment.common;

import java.util.*;

/**
 * A class that can answer the following queries WITHOUT loading any classes
 *
 * Given a class C
 * 1. Find out all sub classes of C
 *
 * 2. Find out all classes that implemetns OR extends C
 *
 * 3. Find out all classes that are annotated with C
 *
 *
 * Usage:
 *
 * ClassDependencyBuilder cdb = new GraphBuilder();
 *      cdb.loadClassData(c1);
 *      cdb.loadClassData(c2);
 *      cdb.loadClassData(c3);
 *      ...
 *      ...
 *
 *      cdb.computeResult(c); // c can be any fully qualified class name (internal format or java format)
 * 
 * @author Mahesh Kannan
 */
public class ClassDependencyBuilder {

    private Map<String, NodeInfo> mappings = new HashMap<String, NodeInfo>();

    public ClassDependencyBuilder() {
    }

    public Set<String> computeResult(String name) {
        name = name.replace('.', '/');
        NodeInfo ni = mappings.get(name);
        if (ni != null) {
            if (ni.isAnnotation()) {
                return getAnnotatedClasses(ni);
            } else if (ni.isClass() || ni.isInterface()) {
                return getSubClasses(ni, ni.isInterface());
            } else {
                //System.out.println("????");
            }
        } else {
            //System.out.println("No info about: " + name);
        }

        return new HashSet<String>();
    }

    private Set<String> getAnnotatedClasses(NodeInfo ni) {
        Set<String> annotatedClasses = new HashSet<String>();
        if (ni != null) {
            Set<NodeInfo> nInfos = ni.getDirectImplementors();
            if ((nInfos != null) && (nInfos.size() > 0)) {
                for (NodeInfo n : nInfos) {
                    annotatedClasses.add(n.getClassName());
                }
            }
        }

        return annotatedClasses;
    }

    private Set<String> getSubClasses(NodeInfo node, boolean addImplementors) {
        Set<String> assignables = new HashSet<String>();
        if (node != null) {
            List<NodeInfo> list = new LinkedList<NodeInfo>();
            while (true) {
                Set<NodeInfo> set = node.getDirectSubClass();
                if ((set != null) && (set.size() > 0)) {
                    for (NodeInfo n : set) {
                        list.add(n);
                    }
                }

                if (addImplementors) {
                    Set<NodeInfo> intfs = node.getDirectImplementors();
                    if ((intfs != null) && (intfs.size() > 0)) {
                        for (NodeInfo n : intfs) {
                            list.add(n);
                        }
                    }
                }

                if (list.size() > 0) {
                    node = list.remove(0);
                    String name = node.getClassName();
                    if (!assignables.contains(name)) {
                        assignables.add(name);
                    }
                } else {
                    break;
                }
            }
        }
        return assignables;
    }

    public void loadClassData(byte[] classData)
            throws Exception {

        NodeInfo node = new NodeInfo(classData);
        String cname = node.getClassName();
        NodeInfo nodeInfo = mappings.get(cname);
        if ((nodeInfo == null) || (!nodeInfo.isParsed())) {
            if (nodeInfo == null) {
                mappings.put(cname, node);
                nodeInfo = node;
            } else {
                nodeInfo.load(classData);
            }
            populateMapping(nodeInfo);
        }
    }

    public int size() {
        return mappings.size();
    }

    private void populateMapping(NodeInfo nodeInfo) {
        String superName = nodeInfo.getSuperClassName();
        if (superName != null) {
            NodeInfo superNodeInfo = mappings.get(superName);
            if (superNodeInfo == null) {
                superNodeInfo = new NodeInfo(superName);
                if (nodeInfo.isClass()) {
                    superNodeInfo.markAsClassType();
                } else {
                    superNodeInfo.markAsInterfaceType();
                }
                mappings.put(superName, superNodeInfo);
            }
            superNodeInfo.addDirectSubClass(nodeInfo);
        }

        String[] interfaces = nodeInfo.getInterfaces();
        if ((interfaces != null) && (interfaces.length > 0)) {
            for (String interf : interfaces) {
                NodeInfo interfNodeInfo = mappings.get(interf);
                if (interfNodeInfo == null) {
                    interfNodeInfo = new NodeInfo(interf);
                    interfNodeInfo.markAsInterfaceType();
                    mappings.put(interf, interfNodeInfo);
                }
                interfNodeInfo.addDirectImplementor(nodeInfo);
            }
        }

        List<String> anns = nodeInfo.getClassLevelAnnotations();
        if ((anns != null) && (anns.size() > 0)) {
            for (String ann : anns) {
                NodeInfo annNode = mappings.get(ann);
                if (annNode == null) {
                    annNode = new NodeInfo(ann);
                    annNode.markAsAnnotaionType();
                    mappings.put(ann, annNode);
                }
                annNode.addDirectImplementor(nodeInfo);
            }
        }

    }

    void printInfo() {
        for (NodeInfo node : mappings.values()) {
            System.out.println(node.toString());
        }
    }


}
