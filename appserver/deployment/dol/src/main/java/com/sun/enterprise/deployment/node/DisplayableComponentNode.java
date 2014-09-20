/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.node;


import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.deployment.xml.TagNames;
import org.w3c.dom.Node;


/**
 * This node class is responsible for handling all the information 
 * related to displayable elements like display-name or icons.
 * 
 * @author  Jerome Dochez
 * @version 
 */
public abstract class DisplayableComponentNode<T extends Descriptor> extends DeploymentDescriptorNode<T> {

    public DisplayableComponentNode() {
        super();
        registerElementHandler(new XMLElement(TagNames.NAME), LocalizedInfoNode.class);       
        registerElementHandler(new XMLElement(TagNames.ICON), IconNode.class);           
        registerElementHandler(new XMLElement(TagNames.SMALL_ICON), IconNode.class);           
        registerElementHandler(new XMLElement(TagNames.LARGE_ICON), IconNode.class);           
    }
    
    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param the descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node parent, T descriptor) {
        Node node = super.writeDescriptor(parent, descriptor);        
        
        // description, display-name, icons...
        writeDisplayableComponentInfo(node, descriptor);
        return node;
    } 
    
    /**
     * write the localized descriptions, display-names and icons info
     *
     * @param the node to write the info to
     * @param the descriptor containing the displayable information
     */
    public void writeDisplayableComponentInfo(Node node, T descriptor) {
        LocalizedNode localizedNode = new LocalizedNode();
        localizedNode.writeLocalizedMap(node, TagNames.DESCRIPTION, descriptor.getLocalizedDescriptions());
        localizedNode.writeLocalizedMap(node, TagNames.NAME, descriptor.getLocalizedDisplayNames());
        IconNode iconNode = new IconNode();
        iconNode.writeLocalizedInfo(node, descriptor);
        
    }       
}
