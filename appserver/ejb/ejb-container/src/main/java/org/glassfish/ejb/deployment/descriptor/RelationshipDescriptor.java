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
 * This class contains information about relationships between
 * EJB2.0 CMP EntityBeans.
 * It represents information in the <ejb-relation> XML element.
 *
 * @author Sanjeev Krishnan
 */

public final class RelationshipDescriptor extends Descriptor {

    private RelationRoleDescriptor source;  // descriptor for source role
    private RelationRoleDescriptor sink; // descriptor for sink role

    private boolean isBidirectional = true;

    public boolean isOneOne() {
	return (!source.getIsMany() && !sink.getIsMany()); 
    }
    
    public boolean isOneMany() {
	return (!source.getIsMany() && sink.getIsMany()); 
    }
    
    public boolean isManyOne() {
	return (source.getIsMany() && !sink.getIsMany()); 
    }
    
    public boolean isManyMany() {
	return (source.getIsMany() && sink.getIsMany()); 
    }

    /**
     * Checks whether an EjbCMPEntityDescriptor
     * is a participant in this relationship.
     */
    public boolean hasParticipant(Descriptor desc) {
        return ( (source.getOwner() == desc) || (sink.getOwner() == desc) );
    }

    public RelationRoleDescriptor getSource()
    {  
	return source;  
    }
    public void setSource(RelationRoleDescriptor source)
    {  
	this.source = source;  
    }

    public void setSink(RelationRoleDescriptor sink)
    {
	this.sink = sink;
    }
    public RelationRoleDescriptor getSink()
    {
	return sink;
    }

    public void setIsBidirectional(boolean isBidirectional)
    {
	this.isBidirectional = isBidirectional;
    }
    public boolean getIsBidirectional()
    {
	return isBidirectional;
    }

    @Override
    public void print(StringBuffer toStringBuffer) {
        toStringBuffer.append("From EJB ").append(getSource().getName()
           ).append(" cmr field : ").append(getSource().getCMRField()
           ).append("(").append(getSource().getCMRFieldType()).append(")  to EJB ").append(getSink().getName()
           ).append(" isMany ").append(getSource().getIsMany()
           ).append(" cascade-delete ").append(getSource().getCascadeDelete());
    }
}
