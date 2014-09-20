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

/*
 * EjbConversionHelper.java
 *
 * Created on March 19, 2002
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.jdo.api.persistence.mapping.ejb.ConversionHelper;
import com.sun.jdo.spi.persistence.support.ejb.model.util.NameMapper;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.IASEjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.PersistenceDescriptor;
import org.glassfish.ejb.deployment.descriptor.PersistentFieldInfo;
import org.glassfish.ejb.deployment.descriptor.RelationRoleDescriptor;
import org.glassfish.ejb.deployment.descriptor.RelationshipDescriptor;

/*
 * This class implements ConversionHelper interface by using data from
 * IASEjbBundleDescriptor.
 *
 * @author Shing Wai Chan
 */
public class EjbConversionHelper implements ConversionHelper {

    private NameMapper nameMapper = null;
    private EjbBundleDescriptorImpl bundle = null;
    private HashMap ejbDescMap = new HashMap();
    private HashMap ejbFieldMap = new HashMap();
    private HashMap ejbKeyMap = new HashMap();
    private HashMap ejbPerDescMap = new HashMap();
    private HashMap ejbRelMap = new HashMap();
    boolean generateFields = true;
    boolean ensureValidation = true;

    public EjbConversionHelper(NameMapper nameMapper) {
        this.nameMapper = nameMapper;
        this.bundle = nameMapper.getBundleDescriptor();

        Iterator iter = bundle.getEjbs().iterator();
        while (iter.hasNext()) {
            Object desc = iter.next();
            if (desc instanceof IASEjbCMPEntityDescriptor) {
                IASEjbCMPEntityDescriptor ejbDesc =
                        (IASEjbCMPEntityDescriptor)desc;

                String ejbName = ejbDesc.getName();
                //collect all ejbdesc
                ejbDescMap.put(ejbName, ejbDesc);

                //collect PersistenceDescriptor
                PersistenceDescriptor pers = ejbDesc.getPersistenceDescriptor();
                ejbPerDescMap.put(ejbName, pers);

                //collect pers fields
                Collection pFields = ejbDesc.getPersistentFields();
                HashMap fieldMap = new HashMap();
                Iterator fIter = pFields.iterator();
                while (fIter.hasNext()) {
                    String fieldName = ((PersistentFieldInfo)fIter.next()).name;
                    fieldMap.put(fieldName, fieldName);
                }
                ejbFieldMap.put(ejbName, fieldMap);

                //collect pseudo cmr fields
                List pseudoFields = nameMapper.getGeneratedRelationshipsForEjbName(ejbName);
                Iterator pIter = pseudoFields.iterator();
                while (pIter.hasNext()) {
                    addField(ejbName, (String)pIter.next());
                }

                //collect all keys
                Collection pKeys = ejbDesc.getPrimaryKeyFields();
                HashMap pKeyMap = new HashMap();
                Iterator kIter = pKeys.iterator();
                while (kIter.hasNext()) {
                    String fieldName = ((PersistentFieldInfo)kIter.next()).name;
                    pKeyMap.put(fieldName, fieldName);
                }
                ejbKeyMap.put(ejbName, pKeyMap);
            }
        }

        //collect relationship
        Set rels = bundle.getRelationships();
        Iterator relIter = rels.iterator();
        while (relIter.hasNext()) {
            RelationshipDescriptor rel = (RelationshipDescriptor)relIter.next();
            RelationRoleDescriptor source = rel.getSource();
            RelationRoleDescriptor sink = rel.getSink();

            //collect source RelationshipDescriptor
            String sourceEjbName = source.getOwner().getName();
            ArrayList sourceRels = (ArrayList)ejbRelMap.get(sourceEjbName);
            if (sourceRels == null) {
                sourceRels = new ArrayList();
                ejbRelMap.put(sourceEjbName, sourceRels);
            }
            sourceRels.add(rel);

            //collect source cmr field
            String sourceCMRField = source.getCMRField();
            if (sourceCMRField != null) {
                addField(sourceEjbName, sourceCMRField);
            }

            //collect sink RelationshipDescriptor
            String sinkEjbName = sink.getOwner().getName();
            ArrayList sinkRels = (ArrayList)ejbRelMap.get(sinkEjbName);
            if (sinkRels == null) {
                sinkRels = new ArrayList();
                ejbRelMap.put(sinkEjbName, sinkRels);
            }
            sinkRels.add(rel);

            //collect sink cmr field
            String sinkCMRField = sink.getCMRField();
            if (sinkCMRField != null) {
                addField(sinkEjbName, sinkCMRField);
            }
        }
    }

    //---- implements interface ConversionHelper ----

    public String getMappedClassName(String ejbName) {
        return nameMapper.getPersistenceClassForEjbName(ejbName);
    }

    /** 
     * If {@link #generateFields} is <code>true</code>, then this method will 
     * check if the field is one of the cmp + cmr + pseudo cmr fields, otherwise
     * the method will check if the field is one of the cmp + cmr fields.
     * @param ejbName The ejb-name element for the bean
     * @param fieldName The name of a container managed field in the named bean 
     * @return <code>true</code> if the bean contains the field, otherwise
     * return <code>false</code> 
     */
    public boolean hasField(String ejbName, String fieldName) {
        if (!generateFields && isGeneratedRelationship(ejbName, fieldName))
            return false;
        else {
            HashMap fieldMap = (HashMap)ejbFieldMap.get(ejbName);
            return (fieldMap != null) ? 
                (fieldMap.get(fieldName) != null) : false;
        }
    }

    /** 
     * If {@link #generateFields} is <code>true</code>, then this method will 
     * return an array of cmp + cmr + pseudo cmr fields, otherwise 
     * the method will return an array of cmp + cmr fields.
     * @param ejbName The ejb-name element for the bean
     * @return an array of fields in the ejb bean 
     */
    public Object[] getFields(String ejbName) {
        HashMap fieldMap = (HashMap)ejbFieldMap.get(ejbName);
        if (fieldMap != null) {
            List fields = new ArrayList(fieldMap.keySet());
            if (!generateFields) {
                fields.removeAll(getGeneratedRelationships(ejbName));
            }
            return fields.toArray();
        }
        return null;
    }

    /**
     * The boolean argument candidate is ignored in this case.
     */
    public boolean isKey(String ejbName, String fieldName, boolean candidate) {
        HashMap keyMap = (HashMap)ejbKeyMap.get(ejbName);
        return (keyMap != null) ? (keyMap.get(fieldName) != null) : false;
    }

    /**
     * This API will only be called from MappingFile when multiplicity is Many
     * on the other role.
     */
    public String getRelationshipFieldType(String ejbName, String fieldName) {
        if (isGeneratedRelationship(ejbName, fieldName)) {
            return java.util.Collection.class.getName();
        } else {
            PersistenceDescriptor pers =
                (PersistenceDescriptor)ejbPerDescMap.get(ejbName);
            return pers.getCMRFieldReturnType(fieldName);
        }
    }

    /**
     * getMultiplicity of the other role on the relationship
     * Please note that multiplicity is JDO style
     */
    public String getMultiplicity(String ejbName, String fieldName) {
        RelationRoleDescriptor oppRole = getRelationRoleDescriptor(ejbName,
                fieldName, false);
        return (oppRole.getIsMany()) ? MANY : ONE;
    }

    public String getRelationshipFieldContent(String ejbName, String fieldName) {
        RelationRoleDescriptor oppRole = getRelationRoleDescriptor(ejbName,
                fieldName, false);
        return oppRole.getOwner().getName();
    }

    /**
     * This method return the fieldName of relation role on the other end.
     */
    public String getInverseFieldName(String ejbName, String fieldName) {
        RelationRoleDescriptor oppRole = getRelationRoleDescriptor(ejbName,
                fieldName, false);
        String inverseName = oppRole.getCMRField();

        // if we are generating relationships, check for a generated inverse
        if ((generateFields) && (inverseName == null))
            inverseName = nameMapper.getGeneratedFieldForEjbField(
                ejbName, fieldName)[1];

        return inverseName;
    }

    /**
     * Returns flag whether the mapping conversion should apply the default
     * strategy for dealing with unknown primary key classes. This method will
     * only be called when {@link #generateFields} returns <code>true</code>.
     * @param ejbName The value of the ejb-name element for a bean.
     * @return <code>true</code> to apply the default unknown PK Class Strategy,
     * <code>false</code> otherwise
     */
    public boolean applyDefaultUnknownPKClassStrategy(String ejbName) {
        IASEjbCMPEntityDescriptor ejbDesc =
                (IASEjbCMPEntityDescriptor)ejbDescMap.get(ejbName);
        String keyClassName = ejbDesc.getPrimaryKeyClassName();
        return keyClassName != null &&
                keyClassName.equals(Object.class.getName());
    }
  
    /**
     * Returns the name used for generated primary key fields.
     * @return a string for key field name
     */
    public String getGeneratedPKFieldName() {
        return nameMapper.GENERATED_KEY_FIELD_NAME;
    }

    /**
     * Returns the prefix used for generated version fields.
     * @return a string for version field name prefix
     */
    public String getGeneratedVersionFieldNamePrefix() {
        return nameMapper.GENERATED_VERSION_FIELD_PREFIX;
    }

    public boolean relatedObjectsAreDeleted(String beanName, String fieldName) {
        RelationRoleDescriptor oppRole = getRelationRoleDescriptor(beanName, fieldName, false);
        return oppRole.getCascadeDelete();
    }

    /**
     * Returns the flag whether the mapping conversion should generate
     * relationship fields and primary key fields to support run-time.
     * The version field is always created even {@link #generateFields} is 
     * <code>false</code> because it holds version column information.
     * @return <code>true</code> to generate fields in the dot-mapping file
     * (if they are not present).
     */
    public boolean generateFields() {
        return generateFields;
    }

    /**
     * Sets the flag whether the mapping conversion should generate relationship
     * fields, primary key fields, and version fields to support run-time.
     * @param generateFields a flag which indicates whether fields should be
     * generated
     */
    public void setGenerateFields(boolean generateFields) {
        this.generateFields = generateFields;
    }

    /** Returns the flag whether the mapping conversion should validate
     * all fields against schema columns.
     * @return <code>true</code> to validate all the fields in the dot-mapping
     * file.
     */
    public boolean ensureValidation() {
        return ensureValidation;
    }

    /**
     * Sets the flag whether the mapping conversion should validate all fields
     * against schema columns.
     * @param isValidating a boolean of indicating validating fields or not
     */
    public void setEnsureValidation(boolean isValidating) {
        ensureValidation = isValidating;
    }

    /**
     * Returns <code>true</code> if the field is generated. There are three
     * types of generated fields: generated relationships, unknown primary key
     * fields, and version consistency fields.
     * @param ejbName The ejb-name element for the bean
     * @param fieldName The name of a container managed field in the named bean 
     * @return <code>true</code> if the field is generated; <code>false</code>
     * otherwise.
     */

    public boolean isGeneratedField(String ejbName, String fieldName) {
        return nameMapper.isGeneratedField(ejbName, fieldName);
    }

    public boolean isGeneratedRelationship(String ejbName, String fieldName) {
        return nameMapper.isGeneratedEjbRelationship(ejbName, fieldName);
    }

    /**
     * Returns a list of generated relationship field names.
     * @param ejbName The ejb-name element for the bean
     * @return a list of generated relationship field names
     */
    public List getGeneratedRelationships(String ejbName) {
        return nameMapper.getGeneratedRelationshipsForEjbName(ejbName);
 
    }

    //-------------------------------------
    private RelationRoleDescriptor getRelationRoleDescriptor(String ejbName,
            String cmrFieldName, boolean self) {
        String myEjbName = ejbName;
        String myCMRFieldName = cmrFieldName;
        boolean myself = self;
        if (isGeneratedRelationship(ejbName, cmrFieldName)) {
            String[] nfPair = nameMapper.getEjbFieldForGeneratedField(
                    ejbName, cmrFieldName);
            myEjbName = nfPair[0];
            myCMRFieldName = nfPair[1];
            myself = !self;
        }
        return getRealRelationRoleDescriptor(myEjbName, myCMRFieldName, myself);
    }

    private RelationRoleDescriptor getRealRelationRoleDescriptor(
            String ejbName, String cmrFieldName, boolean self) {
        ArrayList rels = (ArrayList)ejbRelMap.get(ejbName);
        for (int i = 0; i < rels.size(); i++) {
            RelationshipDescriptor rel = (RelationshipDescriptor)rels.get(i);
            RelationRoleDescriptor source = rel.getSource();
            RelationRoleDescriptor sink = rel.getSink();
            if (ejbName.equals(source.getOwner().getName()) &&
                    cmrFieldName.equals(source.getCMRField())) {
                return (self) ? source : sink;
            } else if (ejbName.equals(sink.getOwner().getName()) &&
                    cmrFieldName.equals(sink.getCMRField())) {
                return (self) ? sink : source;
            }
        }
        throw new IllegalArgumentException();
    }

    private void addField(String ejbName, String fieldName) {
        HashMap fieldMap = (HashMap)ejbFieldMap.get(ejbName);
        if (fieldMap == null) {
            fieldMap = new HashMap();
            ejbFieldMap.put(ejbName, fieldMap);
        }
        fieldMap.put(fieldName, fieldName);
    }
}
