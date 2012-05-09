/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admingui.plugins.sample;

import java.util.ArrayList;
import java.util.List;
import org.glassfish.admingui.plugins.NavigationNode;
import static org.glassfish.admingui.plugins.NavigationNode.*;
import org.glassfish.admingui.plugins.annotations.ConsolePlugin;
import org.glassfish.admingui.plugins.annotations.NavNodes;
import org.glassfish.admingui.plugins.annotations.ViewFragment;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Singleton;

/**
 * Only one of these is needed, strictly speaking, per plugin module. You can
 * use several if you want the separation of data, but it's not necessary.
 * @author jasonlee
 */
@Service
@Scoped(Singleton.class)
public class SamplePlugin implements ConsolePlugin {

    public int priority = 275;
    @ViewFragment(type = "tab")
    public static final String TAB = "/sample/tab.xhtml";
    
    @NavNodes(parent="root")
    public static final List<NavigationNode> navNodes = new ArrayList<NavigationNode>() {{
       add (createNode("fieldNode1",
               "Nodes from a field Test 1",
               "/sample/icons/family-tree.jpg",
               "/sample/page1.xhtml",
               new ArrayList<NavigationNode>() {{  add (new NavigationNode("fieldNode2", "Field Test 1-1")); }}
           )); 
    }};

    @NavNodes(parent = "root")
    public static List<NavigationNode> getNavNodes() {
        List<NavigationNode> nodes = new ArrayList<NavigationNode>();

        nodes.add(new NavigationNode("methodNode1", "Method Test 1"));
        nodes.add(new NavigationNode("methodNode2", "Method Test 2"));
        nodes.add(createNode("nestedNode6", "Nested Test 1", null, null, new ArrayList<NavigationNode>() {{
            add(new NavigationNode("nestedNode7", "Nested Test 1-1"));
            add(new NavigationNode("nestedNode8", "Nested Test 1-2"));
        }}));

        nodes.add(createNode("navTest", "Form Nav Test", null, "/sample/navTest.xhtml", null));

        return nodes;
    }
}