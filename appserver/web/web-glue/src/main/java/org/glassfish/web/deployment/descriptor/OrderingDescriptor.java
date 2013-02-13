/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.deployment.descriptor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.deployment.common.Descriptor;

/**
 * This represents the ordering resided in web-fragment.xml.
 *
 * @author Shing Wai Chan
 */

public class OrderingDescriptor extends Descriptor {
    private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(OrderingDescriptor.class);

    OrderingOrderingDescriptor after = null;
    
    OrderingOrderingDescriptor before = null;

    public OrderingOrderingDescriptor getAfter() {
        return after;
    }

    public void setAfter(OrderingOrderingDescriptor after) {
        this.after = after;
        validate();
    }

    public OrderingOrderingDescriptor getBefore() {
        return before;
    }

    public void setBefore(OrderingOrderingDescriptor before) {
        this.before = before;
        validate();
    }

    public void validate() {
        boolean valid = true;
        if (after != null && before != null) {
            if (after.containsOthers() && before.containsOthers()) {
                valid = false;
            }
            if (valid) {
                for (String name : after.getNames()) {
                    if (before.containsName(name)) {
                        valid = false;
                        break;
                    }
                }
            }
        }

        if (!valid) {
            throw new IllegalStateException(localStrings.getLocalString(
                    "web.deployment.exceptioninvalidordering",
                    "The ordering is not valid as it contains the same name and/or others in both before and after."));
        }

    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (after != null) {
            builder.append("after: " + after + ", ");
        }
        if (before != null) {
            builder.append("before: " + before);
        }
        return builder.toString();
    }


    // ----- sorting logic

    public static void sort(List<WebFragmentDescriptor> wfs) {
        if (wfs == null || wfs.size() <= 1) {
            return;
        }

        // build the graph
        List<Node> graph = new ArrayList<Node>();
        Map<String, Node> name2NodeMap = new HashMap<String, Node>();

        // build the nodes
        Node othersNode = new Node(null);
        for (WebFragmentDescriptor wf : wfs) {
            Node wfNode = new Node(wf);
            String wfName = wf.getName();
            if (wfName != null && wfName.length() > 0) {
                name2NodeMap.put(wfName, wfNode);
            }
            graph.add(wfNode);
        }

        List<Node> remaining = new ArrayList<Node>(graph);

        // build the edges
        // othersNode is not in the loop
        Map<Node, Map<Node, Edge>> map = new HashMap<Node, Map<Node, Edge>>();
        for (int i = 0; i < graph.size(); i++) {
            Node wfNode = graph.get(i);
            WebFragmentDescriptor wf = wfNode.getWebFragmentDescriptor();
            String wfName = wf.getName();
            OrderingDescriptor od = wf.getOrderingDescriptor();
            if (od != null) {
                OrderingOrderingDescriptor after = od.getAfter();
                if (after != null) {
                    if (after.containsOthers()) {
                        // othersNode --> wfNode
                        wfNode.getInEdges().add(getEdge(othersNode, wfNode, map));
                        othersNode.getOutEdges().add(getEdge(othersNode, wfNode, map));
                        remaining.remove(othersNode);
                    }
                    for (String name : after.getNames()) {
                        // nameNode --> wfNode
                        Node nameNode = name2NodeMap.get(name);
                        if (nameNode != null) {
                            wfNode.getInEdges().add(
                                    getEdge(nameNode, wfNode, map));
                            nameNode.getOutEdges().add(
                                    getEdge(nameNode, wfNode, map));
                            remaining.remove(nameNode);
                        }
                    }
                }

                OrderingOrderingDescriptor before = od.getBefore();
                if (before != null) {
                    if (before.containsOthers()) {
                        // wfNode --> othersNode
                        wfNode.getOutEdges().add(getEdge(wfNode, othersNode, map));
                        othersNode.getInEdges().add(getEdge(wfNode, othersNode, map));
                        remaining.remove(othersNode);
                    }
                    for (String name : before.getNames()) {
                        // wfNode --> nameNode
                        Node nameNode = name2NodeMap.get(name);
                        if (nameNode != null) {
                            wfNode.getOutEdges().add(
                                    getEdge(wfNode, nameNode, map));
                            nameNode.getInEdges().add(
                                    getEdge(wfNode, nameNode, map));
                            remaining.remove(nameNode);
                        }
                    }
                }

                boolean hasAfterOrdering = (after != null &&
                        (after.containsOthers() || after.getNames().size() > 0));
                boolean hasBeforeOrdering = (before != null &&
                        (before.containsOthers() || before.getNames().size() > 0));
                if (hasAfterOrdering || hasBeforeOrdering) {
                    remaining.remove(wfNode);
                }
            }
        }

        // if others should be in the graph
        if (othersNode.getOutEdges().size() > 0 || othersNode.getInEdges().size() > 0) {
            // add others to the end
            graph.add(othersNode);

            // populate others info into nodes
            Stack<Node> afterOthersStack = new Stack<Node>();
            for (Edge edge : othersNode.getOutEdges()) {
                edge.setVisited(true);
                afterOthersStack.push(edge.getToNode());
            }
            while (!afterOthersStack.isEmpty()) {
                Node aNode = afterOthersStack.pop();
                aNode.setAfterOthers(true);
                for (Edge edge : aNode.getOutEdges()) {
                    if (!edge.isVisited()) {
                        afterOthersStack.push(edge.getToNode());
                        edge.setVisited(true);
                    }
                }
            }

            Stack<Node> beforeOthersStack = new Stack<Node>();
            for (Edge edge : othersNode.getInEdges()) {
                edge.setVisited(true);
                beforeOthersStack.push(edge.getFromNode());
            }
            while (!beforeOthersStack.isEmpty()) {
                Node aNode = beforeOthersStack.pop();
                aNode.setBeforeOthers(true);
                for (Edge edge : aNode.getInEdges()) {
                    if (!edge.isVisited()) {
                        beforeOthersStack.push(edge.getFromNode());
                        edge.setVisited(true);
                    }
                }
            }
        }

        List<Node> subgraph = new ArrayList<Node>(graph);
        subgraph.removeAll(remaining);
        boolean hasRemaining = (remaining.size() > 0);

        List<Node> sortedNodes = topologicalSort(subgraph, hasRemaining);
        wfs.clear();
        boolean othersProcessed = false;
        for (Node node: sortedNodes) {
            if (node.isOthers()) {
                // others
                othersProcessed = true;
                for (Node rnode: remaining) {
                    wfs.add(rnode.getWebFragmentDescriptor());
                }
            } else {
                wfs.add(node.getWebFragmentDescriptor());
            }
        }

        if (!othersProcessed) {
            for (Node rnode: remaining) {
                wfs.add(rnode.getWebFragmentDescriptor());
            }
        }
    }

    private static Edge getEdge(Node from, Node to, Map<Node, Map<Node, Edge>> map) {
        Edge edge = null;
        Map<Node, Edge> node2Edge = map.get(from);
        if (node2Edge == null) {
            node2Edge = new HashMap<Node, Edge>();
            map.put(from, node2Edge);
        } else {
            edge = node2Edge.get(to);
        }
        if (edge == null) {
            edge = new Edge(from, to);
            node2Edge.put(to, edge);
        }
        return edge;
    }

    /**
     * Note that this processing will modify the graph.
     * It is not intended for public.
     * @param graph
     * @param hasRemaining
     * @return a sorted list of Node
     */
    private static List<Node> topologicalSort(List<Node> graph, boolean hasRemaining) {
        List<Node> sortedNodes = new ArrayList<Node>();

        if (graph.size() == 0) {
            return sortedNodes;
        }

        Stack<Node> roots = new Stack<Node>();
        Stack<Node> rootsBefore = new Stack<Node>();
        Stack<Node> rootsAfter = new Stack<Node>(); // including others if any
        // find nodes without incoming edges
        for (Node node: graph) {
            if (node.getInEdges().size() == 0) {
                if (node.isBeforeOthers()) {
                    rootsBefore.add(node);
                } else if (node.isAfterOthers() || node.isOthers()) {
                    rootsAfter.add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        
        if (roots.empty() && rootsBefore.empty() && rootsAfter.empty()) {
            // check if it is a circle with others and empty remaining
            if (isCircleWithOthersAndNoRemaining(graph, hasRemaining, sortedNodes)) {
                return sortedNodes;
            } else {
                throw new IllegalStateException(localStrings.getLocalString(
                        "web.deployment.exceptioninvalidwebfragmentordering",
                        "The web fragment ordering is not valid and possibly has cycling conflicts."));
            }
        }

        while (!(roots.empty() && rootsBefore.empty() && rootsAfter.empty())) {
            Node node = null;
            if (!rootsBefore.empty()) {
                node = rootsBefore.pop();
            } else if (!roots.empty()) {
                node = roots.pop();
            } else { // !rootsAfter.empty()
                node = rootsAfter.pop();
            } 

            sortedNodes.add(node);
            // for each outcoming edges
            Iterator<Edge> outEdgesIter = node.getOutEdges().iterator();
            while (outEdgesIter.hasNext()) {
                Edge outEdge = outEdgesIter.next();
                Node outNode = outEdge.getToNode();
                // remove the outcoming edge
                outEdgesIter.remove();
                // remove corresponding incoming edge from the outNode
                outNode.getInEdges().remove(outEdge);

                // if no incoming edge
                if (outNode.getInEdges().size() == 0) {
                    // the others node
                    if (node.isBeforeOthers()) {
                        rootsBefore.add(outNode);
                    } else if (node.isAfterOthers() || node.isOthers()) {
                        rootsAfter.add(outNode);
                    } else {
                        roots.add(outNode);
                    }
                }
            }
        }

        boolean hasEdges = false;
        for (Node node: graph) {
            if (node.getInEdges().size() > 0 || node.getOutEdges().size() > 0) {
                hasEdges = true;
                break;
            }
        }
        if (hasEdges) {
            throw new IllegalStateException(localStrings.getLocalString(
                    "web.deployment.exceptioninvalidwebfragmentordering",
                    "The web fragment ordering is not valid and possibly has cycling conflicts."));
        }
        return sortedNodes;
    }

    /**
     * This method will check whether the graph does not have remaining vertices
     * and is a circle with others. It return the sorted result in sortedNodes.
     * @param graph  the input graph
     * @param hasRemaining
     * @param sortedNodes  output sorted result if it is a circle with empty others
     * @return boolean indicating whether it is a circle with an empty others
     */
    private static boolean isCircleWithOthersAndNoRemaining(List<Node> graph,
            boolean hasRemaining, List<Node>sortedNodes) {

        boolean circleWithOthersAndNoRemaining = false;
        int size = graph.size();
        if (size == 0 || hasRemaining) {
            return circleWithOthersAndNoRemaining;
        }

        Node nextNode = graph.get(size - 1);
        if (nextNode.isOthers()) {
            Set<Node> set = new LinkedHashSet<Node>();
            int count = 0;
            while ((count < size) &&
                    nextNode.getOutEdges().size() == 1 &&
                    nextNode.getInEdges().size() == 1) {

                if (!set.add(nextNode)) {
                    break;
                }
                nextNode = nextNode.getOutEdges().iterator().next().getToNode();
                count++;
            }

            if (set.size() == size) {
                circleWithOthersAndNoRemaining = true;
                Iterator<Node> iter = set.iterator();
                // exclude others
                if (iter.hasNext()) {
                    iter.next();
                }
                while (iter.hasNext()) {
                   sortedNodes.add(iter.next());
                }
            }
        }

        return circleWithOthersAndNoRemaining;
    }

    // for debug
    private static void print(WebFragmentDescriptor wf,
            String nullWfString, StringBuilder sb) {

        String wfName = null;
        if (wf != null) {
            wfName = wf.getName();
        } else {
            wfName = nullWfString;
        }
        sb.append(wfName);
    }

    private static class Node {
        private WebFragmentDescriptor webFragmentDescriptor = null;
        private Set<Edge> inEdges = new LinkedHashSet<Edge>();
        private Set<Edge> outEdges = new LinkedHashSet<Edge>();
        // for sorting
        private boolean afterOthers = false;
        private boolean beforeOthers = false;

        private Node(WebFragmentDescriptor wf) {
            webFragmentDescriptor = wf;
        }

        private WebFragmentDescriptor getWebFragmentDescriptor() {
            return webFragmentDescriptor;
        }

        private Set<Edge> getInEdges() {
            return inEdges;
        }

        private Set<Edge> getOutEdges() {
            return outEdges;
        }

        private boolean isOthers() {
            return (webFragmentDescriptor == null);
        }

        private boolean isAfterOthers() {
            return afterOthers;
        }

        private void setAfterOthers(boolean afterOthers) {
            this.afterOthers = afterOthers;
        }

        private boolean isBeforeOthers() {
            return beforeOthers;
        }

        private void setBeforeOthers(boolean beforeOthers) {
            this.beforeOthers = beforeOthers;
        }

        // for debug
        public String toString() {
            StringBuilder sb = new StringBuilder("{name=");
            print(webFragmentDescriptor, "@others", sb);

            sb.append(", inNodes=[");
            boolean first = true;
            for (Edge e: inEdges) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                print(e.getFromNode().getWebFragmentDescriptor(), "@others", sb);
            }
            sb.append("]");

            sb.append(", outNodes=[");
            first = true;
            for (Edge e: outEdges) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                print(e.getToNode().getWebFragmentDescriptor(), "@others", sb);
            }
            sb.append("]}");
            return sb.toString();
        }
    }

    // an edge with data
    private static final class Edge {
        private Node fromNode = null;
        private Node toNode = null;
        private boolean visited = false;

        private Edge(Node from, Node to) {
            fromNode = from;
            toNode = to;
        }

        private Node getFromNode() {
            return fromNode;
        }

        private Node getToNode() {
            return toNode;
        }

        private boolean isVisited() {
            return visited;
        }

        private void setVisited(boolean vt) {
            visited = vt;
        }
    }
}
