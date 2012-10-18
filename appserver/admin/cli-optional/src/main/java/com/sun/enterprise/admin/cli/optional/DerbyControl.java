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

package com.sun.enterprise.admin.cli.optional;

import java.io.File;
import java.lang.reflect.Method;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.glassfish.api.admin.CommandException;
import com.sun.enterprise.util.i18n.StringManager;

/**
 *  This class uses Java reflection to invoke Derby
 *  NetworkServerControl class.
 *  This class is used to start/stop/ping derby database.
 *  The reason for creating this class instead of directly
 *  invoking NetworkServerControl from the StartDatabaseCommand
 *  class is so that a separate JVM is launched when starting the
 *  database and the control is return to CLI.  
 *  @author <a href="mailto:jane.young@sun.com">Jane Young</a>
 *  @version  $Revision: 1.13 $
 */
public final class DerbyControl
{
    final public static String DB_LOG_FILENAME = "derby.log";
    final private String derbyCommand;
    final private String derbyHost;
    final private String derbyPort;
    final private String derbyHome;
    final private boolean redirect;
    final private String derbyUser;
    final private String derbyPassword;


        //constructor 
    public DerbyControl(final String dc, final String dht, final String dp,
                        final String redirect, final String dhe, final String duser, final String dpwd)
    {
        this.derbyCommand = dc;
        this.derbyHost = dht;
        this.derbyPort = dp;
        this.derbyHome = dhe;
	this.redirect = Boolean.valueOf(redirect).booleanValue();
        this.derbyUser = duser;
        this.derbyPassword = dpwd;

	    if (this.redirect) {

	        try {
                String dbLog = "";
                if (this.derbyHome == null) {
                    // if derbyHome is null then redirect the output to a temporary file
                    // which gets deleted after the jvm exists.
		            dbLog = createTempLogFile();
                }
		    else {
		        dbLog = createDBLog(this.derbyHome);
		    }
            
                //redirect stdout and stderr to a file
                PrintStream printStream = new PrintStream(new FileOutputStream(dbLog, true), true);
                System.setOut(printStream);
                System.setErr(printStream);
            }
            catch (Throwable t) {
	            t.printStackTrace();
	            //exit with an error code of 2
	            Runtime.getRuntime().exit(2);
            }
        }
        //do not set derby.system.home if derbyHome is empty
        if (this.derbyHome!=null && this.derbyHome.length()>0) {
            System.setProperty("derby.system.home", this.derbyHome);
        }
	    //set the property to not overwrite log file
	    System.setProperty("derby.infolog.append", "true");
    }
    
        //constructor
    public DerbyControl(final String dc, final String dht, final String dp)
    {
        this(dc,dht,dp,"true", null, null, null);
    }
    
        //constructor
    public DerbyControl(final String dc, final String dht, final String dp, final String redirect)
    {
        this(dc,dht,dp,redirect,null, null, null);
    }

     //constructor
    public DerbyControl(final String dc, final String dht, final String dp, final String redirect, final String dhe)
    {
        this(dc,dht,dp,redirect,dhe, null, null);
    }

    public DerbyControl(final String dc, final String dht, final String dp, final String redirect, final String duser, final String dpassword)
    {
        this(dc,dht,dp,redirect,null, duser, dpassword);
    }

        /**
         * This methos invokes the Derby's NetworkServerControl to start/stop/ping
         * the database.
         */
    private void invokeNetworkServerControl()
    {
        try {
            Class networkServer = Class.forName("org.apache.derby.drda.NetworkServerControl");
            Method networkServerMethod = networkServer.getDeclaredMethod("main",
                                                       new Class[]{String[].class});
            Object [] paramObj = null;
            if (derbyUser == null && derbyPassword == null) {
                paramObj = new Object[]{new String[]{derbyCommand, "-h", derbyHost, "-p", derbyPort}};
            } else {
                paramObj = new Object[]{new String[]{derbyCommand, "-h", derbyHost, "-p", derbyPort, "-user", derbyUser, "-password", derbyPassword}};
            }
           
            networkServerMethod.invoke(networkServer, paramObj);
        }
        catch (Throwable t) {
	        t.printStackTrace();
	        Runtime.getRuntime().exit(2);
        }
    }


        /**
         * create a db.log file that stdout/stderr will redirect to.
         * dbhome is the derby.system.home directory where derb.log
         * gets created.  if user specified --dbhome options, derby.log
         * will be created there.
         **/
    private String createDBLog(final String dbHome) throws Exception
    {
        //dbHome must exist and  have write permission
        final File fDBHome = new File(dbHome);
	    String dbLogFileName = "";

        final StringManager lsm = StringManager.getManager(DerbyControl.class);
        if (fDBHome.isDirectory() && fDBHome.canWrite()) {
            final File fDBLog = new File(dbHome, DB_LOG_FILENAME);
	        dbLogFileName = fDBLog.toString();

            //if the file exists, check if it is writeable
            if (fDBLog.exists() && !fDBLog.canWrite()) {
	        System.out.println(lsm.getString("UnableToAccessDatabaseLog", dbLogFileName));
	        System.out.println(lsm.getString("ContinueStartingDatabase"));
	        //if exist but not able to write then create a temporary 
	        //log file and persist on starting the database
	        dbLogFileName = createTempLogFile();
            }
            else if (!fDBLog.exists()) {
                //create log file
                if (!fDBLog.createNewFile()) {
                    System.out.println(lsm.getString("UnableToCreateDatabaseLog", dbLogFileName));
                }
            }
        }
        else {
            System.out.println(lsm.getString("InvalidDBDirectory", dbHome));
	        System.out.println(lsm.getString("ContinueStartingDatabase"));
	        //if directory does not exist then create a temporary log file
	        //and persist on starting the database
	        dbLogFileName = createTempLogFile();
        }
        return dbLogFileName;
    }

   /**
      creates a temporary log file.
   */
    private String  createTempLogFile() throws CommandException
    {
        String tempFileName = "";
        try {
            final File fTemp = File.createTempFile("foo", null);
            fTemp.deleteOnExit();
	        tempFileName = fTemp.toString();
	    }
	    catch (IOException ioe) {
            final StringManager lsm = StringManager.getManager(DerbyControl.class);
	        throw new CommandException(lsm.getString("UnableToAccessDatabaseLog", tempFileName));
	    }
        return tempFileName;
    }    
    
    public static void main(String[] args) {
        
        if (args.length<3){
            System.out.println("paramters not specified.");
            System.out.println("DerbyControl <derby command> <derby host> <derby port> <derby home> <redirect output>");
            System.exit(1);
        }
        
        DerbyControl derbyControl = null;
        if (args.length == 3)
            derbyControl = new DerbyControl(args[0], args[1], args[2]);
        else if (args.length == 4 )
            derbyControl = new DerbyControl(args[0], args[1], args[2], args[3]);
        else if (args.length == 5)
             derbyControl = new DerbyControl(args[0], args[1], args[2], args[3], args[4]);
        else if (args.length > 5)
            derbyControl = new DerbyControl(args[0], args[1], args[2], args[3], args[4], args[5]);
        if (derbyControl != null)
            derbyControl.invokeNetworkServerControl();
    }
}


    
