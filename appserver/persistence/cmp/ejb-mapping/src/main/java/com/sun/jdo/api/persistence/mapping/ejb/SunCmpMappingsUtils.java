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

package com.sun.jdo.api.persistence.mapping.ejb;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.netbeans.modules.schema2beans.BaseBean;
import org.netbeans.modules.schema2beans.GraphManager;
import org.netbeans.modules.schema2beans.BaseProperty;

import com.sun.jdo.api.persistence.mapping.ejb.beans.*;


/** Finder utilities for elements of the SunCmpMappings
 * object graph.
 * @author vkraemer
 */
public class SunCmpMappingsUtils {
    private static ResourceBundle bundle =
        ResourceBundle.getBundle("com.sun.jdo.api.persistence.mapping.ejb.Bundle"); //NOI18N

    
    /** Creates a new instance of SunCmpMappingsUtils */
    private SunCmpMappingsUtils() {
    }
    
    /** Return the first SunCmpMapping element from the graph.
     * @param scms The root of a SunCmpMappings object graph
     * @param addEmpty flag to add an empty version of the SunCmpMapping element to the graph if one is
     * not found.
     * @return The first SunCmpMapping element in the graph or null if it doesn't exists and
     * addEmpty is false.
     */    
    public static SunCmpMapping getFirstSunCmpMapping(SunCmpMappings scms, boolean addEmpty) {
        SunCmpMapping retVal = null;
        if (0 < scms.sizeSunCmpMapping())
            retVal =  scms.getSunCmpMapping(0);
        if ((null == retVal) && addEmpty) {
            retVal = new SunCmpMapping();
            scms.addSunCmpMapping(retVal);
        }
        return retVal;
    }
        
    
    /** Find the EntityMapping element that correspond to a bean.
     * @return the EntityMapping element, or null if addEmpty is false and the EntityMapping is not
     * found.
     * @param scms the root of the SunCmpMappings graph
     * @param bname The value of the ejb-name element for a bean in the graph
     * @param addEmpty flag to add an empty version of the EntityMapping element to the graph if one is
     * not found.
     * @throws IllegalArgumentException if more than one EntityMapping element has an ejb-name element with the matching
     * value
     */    
    public static EntityMapping findEntityMapping(SunCmpMappings scms, String bname, boolean addEmpty) throws IllegalArgumentException {
        EntityMapping retVal = (EntityMapping) findSingleCompatibleBean(
                "scms", scms, "bname", bname, "ejb-name", EntityMapping.class); // NOI18N
        if ((null == retVal) && addEmpty) {
            retVal = new EntityMapping();
            retVal.setEjbName(bname);
            SunCmpMapping scm = getFirstSunCmpMapping(scms, addEmpty);
            scm.addEntityMapping(retVal);
        }
        return retVal;
    }
    
    /** Find the cmr-field-mapping element for a field in an EntityMapping object
     * @return the CmrFieldMapping element or null if addEmpty is false and the field is not
     * found.
     * @param em The root of the search
     * @param fname the name of the field
     * @param addEmpty flag to add an empty version of the CmrFieldMapping element to the graph if one is
     * not found.
     * @throws IllegalArgumentException If there is more than one cmr-field with a matching cmr-field-name element in
     * the EntityMapping graph.
     */    
    public static CmrFieldMapping findCmrFieldMapping(EntityMapping em, String fname, boolean addEmpty) throws IllegalArgumentException {
        CmrFieldMapping retVal = (CmrFieldMapping) findSingleCompatibleBean(
                "em", em, "fname", fname, "cmr-field-name", CmrFieldMapping.class); // NOI18N
        if ((null == retVal) && addEmpty) {
            retVal = new CmrFieldMapping();
            retVal.setCmrFieldName(fname);
            em.addCmrFieldMapping(retVal);
        }
        return retVal;
    }
    
    /** Find the CmpFieldMapping element with a matching field-name element value
     * @return The CmpFieldMapping element or null if addEmpty is false and the field is not
     * found.
     * @param em the root of the search
     * @param fname the value of the field-name element
     * @param addEmpty flag to add an empty version of the CmpFieldMapping element to the graph if one is
     * not found.
     * @throws IllegalArgumentException If there is more than one cmp-field with a matching field-name element in
     * the EntityMapping graph.
     */    
    public static CmpFieldMapping findCmpFieldMapping(EntityMapping em, String fname, boolean addEmpty) throws IllegalArgumentException {
        CmpFieldMapping retVal = (CmpFieldMapping) findSingleCompatibleBean(
                "em", em, "fname", fname, "field-name", CmpFieldMapping.class); // NOI18N
        if ((null == retVal) && addEmpty) {
            retVal = new CmpFieldMapping();
            retVal.setFieldName(fname);
            em.addCmpFieldMapping(retVal);
        }
        return retVal;
    }
    
    /** helper method */
    private static BaseBean findSingleCompatibleBean(String argOneName, BaseBean argOne, String argTwoName,
            String argTwo, String propName, Class type) {
        BaseBean retVal = null;
        if (null == argTwo || argTwo.length() < 1)
            throw new IllegalArgumentException(argTwoName);
        
        List l = findCompatibleBeansWithValue(argOne, propName, argTwo, 
            type);
        if (null != l) {
            if (l.size() == 1)
                retVal = (BaseBean) l.get(0);
            else if (l.size() > 1)
                throw new IllegalArgumentException(argOneName);
        }
        return retVal;
    }

    /** A utility for finding beans in a graph of BaseBean objects.
     *
     * Search the bean graph, starting at a given root, for beans where the named
     * property has the given value.  The returned list is filtered by assignment
     * compatibility.
     * @return The assignment compatible BaseBeans that were found.
     * @param root The root of a search
     * @param propName The name of the element
     * @param propVal the value of the element
     * @param type The expected type of the value to be returned.
     * @throws IllegalArgumentException If the bean is not part of a complete bean graph.
     */    
    protected static List findCompatibleBeansWithValue(BaseBean root, String propName, String propVal, Class type) throws IllegalArgumentException {
        List retVal = null; 
        GraphManager gm = root.graphManager();
        if (null == gm) 
            throw new IllegalArgumentException(
                    bundle.getString("ERR_DISCONNECTED_NOT_SUPPORTED"));
        String[] props = root.findPropertyValue(propName, propVal);
        int len = 0;
        if (null != props)
            len = props.length;
        if (len > 0)
            retVal = new ArrayList(); 
        for (int i = 0; i < len; i++) {
            // get the bean that is the property's parent.
            BaseBean candidate = gm.getPropertyParent(props[i]); 
            if (type.isInstance(candidate))
                retVal.add(candidate);
        }
        return retVal;
    }
}
