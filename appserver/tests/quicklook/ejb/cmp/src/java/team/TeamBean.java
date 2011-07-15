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
import util.PlayerDetails;

public abstract class TeamBean implements EntityBean {

    private EntityContext context;

    // Access methods for persistent fields

    public abstract String getTeamId();
    public abstract void setTeamId(String id);
    
    public abstract String getName();
    public abstract void setName(String name);

    public abstract String getCity();
    public abstract void setCity(String city);


    // Access methods for relationship fields
             
    public abstract Collection getPlayers();
    public abstract void setPlayers(Collection players);

    public abstract LocalLeague getLeague();
    public abstract void setLeague(LocalLeague league);

    // Select methods

    public abstract double ejbSelectSalaryOfPlayerInTeam(LocalTeam team, String playerName)
        throws FinderException;

    public abstract String ejbSelectByNameWithCONCAT(String part1, String part2)
        throws FinderException;	
                            
    public abstract String ejbSelectByNameSubstring(String substring)
        throws FinderException;	

    public abstract String ejbSelectNameLocate(String substring)
        throws FinderException;	

                            
    // Business methods

    public double getSalaryOfPlayer(String playerName) throws FinderException {
        LocalTeam team = (team.LocalTeam)context.getEJBLocalObject();
        
        return ejbSelectSalaryOfPlayerInTeam(team, playerName);
    }
    
    
    public String getTeamNameWithStringfunctionTests1() throws FinderException {
                                                        
        StringBuffer out = new StringBuffer();
//        LocalTeam team = (team.LocalTeam) context.getEJBLocalObject();
//        out.append("<BR>Name of Team : " + team.getName());
        out.append("<BR>");		
        out.append(ejbSelectByNameWithCONCAT("Cr", "ows"));
        out.append("<BR>");
        
        return out.toString();
    }
    
    public String getTeamNameWithStringfunctionTests2() throws FinderException {
                                                        
        StringBuffer out = new StringBuffer();
        out.append(ejbSelectByNameSubstring("aaaaCrowsaaaaa"));
        out.append("<BR>");

        return out.toString();
    }
                                
    public String getTeamNameWithStringfunctionTests3() throws FinderException {
                                                        
        StringBuffer out = new StringBuffer();
        out.append(ejbSelectNameLocate("row"));
        out.append("<BR>");
        
        return out.toString();
    }

                                  
    public ArrayList getCopyOfPlayers() {

        Debug.print("TeamBean getCopyOfPlayers");
        ArrayList playerList = new ArrayList();
        Collection players = getPlayers();

        Iterator i = players.iterator();
        while (i.hasNext()) {
            LocalPlayer player = (LocalPlayer) i.next();
            PlayerDetails details = new PlayerDetails(player.getPlayerId(),
                player.getName(), player.getPosition(), 0.00);
            playerList.add(details);
        }

        return playerList;
    }

    public void addPlayer(LocalPlayer player) {

        Debug.print("TeamBean addPlayer");
        try {
            Collection players = getPlayers();
            players.add(player);
        } catch (Exception ex) {
            throw new EJBException(ex.getMessage());
        }
    }

    public void dropPlayer(LocalPlayer player) {

        Debug.print("TeamBean dropPlayer");
        try {
            Collection players = getPlayers();
            players.remove(player);
        } catch (Exception ex) {
            throw new EJBException(ex.getMessage());
        }
    }

    // EntityBean  methods

    public String ejbCreate (String id, String name, String city)
        throws CreateException {

        Debug.print("TeamBean ejbCreate");
        setTeamId(id);
        setName(name);
        setCity(city);
        return null;
    }
         
    public void ejbPostCreate (String id, String name, String city)
        throws CreateException { }

    public void setEntityContext(EntityContext ctx) {
        context = ctx;
    }
    
    public void unsetEntityContext() {
        context = null;
    }
    
    public void ejbRemove() {
        Debug.print("TeamBean ejbRemove");
    }
    
    public void ejbLoad() {
        Debug.print("TeamBean ejbLoad");
    }
    
    public void ejbStore() {
        Debug.print("TeamBean ejbStore");
    }
    
    public void ejbPassivate() { }
    public void ejbActivate() { }


} // TeamBean class
