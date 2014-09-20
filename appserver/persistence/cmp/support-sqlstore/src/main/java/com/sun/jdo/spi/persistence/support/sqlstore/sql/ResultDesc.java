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
 * ResultDesc.java
 *
 * Created on March 3, 2000
 *
 */


package com.sun.jdo.spi.persistence.support.sqlstore.sql;

import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import com.sun.jdo.spi.persistence.support.sqlstore.*;
import com.sun.jdo.spi.persistence.support.sqlstore.model.ClassDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.model.FieldDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.model.ForeignFieldDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.model.LocalFieldDesc;
import com.sun.jdo.spi.persistence.support.sqlstore.sql.generator.ColumnRef;
import com.sun.jdo.spi.persistence.utility.StringHelper;
import com.sun.jdo.spi.persistence.utility.FieldTypeEnumeration;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.glassfish.persistence.common.I18NHelper;

import java.io.*;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.*;


/**
 * This class is used by the store to materialize objects from a
 * JDBC resultset. Each ResultDesc binds values from the ResultSet
 * to instances of a persistence capable class.
 */
public class ResultDesc {

    /** List of ResultFieldDesc/ResultDesc. */
    private List fields;

    /** List of field names corresponding to <code>fields</code>. */
    private List fieldNames;

    /** Class descriptor. */
    private ClassDesc config;

    /** Indicates whether this ResultDesc is prefetching relationship fields. */
    private boolean prefetching;

    /** 
     * Maps ForeignFieldDesc to ResultDesc. The ForeignFieldDesc correspond to 
     * prefetched collection relationship fields. The ResultDesc is the 
     * associated result descriptor.
     */
    private Map prefetchedCollectionFields;

    /** The field that is the recipient of the value from this ResultDesc. */
    private ForeignFieldDesc parentField;

    /** Holds the projected local field. */
    private ResultFieldDesc fieldProjection;

    /** Result type for aggregate queries. */
    private int aggregateResultType = FieldTypeEnumeration.NOT_ENUMERATED;

    /** The logger. */
    private static Logger logger = LogHelperSQLStore.getLogger();

    /** I18N message handler. */
    private final static ResourceBundle messages = I18NHelper.loadBundle(
            "com.sun.jdo.spi.persistence.support.sqlstore.Bundle", // NOI18N
            ResultDesc.class.getClassLoader());

    private boolean debug;

    public ResultDesc(ClassDesc config, int aggregateResultType) {
        fields = new ArrayList();
        fieldNames = new ArrayList();
        this.config = config;
        this.aggregateResultType = aggregateResultType;
    }

    /** Create and add a ResultFieldDesc for the given fieldDesc and columnRef.
     *  @param fieldDesc - the field descriptor for the field that is the recipient of
     *  the result value indicated by the columnRef.
     *  @param columnRef - indicates which column in the resultset contains the value.
     *  @param projection - indicates whether the column is a projection
     */
    public void addField(LocalFieldDesc fieldDesc, ColumnRef columnRef,
                         boolean projection) {

            ResultFieldDesc rfd = new ResultFieldDesc(fieldDesc, columnRef);
            // remember the projection
            if (projection) {
                this.fieldProjection = rfd;
            }
            fields.add(rfd);
            fieldNames.add(fieldDesc.getName());
    }

    private void addField(ResultDesc rs) {
        if (rs != null) {
            fields.add(rs);
            fieldNames.add(null);
        }
    }

    public void setPrefetching() {
        prefetching = true;
    }

    /** Set the field that is the recipient of the result of this ResultDesc.
     *  @param parentField - field descriptor for the recipient field of the value
     *  of this ResultDesc.
     */
    public void setParentField(ForeignFieldDesc parentField) {
        this.parentField = parentField;
    }

    /**
     * Get the value to be bound to the field described by <code>fieldDesc</code>
     * from the result set. The conversion to the correct field type might be done
     * by the driver. If we can't get the correct type from the driver, the
     * conversion in done in FieldDesc::convertValue.
     *
     * @param resultData Result set from the database.
     * @param columnRef columnRef for the field.
     * @param fieldDesc Field descriptor of the field to be bound.
     * @param sm State manager for the persistent object being bound.
     * @return
     *   Object with the correct type defined in <code>fieldDesc</code>.
     */
    private static Object getConvertedObject(ResultSet resultData,
                                             ColumnRef columnRef,
                                             FieldDesc fieldDesc,
                                             StateManager sm) {
        Object retVal = null;
        try {
            retVal = getValueFromResultSet(resultData, columnRef, fieldDesc.getEnumType());
            // Create an SCO object in case we want to populate a pc.
            if (retVal != null) {
                // Create a SCO instance in case we want to populate a pc.
                Object scoVal = createSCO(retVal, sm, fieldDesc);
                if (scoVal != null) {
                    retVal = scoVal;
                }
            }
        } catch (SQLException sqlException) {
            //The driver is not able to convert for us
            //We would use resultData.getObject(index) below
            //and let FieldDesc::convertValue() do the conversion
            //Nothing to do here
            try {
                // Get the generic object and let FieldDesc::convertValue() deal with it.
                // This will return an SCO as needed.
                retVal = fieldDesc.convertValue(resultData.getObject(columnRef.getIndex()), sm);
            }
            catch (Exception e) {
                //Resolve : The original code was returning null and not throwing any
                //exception in this case. Should we also do that????
                logger.log(Logger.WARNING,"sqlstore.exception.log",e);
            }
        }

        return retVal;
    }

    /**
     * Gets value at index from resultData. resultData is queried for passed resultType.
     * @param resultData The resultset object.
     * @param columnRef columnRef for the field.
     * @param resultType Type of expected result.
     * @return value from <code>resultData</code> at <code>index</code>.
     */
    private static Object getValueFromResultSet(ResultSet resultData,
                                                ColumnRef columnRef,
                                                        int resultType) throws SQLException {
        int index = columnRef.getIndex();
        int columnType = columnRef.getColumnType();

        return getValueFromResultSet(resultData, index, resultType, columnType);
    }

    /**
     * Gets value at index from <code>resultData</code>.<code>resultData</code>
     * is queried for passed resultType.
     *
     * @param resultData The resultset object.
     * @param index Index at which result needs to be obtained.
     * @param resultType Type of expected result.
     * @return value from <code>resultData</code> at <code>index</code>.
     */
    private static Object getValueFromResultSet(ResultSet resultData,
                                                int index,
                                                int resultType) throws SQLException {

        // Types.OTHER is passed as a placeholder here.
        // It implies don't care for columnType.
        return getValueFromResultSet(resultData, index, resultType, Types.OTHER);
    }


    /**
     * Gets value at index from resultData. resultData is queried for passed resultType.
     * @param resultData The resultset object.
     * @param index Index at which result needs to be obtained.
     * @param resultType Type of expected result.
     * @param columnType Types of column at index <code>index</code> as represented by
     *                      java.sql.Types.
     * @return value from <code>resultData</code> at <code>index</code>.
     */
    private static Object getValueFromResultSet(ResultSet resultData,
                                                int index,
                                                int resultType,
                                                int columnType) throws SQLException {

        Object retVal = null;
        try {
            switch(resultType) {
                case FieldTypeEnumeration.BOOLEAN_PRIMITIVE :
                case FieldTypeEnumeration.BOOLEAN  :
                        boolean booleanValue = resultData.getBoolean(index);
                        if(!resultData.wasNull() )
                            retVal = new Boolean(booleanValue);
                        break;
                case FieldTypeEnumeration.CHARACTER_PRIMITIVE :
                case FieldTypeEnumeration.CHARACTER  :
                        String strValue = resultData.getString(index);
                        if(strValue != null)
                            retVal =  FieldDesc.getCharFromString(strValue);
                        break;
                case FieldTypeEnumeration.BYTE_PRIMITIVE :
                case FieldTypeEnumeration.BYTE  :
                        byte byteValue = resultData.getByte(index);
                        if(!resultData.wasNull() )
                            retVal = new Byte(byteValue);
                        break;
                case FieldTypeEnumeration.SHORT_PRIMITIVE :
                case FieldTypeEnumeration.SHORT  :
                        short shortValue = resultData.getShort(index);
                        if(!resultData.wasNull() )
                            retVal = new Short(shortValue);
                        break;
                case FieldTypeEnumeration.INTEGER_PRIMITIVE :
                case FieldTypeEnumeration.INTEGER  :
                        int intValue = resultData.getInt(index);
                        if(!resultData.wasNull() )
                            retVal = new Integer(intValue);
                        break;
                case FieldTypeEnumeration.LONG_PRIMITIVE :
                case FieldTypeEnumeration.LONG  :
                        long longValue = resultData.getLong(index);
                        if(!resultData.wasNull() )
                            retVal = new Long(longValue);
                        break;
                case FieldTypeEnumeration.FLOAT_PRIMITIVE :
                case FieldTypeEnumeration.FLOAT  :
                        float floatValue = resultData.getFloat(index);
                        if(!resultData.wasNull() )
                            retVal = new Float(floatValue);
                        break;
                case FieldTypeEnumeration.DOUBLE_PRIMITIVE :
                case FieldTypeEnumeration.DOUBLE  :
                        double doubleValue = resultData.getDouble(index);
                        if(!resultData.wasNull() )
                            retVal = new Double(doubleValue);
                        break;
                case FieldTypeEnumeration.BIGDECIMAL :
                case FieldTypeEnumeration.BIGINTEGER :
                        retVal = resultData.getBigDecimal(index);
                        if ((resultType == FieldTypeEnumeration.BIGINTEGER) && (retVal != null)) {
                            retVal = ( (java.math.BigDecimal) retVal).toBigInteger();
                        }
                        break;
                case FieldTypeEnumeration.STRING :
                        if(LocalFieldDesc.isCharLobType(columnType) ) {
                            Reader reader = resultData.getCharacterStream(index);
                            retVal = readCharacterStreamToString(reader);
                        } else {
                            retVal = resultData.getString(index);
                        }
                        break;
                case FieldTypeEnumeration.SQL_DATE :
                        retVal = resultData.getDate(index);
                        break;
                case FieldTypeEnumeration.SQL_TIME :
                        retVal = resultData.getTime(index);
                        break;
                case FieldTypeEnumeration.UTIL_DATE :
                case FieldTypeEnumeration.SQL_TIMESTAMP :
                        //Variable ts is introduced to avoid cast
                        Timestamp ts;
                        ts = resultData.getTimestamp(index);
                        if (resultType == FieldTypeEnumeration.UTIL_DATE && ts != null) {
                            retVal = new Date(ts.getTime());
                        } else {
							retVal = ts;
						}
                        break;
                case FieldTypeEnumeration.ARRAY_BYTE_PRIMITIVE :
                        InputStream is = resultData.getBinaryStream(index);
                        retVal = readInputStreamToByteArray(is);
                        break;
               case FieldTypeEnumeration.NOT_ENUMERATED :
                        //RESOLVE:
                        //We should only get here for getting values for hidden fields.
                        //hiddenFields does not have their java type initialized. Its sort of difficult
                        //to initialize java type without major re-org of the code in ClassDesc :(.
                        //But once it is done, we should throw an exception if we reach here.
                        //
                        //For now retrieve value for hidden fields as object as they are any way
                        //stored as Object in SQLStatemanager.
                        retVal = resultData.getObject(index);
                      break;
               default :
                        //If we reach here, a new type has been added to FieldTypeEnumeration.
                        //Please update this method to handle new type.
                        throw new JDOFatalInternalException(I18NHelper.getMessage(messages,
                            "sqlstore.resultdesc.unknownfieldtype",resultType) );
            }   //switch
        } catch (SQLException e) {
            if(logger.isLoggable(Logger.WARNING) ) {
                Object items[] =
                    { new Integer(index), new Integer(resultType), new Integer(columnType), e};
                logger.log(Logger.WARNING,"sqlstore.resultdesc.errorgettingvalefromresulset",items);
            }
            throw e;
        }

        // RESOLVE: Following is a workaround till we are able to initialize java type for hidden fields.
        // When we are able to determine java type of hidden fields, this code should go back
        // to case FieldTypeEnumeration.String
        if (LocalFieldDesc.isFixedCharType(columnType)
            // For Character fields, this method is expected to return
            // Character. Do not convert them to String.
            && resultType != FieldTypeEnumeration.CHARACTER_PRIMITIVE
            && resultType != FieldTypeEnumeration.CHARACTER
            && retVal != null) {
            // To support char columns, we rtrim fields mapped to fixedchar.
             retVal = StringHelper.rtrim(retVal.toString());
        }

        return retVal;
    }

    /**
     * Creates a SCO corresponding to <code>value</code>.
     * Currently used for dates. The actual SCO conversion for dates is done in
     * {@link com.sun.jdo.spi.persistence.support.sqlstore.model.FieldDesc#createSCO(java.lang.Object, com.sun.jdo.spi.persistence.support.sqlstore.StateManager)}.
     *
     * @param value Value to be converted.
     * @param sm StateManager of the persistent object being populated.
     * @param fieldDesc Field being bound.
     * @return New SCO instance, null if no SCO was created.
     */
    private static Object createSCO(Object value, StateManager sm, FieldDesc fieldDesc) {
        Object retVal = null;

        if (fieldDesc != null) {
            int enumType = fieldDesc.getEnumType();

            // Need to convert Date fields into their SCO equivalents
            switch(enumType) {
                case FieldTypeEnumeration.UTIL_DATE:
                case FieldTypeEnumeration.SQL_DATE:
                case FieldTypeEnumeration.SQL_TIME:
                case FieldTypeEnumeration.SQL_TIMESTAMP:

                    retVal = fieldDesc.createSCO(value, sm);
                    break;
                default:
            }
        }

        return retVal;
    }

    /**
     * Reads from input stream <CODE>is</CODE> into a byte array.
     *
     * @param is Input stream obtained from the database.
     * @return A byte array read from the input stream.
     * @see java.sql.ResultSet#getBinaryStream(int)
     */
    private static byte[] readInputStreamToByteArray(InputStream is) {
        byte[] byteArray = null;
        if (is != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] chunk = new byte[2000];
            int read = 0;

            try {
                while ((read = is.read(chunk)) != -1) {
                    bos.write(chunk, 0, read);
                }
                byteArray = bos.toByteArray();
            } catch (IOException e) {
                // log the exception and don't return any value
                // Eating the exception here. As the caller also does not
                // know how to deal with this exception.
                logger.log(Logger.WARNING,"sqlstore.exception.log",e);
            }
        }
        return byteArray;
    }

    /**
     * Reads from the character stream <code>reader</code> into a String.
     *
     * @param reader Reader obtained from the database.
     * @return A String read from the reader.
     * @see java.sql.ResultSet#getCharacterStream(int)
     */
    private static String readCharacterStreamToString(Reader reader) {
        String retVal = null;
        if(reader != null) {
            BufferedReader buffReader = new BufferedReader(reader);
            StringBuffer buff = new StringBuffer();
            try {
                int charRead;
                while( (charRead = buffReader.read() ) != -1) {
                    buff.append( (char)charRead );
                }
            } catch (IOException e) {
                    // log the exception and don't return any value
                    // Eating the exception here. As the caller also does not
                    // know how to deal with this exception.
                    logger.log(Logger.WARNING,"sqlstore.exception.log",e);
            }
            retVal = buff.toString();
        }
        return retVal;
    }

    /**
     * Materialize data from the result set into objects.
     *
     * @param pm - the PersistenceManager responsible for instantiating objects
     * @param resultData - JDBC ResultSet containing the data to be materialized
     * @return
     *   Collection containing the resulting objects. For aggregate queries,
     *   the returned object type is specified by the caller.
     */
    public Object getResult(PersistenceManager pm, ResultSet resultData) throws SQLException {
        Object result = null;

        debug = logger.isLoggable(Logger.FINEST);

        if (!isAggregate()) {
            Collection resultCollection = new ArrayList();

            // Fill in the data from the current row of resultData.
            while (resultData.next()) {
                Object resultObject = null;

                if (fieldProjection != null) {
                    resultObject = getProjectedField(resultData);
                } else {
                    resultObject = setFields(pm, resultData);
                }
                // resultCollection might contain resultObject if prefetch
                // is enabled. Do not add duplicates. Duplicates are required
                // for projection queries
                if (!prefetching || !resultCollection.contains(resultObject)) {
                    resultCollection.add(resultObject);
                }
            }

            //Iterate over the results obtained and handle deferred collection updates.
            applyDeferredUpdatesToPrefetchedCollections(resultCollection);
            result = resultCollection;
        } else {
            // Aggregate functions return an object instead of a collection.
            result = getAggregateResult(resultData);
        }

        return result;
    }

    /**
     * Iterate the result collection applying updates to deferred collections.
     * @param resultCollection Result collection.
     */
    private void applyDeferredUpdatesToPrefetchedCollections(Collection resultCollection) {
        if (prefetching && prefetchedCollectionFields != null && prefetchedCollectionFields.size() > 0) {
            for (Iterator resultItr = resultCollection.iterator(); resultItr.hasNext(); ) {
                // each result object is guaranteed to be instance of PersistenceCapable
                PersistenceCapable pc = (PersistenceCapable) resultItr.next();

                // pc can be null if this is a projection query 
                if (pc != null) {
                    applyDeferredUpdatesToPrefetchedCollections(pc);
                }
            }
        }
    }

    /**
     * Process deferred updates for the prefetched collection fields.
     * @param pc Instance from the query result.
     */
    private void applyDeferredUpdatesToPrefetchedCollections(PersistenceCapable pc) {
        if (prefetchedCollectionFields != null) {
            StateManager sm = pc.jdoGetStateManager();
            Iterator prefetchedCollectionFieldsIter = prefetchedCollectionFields.keySet().iterator();

            while (prefetchedCollectionFieldsIter.hasNext()) {
                ForeignFieldDesc prefetchedCollectionField =
                        (ForeignFieldDesc) prefetchedCollectionFieldsIter.next();
                ResultDesc prefetchedResultDesc =
                        (ResultDesc) prefetchedCollectionFields.get(prefetchedCollectionField);

                // process deferred updates for prefetched collection relationships
                if (prefetchedCollectionField.cardinalityUPB > 1) {
                    Collection relationshipValue =
                            (Collection) prefetchedCollectionField.getValue(sm);

                    if (relationshipValue instanceof SCOCollection && ((SCOCollection) relationshipValue).isDeferred()){
                        ((SCOCollection) relationshipValue).applyDeferredUpdates(null);
                    }

                    // recursion into the next level
                    for (Iterator iter = relationshipValue.iterator(); iter.hasNext(); ) {
                        PersistenceCapable persistenceCapable = (PersistenceCapable) iter.next();
                        prefetchedResultDesc.applyDeferredUpdatesToPrefetchedCollections(persistenceCapable);
                    }
                }
            }
        }
    }

    /**
     * Get result for Aggregates. Since resultset containing result for aggregates would not
     * contain any other columns, it is assumed that the result is available at index == 1.
     * @param resultData The resultset from which result is to be extracted.
     */
    private Object getAggregateResult(ResultSet resultData) throws SQLException {
        Object result = null;

        if (resultData.next() ) {
            //Aggregate results are always at index 1;
            result = getValueFromResultSet(resultData, 1, aggregateResultType);
        }
        return result;
   }


    /**
     * Returns the projected field from the result set. This field is
     * always a local field. Foreign fields are handled in setFields.
     *
     * We return the database value for projections on local fields.
     * Unless we flush for queries in optimistic transactions the value
     * from the database might be different from the value in memory.
     *
     * @param resultData The SQL result set.
     * @return
     *   The projected value from the result set. This might be a local field
     *   or the result of an aggregate query.
     * @see com.sun.jdo.spi.persistence.support.sqlstore.sql.ResultDesc#setFields(PersistenceManager, ResultSet)
     */
    private Object getProjectedField(ResultSet resultData) {
        //Field projection can never be null if this method gets called.
        FieldDesc f = fieldProjection.getFieldDesc();

        if (debug) {
            logger.finest("sqlstore.resultdesc.returning_field", f.getName()); // NOI18N
        }
        return getConvertedObject(resultData, fieldProjection.getColumnRef(), f, null);
    }

    /**
     * Bind the columns from this ResultSet row to the persistent object described
     * by this ResultDesc. External queries always return only one type of objects
     * and don't have nested ResultDescs. Internal queries can have nested ResultDescs.
     * Run through all the fields of the field list and bind the values in
     * that order. Nested ResultDescs are processed by recursive calls.
     *
     * @param pm The PersistenceManager responsible for instantiating objects.
     * @param resultData JDBC ResultSet containing the data to be materialized.
     * @return
     *   Persistent object corresponding to values from ResultSet row, can be null.
     */
    private Object setFields(PersistenceManager pm, ResultSet resultData) {
        Object pcObj = null;
        // Get the Statemanager corresponding to the current row
        SQLStateManager sm = (SQLStateManager) findOrCreateStateManager(resultData, pm);
        if (sm != null) {
            pcObj = sm.getPersistent();
            sm.getLock();
            try {
                // Fields are read in the order in which they were placed in
                // the sql select statement. This ordering is important while reading
                // from streams corresponding to LONG columns on Oracle.
                for (int i = 0; i < fields.size();  i++) {
                    Object temp = fields.get(i);

                    if (temp instanceof ResultFieldDesc) {
                        ResultFieldDesc rfd = (ResultFieldDesc) temp;
                        LocalFieldDesc f = rfd.getFieldDesc();

                        if (!sm.getPresenceMaskBit(f.absoluteID)) {
                            Object value = getConvertedObject(resultData, rfd.getColumnRef(), f, sm);

                            if (debug) {
                                logger.finest("sqlstore.resultdesc.marking_field", f.getName()); // NOI18N
                            }

                            // Set the field value and presence mask bit.
                            setFieldValue(sm, f, value);
                        }
                    } else {
                        ResultDesc frd = (ResultDesc) temp;
                        ForeignFieldDesc parentField = frd.parentField;

                        // Only try to fetch the field if it is not already present.
                        // If the field is already present, it should be in
                        // consistent state w.r.t. this transaction. Overwriting
                        // it with the value from database might corrupt consistency of data.
                        if (!sm.getPresenceMaskBit(parentField.absoluteID) || parentField.cardinalityUPB > 1) {
                            Object fobj = frd.setFields(pm, resultData);

                            if (parentField.cardinalityUPB > 1) { // parentField is a collection.
                                // Add the value and set the presence mask bit if necessary
                                addCollectionValue(sm, parentField, fobj);
                            } else { // parentField is an object.
                                // Set the field value and presence mask bit.
                                setFieldValue(sm, parentField, fobj);
                            }
                        }
                        if (debug) {
                            logger.finest("sqlstore.resultdesc.marking_foreign_field", // NOI18N
                                    parentField.getName());
                        }
                    }
                }

                sm.initialize(true);
            } finally {
                // Always release the lock.
                sm.releaseLock();
            }
        } else {
            // sm can be null if we can not find or create a statemanager from the result data.
            // This is possible if we are projecting on a foreignfield and there is no
            // result returned.
        }

        return pcObj;
    }

    /**
     * Adds <code>value</code> to the collection for the given field <code>f</code> 
     * and statemanager <code>sm</code>.
     * Also sets presence mask bit for the field in given <code>sm</code>, if not already set.
     * @param sm Given StateManager, is always a SQLStateManager
     * @param f Given field
     * @param value Given value.
     */
    private static void addCollectionValue(SQLStateManager sm, ForeignFieldDesc f, Object value) {
        Collection collection = (Collection) f.getValue(sm);
        if (collection == null) {
            // Initialize the collection.
            sm.replaceCollection(f, null);
            // Get the newly created SCOCollection back.
            collection = (Collection) f.getValue(sm);
        }

        // Set the presence mask if necessary.
        // SCOCollections might be != null and presence mask not set.
        if (!sm.getPresenceMaskBit(f.absoluteID)) {
            sm.setPresenceMaskBit(f.absoluteID);                                    
        }

        if (value != null) {
            if (collection instanceof SCOCollection) {
                ((SCOCollection) collection).addToBaseCollection(value);
            } else {
                // Should never happen.
                collection.add(value);
            }
        }
    }

    /**
     * Sets <code>value</code> for the given field <code>f</code>
     * and statemanager <code>sm</code>.
     * Also sets presence mask bit for the field in given <code>sm</code>.
     * @param sm Given StateManager
     * @param f Given field
     * @param value Given value.
     */
    private static void setFieldValue(StateManager sm, FieldDesc f, Object value) {
        f.setValue(sm, value);
        sm.setPresenceMaskBit(f.absoluteID);
    }

    /**
     *  Specifies, if this was an aggregate query.
     */
    private boolean isAggregate() {
        return aggregateResultType != FieldTypeEnumeration.NOT_ENUMERATED;
    }

    /**
     * Returns a StateManager which PC instance to be populated with the values.
     * If such instance exists in this PersistenceManager cache,
     * it is returned, otherwise a new instance is created.
     */
    private StateManager findOrCreateStateManager(ResultSet resultData,
                                                  PersistenceManager pm) {
        try {
            Class oidClass = config.getOidClass();
            Object oid = oidClass.newInstance();

            // Copy key field values
            Field keyFields[] = config.getKeyFields();
            String keyNames[] = config.getKeyFieldNames();
            for (int i = 0; i < keyFields.length; i++) {
                Field keyField = keyFields[i];
                String keyName = keyNames[i];
                FieldDesc fd = config.getField(keyName);
                int index = fieldNames.indexOf(keyName);

                ResultFieldDesc rfd = (ResultFieldDesc)fields.get(index);

                Object v = getConvertedObject(resultData, rfd.getColumnRef(), fd, null);

                if (debug) {
                    logger.finest("sqlstore.resultdesc.marking_key_field",keyName); // NOI18N
                }

                if (v == null ) {
                    return null;
                }
                keyField.set(oid, v);
            }
            return pm.findOrCreateStateManager(oid, config.getPersistenceCapableClass());

        } catch (Exception e) {
            // RESOLVE...
            throw new JDOFatalInternalException(e.getMessage());
        }
    }

    /**
     * Joins foreignResult with this resultDesc
     * @param foreignResult the foreign ResultDesc
     * @param parentField parentField for the foreind ResultDesc
     */
    public void doJoin(ResultDesc foreignResult, ForeignFieldDesc parentField) {
        addField(foreignResult);
        foreignResult.parentField = parentField;

        // if foreign result correponds to a collection relationship being
        // prefetched, remember it.
        if(parentField.cardinalityUPB > 1) {
            if (prefetchedCollectionFields == null) {
                prefetchedCollectionFields = new HashMap();
            }
            prefetchedCollectionFields.put(parentField, foreignResult);
        }
    }

}
