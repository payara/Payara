/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.flashlight.datatree.impl;

import static com.sun.enterprise.util.StringUtils.ok;
import com.sun.enterprise.util.ObjectAnalyzer;
import static com.sun.enterprise.util.SystemPropertyConstants.MONDOT;
import static com.sun.enterprise.util.SystemPropertyConstants.SLASH;
import org.glassfish.flashlight.datatree.TreeNode;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Harpreet Singh
 * @author Byron Nevins
 * 12/18/2010 -- Added encode/decode.  Note that the encoded form for a dot is
 * NOT something like "\\." -- there is too much code around making assumptions
 * about dots, splitting strings, etc.  So we replace with ___MONDOT___
 */
public abstract class AbstractTreeNode implements TreeNode, Comparable<TreeNode> {
    protected Map<String, TreeNode> children =
            new ConcurrentHashMap<String, TreeNode>();
    // bnevins 6/25/2011  -- why is normalizedChildren static ?!?
    private static Map<String, TreeNode> normalizedChildren =
            new ConcurrentHashMap<String, TreeNode>();
    protected String name;    // The node object itself
    protected String category;
    protected String description;
    protected boolean enabled = false;
    private static String NAME_SEPARATOR = ".";
    private static String REGEX = "(?<!\\\\)\\.";
    private TreeNode parent = null;
    // Special character Regex to be converted to .* for v2 compatibility
    private String STAR = "*";

    @Override
    public String toString() {
        return ObjectAnalyzer.toString(this);
    }

    @Override
    public String getName() {

        return decodeName();
    }

    @Override
    public void setName(String aname) {

        if (aname == null)
            throw new RuntimeException("Flashlight-utils: Tree Node needs a"
                    + " non-null name");
        name = encodeNodeName(aname);
    }

    // should be implemented at the sub-class level
    @Override
    public Object getValue() {
        if (enabled) {
            return getChildNodes();
        }
        return null;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public TreeNode addChild(TreeNode newChild) {
        if (newChild == null) {
            return null;
        }
        else if (newChild.getName() == null) {
            // log it and return null
            return null;
        }
        newChild.setParent(this);
        normalizedChildren.put(decodeNameToDots(newChild.getCompletePathName()), newChild);
        return children.put(newChild.getName(), newChild);
    }

    @Override
    public String getCompletePathName() {

        if (getParent() != null) {
            return getParent().getCompletePathName()
                    + this.NAME_SEPARATOR + getName();
        }
        else {
            return getName();
        }
    }

    @Override
    public void setParent(TreeNode parent) {
        this.parent = parent;
    }

    @Override
    public TreeNode getParent() {
        return this.parent;
    }

    /**
     * Returns a mutable view of the children
     * @return
     */
    @Override
    public Collection<TreeNode> getChildNodes() {
        return children.values();
    }

    /**
     * Returns a mutable view of the children
     * @return
     */
    @Override
    public Collection<TreeNode> getEnabledChildNodes() {
        List<TreeNode> childNodes = new ArrayList();
        for (TreeNode child : children.values()) {
            if (child.isEnabled())
                childNodes.add(child);
        }
        return childNodes;
    }

    public Enumeration<TreeNode> getChildNodesImmutable() {

        return ((ConcurrentHashMap) children).elements();
    }

    @Override
    public boolean hasChildNodes() {
        return !children.isEmpty();

    }

    @Override
    public void removeChild(TreeNode oldChild) {
        String child = oldChild.getName();
        if (child != null) {
            children.remove(child);
        }

        // too fragile to hunt for the matching key...
        Iterator<TreeNode> it = normalizedChildren.values().iterator();
        while (it.hasNext()) {
            if (it.next() == oldChild) {
                it.remove();
                break;
            }
        }
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    // Byron Nevins 6/25/11
    // JIRA 15964 -- what happened was that the childName was "xxx.yyy"
    // there was a node that actually matched but it was not found because
    // its name was "xxx\.yyy"
    @Override
    public TreeNode getChild(String childName) {
        if (childName == null)
            return null;

        childName = normalizeDots(childName);
        Set<Map.Entry<String, TreeNode>> entries = children.entrySet();

        for (Map.Entry<String, TreeNode> entry : entries) {
            String entryKey = entry.getKey();
            String normalizedEntryKey = normalizeDots(entryKey);

            if (childName.equals(entryKey) || childName.equals(normalizedEntryKey))
                return entry.getValue();
        }

        return null;
    }

    @Override
    public TreeNode getNode(String completeName) {
        if (completeName == null) {
            return null;
        }
        completeName = encodePath(completeName);
        Pattern pattern = Pattern.compile(AbstractTreeNode.REGEX);
        String[] tokens = pattern.split(completeName);
        TreeNode n = findNodeInTree(tokens);

        if (n == null)
            n = findNodeInTreeNormalized(completeName);

        return n;
    }

    // confused?  That's expected!  This should be refactored/re-done for 3.2
    // we store dots and slashes encoded.  THe tokens coming in to this method
    // are encoded.  That's because there is lots of other code scattered around
    // that looks for these special meta-characters.  To be safe I'm storing them
    // in the node encoded.
    // But the "children" object has keys that come from the getName() of the node
    // which is the value.
    private TreeNode findNodeInTree(String[] tokens) {
        if (tokens == null) {
            return null;
        }
        TreeNode child = getChild(tokens[0]);

        if (child == null)
            child = getChild(decodeName(tokens[0]));

        if (child == null)
            return null;

        if (tokens.length > 1)
            child = ((AbstractTreeNode) child).findNodeInTree(dropFirstStringToken(tokens));

        return child;

    }

    private String[] dropFirstStringToken(String[] token) {
        if (token.length == 0) {
            return null;
        }
        if (token.length == 1) {
            return null;
        }
        String[] newToken = new String[token.length - 1];
        for (int i = 0; i < newToken.length; i++) {
            newToken[i] = token[i + 1];
        }
        return newToken;
    }

    /**
     * Returns all the nodes under the current tree
     * @return List of all nodes in the current tree
     */
    @Override
    public List<TreeNode> traverse(boolean ignoreDisabled) {
//        System.out.println ("Node: " + this.getName ()+ " is enabled "+isEnabled());
        List<TreeNode> list = new ArrayList<TreeNode>();

        if (ignoreDisabled) {
            if (!this.enabled) {
                return list;
            }
        }
        list.add(this);

        if (!hasChildNodes()) {
            return list;
        }

        Collection<TreeNode> childList = children.values();
        for (TreeNode node : childList) {
            list.addAll(node.traverse(ignoreDisabled));
        }
        return list;
    }

    @Override
    public List<TreeNode> getNodes(String pattern, boolean ignoreDisabled, boolean gfv2Compatible) {
        pattern = pattern.replace("\\.", "\\\\\\."); // \.  goes to \\\.

        // bnevins October 2010
        // design gotcha -- It used to be IMPOSSIBLE to tell the difference between
        // a literal slash in a name and a slash used as a delimiter.  Deep down
        // under dozens of calls in the stack -- Strings are concatanated together
        // Simple solution is to replace literal slashes with a token.  The probe
        // provider code needs to do that.  jndi names are an example of this.
        // Here we replace slash in the given pattern with the token to pull out
        // the right stuff.
        // This is a ARCHITECTURE flaw.  This hack can be replaced with an
        // ARCHITECTURAL fix later if desired.

        List<TreeNode> list = getNodesInternal(pattern, ignoreDisabled, gfv2Compatible);

        if (list.size() <= 0)
            list = getNodesInternal(pattern.replace("/", SLASH), ignoreDisabled, gfv2Compatible);

        if (list.size() <= 0)
            list = getNodesInternal(decodeNameToDots(pattern), ignoreDisabled, gfv2Compatible);

        return list;
    }

    private List<TreeNode> getNodesInternal(String pattern, boolean ignoreDisabled, boolean gfv2Compatible) {
        List<TreeNode> regexMatchedTree = new ArrayList<TreeNode>();


        try {
            if (gfv2Compatible)
                pattern = convertGFv2PatternToRegex(pattern);

            Pattern mPattern = Pattern.compile(pattern);
            List<TreeNode> completeTree = traverse(ignoreDisabled);

            for (TreeNode node : completeTree) {
                String path = node.getCompletePathName();
                String path2 = null;

                if (path.indexOf("\\") >= 0)
                    path2 = path.replace("\\", "");

                Matcher matcher = mPattern.matcher(path);

                if (matcher.matches()) {
                    regexMatchedTree.add(node);
                }
                else if (path2 != null) {
                    Matcher matcher2 = mPattern.matcher(path2);
                    if (matcher2.matches()) {
                        regexMatchedTree.add(node);
                    }
                }
            }
        }
        catch (java.util.regex.PatternSyntaxException e) {
            // log this
            // e.printStackTrace ();
        }
        return regexMatchedTree;
    }

    @Override
    public List<TreeNode> getNodes(String pattern) {
        return getNodes(pattern, true, true);
    }

    private String convertGFv2PatternToRegex(String pattern) {
        if (pattern.equals(STAR)) {
            return ".*";
        }
        // Doing this intermediate step as replacing "*" in a pattern with ".*"
        // is too hassling

        String modifiedPattern = pattern.replaceAll("\\*", ":");
        String regex = modifiedPattern.replaceAll(":", ".*");
        return regex;
    }

    @Override
    public int compareTo(TreeNode other) {
        return getName().compareTo(other.getName());
    }

    @Override
    public boolean equals(Object o) {
        if(this == o)
            return true;

        // don't waste time checking o for null
        // if instanceof's first param is null it will return false
        // see JLS, 15.19.2
        if(!(o instanceof AbstractTreeNode))
            return false;

        // guaranteed to not throw an Exception because of above!
        AbstractTreeNode other = (AbstractTreeNode) o;

        // work hard to avoid NPE's !!
        return (name == null ? other.name == null : name.equals(other.name))
                && (category == null ? other.category == null : category.equals(other.category))
                && (description == null ? other.description == null : description.equals(other.description));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 23 * hash + (this.category != null ? this.category.hashCode() : 0);
        hash = 23 * hash + (this.description != null ? this.description.hashCode() : 0);
        return hash;
    }

    @Override
    public TreeNode getPossibleParentNode(String pattern) {
        // simplify by bailing out early if preconditions are not met...
        if (pattern == null || pattern.length() <= 0 || pattern.indexOf('*') >= 0)
            return null;

        TreeNode node = null;
        int longest = 0;

        for (TreeNode n : traverse(true)) {
            String aname = n.getCompletePathName();

            if (aname == null)
                continue;   // defensive pgming

            // JIRA 15500 -- there may be a backslash in the name!

            if (pattern.startsWith(aname) || pattern.startsWith(aname.replace("\\", ""))) {
                int thisLength = aname.length();

                // keep the longest match ONLY!
                if (node == null || thisLength > longest) {
                    node = n;
                    longest = thisLength;
                }
            }
        }

        return node;
    }

    private String encodeNodeName(String nodeName) {
        // The order is important!!
        return nodeName.replace("\\.", MONDOT).replace(".", MONDOT).replace("\\/", SLASH).replace("/", SLASH);
    }

    private String encodePath(String thePath) {
        // REST encodes (1) to (2)
        //  aaa    bbb.x   cccc
        //  aaa.bbb\\x.cccc
        // we want aaa.bbb___MONDOT___x.cccc

        return thePath.replace("\\/", SLASH).replace("\\.", MONDOT);
    }

    // todo replace with \\. ???
    private String decodeName() {
        return decodeName(name);
    }

    private static String decodeName(String s) {
        return s.replace(SLASH, "/").replace(MONDOT, "\\.");
    }

    private static String decodeNameToDots(String s) {
        return s.replace(SLASH, ".").replace(MONDOT, ".").replace("\\/", ".").replace("\\.", ".").replace('/', '.');
    }

    private TreeNode findNodeInTreeNormalized(String desiredName) {
        // this is ONLY called when there is no match using the tools prior to 2/10/11
        // so the performance hit should be reasonable

        if (!ok(desiredName))
            return null;

        desiredName = decodeNameToDots(desiredName);
        TreeNode node = normalizedChildren.get(desiredName);

        // one more try.  GUI sometimes chops off the starting "server".
        if (node == null && !desiredName.startsWith("server.")) {
            node = normalizedChildren.get("server." + desiredName);

            // one final try.  The string might be "instance1.blah" -- we must change that
            // to "instance1.server.blah"
            if (node == null && (desiredName = insertServerString(desiredName)) != null)
                node = normalizedChildren.get(desiredName);
        }

        return node;
    }

    private static String insertServerString(String desiredName) {
        try {
            String[] bits = desiredName.split("\\.");

            if (bits.length >= 2) {

                if (bits[1].equals("server"))
                    return desiredName;

                StringBuilder sb = new StringBuilder(bits[0]);
                sb.append(".server");

                for (int i = 1; i < bits.length; i++) {
                    sb.append('.').append(bits[i]);
                }
                return sb.toString();
            }
        }
        catch (Exception e) {
            // fall through
        }
        return null;
    }

    private static String normalizeDots(String s) {
        // Normalized means a literal dot is "\."  (one 'real' backslash followed by dot
        // in a java string in source code it would be:  "\\."  -- where one of the backslashes isn't 'real'

        if(s == null || !hasDots(s)) // return quick for performance
            return s;

        // to avoid expensive regexp work I do it this way...
        // it changes all occurences of "." with no preceding '\' with "\." and all
        // MONDOTS also.
        // I.e. all dots leave here with a backslash in front of them!

        return s.replace("\\.", MONDOT).replace(".", MONDOT).replace(MONDOT, "\\.");
    }

    private static boolean hasDots(String s) {
        if(s == null)
            return false;
        if(s.indexOf(MONDOT) >= 0)
            return true;
        if(s.indexOf(".") >= 0)
            return true;
        return false;
    }

}
