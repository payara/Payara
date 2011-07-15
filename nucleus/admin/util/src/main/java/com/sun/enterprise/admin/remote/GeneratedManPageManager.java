/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.remote;

import com.sun.enterprise.universal.NameValue;
import com.sun.enterprise.universal.glassfish.AdminCommandResponse;
import java.util.*;

/**
 *
 * @author bnevins
 */
class GeneratedManPageManager implements ResponseManager{
    GeneratedManPageManager(AdminCommandResponse response) {
        this.response = response;
    }

    public void process() throws RemoteException {
        String usageText = response.getValue(AdminCommandResponse.SYNOPSIS);

        if(usageText == null) {
            // this is one way to figure out there was an error!
            throw new RemoteFailureException("XXXXXXXXXXXXXXXXXXXX", 
                    response.getMainMessage());
        }
        setName();
        setSynopsis();
        setParamsAndOperands();
        StringBuilder sb = new StringBuilder();
        sb.append(EOL);
        printName(sb);
        printSynopsis(sb);
        printParams(sb);
        printOperands(sb);
        throw new RemoteSuccessException(sb.toString());
    }

    private void setName() {
        name = displayInProperLen(response.getMainMessage());
    }

    private void printName(StringBuilder sb) {
        sb.append("NAME :").append(EOL);
        sb.append(name).append(EOL);
    }

    private void setSynopsis() {
        synopsis = response.getValue(AdminCommandResponse.SYNOPSIS);
        
        if (synopsis.startsWith("Usage: ")) {
            synopsis = synopsis.substring(7);     
        }
        // this looks too horrible - go with one long line...
        //synopsis = displayInProperLen(synopsis);
    }
    private void printSynopsis(StringBuilder sb) {
        sb.append("SYNOPSIS :").append(EOL);
        sb.append(TAB).append(synopsis).append(EOL);
        sb.append(EOL);
    }

    private void setParamsAndOperands() {
        List<NameValue<String,String>> list = response.getMainKeys();
        
        for(NameValue<String,String> nv : list) {
            String name = nv.getName();
            if(name.equals(AdminCommandResponse.SYNOPSIS))
                continue;
            if(name.endsWith("operand")) 
                operands.add(nv);
            else
                params.add(nv);
        }
    }

    private void printParams(StringBuilder sb) {
        sb.append("OPTIONS :").append(EOL);
        
        for(NameValue<String,String> nv : params) {
            sb.append(TAB + "--").append(nv.getName()).append(EOL);
            sb.append(displayInProperLen(nv.getValue()));
            sb.append(EOL);
        }
    }

    private void printOperands(StringBuilder sb) {

        sb.append("OPERANDS :").append(EOL);

        for(NameValue<String,String> nv : operands) {
            String key = nv.getName();
            // peel off "_operand"
            key = key.substring(0, key.length() - 8);
            String value = nv.getValue();
            sb.append(displayInProperLen(key + " - " + value));
            sb.append(EOL);
        }
    }
       
    // bnevins -- original code reused, this looks painful to change...
    private String displayInProperLen(String strToDisplay) {
        int index = 0;
        StringBuilder sb = new StringBuilder();
        
        for (int ii=0; ii+70<strToDisplay.length();ii+=70) {
            index=ii+70;
            String subStr = strToDisplay.substring(ii, index+1);
            if (subStr.endsWith(" ") || subStr.endsWith(",") ||
                subStr.endsWith(".") || subStr.endsWith("-") ) {
                sb.append(TAB + subStr + EOL);
                ii++;
                index++;
            } else {
                sb.append(TAB + strToDisplay.substring(ii, index) + "-" + EOL);
            }
        }
        if (index < strToDisplay.length()) {
            sb.append(TAB + strToDisplay.substring(index) + EOL);
        }
        
        return sb.toString();
    }

    AdminCommandResponse response;
    private String name; 
    private String synopsis; 
    List<NameValue<String,String>> params = new LinkedList<NameValue<String,String>>();
    List<NameValue<String,String>> operands = new LinkedList<NameValue<String,String>>();
    private static final String TAB = "    ";
    private static final String EOL = System.getProperty("line.separator");
}
/*
 --------  RESPONSE DUMP         --------------
Signature-Version: 1.0
keys: GeneratedHelp;connectionpoolid;enabled;description;jndi_name_ope
 rand;property;target;SYNOPSYS
enabled_value: Determines whether the JDBC resource is enabled at runt
 ime. The default value is true.
SYNOPSYS_name: SYNOPSYS
target_name: target
message: create-jdbc-resource - creates a JDBC resource with the speci
 fied JNDI name
connectionpoolid_value: The name of the JDBC connection pool. If two o
 r more JDBC resource elements point to the same connection pool eleme
 nt, they use the same pool connection at runtime.
description_value: Text providing descriptive details about the JDBC r
 esource.
property_value: 
jndi_name_operand_name: jndi_name_operand
enabled_name: enabled
exit-code: SUCCESS
property_name: property
GeneratedHelp_value: true
SYNOPSYS_value: Usage: create-jdbc-resource --connectionpoolid=connect
 ionpoolid [--enabled=true] [--description=description] [--property=pr
 operty] [--target=server] jndi_name 
GeneratedHelp_name: GeneratedHelp
connectionpoolid_name: connectionpoolid
description_name: description
jndi_name_operand_value: The JNDI name of this JDBC resource.
target_value: 


----------------------------------------------
NAME :
	create-jdbc-resource - creates a JDBC resource with the specified JNDI 
	name

SYNOPSIS :
	create-jdbc-resource --connectionpoolid=connectionpoolid [--enabled=true] [--description=description] [--property=property] [--target=server] jndi_name 

OPTIONS : 
	--GeneratedHelp
	true

	--connectionpoolid
	The name of the JDBC connection pool. If two or more JDBC resource ele-
	ments point to the same connection pool element, they use the same poo-
	l connection at runtime.

	--enabled
	Determines whether the JDBC resource is enabled at runtime. The defaul-
	t value is true.

	--description
	Text providing descriptive details about the JDBC resource.

	--property

	--target

OPERANDS : 
	jndi_name - The JNDI name of this JDBC resource.

Command create-jdbc-resource executed successfully. 
 */
