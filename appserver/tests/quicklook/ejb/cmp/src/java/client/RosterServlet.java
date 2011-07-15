/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import roster.Roster;
import roster.RosterHome;
import util.LeagueDetails;
import util.PlayerDetails;
import util.TeamDetails;
import java.util.Iterator;
import java.util.Set;



/**
 * @author unbekannt
 * @version 1.0
 */
public class RosterServlet extends HttpServlet{

    private PrintWriter out = null;

    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, java.io.IOException {
        processRequest(request, response);
    }

    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, java.io.IOException {
        processRequest(request, response);
    }

    /**
     *
     * @param req
     * @param res
     * @throws ServletException
     * @throws IOException
     */
    private void processRequest(HttpServletRequest req, HttpServletResponse res)
                      throws ServletException, IOException {

      res.setContentType("text/html");
      out = res.getWriter();

      out.println("<html>");
      out.println("<head>");
      out.println("<title>RosterApp Servlet-Client</title>");
      out.println("</head>");
      out.println("<body>");

       try {
           Context initial = new InitialContext();
           Object objref = initial.lookup("java:comp/env/ejb/SimpleRoster");

           RosterHome home =
               (RosterHome)PortableRemoteObject.narrow(objref,
                                            RosterHome.class);

//System.err.println("XXXX-1");
           Roster myRoster = home.create();
//System.err.println("XXXX-2");
            
           // deleting all exisiting DB-entries
           myRoster.cleanUp();
//System.err.println("XXXX-3");

           insertInfo(myRoster);
//System.err.println("XXXX-4");
           getSomeInfo(myRoster);
//System.err.println("XXXX-5");

           getMoreInfo(myRoster);
//System.err.println("XXXX-6");
           out.println("<BR>ROSTER-FINISHED-OK");

       } catch (Exception ex) {
           out.println("Caught an exception:");
           ex.printStackTrace(out);
           ex.printStackTrace();
       }

      out.println("</body>");
      out.println("</html>");
   }


    /**
     *
     * @param myRoster
     */
    private void getSomeInfo(Roster myRoster) {

       try {

           ArrayList playerList;
           ArrayList teamList;
           ArrayList leagueList;

           playerList = myRoster.getPlayersOfTeam("T2");
           printDetailsList(playerList, out);

           teamList = myRoster.getTeamsOfLeague("L1");
           printDetailsList(teamList, out);

           playerList = myRoster.getPlayersByPosition("defender");
           printDetailsList(playerList, out);


           leagueList = myRoster.getLeaguesOfPlayer("P28");
           printDetailsList(leagueList, out);

       } catch (Exception ex) {
           System.err.println("Caught an exception:");
           ex.printStackTrace();
       }

    } // getSomeInfo




    /**
     *
     * @param myRoster
     */
    private void getMoreInfo(Roster myRoster) {

       try {

           LeagueDetails leagueDetails;
           TeamDetails teamDetails;
           PlayerDetails playerDetails;
           ArrayList playerList;
           ArrayList teamList;
           ArrayList leagueList;
           ArrayList sportList;

           leagueDetails = myRoster.getLeague("L1");
           out.println("<BR>" + leagueDetails.toString());
           out.println();
           out.println("<BR> ----------------------------------------------------------");

           teamDetails = myRoster.getTeam("T3");
           out.println("<BR>" + teamDetails.toString());
           out.println();
           out.println("<BR> ----------------------------------------------------------");

           playerDetails = myRoster.getPlayer("P20");
           out.println("<BR>" + playerDetails.toString());
           out.println();
           out.println("<BR> ----------------------------------------------------------");

           playerList = myRoster.getPlayersOfTeam("T2");
           printDetailsList(playerList, out);

           teamList = myRoster.getTeamsOfLeague("L1");
           printDetailsList(teamList, out);

           playerList = myRoster.getPlayersByPosition("defender");
           playerList = myRoster.getAllPlayers();
           playerList = myRoster.getPlayersNotOnTeam();
           playerList = myRoster.getPlayersByPositionAndName("power forward",
               "Jack Patterson");
           playerList = myRoster.getPlayersByCity("Truckee");
           playerList = myRoster.getPlayersBySport("Soccer");
           playerList = myRoster.getPlayersByLeagueId("L1");

           playerList = myRoster.getPlayersByHigherSalary("Ian Carlyle");
           out.println("<BR>/////////////////////////////////////////////");
           printDetailsList(playerList, out);
           out.println("<BR>/////////////////////////////////////////////");
           playerList = myRoster.getPlayersBySalaryRange(500.00, 800.00);
           playerList = myRoster.getPlayersOfTeamCopy("T5");

           leagueList = myRoster.getLeaguesOfPlayer("P28");
           printDetailsList(leagueList, out);

           sportList = myRoster.getSportsOfPlayer("P28");
           printDetailsList(sportList, out);

           /****************************************************************
            *
            * new additions!!!!
            *
            ****************************************************************/
           leagueDetails = myRoster.getLeagueByName("Valley");
           out.println("<BR>" + leagueDetails.toString());
           out.println("<BR> ----------------------------------------------------------");

           leagueDetails = myRoster.getLeagueByName("Mountain");
           out.println("<BR>" + leagueDetails.toString());
           out.println("<BR> ----------------------------------------------------------");

           teamList = myRoster.getTeamsByPlayerAndLeague("P1", "L1");
           printDetailsList(teamList, out);

           Set cities = myRoster.getCitiesOfLeague("L2");
           Iterator it = cities.iterator();
           while (it.hasNext()) {
               out.println("<BR>" + it.next());
           }
           out.println("<BR> ----------------------------------------------------------");


           teamDetails = myRoster.getTeamOfLeagueByCity("L2", "Truckee");
           out.println("<BR>" + teamDetails.toString());
           out.println("<BR> ----------------------------------------------------------");

           out.println("<BR>" + myRoster.getTeamsNameOfLeagueByCity("L2", "Truckee"));
           out.println("<BR> ----------------------------------------------------------");

           out.println("<BR>" + myRoster.getSalaryOfPlayerFromTeam("T3", "Ben Shore"));
           out.println("<BR> ----------------------------------------------------------");

           playerList = myRoster.getPlayersOfLeague("L2");
           printDetailsList(playerList, out);

           playerList = myRoster.getPlayersWithPositionsGoalkeeperOrDefender();
           printDetailsList(playerList, out);

           playerList = myRoster.getPlayersWithNameEndingWithON();
           printDetailsList(playerList, out);

           playerList = myRoster.getPlayersWithNullName();
           printDetailsList(playerList, out);

           playerList = myRoster.getPlayersWithTeam("T5");
           printDetailsList(playerList, out);

           out.println("<BR>" + myRoster.getTeamNameVariations("T5"));
           out.println("<BR> ----------------------------------------------------------");


           playerList = myRoster.getPlayersWithSalaryUsingABS(100.1212121);
           printDetailsList(playerList, out);

           playerList = myRoster.getPlayersWithSalaryUsingSQRT(10000);
           printDetailsList(playerList, out);


           out.println("<BR> ----------------------------------------------------------");

           // internal NULL - parameter for finder
           playerList = myRoster.getPlayersByLeagueIdWithNULL("L1");
           printDetailsList(playerList, out);


       } catch (Exception ex) {
           System.err.println("Caught an exception:");
           ex.printStackTrace();
       }



    } // getMoreInfo


    /**
     *
     * @param list
     * @param out
     */
    private void printDetailsList(ArrayList list, PrintWriter out) {

        Iterator i = list.iterator();
        while (i.hasNext()) {
            Object details = (Object)i.next();
            out.println("<BR>" + details.toString());
        }
        out.println();
        out.println("<BR> ----------------------------------------------------------");
    } // printDetailsList


    /**
     *
     * @param myRoster
     */
    private void insertInfo(Roster myRoster) {

       try {
           // Leagues

           myRoster.createLeague(new LeagueDetails(
              "L1", "Mountain", "Soccer"));

           myRoster.createLeague(new LeagueDetails(
              "L2", "Valley", "Basketball"));

           // Teams

           myRoster.createTeamInLeague(new TeamDetails(
              "T1", "Honey Bees", "Visalia"), "L1");

           myRoster.createTeamInLeague(new TeamDetails(
              "T2", "Gophers", "Manteca"), "L1");

           myRoster.createTeamInLeague(new TeamDetails(
              "T3", "Deer", "Bodie"), "L2");

           myRoster.createTeamInLeague(new TeamDetails(
              "T4", "Trout", "Truckee"), "L2");

           myRoster.createTeamInLeague(new TeamDetails(
              "T5", "Crows", "Orland"), "L1");

           // Players, Team T1

           myRoster.createPlayer(new PlayerDetails(
              "P1", "Phil Jones", "goalkeeper", 100.00));
           myRoster.addPlayer("P1", "T1");

           myRoster.createPlayer(new PlayerDetails(
              "P2", "Alice Smith", "defender", 505.00));
           myRoster.addPlayer("P2", "T1");

           myRoster.createPlayer(new PlayerDetails(
              "P3", "Bob Roberts", "midfielder", 65.00));
           myRoster.addPlayer("P3", "T1");

           myRoster.createPlayer(new PlayerDetails(
              "P4", "Grace Phillips", "forward", 100.00));
           myRoster.addPlayer("P4", "T1");

           myRoster.createPlayer(new PlayerDetails(
              "P5", "Barney Bold", "defender", 100.00));
           myRoster.addPlayer("P5", "T1");

           // Players, Team T2

           myRoster.createPlayer(new PlayerDetails(
              "P6", "Ian Carlyle", "goalkeeper", 555.00));
           myRoster.addPlayer("P6", "T2");

           myRoster.createPlayer(new PlayerDetails(
              "P7", "Rebecca Struthers", "midfielder", 777.00));
           myRoster.addPlayer("P7", "T2");

           myRoster.createPlayer(new PlayerDetails(
              "P8", "Anne Anderson", "forward", 65.00));
           myRoster.addPlayer("P8", "T2");

           myRoster.createPlayer(new PlayerDetails(
              "P9", "Jan Wesley", "defender", 100.00));
           myRoster.addPlayer("P9", "T2");

           myRoster.createPlayer(new PlayerDetails(
              "P10", "Terry Smithson", "midfielder", 100.00));
           myRoster.addPlayer("P10", "T2");

           // Players, Team T3

           myRoster.createPlayer(new PlayerDetails(
              "P11", "Ben Shore", "point guard", 188.00));
           myRoster.addPlayer("P11", "T3");

           myRoster.createPlayer(new PlayerDetails(
              "P12", "Chris Farley", "shooting guard", 577.00));
           myRoster.addPlayer("P12", "T3");

           myRoster.createPlayer(new PlayerDetails(
              "P13", "Audrey Brown", "small forward", 995.00));
           myRoster.addPlayer("P13", "T3");

           myRoster.createPlayer(new PlayerDetails(
              "P14", "Jack Patterson", "power forward", 100.00));
           myRoster.addPlayer("P14", "T3");

           myRoster.createPlayer(new PlayerDetails(
              "P15", "Candace Lewis", "point guard", 100.00));
           myRoster.addPlayer("P15", "T3");

           // Players, Team T4

           myRoster.createPlayer(new PlayerDetails(
              "P16", "Linda Berringer", "point guard", 844.00));
           myRoster.addPlayer("P16", "T4");

           myRoster.createPlayer(new PlayerDetails(
              "P17", "Bertrand Morris", "shooting guard", 452.00));
           myRoster.addPlayer("P17", "T4");

           myRoster.createPlayer(new PlayerDetails(
              "P18", "Nancy White", "small forward", 833.00));
           myRoster.addPlayer("P18", "T4");

           myRoster.createPlayer(new PlayerDetails(
              "P19", "Billy Black", "power forward", 444.00));
           myRoster.addPlayer("P19", "T4");

           myRoster.createPlayer(new PlayerDetails(
              "P20", "Jodie James", "point guard", 100.00));
           myRoster.addPlayer("P20", "T4");

           // Players, Team T5

           myRoster.createPlayer(new PlayerDetails(
              "P21", "Henry Shute", "goalkeeper", 205.00));
           myRoster.addPlayer("P21", "T5");

           myRoster.createPlayer(new PlayerDetails(
              "P22", "Janice Walker", "defender", 857.00));
           myRoster.addPlayer("P22", "T5");

           myRoster.createPlayer(new PlayerDetails(
              "P23", "Wally Hendricks", "midfielder", 748.00));
           myRoster.addPlayer("P23", "T5");

           myRoster.createPlayer(new PlayerDetails(
              "P24", "Gloria Garber", "forward", 777.00));
           myRoster.addPlayer("P24", "T5");

           myRoster.createPlayer(new PlayerDetails(
              "P25", "Frank Fletcher", "defender", 399.00));
           myRoster.addPlayer("P25", "T5");

           // Players, no team

           myRoster.createPlayer(new PlayerDetails(
              "P26", "Hobie Jackson", "pitcher", 582.00));

           myRoster.createPlayer(new PlayerDetails(
              "P27", "Melinda Kendall", "catcher", 677.00));

           myRoster.createPlayer(new PlayerDetails(
              "P99", null, "_", 666.66));

           // Players, multiple teams

           myRoster.createPlayer(new PlayerDetails(
              "P28", "Constance Adams", "substitue", 966.00));
           myRoster.addPlayer("P28", "T1");
           myRoster.addPlayer("P28", "T3");




       } catch (Exception ex) {
           System.err.println("Caught an exception:");
           ex.printStackTrace();
       }

    } // insertInfo


}



