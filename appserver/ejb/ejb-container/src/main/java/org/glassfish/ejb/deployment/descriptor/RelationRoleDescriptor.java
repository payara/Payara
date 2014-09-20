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

package org.glassfish.ejb.deployment.descriptor;

import org.glassfish.deployment.common.Descriptor;


/** 
 * This class contains information about one of the partners in 
 * a relationship between EJB2.0 CMP EntityBeans.
 * It represents information in the <ejb-relation-role> XML element.
 *
 * @author Sanjeev Krishnan
 */

public final class RelationRoleDescriptor extends Descriptor {

    // Bean for owner (role-source) of this side of 
    // the relationship
    private EjbCMPEntityDescriptor owner;
    private PersistenceDescriptor pers;
    private RelationshipDescriptor relnDesc;

    private String roleSourceDescription;
    private String cmrField; // CMR field name in owner's class
    private String cmrFieldDescription;
    private String cmrFieldType; // Java type of cmr-field
    private boolean isMany; 
    private RelationRoleDescriptor partner;
    private boolean cascadeDelete;
    private CMRFieldInfo cmrFieldInfo;
    private String relationRoleName;

    public RelationRoleDescriptor() {}

    /**
     * May return null if the role-source for this relationship role
     * is a remote-ejb-name
     */
    public PersistenceDescriptor getPersistenceDescriptor()
    {
        return pers;
    }

    /*
     * Can be set to null if there is no associated persistence descriptor.
     */
    public void setPersistenceDescriptor(PersistenceDescriptor newPers)
    {
        if( this.pers != null ) {
            // first invalidate cmr stuff in original persistence descriptor
            this.pers.invalidate();
        }

        this.pers = newPers;
        if( newPers != null ) {
            this.owner = (EjbCMPEntityDescriptor)newPers.getParentDescriptor();
        }
        invalidateCMRFieldStuff();
    }

    private void invalidateCMRFieldStuff() {
        cmrFieldInfo = null;
        if( pers != null ) {
            pers.invalidate();
        }
    }

    public RelationshipDescriptor getRelationshipDescriptor()
    {
	return relnDesc;
    }

    public void setRelationshipDescriptor(RelationshipDescriptor relnDesc)
    {
	this.relnDesc = relnDesc;
    }

    public void setOwner(EjbCMPEntityDescriptor owner)
    {
	this.owner = owner;
        invalidateCMRFieldStuff();
    }

    public EjbCMPEntityDescriptor getOwner()
    {
	return owner;
    }

    /**
     * The other role in the relationship I participate in.
     */
    public RelationRoleDescriptor getPartner()
    {
        return partner;
    }
    public void setPartner(RelationRoleDescriptor partner)
    {
        this.partner = partner;
    }

    public String getRelationRoleName()
    {
        return relationRoleName;
    }
    public void setRelationRoleName(String relationRoleName)
    {
        this.relationRoleName = relationRoleName;
    }

    public void setRoleSourceDescription(String roleSourceDescription)
    {
        this.roleSourceDescription = roleSourceDescription;
    }
    public String getRoleSourceDescription()
    {
	if ( roleSourceDescription == null )
	    roleSourceDescription = "";
        return roleSourceDescription;
    }

    /**
     * Set to NULL to indicate no cmr field
     */
    public void setCMRField(String cmrField)
    {
	this.cmrField =  cmrField;
        invalidateCMRFieldStuff();
    }
    public String getCMRField()
    {
        return cmrField;
    }

    public void setCMRFieldDescription(String cmrFieldDescription)
    {
        this.cmrFieldDescription = cmrFieldDescription;
    }
    public String getCMRFieldDescription()
    {
	if ( cmrFieldDescription == null )
	    cmrFieldDescription = "";
        return cmrFieldDescription;
    }

    /**
     * Only applicable when partner is collection-valued.
     * Set to NULL to indicate no field type is not applicable.
     */
    public void setCMRFieldType(String newCmrFieldType)
    {
        if( newCmrFieldType == null ) {
            this.cmrFieldType = null; 
            invalidateCMRFieldStuff();
        } else if ( newCmrFieldType.equals("java.util.Collection") 
                    || newCmrFieldType.equals("java.util.Set") ) {
	    this.cmrFieldType =  newCmrFieldType;
            invalidateCMRFieldStuff();
	} else {
	    throw new IllegalArgumentException
                ("cmr-field-type is " + newCmrFieldType + 
                 ", must be java.util.Collection or java.util.Set");
	}
    }
    public String getCMRFieldType()
    {
        return cmrFieldType;
    }

    public void setIsMany(boolean isMany)
    {
        this.isMany =  isMany;
        invalidateCMRFieldStuff();        
    }
    public boolean getIsMany()
    {
        return isMany;
    }

    public void setCascadeDelete(boolean cascadeDelete)
    {
        this.cascadeDelete =  cascadeDelete;
    }
    public boolean getCascadeDelete()
    {
        return cascadeDelete;
    }

    public void setCMRFieldInfo(CMRFieldInfo cmrFieldInfo)
    {
	this.cmrFieldInfo = cmrFieldInfo;
    }
    public CMRFieldInfo getCMRFieldInfo()
    {
	if ( cmrFieldInfo == null && pers != null ) 
	    pers.getCMRFieldInfo(); // tell pers to initialize its CMRFieldInfos
	return cmrFieldInfo;
    }

    public String composeReverseCmrFieldName() {
        return "_" + getPartner().getOwner().getName() + "_" + getPartner().getCMRField();
    }
}

