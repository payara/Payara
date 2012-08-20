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

package com.sun.enterprise.deployment;

import java.io.Serializable;

import org.glassfish.deployment.common.Descriptor;

    /** I am a pairing between a descriptor and a descriptor that has a JNDI name.
    *@author Danny Coward
    */

public class NamedReferencePair implements Serializable {
    
    // Types of named reference pairs
    public static final int EJB = 1;
    public static final int EJB_REF = 2;
    public static final int RESOURCE_REF = 3;
    public static final int RESOURCE_ENV_REF = 4;

    private Descriptor referant;
    private NamedDescriptor referee;
    private int type;
    
    public static NamedReferencePair createEjbPair
        (EjbDescriptor referant, EjbDescriptor referee) 
    {
      if (referant instanceof Descriptor)
        return new NamedReferencePair((Descriptor) referant, referee, EJB); // FIXME by srini - can we extract intf to avoid this
      else
        return null;
    }
        
    public static NamedReferencePair createEjbRefPair
        (Descriptor referant, EjbReferenceDescriptor referee)
    {
        return new NamedReferencePair(referant, referee, EJB_REF);
    }

    public static NamedReferencePair createResourceRefPair
        (Descriptor referant, ResourceReferenceDescriptor referee)
    {
        return new NamedReferencePair(referant, referee, RESOURCE_REF);
    }

    public static NamedReferencePair createResourceEnvRefPair
        (Descriptor referant, ResourceEnvReferenceDescriptor referee)
    {
        return new NamedReferencePair(referant, referee, RESOURCE_ENV_REF);
    }
    
    /** Construct a pairing between the given descriptor and the object
    * it has with a jndi name.*/
    protected NamedReferencePair(Descriptor referant, NamedDescriptor referee, 
                                 int type) {
	this.referant = referant;
	this.referee  = referee;
        this.type     = type;
    }

    /** Gets the descriptor with the named descriptor. */
    public Descriptor getReferant() {
	return this.referant;
    }
    
    /** Gets the named descriptor for the decriptor.*/
    public NamedDescriptor getReferee() {
	return this.referee;
    }

    public String getPairTypeName() {
        switch(this.type) {
            case EJB : return "EJB"; 
            case EJB_REF : return "EJB REF";
            case RESOURCE_REF : return "RESOURCE REF";
            case RESOURCE_ENV_REF : return "RESOURCE ENV REF";
        }
        throw new IllegalStateException("unknown type = " + type);        
    }

    public int getPairType() {
        return this.type;
    }

    /** My pretty format. */
    public void print(StringBuffer toStringBuffer) {
	toStringBuffer.append("NRP: ").append(referant.getName()).append(" -> ").append(((Descriptor) referee).getName());
    }
    
}
