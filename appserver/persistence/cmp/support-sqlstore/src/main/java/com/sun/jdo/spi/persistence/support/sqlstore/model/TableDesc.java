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
 * TableDesc.java
 *
 * Created on March 3, 2000
 *
 */

package com.sun.jdo.spi.persistence.support.sqlstore.model;

import org.netbeans.modules.dbschema.TableElement;
import com.sun.jdo.api.persistence.model.mapping.MappingClassElement;
import java.util.ArrayList;

/**
 * This class is used to represent a database table.
 */
public class TableDesc {

    /** primary key for the table */
    private KeyDesc key;

    /** array of ReferenceKeyDescs referencing secondary tables */
    private ArrayList secondaryTableKeys;

    /** ReferenceKeyDesc referencing the primary table */
    private ReferenceKeyDesc primaryTableKey;

    /** actual TableElement from the dbmodel */
    private TableElement tableElement;

    /** Consistency level for this table defined in the model */
    private int consistencyLevel;

    /** indicates this table is a join table */
    private boolean isJoinTable;

    /** Name of the table */
    private String name;

    /** Version field used for version consistency */
    private LocalFieldDesc versionField;

    public TableDesc(TableElement tableElement) {
        this.tableElement = tableElement;

        name = tableElement.getName().getName();
        consistencyLevel = MappingClassElement.NONE_CONSISTENCY;
    }

    /** Return all secondary table keys.
     *  @return an ArrayList of ReferenceKeyDescs for secondary tables
     */
    public ArrayList getSecondaryTableKeys() {
        return secondaryTableKeys;
    }

    /** Add a new reference key to the list of secondary table keys.
     *  @param key - ReferenceKeyDesc to be added
     */
    void addSecondaryTableKey(ReferenceKeyDesc key) {
        if (secondaryTableKeys == null)
            secondaryTableKeys = new ArrayList();

        secondaryTableKeys.add(key);
    }

    /** Return the reference key referencing the primary table.
     *  @return the ReferenceKeyDesc referencing the primary table
     */
    public ReferenceKeyDesc getPrimaryTableKey() {
        return primaryTableKey;
    }

    /** Set the reference key referencing the primary table.
     *  @param key - ReferenceKeyDesc to be added
     */
    void setPrimaryTableKey(ReferenceKeyDesc key) {
        this.primaryTableKey = key;
    }

    /** Return the primary key for the table.
     *  @return the KeyDesc representing the primary key for the table
     */
    public KeyDesc getKey() {
        return key;
    }

    /** Set the primary key for the table.
     *  @param key - KeyDesc to be set as the primary key
     */
    void setKey(KeyDesc key) {
        this.key = key;
    }

    /** Return the actual dbmodel TableElement for this table.
     *  @return TableElement associated with this table
     */
    public TableElement getTableElement() {
        return tableElement;
    }

    /** Return the name of the table.
     *  @return the name of the table.
     */
    public String getName() {
        return name;
    }

    /** Return true if this table is a join table. */
    public boolean isJoinTable() {
        return isJoinTable;
    }

    /** Set consistencyLevel to value. */
    void setConsistencyLevel(int value) {
        consistencyLevel = value;
        //TODO :
        //if(isUpdateLockRequired() )
            //Check for DBVendorType.isUpdateLockSupported()
            //Log to trace if !DBVendorType.isUpdateLockSupported()
            //If this table is ever used, user would get an exception

    }

    /** Determins if an update lock is required on this table. */
    public boolean isUpdateLockRequired() {
        return consistencyLevel == MappingClassElement.LOCK_WHEN_LOADED_CONSISTENCY;
    }

    /** Set isJoinTable to value */
    void setJoinTable(boolean value) {
        isJoinTable = value;
    }

    void setVersionField(LocalFieldDesc field) {
        versionField = field;
    }

    /**
     * Returns the field representing the version column for this
     * table. The version column is used for verification with version
     * consistency. Each table can have only one version column.
     * 
     * @return Version field.
     */
    public LocalFieldDesc getVersionField() {
        return versionField;
    }

}





