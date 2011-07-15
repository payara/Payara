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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.flashlight.datatree;

import java.util.Collection;
import java.util.List;

/**
 * TreeNode maintains all the Runtime Monitoring Data
 * @author Harpreet Singh
 */
public interface TreeNode extends TreeElement {


    public String getCategory ();
    public void setCategory (String category);
    
    public boolean isEnabled ();
    public void setEnabled (boolean enabled);

    public void setDescription (String description);
    public String getDescription ();
    
    // Children utility methods
    public TreeNode addChild (TreeNode newChild);
    public void removeChild(TreeNode oldChild);
    public void setParent (TreeNode parent);
    public TreeNode getParent ();
    
    /**
     * 
     * @return complete dotted name to this node
     */
    public String getCompletePathName ();

    public boolean hasChildNodes ();

    /**
     * 
     * @return Collection<TreeNode> collection of children
     */
    public Collection<TreeNode> getChildNodes ();
    
    /**
     *
     * @return Collection<TreeNode> collection of children
     */
    public Collection<TreeNode> getEnabledChildNodes ();

    /**
     * 
     * @param completeName dotted name to the node
     * @return TreeNode uniquely identified tree node. Null if no matching tree node.
     */
    
    public TreeNode getNode (String completeName);
      
    /**
     * Performs a depth first traversal of the tree. Returns all the nodes in the
     * tree unless ignoreDisabled flag is turned on.
     * @param ignoreDisabled will ignore a disabled node and its children 
     * @return List<TreeNode> lists all nodes under the current sub tree.
     */

    public List<TreeNode> traverse (boolean ignoreDisabled);
    /**
     * 
     * Returns all nodes that match the given Regex pattern as specified by the
     * <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/util/regex/Pattern.html"> Pattern</a>  class.
     * Admin CLI in GlassFish v2 did not use Pattern's specified in java.util.Pattern. It had
     * a simpler mechanism where * was equivalent to .* from {@linkplain java.util.regex.Pattern}
     * If the V2Compatible flag is turned on, then the pattern is considered a v2 pattern.
     * @param pattern Find a node that matches the pattern. By default pattern should follow the conventions
     * outlined by the java.util.regex.Pattern class.
     * @param ignoreDisabled will ignore a disabled node and its children
     * @param gfv2Compatible in this mode, * has the same meaning as <i>.*</i> in the Pattern class.
     * The implementation should consider pattern as a v2 pattern.
     * @return
     */
    public List<TreeNode> getNodes (String pattern, boolean ignoreDisabled, boolean gfv2Compatible);

    /**
     * Behaves as {@link #getNodes (String, boolean, boolean) with ignoreDisabled set to true
     * and gfV2Compatible set to true
     * Pattern is considered to be a GFV2 Compatible Pattern
     */
    public List<TreeNode> getNodes (String pattern);

    public TreeNode getChild(String childName);

    /**
     * Get the "parent" matching the given pattern.
     * E.g "server.jvm.memory.maxheapsize-count" is the parent of
     * "server.jvm.memory.maxheapsize-count-count"
     * Note that in V3 the latter will NOT be found with getNodes()
     *
     * @param pattern Find a node that matches the pattern. By default pattern should follow the conventions
     * outlined by the java.util.regex.Pattern class.
     * @return The parent node if found otherwise null.
     */
    TreeNode getPossibleParentNode(String pattern);
}
