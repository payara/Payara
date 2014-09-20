/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.jvnet.hk2.annotations.Contract;

/**
 * Contract for all BundleNode implementations.
 * 
 * @author Tim Quinn
 */
@Contract
public interface BundleNode {
    
    /**
     * Registers the standard bundle node in the map.
     * <p>
     * The implementation class must add to the map an entry with the key
     * equal to the public ID of the DTD and the value the system ID. 
     * 
     * @param publicIDToSystemIDMapping map prepared by the caller
     * @param versionUpgrades The list of upgrades from older versions
     * @return top-level element name for the standard descriptor
     */
  String registerBundle(final Map<String,String> publicIDToSystemIDMapping);
    
    /**
     * Registers all appropriate runtime bundle nodes for this standard node
     * into the map.
     * <p>
     * The implementation class must add to the map one entry for each associated
     * runtime descriptor node, with the entry key equal to the public ID of the 
     * runtime DTD and the value the system ID of the runtime DTD.  The 
     * implementation must also return a map containing one entry for each
     * associated runtime node, with the entry key equal to the top-level 
     * element name for the runtime descriptor and the entry value equal to the
     * class of the runtime node.
     * 
     * @param publicIDToSystemIDMapping
     * @param versionUpgrades The list of upgrades from older versions
     * to the latest schema
     * @return map from top-level runtime descriptor element name to the corresponding runtime node class
     */
    Map<String,Class> registerRuntimeBundle(final Map<String,String> publicIDToSystemIDMapping, final Map<String, List<Class>> versionUpgrades);
    
    /**
     * Returns the element names related to the standard or related runtime nodes 
     * for which the parser should allow empty values.
     */
    Collection<String> elementsAllowingEmptyValue();
    
    /**
     * Returns the element names related to the standard or related runtime nodes 
     * for which the parser should preserve whitespace.
     */
    Collection<String> elementsPreservingWhiteSpace();
}
