/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2001-2010 Oracle and/or its affiliates. All rights reserved.
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

package team;

import java.util.*;
import javax.ejb.*;
import javax.naming.*;
import util.Debug;

public abstract class PlayerBean implements EntityBean {

    private EntityContext context;

    // Access methods for persistent fields

    public abstract String getPlayerId();
    public abstract void setPlayerId(String id);
    
    public abstract String getName();
    public abstract void setName(String name);

    public abstract String getPosition();
    public abstract void setPosition(String position);

    public abstract double getSalary();
    public abstract void setSalary(double salary);

    // Access methods for relationship fields

    public abstract Collection getTeams();
    public abstract void setTeams(Collection teams);

    // Select methods

    public abstract Collection ejbSelectLeagues(LocalPlayer player)
        throws FinderException;

    public abstract Collection ejbSelectSports(LocalPlayer player)
        throws FinderException;

                
        
    // Business methods

    public Collection getLeagues() throws FinderException {

         LocalPlayer player = 
             (team.LocalPlayer)context.getEJBLocalObject();
         return ejbSelectLeagues(player);
    }

    public Collection getSports() throws FinderException {

         LocalPlayer player = 
             (team.LocalPlayer)context.getEJBLocalObject();
         return ejbSelectSports(player);
    }

    // EntityBean  methods

    public String ejbCreate (String id, String name, String position,
        double salary) throws CreateException {

        Debug.print("PlayerBean ejbCreate");
        setPlayerId(id);
        setName(name);
        setPosition(position);
        setSalary(salary);
        return null;
    }
         
    public void ejbPostCreate (String id, String name, String position,
        double salary) throws CreateException { }

    public void setEntityContext(EntityContext ctx) {
        context = ctx;
    }
    
    public void unsetEntityContext() {
        context = null;
    }
    
    public void ejbRemove() {
        Debug.print("PlayerBean ejbRemove");
    }
    
    public void ejbLoad() {
        Debug.print("PlayerBean ejbLoad");
    }
    
    public void ejbStore() {
        Debug.print("PlayerBean ejbStore");
    }
    
    public void ejbPassivate() { }
    
    public void ejbActivate() { }

} // PlayerBean class
