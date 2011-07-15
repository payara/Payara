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

package com.sun.enterprise.tools.upgrade.common;


import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.tools.upgrade.common.arguments.ArgumentHandler;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.tools.upgrade.common.arguments.ARG_target;
import com.sun.enterprise.tools.upgrade.common.arguments.ARG_source;
import com.sun.enterprise.tools.upgrade.logging.LogService;

/**
 * Parse the arguments for the upgrade tool
 * and invoke the appropriate handler
 *
 * @author Hans Hrasna
 */
public class ArgsParser {

    private static final Logger _logger = LogService.getLogger();
    private static final StringManager sm =
        StringManager.getManager(ArgsParser.class);

    private CommonInfoModel commonInfo;
    
    /** Creates a new instance of ArgsParser */
	public ArgsParser() {
        commonInfo = CommonInfoModel.getInstance();
    }
		
	public ArrayList<ArgumentHandler> parse(String [] args){
		ArrayList<ArgumentHandler> aList = new ArrayList<ArgumentHandler>();
		String tmpArg;
        int srcIndx = 0;
        int trgIndx = 0;
		for(int i =0; i < args.length; i++){
			//- System.out.println("lenght: " + args.length + "\ti: " + i);
			tmpArg = args[i];
			if (tmpArg.startsWith("-")){
				//- strip option designator '-' or '--'
				tmpArg = tmpArg.substring(tmpArg.lastIndexOf("-") + 1);
				ArgumentHandler aHandler = getArgHandler(tmpArg);
				if(aHandler.isRequiresParameter() && i+1 < args.length){
					aHandler.setRawParameters(args[++i]);
				}
				aList.add(aHandler);
				// --passwordfile/-f creates credential ArgumentHandlers
				aList.addAll(aHandler.getChildren());
                if (aHandler instanceof ARG_target){
                    trgIndx = aList.indexOf(aHandler);
                }
                if (aHandler instanceof ARG_source){
                    srcIndx = aList.indexOf(aHandler);
                }
			} 
			//-System.out.println("   tmpArg: " + tmpArg);
		}

        //- force the src location to preceed target location
        //- so future processing will proceed properly.
        if (srcIndx > trgIndx){
            ArgumentHandler tmpA = aList.remove(srcIndx);
            aList.add(trgIndx, tmpA);
        }
		return aList;
	}

    
	private ArgumentHandler getArgHandler(String cmd) {
		ArgumentHandler aHandler = null;
		Class clazz = null;
		try {
			clazz = Class.forName("com.sun.enterprise.tools.upgrade.common.arguments.ARG_" + cmd);
		} catch (ClassNotFoundException cnf) {
			try {
				clazz = Class.forName("com.sun.enterprise.tools.upgrade.common.arguments.ARG_UnknownCmd");
			} catch (ClassNotFoundException cnf1) {
				_logger.log(Level.INFO, sm.getString(
					"enterprise.tools.upgrade.cli.arg_unknow_class_not_found"));
			}
		} catch (Exception e1) {
			_logger.log(Level.INFO, sm.getString("enterprise.tools.upgrade.cli.invalid_option",e1), e1);
		}
		
		try {
			aHandler = (ArgumentHandler)clazz.getConstructor().newInstance();
			aHandler.setCmd(cmd);
		} catch (Exception ex) {
			_logger.log(Level.INFO,
				sm.getString("enterprise.tools.upgrade.cli.invalid_option",cmd), ex);
			System.exit(1);
		}
		return aHandler;
	}
}
