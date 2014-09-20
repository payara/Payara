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
 * MappingFile.java
 *
 * Created on February 1, 2002, 9:47 PM
 */

package com.sun.jdo.api.persistence.mapping.ejb;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;

import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.Iterator;
import java.io.IOException;

import java.text.MessageFormat;

import org.netbeans.modules.dbschema.*;

import com.sun.jdo.api.persistence.mapping.ejb.beans.*;
import com.sun.jdo.spi.persistence.utility.StringHelper;
import com.sun.jdo.api.persistence.model.*;
import com.sun.jdo.api.persistence.model.mapping.*;
import com.sun.jdo.api.persistence.model.mapping.impl.*;
import com.sun.jdo.api.persistence.model.jdo.*;
import com.sun.jdo.api.persistence.model.jdo.impl.*;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.glassfish.persistence.common.I18NHelper;

import org.netbeans.modules.schema2beans.Schema2BeansException;

/** This class supports the conversion between the iAS mapping file
 * format and the object used to represent that mapping to support
 * the iAS EJBC process and iAS CMP run-time.
 *
 * @author vbk
 * @version 1.0
 */
public class MappingFile {

    private static final String JAVA_TYPE_SET = "java.util.Set"; //NOI18N
    private static final String JAVA_TYPE_COLLECTION = "java.util.Collection"; //NOI18N
    private static final List types = new ArrayList();

    /** Definitive location for a  mapping file in an ejb-jar file. */
    public static final String DEFAULT_LOCATION_IN_EJB_JAR;

    /** The logger */
    private static final Logger logger =
        LogHelperMappingConversion.getLogger();

    /**
     * I18N message handler
     */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
            MappingFile.class);

    static {
        types.add(JAVA_TYPE_SET);
        types.add(JAVA_TYPE_COLLECTION);

        DEFAULT_LOCATION_IN_EJB_JAR = new StringBuffer(I18NHelper.getMessage(
            messages, "CONST_IAS_MAPPING_FILE_LOC")). //NOI18N
            append(File.separator).
            append(I18NHelper.getMessage(
            messages, "CONST_IAS_MAPPING_FILE")).toString(); //NOI18N
    }

    private Map inverseRelationships = null;
    private Map namedGroups = null;
    private int groupCount = MappingFieldElement.GROUP_INDEPENDENT;

    private ClassLoader classLoader = null;

    private static int MINIMUM_PRECISION = 19;

    /**
     * An object which implements ConversionHelper interface to
     * access data from other available sources to support the mapping
     * generation.
     */
    private ConversionHelper helper = null;

    private HashMap loadedSchema = new HashMap();

    /** Creates new MappingFile */
    public  MappingFile() {
        classLoader = null;
    }

    /** Creates new MappingFile */
    public  MappingFile(ClassLoader cl) {
        this();
        classLoader = cl;
    }

    /** Convert an SunCmpMapping object into the equivelent collection of
     * MappingClassElement objects
     * @param content An SunCmpMapping object that describes a mapping
     * for one or more beans
     * @param helper An object which implements ConversionHelper interface to
     * access data from other available sources to support the mapping
     * generation
     * @return A Collection of MappingClassElement objects
     * @throws DBException
     * @throws ModelException
     * @throws ConversionException 
     */
    public Map intoMappingClasses(SunCmpMappings content,
        ConversionHelper helper)
        throws DBException, ModelException, ConversionException {
        Map mces = new java.util.HashMap();
        this.helper = helper;
        boolean ensureValidation = helper.ensureValidation();

        for (int i = 0; i < content.sizeSunCmpMapping(); i++) {
            SunCmpMapping beanSet = content.getSunCmpMapping(i);
            inverseRelationships = new HashMap();
            namedGroups = new HashMap();
            String schema = beanSet.getSchema();

            if (helper.generateFields()) {

                // sweep through the mappings to complete them
                completeCmrMappings(beanSet);
            }

            // process bean mapping and fields mapping
            for (int k = 0; k < beanSet.sizeEntityMapping(); k++) {
                EntityMapping beanMapping = beanSet.getEntityMapping(k);
                MappingClassElement aTpMapping = null;
                if (ensureValidation) {
                    aTpMapping = mapFieldsOfBean(beanMapping, schema);
                }
                else {
                    try {
                        aTpMapping = mapFieldsOfBean(beanMapping, schema);
                    }
                    catch (ConversionException t) {
                        if (logger.isLoggable(Logger.FINE))
                            logger.fine(
                                I18NHelper.getMessage(
                                messages,
                                "MESSAGE_CONV_EXC", //NOI18N
                                t.toString()));
                    }
                }

                mces.put(beanMapping.getEjbName(), aTpMapping);
            }
        }
        return mces;
    }

    /** Convert an SunCmpMapping object into the equivelent collection of
     * MappingClassElement objects
     * @param content An InputStream that contains an xml data stream that
     * conforms to the sun-cmp-mapping.dtd
     * @param helper An object which implements ConversionHelper interface to
     * access data from other available sources to support the mapping
     * generation
     * @return A Collection of MappingClassElement objects
     * @throws DBException
     * @throws ModelException
     * @throws ConversionException
     */
    public Map intoMappingClasses(InputStream content, ConversionHelper helper)
        throws DBException, ModelException, ConversionException {
        SunCmpMappings foo = null;
        try {
            foo = SunCmpMappings.createGraph(content);
        }
        catch (Schema2BeansException t) {
            if (helper.ensureValidation()) {
                throw new ConversionException(
                    I18NHelper.getMessage(messages,
                    "XML_ERROR_IN_MAPPING_FILE", //NOI18N
                    DEFAULT_LOCATION_IN_EJB_JAR));
            }
            foo = SunCmpMappings.createGraph();
        }

        return intoMappingClasses(foo, helper);
    }

    /** Creates an SunCmpMapping object from a Collection of
     * MappingClassElement objects
     * @param dest The output for processing
     * @param mappingClasses The Collection of MappingClassElements
     * @param helper An object which implements ConversionHelper interface to
     * access data from other available sources to support the mapping
     * generation
     * @throws IOException Thrown if there is a problem
     * sending the data out on <CODE>dest</CODE>.
     * @throws Schema2BeansException
     */
    public void fromMappingClasses(OutputStream dest, Map mappingClasses,
        ConversionHelper helper)
        throws IOException, Schema2BeansException {
        SunCmpMappings tmp = fromMappingClasses(mappingClasses, helper);
        tmp.write(dest);
    }

    /** Creates an SunCmpMapping object from a Collection of
     * MappingClassElement objects
     * @param mappingClasses The Collection of MappingClassElements
     * @param helper An object which implements ConversionHelper interface to
     * access data from other available sources to support the mapping
     * generation.
     * @return The SunCmpMapping object that is equivelent to the
     * collection of MappingClassElements.
     * throws Schema2BeansException
     */
    public SunCmpMappings fromMappingClasses(Map mappingClasses,
        ConversionHelper helper) throws Schema2BeansException {
        Iterator keyIter = mappingClasses.keySet().iterator();
        Map mapOfMapping = new java.util.HashMap();
        while (keyIter.hasNext()) {
            String ejbName = (String) keyIter.next();
            MappingClassElement mce = (MappingClassElement)
                mappingClasses.get(ejbName);
            EntityMapping beanMapping = new EntityMapping();

            if (null != mce) {
                setConsistency(mce, beanMapping);

                String schemaName = mce.getDatabaseRoot();
                SunCmpMapping aMapping = (SunCmpMapping) mapOfMapping.get(
                    schemaName);
                if (null == aMapping) {
                    aMapping = new SunCmpMapping();
                    aMapping.setSchema(schemaName);
                    mapOfMapping.put(schemaName, aMapping);
                }
                List tables = mce.getTables();
                MappingTableElement primary = null;
                if (tables.size() > 0) {
                    primary = (MappingTableElement) tables.get(0);
                    beanMapping.setTableName(primary.getName());
                }
                beanMapping.setEjbName(ejbName);
                if (null != primary) {
                    List refKeys = primary.getReferencingKeys();
                    for (int i = 0; refKeys != null && i < refKeys.size(); i++) {
                        SecondaryTable sT = new SecondaryTable();
                        MappingReferenceKeyElement mrke =
                            (MappingReferenceKeyElement) refKeys.get(i);
                        MappingTableElement mte = mrke.getTable();
                        if (null != mte) {
                            sT.setTableName(mte.getName());
                            List cpnames = mrke.getColumnPairNames();
                            boolean hasPairs = false;
                            for (int j = 0; cpnames != null &&
                                j < cpnames.size(); j++) {
                                List token =
                                    StringHelper.separatedListToArray((String)cpnames.get(j),";"); //NOI18N
                                ColumnPair cp = new ColumnPair();
                                Iterator iter = token.iterator();
                                while (iter.hasNext()) {
                                    String columnName = (String)iter.next();
                                    cp.addColumnName(columnName);
                                }
                                sT.addColumnPair(cp);
                                hasPairs = true;
                            }
                            if (hasPairs)
                                beanMapping.addSecondaryTable(sT);
                            else
                                if (logger.isLoggable(Logger.FINE))
                                    logger.fine( 
                                        I18NHelper.getMessage(
                                        messages,
                                        "WARN_ILLEGAL_PAIR", //NOI18N
                                        new Object [] {ejbName, mte.getName(), cpnames}));
                        }
                        else {
                            if (logger.isLoggable(Logger.FINE))
                                logger.fine(
                                    I18NHelper.getMessage(
                                    messages,
                                    "WARN_MISSING_TABLE", //NOI18N
                                    new Object [] {ejbName, primary.getName()}));
                        }
                    }
                }
                else {
                    if (logger.isLoggable(Logger.FINE))
                        logger.fine(
                            I18NHelper.getMessage(
                            messages,
                            "WARN_NO_PRIMARY", //NOI18N
                            ejbName));
                }

                // transform the field mappings
                PersistenceClassElement pce = null;
                PersistenceFieldElement pfields[] = null;
                if (mce instanceof MappingClassElementImpl) {
                    MappingClassElementImpl mcei =
                        (MappingClassElementImpl) mce;
                    pce = mcei.getPersistenceElement();
                    pfields = pce.getFields();
                }
                int len = 0;
                if (null != pfields)
                    len = pfields.length;
                for (int i = 0; i < len; i++) {
                    PersistenceFieldElement pfield = pfields[i];
                    String fieldName = pfield.getName();
                    if (helper.isGeneratedField(ejbName, fieldName)) {
                        continue;
                    }
                    if (pfield instanceof RelationshipElement) {
                        MappingRelationshipElement mre =
                            (MappingRelationshipElement) mce.getField(fieldName);
                        MappingFieldElement mfe = mre;
                        CmrFieldMapping cfm = new CmrFieldMapping();
                        cfm.setCmrFieldName(fieldName);
                        List cols = null;
                        if (null != mfe) {
                            cols = mfe.getColumns();
                            int fgVal = mfe.getFetchGroup();
                            setFetchedWith(cfm, fgVal);
                        }
                        for (int j = 0; null != cols && j < cols.size(); j++) {
                            String cpstring = (String) cols.get(j);
                            int slen = cpstring.indexOf(';');
                            ColumnPair cp = new ColumnPair();
                            cp.addColumnName(cpstring.substring(0,slen));
                            cp.addColumnName(cpstring.substring(slen+1));
                            cfm.addColumnPair(cp);
                        }
                        if (null != mre)
                            cols = mre.getAssociatedColumns();
                        for (int j = 0; null != cols && j < cols.size(); j++) {
                            String cpstring = (String) cols.get(j);
                            int slen = cpstring.indexOf(';');
                            ColumnPair cp = new ColumnPair();
                            cp.addColumnName(cpstring.substring(0,slen));
                            cp.addColumnName(cpstring.substring(slen+1));
                            cfm.addColumnPair(cp);
                        }
                        beanMapping.addCmrFieldMapping(cfm);
                    }
                    else {
                        MappingFieldElement mfe = mce.getField(fieldName);
                        CmpFieldMapping cfm = new CmpFieldMapping();
                        cfm.setFieldName(fieldName);
                        List cols = null;
                        if (null != mfe) {
                            cols = mfe.getColumns();
                            for (int j = 0; null != cols &&
                                j < cols.size(); j++) {
                                cfm.addColumnName((String)cols.get(j));
                            }
                            int fgVal = mfe.getFetchGroup();
                            setFetchedWith(cfm,fgVal);
                        }
                        beanMapping.addCmpFieldMapping(cfm);
                    }
                }
                aMapping.addEntityMapping(beanMapping);
            }
        }
        SunCmpMappings retVal = null;
        retVal = new SunCmpMappings();
        Iterator mapOfMappingIter = mapOfMapping.values().iterator();
        while (mapOfMappingIter.hasNext()) {
            SunCmpMapping aVal = (SunCmpMapping) mapOfMappingIter.next();
            retVal.addSunCmpMapping(aVal);
        }
        return retVal;
    }

    /** Set fetchgroup in schema2beans FetchedWith bean
     * @param cfm An object that represents HasFetchedWith object
     * in schema2beans
     * @param fgVal integer that represents fetch group value
     */
    private void setFetchedWith(HasFetchedWith cfm, int fgVal) {
        FetchedWith fw = new FetchedWith();
        if (fgVal <= MappingFieldElement.GROUP_INDEPENDENT) {
            String key = "IndependentFetchGroup"+fgVal; //NOI18N
            fw.setNamedGroup(key);
        }
        else if (fgVal == MappingFieldElement.GROUP_DEFAULT) {
            fw.setDefault(true);
        }
        else if (fgVal > MappingFieldElement.GROUP_DEFAULT) {
            fw.setLevel(fgVal-1);
        }
        else if (fgVal == MappingFieldElement.GROUP_NONE) {
            fw.setNone(true);
        }
        cfm.setFetchedWith(fw);
    }

    /** Convert a bean's cmp-field-mapping and cmr-field-mapping elements into
     * mapping model objects.
     *
     * The method can be called in two distinct cases:
     *
     * 1. At deployment time. The mapping must be complete enough to create a
     * valid mapping model, in order to support execution.
     *
     * 2. During mapping development.  The mapping may be incomplete, but the
     * model objects that are created need to be as consistent as possible.
     * The absence of data should not be fatal, if possible.
     *
     * @param mapping The schema2beans object that represents the mapping
     * for a particular bean.
     * @param schemaArg The name of the schema that the bean is mapped against.
     * @return The MappingClassElement that corresponds to the
     * schema2beans object.
     * @throws DBException
     * @throws ModelException
     * @throws ConversionException
     */
    private MappingClassElement mapFieldsOfBean(EntityMapping mapping,
                                                String schemaArg)
        throws DBException, ModelException, ConversionException {

        String beanName = mapping.getEjbName();
        MappingClassElement mce = null;
        List tablesOfBean = new ArrayList();
        Map knownTables = new HashMap();

        if (logger.isLoggable(Logger.FINE))
            logger.fine(
                I18NHelper.getMessage(
                messages,
                "MESSAGE_START_BEAN", //NOI18N
                beanName));

        String jdoClassName = helper.getMappedClassName(beanName);
        if (logger.isLoggable(Logger.FINE))
            logger.fine(
                I18NHelper.getMessage(
                messages,
                "MESSAGE_JDOCLASS_NAME", //NOI18N
                beanName, jdoClassName));

        if (null == jdoClassName) {
            throw new ConversionException(
                I18NHelper.getMessage(
                messages,
                "ERR_INVALID_CLASS", //NOI18N
                beanName));
        }

        // create the mapping class element and its children
        PersistenceClassElementImpl persistElImpl =
            new PersistenceClassElementImpl(jdoClassName);
        persistElImpl.setKeyClass(jdoClassName+".Oid"); //NOI18N
        mce = new MappingClassElementImpl(
            new PersistenceClassElement(persistElImpl));

        SchemaElement schema = null;

        // Assign the schema
        if (!StringHelper.isEmpty(schemaArg))
            schema = setDatabaseRoot(mce, schemaArg,
                helper.ensureValidation());

        // Map the table information
        // Ensure the bean is mapped to a primary table.
        if (!StringHelper.isEmpty(mapping.getTableName())) {

            mapPrimaryTable(mapping, mce,
                schema, knownTables, tablesOfBean);
            mapSecondaryTables(mapping, mce,
                schema, knownTables, tablesOfBean);
        }

        ColumnElement candidatePK = null;

        // map the simple fields.
        candidatePK = mapNonRelationshipFields(mapping, mce,
            beanName, schema, knownTables);

        createMappingForUnknownPKClass(mapping, mce,
            beanName, candidatePK);

        createMappingForConsistency(mapping, mce,
            beanName, knownTables);

        // map the relationship fields.
        mapRelationshipFields(mapping, mce,
            beanName, schema, knownTables, tablesOfBean);

        // map any unmapped fields.
        mapUnmappedFields(mce, beanName);

        return mce;
    }

    /** Create field mapping for unknown primary key
     * @param mapping The schema2beans object that represents the mapping
     * for a particular bean.
     * @param mce Mapping class element
     * @param beanName Bean name
     * @param candidatePK A ColumnElement object which is a candidate column for
     * mapping to primary key field if it is unknown primary key
     * @throws ModelException
     * @throws ConversionException
     */
    private void createMappingForUnknownPKClass(EntityMapping mapping,
                                                MappingClassElement mce,
                                                String beanName,
                                                ColumnElement candidatePK)
        throws ModelException, ConversionException {
        String primaryTableName = mapping.getTableName();

        if (helper.generateFields()
            && helper.applyDefaultUnknownPKClassStrategy(beanName)) {
            if (null != candidatePK) {
                String fieldName = helper.getGeneratedPKFieldName();

                // Fix later. Since mapping and persistence classes in the
                // cache are only skeletons and not the one we want,
                // put pce in a map at pce creation time for persistence
                // class look up later to avoid a cast
                // to MappingClassElementImpl.
                PersistenceFieldElement pfe = createPersistenceField(mce,
                    fieldName);
                pfe.setKey(true);
                MappingFieldElement mfe = createMappingField(mce, fieldName,
                    candidatePK);
            }
            else {
                // There is no column which meets primary key criteria.
                // Report error.
                throw new ConversionException(
                    I18NHelper.getMessage(
                    messages,
                    "WARN_NO_PKCOLUMN", //NOI18N
                    primaryTableName));
            }
        }
    }

    /** Load the consistency information. if it is version consistency, create
     * field mapping for version columns.
     * @param mapping The schema2beans object that represents the mapping
     * for a particular bean.
     * @param mce Mapping class element
     * @param beanName Bean name
     * @param knownTables A Map which contains primary and secondary tables
     * for beans in the set. Keys: table names Values: TableElement objects
     * @throws ModelException
     * @throws DBException
     * @throws ConversionException
     */
    private void createMappingForConsistency(EntityMapping mapping,
                                             MappingClassElement mce,
                                             String beanName,
                                             Map knownTables)
        throws ModelException, DBException, ConversionException {

        // load the consistency information
        List versionColumns = loadConsistency(mapping, mce);

        // If the consistency level is version consistency, the version field
        // is always created, independent of the ConversionHelper's value for
        // generateFields.  This is because a generated version field is
        // needed to hold the version column information.
        if (versionColumns != null) {

            String primaryTableName = mapping.getTableName();

            // For 8.1 release, we only support one version column per bean
            if (versionColumns.size() > 1) {
                throw new ConversionException(
                    I18NHelper.getMessage(
                    messages,
                    "ERR_INVALID_VERSION_COLUMNS")); //NOI18N
            }
            String versionColumn = (String)versionColumns.get(0);

            String sourceTableName = getTableName(versionColumn,
                    primaryTableName);

            // we do not support version column from secondary table
            // in 8.1 release
            if (!sourceTableName.equals(primaryTableName)) {
                throw new ConversionException(
                    I18NHelper.getMessage(
                    messages,
                    "WARN_VERSION_COLUMN_INVALID_TABLE", //NOI18N
                    primaryTableName, beanName, versionColumn));
            }

            TableElement sourceTableEl = (TableElement) knownTables.get(
                    sourceTableName);
            String versionColumnName = getColumnName(versionColumn);
            ColumnElement versionCol = getColumnElement(sourceTableEl,
                    DBIdentifier.create(versionColumnName), helper);

            if (null != versionCol) {

                // Since 8.1 release only support one version column per bean,
                // use prefix as the version  field name
                String fieldName = helper.getGeneratedVersionFieldNamePrefix();
                PersistenceFieldElement pfe = createPersistenceField(mce,
                    fieldName);
                MappingFieldElement mfe = createMappingField(mce, fieldName,
                    versionCol);
                mfe.setVersion(true);
            }
            else {
                // There is no version column.
                // Report error.
                throw new ConversionException(
                    I18NHelper.getMessage(
                    messages,
                    "WARN_VERSION_COLUMN_MISSING", //NOI18N
                    primaryTableName, beanName));
            }
        }
    }

    /** Map simple cmp field to MappingFieldElement
     * @param mapping The schema2beans object that represents the mapping
     * for a particular bean.
     * @param mce Mapping class element
     * @param beanName Bean name
     * @param schema dbschema information for all beans
     * @param knownTables A Map which contains primary and secondary tables
     * for beans in the set. Keys: table names Values: TableElement objects
     * @return candidate primary key column for unknown
     * primary key class mapping, if applicable
     * @throws DBException
     * @throws ModelException
     * @throws ConversionException
     */
    private ColumnElement mapNonRelationshipFields(EntityMapping mapping,
                                                   MappingClassElement mce,
                                                   String beanName,
                                                   SchemaElement schema,
                                                   Map knownTables)
        throws DBException, ModelException, ConversionException {

        CmpFieldMapping [] mapOfFields = mapping.getCmpFieldMapping();
        String primaryTableName = mapping.getTableName();
        ColumnElement candidatePK = null;

        // get candidate column only used for unknown primary key
        if (helper.generateFields()
            && helper.applyDefaultUnknownPKClassStrategy(beanName)) {

            candidatePK = getCandidatePK(schema, primaryTableName);
        }

        for (int i = 0; i < mapOfFields.length; i++) {
            CmpFieldMapping mapForOneField = mapOfFields[i];

            String fieldName = mapForOneField.getFieldName();
            if (!validateField(mce, beanName, fieldName,
                helper.ensureValidation())) {
                if (logger.isLoggable(Logger.FINE))
                    logger.fine(
                        I18NHelper.getMessage(
                        messages,
                        "WARN_INVALID_FIELD", //NOI18N
                        beanName, fieldName));

                continue;
            }

            String columnNames[] = mapForOneField.getColumnName();
            MappingFieldElement mfe = createUnmappedField(mce, beanName,
                fieldName);
            boolean fieldMappedToABlob = false;

            for (int j = 0; j < columnNames.length; j++) {
                String sourceTableName = getTableName(columnNames[j],
                    primaryTableName);
                if (null == sourceTableName) {
                    throw new ConversionException(
                        I18NHelper.getMessage(
                        messages,
                        "ERR_UNDEFINED_TABLE")); //NOI18N
                }

                String sourceColumnName = getColumnName(columnNames[j]);

                // get the table element
                TableElement sourceTableEl = getTableElement(sourceTableName,
                    knownTables, schema);
                ColumnElement aCol = getColumnElement(sourceTableEl,
                    DBIdentifier.create(sourceColumnName), helper);
                if (logger.isLoggable(Logger.FINE))
                    logger.fine(
                        I18NHelper.getMessage(
                        messages,
                        "MESSAGE_ADD_COLUMN", //NOI18N
                        new Object [] {aCol, fieldName}));

                // If candidatePK is mapped to a field, then it can not
                // treated as a primary key for unknown primary key
                if (helper.generateFields()
                    && helper.applyDefaultUnknownPKClassStrategy(beanName)) {
                    if (candidatePK != null && candidatePK.equals(aCol)) {
                        candidatePK = null;
                    }
                }

                fieldMappedToABlob |= aCol.isBlobType();
                mfe.addColumn(aCol);
            }

            FetchedWith fw = mapForOneField.getFetchedWith();
            setFetchGroup(fw, mfe, beanName, fieldMappedToABlob);
            mce.addField(mfe);
        }

        return candidatePK;
    }

    /**
     * Converts entity mapping relationship information from sun-cmp-mappings.xml
     * into JDO mapping file information.
     *
     * @param mapping The schema2beans object that represents the mapping
     * for a particular bean.
     * @param mce Mapping class element being populated corresponding to the bean.
     * @param beanName Bean name.
     * @param knownTables A Map which contains primary and secondary tables
     * for beans in the set. Keys: table names Values: TableElement objects
     * @param tablesOfBean contains primary table and
     * secondary tables of the bean <code>beanName</code>.
     * @throws ModelException
     * @throws DBException
     * @throws ConversionException
     */
    private void mapRelationshipFields(EntityMapping mapping,
                                       MappingClassElement mce,
                                       String beanName,
                                       SchemaElement schema,
                                       Map knownTables,
                                       List tablesOfBean)
        throws ModelException, DBException, ConversionException {

        String primaryTableName = mapping.getTableName();
        CmrFieldMapping mapOfRelations[] = mapping.getCmrFieldMapping();
        PersistenceClassElement pce = ((MappingClassElementImpl)mce).getPersistenceElement();

        for (int i = 0; mapOfRelations != null && i < mapOfRelations.length;
                i++) {
            CmrFieldMapping aRelation = mapOfRelations[i];
            String fieldName = aRelation.getCmrFieldName();

            if (!validateField(mce, beanName, fieldName, helper.ensureValidation())) {
                if (logger.isLoggable(Logger.FINE))
                    logger.fine(
                        I18NHelper.getMessage(
                        messages,
                        "WARN_INVALID_CMRFIELD", //NOI18N
                        beanName, fieldName));
                continue;
            }

            RelationshipElement rel = new RelationshipElement(
                new RelationshipElementImpl(fieldName), pce);
            MappingRelationshipElement mre =
                new MappingRelationshipElementImpl(fieldName, mce);

            // Register the inverse RelationshipElement
            registerInverseRelationshipElement(rel, beanName, fieldName);

            final ColumnPair pairs[] = aRelation.getColumnPair();
            final String relationId = mapping.getEjbName() + "_Relationship_" + i; //NOI18N
            Collection primaryTableColumns = convertToColumnPairElements(
                    pairs,
                    primaryTableName, schema, knownTables, tablesOfBean,
                    relationId,
                    mre);

            setUpperBound(rel, beanName, fieldName);
            setCascadeDeleteAction(rel, beanName, fieldName);
            setLowerBound(rel,
                    primaryTableColumns,
                    primaryTableName,schema, knownTables,
                    beanName, fieldName);
            setFetchGroup(aRelation.getFetchedWith(), mre, beanName, false);

            pce.addField(rel);
            mce.addField(mre);
        }
    }

    /** Map unmapped field to mapping field object.
     * @param mce Mapping class element
     * @param beanName Bean name
     * @throws ModelException
     */
    private void mapUnmappedFields(MappingClassElement mce, String beanName)
        throws ModelException {

        Object[] fields = helper.getFields(beanName);
        if (!helper.ensureValidation()) {
            for (int i = 0; i < fields.length; i++) {
                String fieldName = (String) fields[i];
                MappingFieldElement mfe = mce.getField(fieldName);

                if (null == mfe) {

                    // this field needs a mapping created for it...
                    mfe = createUnmappedField(mce, beanName, fieldName);
                    mce.addField(mfe);
                }
            }
        }
    }

    /** Set consistency from MappingClassElement into schema2beans
     * Consistency bean.
     * @param mce An object that represents the mapping object
     * @param beanMapping The schema2beans object that represents the mapping
     * for a particular bean.
     */
    private void setConsistency(MappingClassElement mce,
        EntityMapping beanMapping) {
        int consistency = mce.getConsistencyLevel();
        if (MappingClassElement.NONE_CONSISTENCY != consistency) {
            Consistency c = new Consistency();
            if (MappingClassElement.LOCK_WHEN_MODIFIED_CHECK_ALL_AT_COMMIT_CONSISTENCY == consistency) {
                c.setLockWhenModified(true);
                c.setCheckAllAtCommit(true);
            }
            if (MappingClassElement.LOCK_WHEN_MODIFIED_CONSISTENCY == consistency)
                c.setLockWhenModified(true);
            if (MappingClassElement.CHECK_ALL_AT_COMMIT_CONSISTENCY == consistency)
                c.setCheckAllAtCommit(true);
            if (MappingClassElement.LOCK_WHEN_LOADED_CONSISTENCY  == consistency)
                c.setLockWhenLoaded(true);
            if (MappingClassElement.CHECK_MODIFIED_AT_COMMIT_CONSISTENCY == consistency)
                c.setCheckModifiedAtCommit(true);
            if (MappingClassElement.VERSION_CONSISTENCY == consistency) {
                CheckVersionOfAccessedInstances versionIns =
                    new CheckVersionOfAccessedInstances();
                Iterator iter = mce.getVersionFields().iterator();
                while (iter.hasNext()) {
                    List columnNames = ((MappingFieldElement)iter.next()).
                        getColumns();

                    // vesion field only allow to map to one column
                    if (columnNames != null && columnNames.size() > 0)
                        versionIns.addColumnName((String)columnNames.get(0));
                }
                c.setCheckVersionOfAccessedInstances(versionIns);
            }
            beanMapping.setConsistency(c);
        }
    }

    /** Load consistency from schema2beans into MappingClassElement
     * @param mapping The schema2beans object that represents the mapping
     * for a particular bean.
     * @param mce An object that represents mapping object
     * @return a list of version column names if applicable; return null
     * otherwise.
     * @throws ModelException
     * @throws ConversionException
     */
    private List loadConsistency(EntityMapping mapping, MappingClassElement mce)
        throws ModelException, ConversionException {
        Consistency c = mapping.getConsistency();
        if (null == c) {
            mce.setConsistencyLevel(MappingClassElement.NONE_CONSISTENCY);
        }
        else {
            CheckVersionOfAccessedInstances versionIns =
                (CheckVersionOfAccessedInstances)
                c.getCheckVersionOfAccessedInstances();

            if (c.isCheckModifiedAtCommit())
                mce.setConsistencyLevel(
                    MappingClassElement.CHECK_MODIFIED_AT_COMMIT_CONSISTENCY);
            else if(c.isLockWhenLoaded())
                mce.setConsistencyLevel(
                    MappingClassElement.LOCK_WHEN_LOADED_CONSISTENCY);
            else if(c.isCheckAllAtCommit())
                mce.setConsistencyLevel(
                    MappingClassElement.CHECK_ALL_AT_COMMIT_CONSISTENCY);
            else if(c.isLockWhenModified())
                mce.setConsistencyLevel(
                    MappingClassElement.LOCK_WHEN_MODIFIED_CONSISTENCY);
            else if(c.isLockWhenModified() && c.isCheckAllAtCommit())
                mce.setConsistencyLevel(MappingClassElement.
                    LOCK_WHEN_MODIFIED_CHECK_ALL_AT_COMMIT_CONSISTENCY);
            else if(c.isNone())
                mce.setConsistencyLevel(MappingClassElement.NONE_CONSISTENCY);
            else if (versionIns != null) {
                mce.setConsistencyLevel(MappingClassElement.VERSION_CONSISTENCY);
                List versionColumns = new ArrayList();
                for (int i = 0; i < versionIns.sizeColumnName(); i++) {
                    versionColumns.add(versionIns.getColumnName(i));
                }
                return versionColumns;
            }
            else {
                throw new ConversionException(
                    I18NHelper.getMessage(
                    messages,
                    "ERR_INVALID_CONSISTENCY_VALUE", mce)); //NOI18N
            }
        }
        return null;
    }

    /** Map the primary table of the bean.
     * @param mapping The schema2beans object that represents the mapping
     * for a particular bean.
     * @param mce Mapping class element
     * @param schema dbschema information for all beans
     * @param knownTables A Map which contains primary and secondary tables
     * for beans in the set. Keys: table names Values: TableElement objects
     * @param tablesOfBean contains primary table and
     * secondary tables of the bean corresponding to the <code>mapping</code>
     * @throws DBException
     * @throws ModelException
     * @throws ConversionException
     */
    private void mapPrimaryTable(EntityMapping mapping,
                                  MappingClassElement mce,
                                  SchemaElement schema,
                                  Map knownTables,
                                  List tablesOfBean)
        throws DBException, ModelException, ConversionException {

        String primaryTableName = mapping.getTableName();
        TableElement primTabEl = getTableElement(primaryTableName, knownTables, schema);

        mce.addTable(primTabEl);
        tablesOfBean.add(primaryTableName);
    }

    /** Get the candidate pk column for unknown primary key.
     * @param schema dbschema information for all beans
     * @param primaryTableName Primary table name
     * @return candidate pk column which will be used for unknown primary key,
     * if applicable
     * @throws DBException
     * @throws ConversionException
     */
    private ColumnElement getCandidatePK(SchemaElement schema,
                                         String primaryTableName)
        throws DBException, ConversionException {
        ColumnElement candidatePK = null;

        TableElement primTabEl = getTableElement(schema,
            DBIdentifier.create(primaryTableName), helper);
        // Check if the candidatePK is really satisfying primary key
        // criteria. It will be used only for unknown primary key.
        UniqueKeyElement uke = primTabEl.getPrimaryKey();

        if (null != uke) {
            ColumnElement cols[] = uke.getColumns();
            if (null != cols && 1 == cols.length) {
                candidatePK = cols[0];
                if (logger.isLoggable(Logger.FINE))
                    logger.fine(
                        I18NHelper.getMessage(
                        messages,
                        "MESSAGE_CANDIDATE_PK", //NOI18N
                        candidatePK.getName()));

                Integer pre = candidatePK.getPrecision();
                if (null != candidatePK && !candidatePK.isNumericType()) {
                    candidatePK = null;
                }
                if (null != candidatePK && (null != pre) && pre.intValue() < MINIMUM_PRECISION) {
                    candidatePK = null;
                }
            }
        }
        return candidatePK;
    }

    /**
     * Converts column pair information from sun-cmp-mappings.xml into
     * column pair elements from the JDO mapping definition.
     *
     * @param pairs ColumnPair information from sun-cmp-mappings.xml.
     * @param primaryTableName Name of the bean's primary table.
     * @param schema dbschema information for all beans.
     * @param knownTables A Map which contains primary and secondary tables
     * for beans in the set. Keys: table names Values: TableElement objects
     * @param tablesOfBean contains primary table and
     * secondary tables of the bean corresponding to the <code>mre</code>'s
     * declaring class.
     * @param relationId Relationship id used to name the ColumnPairElements.
     * @param mre Mapping relationship element (== JDO information).
     * @return Collection of column elements, including all columns from the
     * current cmr definition that are part of the primary table.
     * @throws DBException
     * @throws ModelException
     * @throws ConversionException
     */
    private Collection convertToColumnPairElements(ColumnPair pairs[],
                                                   String primaryTableName,
                                                   SchemaElement schema,
                                                   Map knownTables,
                                                   List tablesOfBean,
                                                   String relationId,
                                                   MappingRelationshipElement mre)
            throws DBException, ModelException, ConversionException {

        Collection primaryTableColumns = new ArrayList();
        boolean isJoin = false;

        for (int i = 0; null != pairs && i < pairs.length; i++) {
            ColumnPair pair = pairs[i];
            ColumnPairElement cpe = new ColumnPairElement();
            boolean localSet = false;

            cpe.setName(DBIdentifier.create(relationId + "_ColumnPair_" + i)); //NOI18N

            for (int j = 0; j < 2; j++) {
                String columnName = pair.getColumnName(j);
                String sourceTableName = getTableName(columnName,
                    primaryTableName);
                String sourceColumnName = getColumnName(columnName);
                TableElement sourceTableEl = getTableElement(sourceTableName,
                    knownTables, schema);
                ColumnElement ce = getColumnElement(sourceTableEl,
                    DBIdentifier.create(sourceColumnName), helper);
                ce.toString();

                // tablesOfBean stores the primary table and the secondary
                // tables for the bean.
                // It can be used for checking join table if sourceTableName
                // is not in the tablesOfBean.
                // If it is join table, should use addLocalColumn instead of
                // addColumn.
                if (j == 0) {
                    if (tablesOfBean.contains(sourceTableName)) {
                        localSet = true;

                        // Remember local columns for lower bound determination.
                        primaryTableColumns.add(ce);
                    }
                    else {

                        // join table
                        isJoin = true;
                        localSet = false;
                    }
                }

                if (cpe.getLocalColumn() == null) {
                    cpe.setLocalColumn(ce);
                }
                else {
                    cpe.setReferencedColumn(ce);
                }
            }

            if (localSet) {
                if (!isJoin) {
                    mre.addColumn(cpe);
                }
                else {
                    mre.addLocalColumn(cpe);
                }
            }
            else if (isJoin) {
                mre.addAssociatedColumn(cpe);
            }
        }

        return primaryTableColumns;
    }

    /**
     * If a non-collection relationship field is mapped to a
     * non-nullable column, the lower bound of the relationship might
     * be set to 1. To set the lower bound, we have to determine the
     * dependent side of the relationship. We set the lower bound to 1
     * based on the following rules:
     *
     * <ul>
     * <li>If the non-nullable column is not part of the primary key.</li>
     * <li>If the local side has got a foreign key.</li>
     * <li>If the local columns are a real subset of the primary key.</li>
     * <li>If the user specified the local side for cascade delete.</li>
     * </ul>
     *
     * @param rel JDO relationship information being constructed.
     * @param primaryTableColumns  Collection of all columns from the
     * current cmr definition that are part of the primary table.
     * @param primaryTableName Name of the bean's primary table.
     * @param knownTables A Map which contains primary and secondary tables
     * for beans in the set. Keys: table names Values: TableElement objects
     * @param schema dbschema information for all beans.
     * @param beanName Bean name.
     * @param fieldName Relationship field name.
     * @throws ModelException
     * @throws DBException
     * @throws ConversionException
     */
    private void setLowerBound(RelationshipElement rel,
                               Collection primaryTableColumns,
                               String primaryTableName,
                               SchemaElement schema,
                               Map knownTables,
                               String beanName,
                               String fieldName)
                               throws ModelException, DBException,
                               ConversionException {

        rel.setLowerBound(0);
        if (logger.isLoggable(Logger.FINE))
            logger.fine(
                I18NHelper.getMessage(
                messages,
                "MESSAGE_LWB_NULL", //NOI18N
                beanName, fieldName));

        if (1 == rel.getUpperBound() && null != primaryTableName) {

            boolean isPartOfPrimaryKey = false;
            TableElement primaryTable = getTableElement(primaryTableName,
                knownTables, schema);
            UniqueKeyElement pk = primaryTable.getPrimaryKey();
            ForeignKeyElement fks[] = primaryTable.getForeignKeys();
            Iterator iter = primaryTableColumns.iterator();

            while (iter.hasNext() && 0 == rel.getLowerBound()) {
                ColumnElement ce = (ColumnElement) iter.next();
                if (!ce.isNullable()) {
                    isPartOfPrimaryKey |= isPartOfPrimaryKey(ce, pk);

                    if (!isPartOfPrimaryKey) {
                        // We suppose that all primary key columns are non-nullable.
                        // If the non-nullable column is not part of the primary key,
                        // this is the dependent side.
                        rel.setLowerBound(1);
                        if (logger.isLoggable(Logger.FINE))
                            logger.fine(
                                I18NHelper.getMessage(
                                messages,
                                "MESSAGE_LWB_NOPK", //NOI18N
                                beanName, fieldName));
                    }
                    // Check the foreign key constraint
                    else if (isPartOfForeignKey(ce, fks)) {
                        // If the non-nullable column is part of the foreign key,
                        // this is the dependent side.
                        rel.setLowerBound(1);
                        if (logger.isLoggable(Logger.FINE))
                            logger.fine(
                                I18NHelper.getMessage(
                                messages,
                                "MESSAGE_LWB_FK", //NOI18N
                                beanName, fieldName));
                    }
                }
            }

            if (0 == rel.getLowerBound() && isPartOfPrimaryKey) {
                // The lower bound is still unset and all local columns
                // are part of the primary key.
                if (primaryTableColumns.size() < pk.getColumns().length) {
                    // The local columns are a real subset of the primary key.
                    // ==> This must be the dependent side.
                    rel.setLowerBound(1);
                    if (logger.isLoggable(Logger.FINE))
                        logger.fine(
                            I18NHelper.getMessage(
                            messages,
                            "MESSAGE_LWB_PKSUBSET", //NOI18N
                            beanName, fieldName));
                }
                else if (isCascadeDelete(beanName, fieldName)) {
                    // This relationship side is marked as dependent side by the user.
                    rel.setLowerBound(1);
                    if (logger.isLoggable(Logger.FINE))
                        logger.fine(
                            I18NHelper.getMessage(
                            messages,
                            "MESSAGE_LWB_CASCADE", //NOI18N
                            beanName, fieldName));
                }
                else {
                    if (logger.isLoggable(Logger.FINE))
                        logger.fine(
                            I18NHelper.getMessage(
                            messages,
                            "MESSAGE_LWB_NODEPENDENT", //NOI18N
                            beanName, fieldName));
                }
            }
        }
    }

    /**
     * Looks up the table element for <code>tableName</code> in the
     * table element cache <code>knownTables</code>. If the table
     * element is not found, the table name is looked up in the
     * database schema and registered in the cache.
     *
     * @param tableName Table name.
     * @param knownTables A Map which contains primary and secondary tables
     * for beans in the set. Keys: table names Values: TableElement objects
     * @param schema dbschema information for all beans.
     * @return Table element for table <code>tableName</code>.
     * @exception DBException
     * @exception ConversionException
     */
    private TableElement getTableElement(String tableName,
                                         Map knownTables,
                                         SchemaElement schema)
        throws DBException, ConversionException {

        TableElement te = (TableElement) knownTables.get(tableName);

        if (null == te) {
            te = getTableElement(schema, DBIdentifier.create(tableName),
                helper);
            knownTables.put(tableName, te);
        }

        return te;
    }

    /**
     * Checks, if the column <code>ce</code> is part of the
     * primary key <code>pk</code>.
     * RESOLVE: Method isPrimaryKeyColumn in ModelValidator
     * implements similar functionality.
     *
     * @param ce Column element for the column to be tested.
     * @param pk Primary key element. The column to be tested
     * <b>must</b> be defined in the same table.
     * @return True, if the column is part of the primary key,
     * false otherwise.
     * @see com.sun.jdo.api.persistence.model.util.ModelValidator
     */
    private boolean isPartOfPrimaryKey(ColumnElement ce, UniqueKeyElement pk) {
        return null != pk && ce.equals(pk.getColumn(ce.getName()));
    }

    /**
     * Checks, if the column <code>ce</code> is part of one
     * of the foreign key constraints defined in <code>fks</code>.
     * RESOLVE: Method matchesFK in ModelValidator implements similar
     * functionality.
     *
     * @param ce Column element for the column to be tested.
     * @param fks Array of foreign key elements. The column to be
     * tested <b>must</b> be defined in the same table.
     * @return True, if the column is part of one of the foreign keys,
     * false otherwise.
     * @see com.sun.jdo.api.persistence.model.util.ModelValidator
     */
    private boolean isPartOfForeignKey(ColumnElement ce,
        ForeignKeyElement[] fks) {

        // RESOLVE: Column ce might be included in multiple foreign
        // keys. The foreign key check applies to ALL relationships
        // mapped to column ce. How can we find out, that a foreign
        // key matches exactly the relationship being checked here?

        if (fks != null) {
            for (int index = 0; index < fks.length; index++) {
                if (ce.equals(fks[index].getColumn(ce.getName()))) {
                    // The current ce is part of the foreign key.
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns the cascade delete setting for the current relationship side.
     * The ConversionHelper interface provides cascade delete information
     * for the related side only.
     *
     * @param beanName Bean name.
     * @param fieldName Relationship field name.
     * @return True, if the current relationship side is marked for cascade delete,
     * false otherwise.
     */
    private boolean isCascadeDelete(String beanName, String fieldName) {
        final String beanInField = helper.getRelationshipFieldContent(beanName,             fieldName);
        final String inverseField = helper.getInverseFieldName(beanName,
            fieldName);

        return (null != beanInField && null != inverseField) ?
                helper.relatedObjectsAreDeleted(beanInField, inverseField) : false;
    }

    /**
     * Registers relationship element <code>rel</code> in the cache
     * mapping field names to inverse relationship elements. The
     * inverse bean- and field names for the registration are
     * determined with the conversion helper.  Returns the
     * relationship element for the inverse field, if this field has
     * been already processed, null otherwise.
     *
     * @param rel JDO relationship information being constructed.
     * @param beanName Bean name.
     * @param fieldName Relationship field name.
     * @return The relationship element for the inverse field, if this field
     * has been already processed, null otherwise.
     * @throws ModelException
     */
    private RelationshipElement registerInverseRelationshipElement(
        RelationshipElement rel, String beanName, String fieldName)
        throws ModelException {

        String key = beanName + "." + fieldName; //NOI18N
        RelationshipElement inverse = (RelationshipElement) inverseRelationships.get(key);

        if (null == inverse) {
            final String beanInField = helper.getRelationshipFieldContent(
                beanName, fieldName);
            final String inverseField = helper.getInverseFieldName(beanName,
                fieldName);

            if (null != beanInField && null != inverseField) {
                key = beanInField + "." + inverseField; //NOI18N
                inverseRelationships.put(key, rel);
            }
        }
        else {
            rel.changeInverseRelationship(inverse);
            inverse.changeInverseRelationship(rel);
        }

        return inverse;
    }

    /**
     * Sets the upper bound for relationship element <code>rel</code>
     * depending on the upper bound information from the deployment
     * descriptor and defines the element class for collection
     * relationship fields.
     *
     * @param rel JDO relationship information being constructed.
     * @param beanName Bean name.
     * @param fieldName Relationship field name.
     * @throws ModelException
     * @throws ConversionException
     */
    private void setUpperBound(RelationshipElement rel,
                               String beanName, String fieldName)
        throws ModelException, ConversionException {

        String beanInField = helper.getRelationshipFieldContent(beanName,
            fieldName);
        String classInJdoField = helper.getMappedClassName(beanInField);
        String multiplicity = helper.getMultiplicity(beanName, fieldName);

        // Set the upper bound.
        if (multiplicity.equals(helper.MANY)) {
            rel.setUpperBound(Integer.MAX_VALUE);
            rel.setElementClass(classInJdoField);
            String collectionClass = helper.getRelationshipFieldType(beanName,
                fieldName);
            if (types.contains(collectionClass)) {
                rel.setCollectionClass(collectionClass);
            }
            else {
                rel.setCollectionClass(null);
                if (logger.isLoggable(Logger.WARNING))
                    logger.warning(
                        I18NHelper.getMessage(
                        messages,
                        "WARN_INVALID_RELATIONSHIP_FIELDTYPE", //NOI18N
                        beanName, fieldName, collectionClass));
            }
        }
        else if (multiplicity.equals(helper.ONE)) {
            rel.setUpperBound(1);
            // Fix later. This code should be removed because in one side
            // setElementClass should not be called.
            // This line of code is for bug 4665051 which is plugin bug.
            // It is likely that bug 4665051 indicates that there is code
            // in the plugin which depends on the element class being set
            // for a one side relationship.
            rel.setElementClass(classInJdoField);
        }
        else {
            throw new ConversionException(
                I18NHelper.getMessage(
                messages,
                "ERR_BAD_MULTIPLICTY", //NOI18N
                multiplicity, rel.getName()));
        }
    }

    /**
     * Sets the cascade delete option for relationship element
     * <code>rel</code> depending on the information from the
     * deployment descriptor.  While the deployment descriptor
     * specifies cascade delete option on the dependent side, JDO
     * specifies it on the primary side.  For this reason, the
     * information must be "switched".
     *
     * @param rel JDO relationship information being constructed.
     * @param beanName Bean name.
     * @param fieldName Relationship field name.
     * @throws ModelException
     */
    private void setCascadeDeleteAction(RelationshipElement rel,
                                        String beanName, String fieldName)
            throws ModelException {

        if (helper.relatedObjectsAreDeleted(beanName, fieldName)) {
            if (logger.isLoggable(Logger.FINE))
                logger.fine(
                    I18NHelper.getMessage(
                    messages,
                    "MESSAGE_REL_OBJ_DEL", //NOI18N
                    beanName, fieldName));
            rel.setDeleteAction(RelationshipElement.CASCADE_ACTION);
        }
    }

    private SchemaElement setDatabaseRoot(MappingClassElement foo,
        String schemaElementValue, boolean strict)
        throws ModelException, DBException, ConversionException {
        SchemaElement bar = null;
        if (null != classLoader) {
            if (loadedSchema.get(schemaElementValue) == null) {
                SchemaElement.removeFromCache(schemaElementValue);
                loadedSchema.put(schemaElementValue, schemaElementValue);
            }
            bar = SchemaElement.forName(schemaElementValue,classLoader);
        }
        else
            bar = SchemaElement.forName(schemaElementValue);
        if (strict) {
            if (bar == null) {
                // Prepare for a schema related error
                throw new ConversionException(
                    I18NHelper.getMessage(
                    messages,
                    "ERR_CANNOT_FIND_SCHEMA", //NOI18N
                    new Object [] {schemaElementValue, classLoader}));
            }
        }
        else {
            if (null == bar) {
                // conjure up a schema element and set its name...
                // need to create a dummy for invalid mappings because the
                // mapping model setter methods don't accept strings even
                // though that is what they store.
                bar = new SchemaElement();
                DBIdentifier n = DBIdentifier.create(schemaElementValue);
                n.setFullName(schemaElementValue);
                bar.setName(n);
            }
        }
        foo.setDatabaseRoot(bar);
        return bar;
    }

    private String getTableName(String columnName, String defaultName) {
        String retVal = defaultName;
        int len = columnName.lastIndexOf('.');
        if (len > 0)
            retVal = columnName.substring(0,len);
        return retVal;
    }

    private String getColumnName(String columnName) {
        String retVal = columnName;
        int len = columnName.lastIndexOf('.');
        if (len > 0)
            retVal = columnName.substring(len+1);
        return retVal;
    }

    /** Map the secondary tables of the bean.
     * @param mapping The schema2beans object that represents the mapping
     * for a particular bean.
     * @param mce Mapping class element
     * @param schema dbschema information for all beans
     * @param knownTables A Map which contains primary and secondary tables
     * for beans in the set. Keys: table names Values: TableElement objects
     * @param tablesOfBean contains primary table and
     * secondary tables of the bean corresponding to the <code>mapping</code>
     * @throws ModelException
     * @throws DBException
     * @throws ConversionException
     */
    private void mapSecondaryTables(EntityMapping mapping,
                                    MappingClassElement mce,
                                    SchemaElement schema,
                                    Map knownTables,
                                    List tablesOfBean)
        throws ModelException, DBException, ConversionException {

        SecondaryTable [] tableList = mapping.getSecondaryTable();
        List tl = mce.getTables();

        if (null != tl && tl.size() > 0 && null != tl.get(0)) {
            MappingTableElement primary = (MappingTableElement) tl.get(0);

            for (int i = 0; null != tableList && i < tableList.length; i++) {
                String tn = tableList[i].getTableName();
                if (StringHelper.isEmpty(tn))
                    continue;
                TableElement te = getTableElement(schema,
                    DBIdentifier.create(tn.trim()), helper);
                ColumnPair pairs[] = tableList[i].getColumnPair();
                int len = 0;
                if (null != pairs)
                    len = pairs.length;
                if (0 == len) {
                    if (logger.isLoggable(Logger.WARNING))
                        logger.warning(
                            I18NHelper.getMessage(
                            messages,
                            "WARN_NO_PAIRS", //NOI18N
                            new Object [] {mce, tn}));
                    continue;
                }
                MappingReferenceKeyElement mrke = mce.addSecondaryTable(
                    primary,te);
                for (int j = 0; null != pairs && j < pairs.length; j++) {
                    ColumnPairElement cpe = new ColumnPairElement();
                    DBIdentifier dbId = DBIdentifier.create("SecondaryTable"+j); //NOI18N
                    cpe.setName(dbId);
                    ColumnPair pair = pairs[j];
                    for (int k = 0; k < 2; k++) {
                        String nameOne = pair.getColumnName(k);
                        String sourceTableName = getTableName(
                            nameOne.trim(),
                            primary.getName().toString());
                        String sourceColumnName = getColumnName(nameOne);
                        dbId = DBIdentifier.create(sourceTableName);
                        TableElement sourceTableEl = getTableElement(schema,
                            dbId, helper);
                        dbId = DBIdentifier.create(sourceColumnName);
                        ColumnElement ce = getColumnElement(sourceTableEl,
                            dbId, helper);
                        if (k == 0)
                            cpe.setLocalColumn(ce);
                        else
                            cpe.setReferencedColumn(ce);
                    }
                    mrke.addColumnPair(cpe);
                }
                knownTables.put(tn,te);
                tablesOfBean.add(tn);
            }
        }
        else
            throw new ConversionException(
                    I18NHelper.getMessage(
                    messages,
                    "WARN_NOT_MAPPED_TO_PRIMARY", //NOI18N
                    mce.getName()));
    }

    private boolean validateField(MappingClassElement mce, String beanName,
        String fieldName, boolean throwEx) throws ConversionException {
        MappingFieldElement mfe = mce.getField(fieldName);
        if (null != mfe) {
            if (throwEx)
                throw new ConversionException(
                    I18NHelper.getMessage(
                    messages,
                    "ERR_FIELD_MAPPED_TWICE", //NOI18N
                    beanName, fieldName));
            else
                return false;
        }
        if (!helper.hasField(beanName,fieldName)) {
            if (throwEx)
                throw new ConversionException(
                I18NHelper.getMessage(
                messages,
                "ERR_INVALID_FIELD", //NOI18N
                beanName, fieldName));
            else
                return false;
        }
        return true;
    }

    // loop through the mappings to create a hash from bean name to em objects
    private Map getBean2EntityMappingMap(SunCmpMapping beanSet) {
        Map retVal = new HashMap();
        EntityMapping [] entityMappingsInSet = beanSet.getEntityMapping();
        int len = 0;
        if (null != entityMappingsInSet)
            len = entityMappingsInSet.length;
        for (int k = 0; k < len; k++) {
            EntityMapping anEntityMapping = entityMappingsInSet[k];
            String beanName = anEntityMapping.getEjbName();
            beanName.trim().charAt(0);
            retVal.put(beanName,anEntityMapping);
        }
        return retVal;
    }

    // for each cmr field in the mapping
    // determine is the inverse is a generated field
    // create the mapping data for this generated field
    private boolean completeCmrMappings(SunCmpMapping beanSet)
        throws ConversionException {

        // loop through the mappings to create a hash from bean name to em objects
        Map beanName2EntityMapping = getBean2EntityMappingMap(beanSet);
        Iterator emIter = beanName2EntityMapping.values().iterator();
        boolean retVal = false;
        String errorMsg = I18NHelper.getMessage(
            messages,
            "ERR_BAD_CONVERSION_HELPER"); //NOI18N

        while (emIter.hasNext()) {
            EntityMapping anEM = (EntityMapping) emIter.next();
            String beanName = anEM.getEjbName();
            String pt = anEM.getTableName();
            CmrFieldMapping[]  cmrsInEM = anEM.getCmrFieldMapping();
            int len = 0;
            if (null != cmrsInEM && !StringHelper.isEmpty(beanName))
                len = cmrsInEM.length;
            for (int i = 0; i < len; i++) {
                String fieldName = cmrsInEM[i].getCmrFieldName();
                if (!helper.hasField(beanName, fieldName)) {
                    throw new ConversionException(I18NHelper.getMessage(
                        messages,
                        "WARN_INVALID_CMRFIELD", //NOI18N
                        beanName, fieldName));
                } 
                fieldName.trim().charAt(0);
                String otherField = helper.getInverseFieldName(beanName,
                    fieldName);
                if (otherField == null) {
                    throw new ConversionException(errorMsg);
                }
                String otherBean = helper.getRelationshipFieldContent(beanName,
                    fieldName);
                if (otherBean == null) {
                    throw new ConversionException(errorMsg);
                }

                if (helper.isGeneratedRelationship(otherBean,otherField)) {
                    retVal = true;
                    String otherBeanName = helper.getRelationshipFieldContent(
                        beanName, fieldName);
                    otherBeanName.trim().charAt(0);
                    EntityMapping otherEM =
                        (EntityMapping) beanName2EntityMapping.get(
                            otherBeanName);
                    CmrFieldMapping inverseMapping = new CmrFieldMapping();
                    inverseMapping.setCmrFieldName(otherField);
                    inverseMapping.setColumnPair(
                        reverseCPArray(cmrsInEM[i].getColumnPair(), pt, 
                            beanName, fieldName));
                    otherEM.addCmrFieldMapping(inverseMapping);
                }
            }
        }
        return retVal;
    }

    private ColumnPair[] reverseCPArray(ColumnPair[] cpa, String primeTable,
        String beanName, String fieldName)
        throws ConversionException {
        int len = (cpa == null) ? 0 : cpa.length;
        if (len == 0) {
            throw new ConversionException(
                I18NHelper.getMessage(
                messages,
                "ERR_COLUMN_PAIR_MISSING", //NOI18N
                beanName, fieldName));
        }
        ColumnPair [] retVal = new ColumnPair[len];
        for (int index = 0; index < len; index++) {
            retVal[index] = new ColumnPair();
            retVal[index].addColumnName(
                qualify(primeTable,cpa[index].getColumnName(1)));
            retVal[index].addColumnName(
                qualify(primeTable,cpa[index].getColumnName(0)));
        }
        return retVal;
    }

    private String qualify(String tn, String cn) {
        int tmp = cn.indexOf('.');
        String retVal = cn;
        if (-1 == tmp)
            retVal = tn + "." + cn; // NOI18N
        return retVal;
    }

    private TableElement getTableElement(SchemaElement schema,
        DBIdentifier dbId, ConversionHelper helper)
        throws DBException, ConversionException {

        TableElement retVal = ((schema != null) ? 
            schema.getTable(dbId) : null);

        if (null == retVal && !helper.ensureValidation()) {
            // Need to create a dummy for invalid mappings because
            // the mapping model setter methods don't accept
            // strings even though that is what they store.
            // Create the table and add it to the knownTables list
            // for later

            retVal = new TableElement();
            retVal.setName(dbId);
            retVal.setDeclaringSchema(schema);
            org.netbeans.modules.dbschema.UniqueKeyElement tkey =
                new org.netbeans.modules.dbschema.UniqueKeyElement();
            ColumnElement fakeKeyCol = new ColumnElement();
            fakeKeyCol.setName(DBIdentifier.create(retVal.getName().getName()+ "."+"fookeyng")); //NOI18N

            // Type numeric=2
            fakeKeyCol.setType(2);
            fakeKeyCol.setPrecision(new Integer(MINIMUM_PRECISION));
            tkey.setPrimaryKey(true);
            tkey.addColumn(fakeKeyCol);
            retVal.addColumn(fakeKeyCol);
            retVal.addKey(tkey);
        }
        if (retVal == null) {
            throw new ConversionException(
                I18NHelper.getMessage(
                messages,
                "ERR_INVALID_TABLE", //NOI18N
                new Object [] {dbId.getName(), schema}));
        }
        return retVal;
    }

    private ColumnElement getColumnElement(TableElement sourceTableEl,
        DBIdentifier sourceColumnName, ConversionHelper helper)
        throws DBException, ConversionException {

        ColumnElement aCol = sourceTableEl.getColumn(sourceColumnName);
        if (null == aCol && !helper.ensureValidation()) {
            aCol = new ColumnElement();
            aCol.setName(DBIdentifier.create(sourceTableEl.getName().toString()+"."+sourceColumnName.toString())); // NOI18N
            aCol.setDeclaringTable(sourceTableEl);
            aCol.setNullable(true);
        }
        if (aCol == null) {
            throw new ConversionException(
                I18NHelper.getMessage(
                messages,
                "ERR_INVALID_COLUMN", //NOI18N
                new Object [] {sourceColumnName, sourceTableEl}));
        }
        return aCol;
    }

    private MappingFieldElement createUnmappedField(MappingClassElement mce,
        String beanName, String fieldName)
        throws ModelException {

        PersistenceClassElement pce = ((MappingClassElementImpl)mce).
            getPersistenceElement();
        PersistenceFieldElementImpl pfei =
            new PersistenceFieldElementImpl(fieldName);
        PersistenceFieldElement pfe =
            new PersistenceFieldElement(pfei, pce);

        pfe.setKey(helper.isKey(beanName,fieldName,false));
        pce.addField(pfe);
        MappingFieldElement mfe = new MappingFieldElementImpl(fieldName, mce);
        return mfe;
    }

    /**
     * Set fetch group level for mapping field and mapping relationship
     * element. If there is fetch level information, then set mapping field
     * element with the level. If there is no fetch group level information,
     * set mapping field element to GROUP_NONE if it is mapping relationship
     * element or blob type field otherwise set to GROUP_DEFAULT.
     * @param fw An object having fetch group level information
     * @param mfe An mapping field or mapping relationship element to be set
     * fetch group level
     * @param fieldMappedToABlob boolean type to indicate that field is blob
     * type or not
     * @throws ModelException
     * @throws ConversionException 
     */
    private void setFetchGroup(FetchedWith fw, MappingFieldElement mfe,
        String beanName, boolean fieldMappedToABlob)
        throws ModelException, ConversionException {
        if (null != fw) {
            boolean tryLevel = false;
            int level = 0;
            try {
                level = fw.getLevel();
                tryLevel = true;
            }
            catch (RuntimeException e) {
                // If there is no level set, schema2beans throws 
                // RuntimeException.
                // Need to do more investigation on why throws this exception. 
                // it is very likely that there is no level set, which would
                // throw an exception here..  Which we are going to ignore
            }
            if (tryLevel) {
                if (level < 1) {
                    throw new ConversionException(
                        I18NHelper.getMessage(
                        messages,
                        "ERR_INVALID_FG_LEVEL", //NOI18N
                        beanName, mfe.getName(), ""+level)); //NOI18N
                }
                mfe.setFetchGroup(level+1);
            }
            String ig = fw.getNamedGroup();
            if (null != ig) {
                Integer fgval = (Integer) namedGroups.get(ig);
                if (null == fgval) {
                    fgval = new Integer(groupCount--);
                    namedGroups.put(ig,fgval);
                }
                mfe.setFetchGroup(fgval.intValue());
            }
            if (fw.isNone())
                mfe.setFetchGroup(MappingFieldElement.GROUP_NONE);
            if (fw.isDefault())
                mfe.setFetchGroup(MappingFieldElement.GROUP_DEFAULT);
        }
        else {
            if (mfe instanceof MappingRelationshipElement)
                mfe.setFetchGroup(MappingFieldElement.GROUP_NONE);
            else {
                if (fieldMappedToABlob)
                    mfe.setFetchGroup(MappingFieldElement.GROUP_NONE);
                else
                    mfe.setFetchGroup(MappingFieldElement.GROUP_DEFAULT);
            }
        }
    }

    private PersistenceFieldElement createPersistenceField(
        MappingClassElement mce, String fieldName) throws ModelException {
        PersistenceClassElement pce =
            ((MappingClassElementImpl)mce).getPersistenceElement();
        PersistenceFieldElementImpl pfei =
            new PersistenceFieldElementImpl(fieldName);
        PersistenceFieldElement pfe =
            new PersistenceFieldElement(pfei, pce);
        pce.addField(pfe);
        return pfe;
    }

    private MappingFieldElement createMappingField(MappingClassElement mce,
        String fieldName, ColumnElement col) throws ModelException {
        MappingFieldElement mfe =
            new MappingFieldElementImpl(fieldName, mce);
        mce.addField(mfe);
        if (col != null)
            mfe.addColumn(col);
        return mfe;
    }
}

