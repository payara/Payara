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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2019] Payara Foundation and/or affiliates

package org.glassfish.ejb.deployment.descriptor;

import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;

import java.util.logging.Logger;

import org.glassfish.deployment.common.Descriptor;

/**
 * This class contains deployment information for an EntityBean with
 * bean-managed persistence.
 * Subclasses contains additional information for EJB1.1/EJB2.0 CMP EntityBeans.
 *
 * @author Danny Coward
 * @author Sanjeev Krishnan
 * @author Vivek Nagar
 */

public class EjbEntityDescriptor extends EjbDescriptor {

    public static final String TYPE = "Entity";
    public static final String BEAN_PERSISTENCE = "Bean";
    public static final String CONTAINER_PERSISTENCE = "Container";
    public static final String TRUE = "true";
    public static final String FALSE = "false";

    // EntityBean attributes
    protected String persistenceType;
    protected boolean isReentrant = false;
    protected String primaryKeyClassName;

    private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(EjbEntityDescriptor.class);

    static Logger _logger = DOLUtils.getDefaultLogger();



    /**
     * The default constructor.
     */
    public EjbEntityDescriptor() {
    }

    /**
     * The copy constructor.
     */
    public EjbEntityDescriptor(EjbDescriptor other) {
        super(other);
        if (other instanceof EjbEntityDescriptor) {
            EjbEntityDescriptor entity = (EjbEntityDescriptor) other;
            this.persistenceType = entity.persistenceType;
            this.isReentrant = entity.isReentrant;
            this.primaryKeyClassName = entity.primaryKeyClassName;
        }
    }

    @Override
    public String getEjbTypeForDisplay() {
        return "EntityBean";
    }

    /**
     * Gets the container transaction type for this entity bean. Entity
     * beans always have CONTAINER_TRANSACTION_TYPE transaction type.
     */
    @Override
    public String getTransactionType() {
        return super.transactionType;
    }

    /**
     * Sets the transaction type for this entity bean.
     * Throws an illegal argument exception if this type is not
     * CONTAINER_TRANSACTION_TYPE.
     */
    @Override
    public void setTransactionType(String transactionType) {
        if (!CONTAINER_TRANSACTION_TYPE.equals(transactionType)
                && Descriptor.isBoundsChecking()) {
            throw new IllegalArgumentException(localStrings.getLocalString(
                    "enterprise.deployment.exceptionentitybeancanonlyhavecntnrtxtype",
                    "Entity beans can only have Container transaction type. The type was being set to {0}", new Object[] {transactionType}));
        }
        super.transactionType = transactionType;
    }

    @Override
    public String getContainerFactoryQualifier() {
        return "EntityContainerFactory";
    }


    /**
     * Return true if this entity bean is reentrant, false else.
     */
    public boolean isReentrant() {
        return this.isReentrant;
    }

    public String getReentrant() {
        return isReentrant() ? TRUE : FALSE;
    }

    public void setReentrant(String reentrantString) {
        if (TRUE.equalsIgnoreCase(reentrantString)) {
            this.setReentrant(true);
            return;
        }
        if (FALSE.equalsIgnoreCase(reentrantString)) {
            this.setReentrant(false);
            return;
        }
        if (Descriptor.isBoundsChecking()) {
            throw new IllegalArgumentException(localStrings.getLocalString(
                    "enterprise.deployment.exceptionstringnotlegalvalue",
                    "{0} is not a legal value for entity reentrancy", new Object[] {reentrantString}));
        }
    }

    /**
     * Sets the isReentrant flag for this bean.
     */
    public void setReentrant(boolean isReentrant) {
        this.isReentrant = isReentrant;

    }

    /**
     * Returns the persistence type for this entity bean. Defaults to BEAN_PERSISTENCE.
     */
    public String getPersistenceType() {
        if (this.persistenceType == null) {
            this.persistenceType = BEAN_PERSISTENCE;
        }
        return this.persistenceType;
    }

    /**
     * Sets the persistence type for this entity bean. Allowable values are BEAN_PERSISTENCE
     * or CONTAINER_PERSISTENCE, or else an IllegalArgumentException is thrown.
     */
    public void setPersistenceType(String persistenceType) {
        boolean isValidChange = (BEAN_PERSISTENCE.equals(persistenceType) || CONTAINER_PERSISTENCE.equals(persistenceType));
        if (isValidChange || !Descriptor.isBoundsChecking()) {
            this.persistenceType = persistenceType;

        } else {
            //_logger.log(Level.FINE,"Warning " + persistenceType + " is not an allowed persistence type");
            throw new IllegalArgumentException(localStrings.getLocalString(
                    "enterprise.deployment.exceptionpersistenceisnotallowedtype",
                    "{0} is not an allowed persistence type", new Object[] {persistenceType}));
        }
    }

    /**
     * Return the classname of the primary key for this bean, or the empty
     * string if none has been set.
     */
    public String getPrimaryKeyClassName() {
        if (this.primaryKeyClassName == null) {
            this.primaryKeyClassName = Object.class.getName();
        }
        return this.primaryKeyClassName;
    }

    /**
     * Set the classname of the primary key used by this bean.
     */
    public void setPrimaryKeyClassName(String primaryKeyClassName) {
        this.primaryKeyClassName = primaryKeyClassName;

    }

    /**
     * Returns the type of this bean. EjbEntityDescriptor.TYPE
     */
    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Sets my type String.
     */
    @Override
    public void setType(String type) {
        throw new IllegalArgumentException(localStrings.getLocalString(
                "enterprise.deployment.exceptioncannotsettypeonentitybean",
                "Cannon set type on an entity bean"));
    }

    /**
     * Return my formatted string representation.
     */
    @Override
    public void print(StringBuilder toStringBuilder) {
        super.print(toStringBuilder);
        toStringBuilder.append("\n Entity descriptor");
        toStringBuilder.append("\n isReentrant ").append(isReentrant);
        toStringBuilder.append("\n primaryKeyClassName ").append(primaryKeyClassName);
        toStringBuilder.append("\n persistenceType ").append(persistenceType);
    }
}
