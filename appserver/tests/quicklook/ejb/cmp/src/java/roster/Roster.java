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

package roster;

import java.util.ArrayList;
import javax.ejb.EJBLocalObject;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import util.LeagueDetails;
import util.PlayerDetails;
import util.TeamDetails;
import java.util.Set;

public interface Roster extends EJBLocalObject {
 
    // Players

    public void createPlayer(PlayerDetails details) 
        ;

    public void addPlayer(String playerId, String teamId) 
        ;

    public void removePlayer(String playerId) 
        ;

    public void dropPlayer(String playerId, String teamId) 
        ;

    public PlayerDetails getPlayer(String playerId) 
        ;

    public ArrayList getPlayersOfTeam(String teamId) 
        ;

    public ArrayList getPlayersOfTeamCopy(String teamId) 
        ;

    public ArrayList getPlayersByPosition(String position) 
        ;

    public ArrayList getPlayersByHigherSalary(String name) 
        ;

    public ArrayList getPlayersBySalaryRange(double low, double high) 
        ;

    public ArrayList getPlayersByLeagueId(String leagueId) 
        ;

    public ArrayList getPlayersBySport(String sport) 
        ;

    public ArrayList getPlayersByCity(String city) 
        ;

    public ArrayList getAllPlayers() 
        ;

    public ArrayList getPlayersNotOnTeam() 
        ;

    public ArrayList getPlayersByPositionAndName(String position, 
        String name) ;

    public ArrayList getLeaguesOfPlayer(String playerId)
        ;

    public ArrayList getSportsOfPlayer(String playerId)
        ;
        
    public double getSalaryOfPlayerFromTeam(String teamID, String playerName)
        ;

    public ArrayList getPlayersOfLeague(String leagueId)
        ;   
        

    public ArrayList getPlayersWithPositionsGoalkeeperOrDefender()
        ;   

    public ArrayList getPlayersWithNameEndingWithON() 
        ;

    public ArrayList getPlayersWithNullName()
        ;   

    public ArrayList getPlayersWithTeam(String teamId)
        ;
        
    public ArrayList getPlayersWithSalaryUsingABS(double salary)
        ; 

    public ArrayList getPlayersWithSalaryUsingSQRT(double salary) 
        ;
    
           
    // Teams

    public ArrayList getTeamsOfLeague(String leagueId) 
        ;

    public void createTeamInLeague(TeamDetails details, String leagueId) 
        ;

    public void removeTeam(String teamId) 
        ;

    public TeamDetails getTeam(String teamId) 
        ;

    public ArrayList getTeamsByPlayerAndLeague(String playerKey,
                                               String leagueKey)
                                               ;	
 
    public Set getCitiesOfLeague(String leagueKey) ;

    public TeamDetails getTeamOfLeagueByCity(String leagueKey, String city)
        ;	   

    public String getTeamsNameOfLeagueByCity(String leagueKey, String city)
        ;	   

    public  String getTeamNameVariations(String teamId) ;

    // Leagues

    public void createLeague(LeagueDetails details) 
        ;

    public void removeLeague(String leagueId) 
        ;

    public LeagueDetails getLeague(String leagueId) 
        ;

    public LeagueDetails getLeagueByName(String name)
        ;
        
    // Test

    public ArrayList getPlayersByLeagueIdWithNULL(String leagueId)  ;

    public ArrayList testFinder(String parm1, String parm2, String parm3)
        ;
        
    public void cleanUp() throws FinderException, RemoveException;
        
}
