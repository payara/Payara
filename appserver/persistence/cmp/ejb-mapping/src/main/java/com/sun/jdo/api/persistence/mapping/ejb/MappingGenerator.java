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

/*
 * MappingGenerator.java
 *
 * Created on Aug 18, 2003
 */

package com.sun.jdo.api.persistence.mapping.ejb;

import java.util.*;
import java.sql.Types;
import java.io.IOException;

import com.sun.jdo.api.persistence.mapping.ejb.beans.*;

import com.sun.jdo.api.persistence.model.Model;
import com.sun.jdo.api.persistence.model.ModelException;
import com.sun.jdo.api.persistence.model.mapping.MappingClassElement;
import com.sun.jdo.api.persistence.model.mapping.MappingFieldElement;
import com.sun.jdo.api.persistence.model.jdo.PersistenceClassElement;
import com.sun.jdo.api.persistence.model.jdo.PersistenceFieldElement;

import com.sun.jdo.spi.persistence.utility.StringHelper;
import com.sun.jdo.spi.persistence.utility.JavaTypeHelper;

import com.sun.jdo.spi.persistence.generator.database.DatabaseGenerator;
import com.sun.jdo.spi.persistence.generator.database.MappingPolicy;

import org.netbeans.modules.dbschema.*;
import org.netbeans.modules.dbschema.jdbcimpl.SchemaElementImpl;
import org.netbeans.modules.dbschema.util.NameUtil;

import org.netbeans.modules.schema2beans.Schema2BeansException;

/*
 * This class will generate mapping classes from sun-cmp-mappings.xml
 * and dbschema if they are available in the jar, or it will generate mapping 
 * classes based on ejb-jar.xml, bean classes and policy by invoking the 
 * database generation backend.
 *
 * @author Jie Leng 
 */
public class MappingGenerator {

    // Since "_JDOState" is defined as private in IASEjbCMPEntityDescriptor,
    // redefined here for passing it in DatabaseGenerator.
    private static final String CLASS_SUFFIX = "_JDOState"; // NOI18N

    private static final String FAKE_NAME = "fakename"; // NOI18N

    private final EJBInfoHelper infoHelper;
    private final Model model;
    private final AbstractNameMapper nameMapper;
    private final ClassLoader loader;
    private final ConversionHelper ddHelper;

    /** a boolean indicating whether the jdo model and mapping model should
     * contain generated fields
     */
    private boolean skipGeneratedFields = false;

    //hold strong reference to mapping class elements 
    private List strongRefs = new ArrayList();

    /** 
     * Constructor
     * @param infoHelper an instance of an EJBInfoHelper
     * @param loader a class loader
     * @param skipGeneratedFields a boolean indicating to remove generated 
     * fields from jdo model and mapping model 
     */
    public MappingGenerator(EJBInfoHelper infoHelper, 
            ClassLoader loader, boolean skipGeneratedFields) {
        this.infoHelper = infoHelper;
        this.model = infoHelper.getModel();
        this.loader = loader;
        this.nameMapper = infoHelper.getNameMapper();
        this.ddHelper = infoHelper.createConversionHelper();
        this.skipGeneratedFields = skipGeneratedFields;
    }

    protected EJBInfoHelper getInfoHelper() {
        return infoHelper;
    }

    protected ClassLoader getClassLoader() {
        return loader;
    }

    protected AbstractNameMapper getNameMapper() {
        return nameMapper;
    }

    protected ConversionHelper getConversionHelper() {
        return ddHelper;
    }

    /**
     * Create mapping classes and schema based on database vendor name.
     * @param dbName a string for database vendor name
     * @param uniqueTableNames a Boolean to determin if use unique table names
     * during database generation
     * @param userPolicy a property object holding user overrides
     * @param inputFilesPath a directory where sun-cmp-mappings.xml is located
     * @throws IOException
     * @throws Schema2BeansException
     * @throws ModelException
     * @throws DBException
     * @throws ConversionException
     */
    public DatabaseGenerator.Results generateMappingClasses(String dbName,
            Boolean uniqueTableNames, Properties userPolicy,
            String inputFilesPath)
            throws IOException, Schema2BeansException, ModelException,
            DBException, ConversionException {

        // generate mapping classes and dbschema in memory
        SunCmpMappings sunCmpMappings = null;

        // sun-cmp-mappings.xml does not exist, use DatabaseGenerator
        // to generate sun-cmp-mappings.xml, *.dbschema

        List pcClasses = new ArrayList();
        sunCmpMappings = getPartialSunCmpMappings(pcClasses, 
                (uniqueTableNames != null)? uniqueTableNames.booleanValue() : false);

        // load real jdo model and fake mapping model in memory
        ddHelper.setEnsureValidation(false);

        // create fake schema for partial mapping
        SchemaElement fakeSchema = new SchemaElement(new SchemaElementImpl());
        fakeSchema.setName(DBIdentifier.create(FAKE_NAME));

        // add newly created fake schema to SchemaElement cache
        SchemaElement.addToCache(fakeSchema);

        // pass null as class loader in order for MappingFile to load schema
        // from cache not from disk.
        loadMappingClasses(sunCmpMappings, null);

        DatabaseGenerator.Results results = generateSchema(pcClasses,
                dbName, uniqueTableNames, userPolicy);

        SchemaElement schema = results.getSchema();
        Set mappingClasses = results.getMappingClasses();

        // remove fake schema from cache since the correct schema is generated.
        SchemaElement.removeFromCache(FAKE_NAME);

        // clean up old version of schema in SchemaElement cache
        // if there is one
        SchemaElement.removeFromCache(schema.getName().getName());

        // add newly created schema to SchemaElement cache
        SchemaElement.addToCache(schema);

        // update mapping classes
        updateMappingClasses(mappingClasses);

        // If skipGeneratedFields is set to true, the generated fields should 
        // not be kept in jdo model and mapping model. 
        // Remove generated fields from jdo model and mapping 
        // model before returning the result.
        if (skipGeneratedFields) {
            Iterator iter = mappingClasses.iterator();
            while (iter.hasNext()) {
                MappingClassElement mapClassElt = (MappingClassElement)iter.next();
                if (mapClassElt != null) {
                    String className = mapClassElt.getName();
                    String ejbName = nameMapper.getEjbNameForPersistenceClass(
                        className);

                    PersistenceClassElement pce = (PersistenceClassElement)
                            model.getPersistenceClass(className);
                    PersistenceFieldElement[] allFields = pce.getFields();
                    if (allFields != null) {
                        List generatedFieldList = new ArrayList();

                        // In order to avoid concurrentmod exception,
                        // loop through all persistence fields to put generated
                        // fields in a list, loop though the list to remove
                        // the generated fields from the model.
                        for (int i = 0; i < allFields.length; i++) { 
                            PersistenceFieldElement pfe = allFields[i];
                            if (pfe != null) { 
                                String pFieldName = pfe.getName();
                                String ejbFieldName = nameMapper.
                                    getEjbFieldForPersistenceField(className, 
                                    pFieldName);
                                if (nameMapper.isGeneratedField(ejbName, 
                                    ejbFieldName)) {
                                    generatedFieldList.add(pfe);
                                }
                            }
                        }

                        // If the field is a version field, don't remove it 
                        // from the model even though it is generated because 
                        // it is needed to hold the version column information.
                        Iterator iterator = generatedFieldList.iterator();
                        while (iterator.hasNext()) {
                            PersistenceFieldElement pfe = 
                                (PersistenceFieldElement)iterator.next();
                            MappingFieldElement mfe = mapClassElt.
                                 getField(pfe.getName());
                            if (mfe != null && (!mfe.isVersion())) {
                                model.removeFieldElement(pfe);
                                mapClassElt.removeField(mfe);
                            }
                        }
                    }
                }
            }
        }

        return results;
    }

    /**
     * load mapping classes from SunCmpMappings object
     * @param sunMapping a SunCmpMappings object representing
     * sun-cmp-mappings.xml in memory
     * @param classLoader a class loader object
     * @return a map object containing ejb names and mapping classes
     * @throws DBException
     * @throws ModelException
     * @throws ConversionException
     */
    protected Map loadMappingClasses(SunCmpMappings sunMapping, 
        ClassLoader classLoader) 
        throws DBException, ModelException, ConversionException {
        MappingFile mapFile = new MappingFile(classLoader);

        Map allMappings = mapFile.intoMappingClasses(sunMapping, ddHelper);

        updateMappingClasses(allMappings.values());

        return allMappings;
    }

    /** 
     * Clean up strong reference. It should be called by end of deployment
     * or deploytool.
     */
    public void cleanup() {
        // Remove the strong references to MappingClassElements
        // needed during deployment. The mapping class cache
        // can now be cleaned up by the garbage collector.
        strongRefs.clear();
    }

    /** 
     * Call DatabaseGenerator to generate database model and mapping model
     * @param pcClasses a list of DatabaseGenerator.GeneratorNameTuple objects 
     * @param dbVendorName the string of database name
     * @param useUniqueTableNames the string to determine use of unique table
     * names for database generation
     * @param userPolicy the property having user defined mappings between
     * class field and jdbc type
     * @return DatabaseGenerator.Results contains mapping classes and schema
     * @throws IOException
     * @throws DBException
     * @throws ModelException
     */
    private DatabaseGenerator.Results generateSchema(List pcClasses, 
            String dbName, Boolean useUniqueTableNames, 
            Properties userPolicy) 
            throws IOException, DBException, ModelException {

        MappingPolicy mappingPolicy = MappingPolicy.getMappingPolicy(dbName);
        mappingPolicy.setUserPolicy(userPolicy);

        if (useUniqueTableNames != null) {
            // It was explicitly set.
            mappingPolicy.setUniqueTableName(useUniqueTableNames.booleanValue());
        }

       return DatabaseGenerator.generate(
                model, pcClasses, mappingPolicy, 
                infoHelper.getSchemaNameToGenerate(), CLASS_SUFFIX, true);
    }

    /** 
     * Puts mapping classes into model's cache
     * @param mappingClasses a collection of mapping classes
     */ 
    private void updateMappingClasses(Collection mappingClasses) {
        Iterator iter = mappingClasses.iterator();
        while (iter.hasNext()) {
            MappingClassElement mapClassElt = (MappingClassElement)iter.next();
            //put it in the models' cache
            model.updateKeyForClass(mapClassElt, null);
            //keep a strong ref
            strongRefs.add(mapClassElt);
        }
    }

    /** 
     * Generates partial sun-cmp-mapping (contains fake table name and 
     * fake column name) for MappingFile.intoMappings()
     * @param pcClasses a list of DatabaseGenerator.NameTuple objects 
     * @param useUniqueTableNames a boolean to determine whether to use 
     * unique table names during database generation
     * @return a SunCmpMappings object
     * @throws Schema2BeansException
     */
    private SunCmpMappings getPartialSunCmpMappings(List pcClasses, 
             boolean useUniqueTableNames) throws Schema2BeansException {

       // Create a new name mapper with perisistence class name differing
        // from bean name if useUniqueTableName flag is true. 
        // So persistence class name can be used for unique table name.
        AbstractNameMapper nameMapper2 = (useUniqueTableNames) ? 
                infoHelper.createUniqueNameMapper() : nameMapper;

        SunCmpMappings mappings = null;
        mappings = new SunCmpMappings();
        SunCmpMapping mapping = new SunCmpMapping();
        mapping.setSchema(FAKE_NAME);

        Iterator iter = infoHelper.getEjbNames().iterator();
        while (iter.hasNext()) {
            String ejbName = (String)iter.next();
            String pcClass = ddHelper.getMappedClassName(ejbName);
            String hashClassName = JavaTypeHelper.getShortClassName(pcClass);

            // Make sure hash class name differs from ejb name 
            // if useUniqueTableName flag is true.
            // if useUniqueTableName flag is false, ejb name is used for 
            // table name and hash class name is ignored. 
            if (useUniqueTableNames && hashClassName.equals(ejbName)) {
                hashClassName = JavaTypeHelper.getShortClassName(
                       nameMapper2.getPersistenceClassForEjbName(ejbName));
                pcClasses.add(new DatabaseGenerator.NameTuple(
                       pcClass, ejbName, hashClassName));
            }
            else {
                pcClasses.add(new DatabaseGenerator.NameTuple(
                       pcClass, ejbName));
            }

            EntityMapping entity = new EntityMapping();
            entity.setEjbName(ejbName);
            entity.setTableName(FAKE_NAME);
            Collection fields = infoHelper.getFieldsForEjb(ejbName);
            Collection rels = infoHelper.getRelationshipsForEjb(ejbName);
            fields.removeAll(rels);
            // cmp field
            Iterator fIter = fields.iterator();
            while (fIter.hasNext()) {
                String fieldName = (String)fIter.next();
                CmpFieldMapping cmpField = new CmpFieldMapping();
                cmpField.setFieldName(fieldName);
                cmpField.addColumnName(FAKE_NAME);
                entity.addCmpFieldMapping(cmpField);
            } 
            // cmr field
            fIter = rels.iterator();
            while (fIter.hasNext()) {
                String fieldName = (String)fIter.next();
                CmrFieldMapping cmrField = new CmrFieldMapping();
                cmrField.setCmrFieldName(fieldName);
                ColumnPair columnPair = new ColumnPair();
                columnPair.addColumnName(FAKE_NAME);
                columnPair.addColumnName(FAKE_NAME);
                cmrField.addColumnPair(columnPair);
                entity.addCmrFieldMapping(cmrField);
            }
            mapping.addEntityMapping(entity);
        }

        mappings.addSunCmpMapping(mapping);

        return mappings;
    }

    /** 
     * Returns <code>true</code> if the specified propertyValue represents
     * a defined value, <code>false</code> otherwise.  This implementation 
     * returns <code>true</code> if the value is not empty, but subclasses
     * may override this method to compare to a constant which represents an
     * undefined value.
     * @param propertyValue the value to be tested for defined
     * @return <code>true</code> if the specified propertyValue represents
     * a defined value, <code>false</code> otherwise
     */
    protected boolean isPropertyDefined(String propertyValue) {
        return !StringHelper.isEmpty(propertyValue);
    }

    /** 
     * Update column in the SchemaElement with jdbc type and its length, 
     * scale and precision.
     * @param column a ColumnElement to be updated
     * @param jdbcType jdbc type from java.sql.Types
     * @param length an Integer for length or <code>null</code> 
     * if it does not apply
     * @param scale an Integer for scale or <code>null</code> 
     * if it does not apply
     * @param precision an Integer for precision or <code>null</code> 
     * if it does not apply
     */
    public static void updateColumn(ColumnElement column, int jdbcType, 
            Integer length, Integer scale, Integer precision)
            throws DBException {

        column.setType(jdbcType);
        column.setLength(length);
        column.setScale(scale);
        column.setPrecision(precision);
    }

    /**
     * This method updates properties which stores user override policy.
     * @param prop the property for user override
     * @param className a string for bean class
     * @param fieldName a string for field
     * @param jdbcType jdbc type from java.sql.Types
     * @param length an Integer for length or <code>null</code>
     * if it does not apply
     * @param scale an Integer for scale or <code>null</code>
     * if it does not apply
     * @param precision an Integer for precision or <code>null</code>
     * if it does not apply
     */
    public static void updateProperties(Properties prop, String className, 
            String fieldName, int jdbcType, Integer length, Integer scale, 
            Integer precision) {

        prop.setProperty(
                MappingPolicy.getOverrideForType(className, fieldName),
                MappingPolicy.getJdbcTypeName(jdbcType));

        updateProperty(prop, MappingPolicy.getOverrideForLength(
                className, fieldName), length);

        updateProperty(prop, MappingPolicy.getOverrideForScale(
                className, fieldName), scale);

        updateProperty(prop, MappingPolicy.getOverrideForPrecision(
                className, fieldName), precision);
    }

    /**
     * This method updates property. If the value is not <code>null</code>,
     * update the property. If the value is <code>null</code>, 
     * remove the property.
     * @param prop a property object which needs to be updated
     * @param key a key for the property
     * @param value a value for the propety
     */
    private static void updateProperty(Properties prop, String key, 
            Integer value) {
        if (value != null) {
            prop.setProperty(key, value.toString());
        }
        else {
           prop.remove(key);
        }
    }

    /**
     * The contents of this class will eventually be added to SQLTypeUtil 
     * in dbmodel. It is an util class which provides methods for jdbc type 
     * compatible and jdbc attribute.
     */
    public static class SQLTypeUtil {

        private static final Map characterMap = new HashMap();
        private static final Map numericMap = new HashMap();
        private static final Map blobMap = new HashMap();
        private static final Map timeMap = new HashMap();
        
        private static final String NONE_ATTRIBUTE = "none";
        private static final String LENGTH_ATTRIBUTE = "length";
        private static final String SCALE_ATTRIBUTE = "scale";
        private static final String SCALE_PRECISION_ATTRIBUTE = "scale-precision";

        static {
            characterMap.put(new Integer(Types.CHAR), LENGTH_ATTRIBUTE);
            characterMap.put(new Integer(Types.VARCHAR), LENGTH_ATTRIBUTE);
            characterMap.put(new Integer(Types.CLOB), LENGTH_ATTRIBUTE);

            numericMap.put(new Integer(Types.BIT), NONE_ATTRIBUTE);
            numericMap.put(new Integer(Types.TINYINT), NONE_ATTRIBUTE);
            numericMap.put(new Integer(Types.SMALLINT), NONE_ATTRIBUTE);
            numericMap.put(new Integer(Types.BIGINT), NONE_ATTRIBUTE);
            numericMap.put(new Integer(Types.INTEGER), NONE_ATTRIBUTE);
            numericMap.put(new Integer(Types.DOUBLE), NONE_ATTRIBUTE);
            numericMap.put(new Integer(Types.DECIMAL), SCALE_PRECISION_ATTRIBUTE);
            numericMap.put(new Integer(Types.REAL), NONE_ATTRIBUTE);

            blobMap.put(new Integer(Types.BLOB), LENGTH_ATTRIBUTE);

            timeMap.put(new Integer(Types.DATE), NONE_ATTRIBUTE);
            timeMap.put(new Integer(Types.TIME), NONE_ATTRIBUTE);
            timeMap.put(new Integer(Types.TIMESTAMP), NONE_ATTRIBUTE);
        }

        /** Returns if the given data type is numeric type or not.
         * @param jdbcType the type from java.sql.Types
         * @return <code>true</code> if the given type is numeric type; 
         * <code>false</code> otherwise
         */
        public static boolean isNumeric (int jdbcType) {
            return checkType(jdbcType, numericMap);
        }

        /** Returns if the given data type is character type or not.
         * @param jdbcType the type from java.sql.Types
         * @return <code>true</code> if the given type is character type; 
         * <code>false</code> otherwise
         */
        public static boolean isCharacter (int jdbcType) {
            return checkType(jdbcType, characterMap);
        }

        /** Returns if a given data type is blob type or not.
         * @param jdbcType the type from java.sql.Types
         * @return <code>true</code> if the give type is blob type; 
         * <code>false</code> otherwise
         */
        public static boolean isBlob (int jdbcType) {
            return checkType(jdbcType, blobMap);
        }

        /** Returns if a given data type is time type or not.
         * @param jdbcType the type from java.sql.Types
         * @return <code>true</code> if the give type is time type; 
         * <code>false</code> otherwise
         */
        public static boolean isTime (int jdbcType) {
            return checkType(jdbcType, timeMap);
        }

        private static boolean checkType(int jdbcType, Map jdbcTypes) {
            return jdbcTypes.containsKey(new Integer(jdbcType));
        }

        /** Returns a collection of compatible jdbc types.
         * @param jdbcType the type from java.sql.Types 
         * @return a collection of compatible jdbc types 
         */
        public static Collection getCompatibleTypes(int jdbcType) {
            if (isNumeric(jdbcType)) {
                return numericMap.keySet();
            }
            else if (isCharacter(jdbcType)) {
                return characterMap.keySet();
            }
            else if (isBlob(jdbcType)) {
                return blobMap.keySet();
            }
            else if (isTime(jdbcType)) {
                return timeMap.keySet();
            }
            return null;
        }

        /**
         * This method returns true if the jdbc type has scale. 
         * @param jdbcType a jdbc type from java.sql.Types
         * @return <code>true</code> if the type has scale;
         * <code>false</code> otherwise 
         */
        public static boolean hasScale(int jdbcType) {
            if (getAttribute(jdbcType).equals(SCALE_ATTRIBUTE)
                    || getAttribute(jdbcType).equals(SCALE_PRECISION_ATTRIBUTE))
                return true;
            return false;
        }

        /**
         * This method returns true if the jdbc type has precision. 
         * If the jdbc type has the precision, it means it also has scale. 
         * @param jdbcType a jdbc type from java.sql.Types
         * @return <code>true</code> if the type has precision; 
         * <code>false</code> otherwise 
         */
        public static boolean hasPrecision(int jdbcType) {
            if (getAttribute(jdbcType).equals(SCALE_PRECISION_ATTRIBUTE))
                return true;
            return false;
        }

        /**
         * This method returns true if the jdbc type has length
         * @param jdbcType a jdbc type from java.sql.Types
         * @return <code>true</code> if the type has length;
         * <code>false</code> otherwise
         */
        public static boolean hasLength(int jdbcType) {
            if (getAttribute(jdbcType).equals(LENGTH_ATTRIBUTE))
                return true;
            return false;
        }

        private static String getAttribute(int jdbcType) {
            if (isNumeric(jdbcType)) {
                return (String)numericMap.get(new Integer(jdbcType));
            }
            else if (isCharacter(jdbcType)) {
                return (String)characterMap.get(new Integer(jdbcType));
            }
            else if (isBlob(jdbcType)) {
                return (String)blobMap.get(new Integer(jdbcType));
            }
            else if (isTime(jdbcType)) {
                return (String)timeMap.get(new Integer(jdbcType));
            }
            return NONE_ATTRIBUTE;
        }
    }
}
