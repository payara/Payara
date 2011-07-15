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

package org.glassfish.jms.admin.cli;

import java.io.Serializable;
import java.util.Properties;


/**
    A class representing the <code> information </code> on one JMS destination
    created by the JMS provider.

   @author  Isa Hashim
    @version  1.0
*/

public class JMSDestinationInfo implements Serializable
{
    private String	destName,
			destType;
    private Properties	attrs;

    public JMSDestinationInfo (String destName, String destType)
    {
       //todo: need to enabel this
       // ArgChecker.checkValid(destName, "destName",
         //                     StringValidator.getInstance()); //noi18n
        //ArgChecker.checkValid(destType, "destType",
          //                    StringValidator.getInstance()); //noi18n

        this.destName = destName;
        this.destType = destType;
        this.attrs = new Properties();
    }

    public JMSDestinationInfo (String destName, String destType, Properties attrs)
    {
        this(destName, destType);
        //ArgChecker.checkValid(attrs, "attrs"); //noi18n
        this.attrs = attrs;
    }

    public String getDestinationName()  {
        return (destName);
    }

    public String getDestinationType()  {
        return (destType);
    }

    public Properties getAttrs()  {
        return (attrs);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(destName);
        sb.append(' ');
        sb.append(destType);
        sb.append(' ');
        sb.append(attrs);
        return sb.toString();
    }
}
