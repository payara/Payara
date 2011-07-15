/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.util;

import java.io.File;
import java.util.List;
import java.util.Properties;
import com.sun.enterprise.config.serverbeans.*;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandParameters;
import org.jvnet.hk2.config.types.Property;

/**
 * Parameters for the remote instance register instance command, which includes
 * params from _create-node and _register-instance
 * 
 * @author Jennifer Chou
 */
public class InstanceRegisterInstanceCommandParameters extends RegisterInstanceCommandParameters {

    @Param(name = ParameterNames.PARAM_NODEDIR, optional = true)
    public String nodedir = null;
    @Param(name = ParameterNames.PARAM_NODEHOST, optional = true)
    public String nodehost = null;
    @Param(name = ParameterNames.PARAM_INSTALLDIR, optional = true)
    public String installdir = null;
    @Param(name = ParameterNames.PARAM_TYPE, optional = true, defaultValue = "CONFIG")
    public String type = "CONFIG";
    @Param(name = ParameterNames.PARAM_SYSTEMPROPERTIES, optional = true, separator = ':')
    public Properties systemProperties;
    /*@Param(name = ParameterNames.PARAM_SSHPORT, optional = true)
    public String sshPort = "-1";
    @Param(name = ParameterNames.PARAM_SSHHOST, optional = true)
    public String sshHost = null;
    @Param(name = ParameterNames.PARAM_SSHUSER, optional = true)
    public String sshuser = null;
    @Param(name = ParameterNames.PARAM_SSHKEYFILE, optional = true)
    public String sshkeyfile;
    @Param(name = ParameterNames.PARAM_SSHPASSWORD, optional = true)
    public String sshpassword;
    @Param(name = ParameterNames.PARAM_SSHKEYPASSPHRASE, optional = true)
    public String sshkeypassphrase;*/

    /* instance params */
    //@Param(name = "resourceref", optional = true)
    //public  List<String> resourceRefs;
    //@Param(name = "applicationref", optional = true)
    //public  List<String> appRefs;

    public static class ParameterNames {

        //public static final String PARAM_RESOURCEREF = "resourceref";
        //public static final String PARAM_APPLICATIONREF = "applicationref";
        public static final String PARAM_NODEDIR = "nodedir";
        public static final String PARAM_NODEHOST = "nodehost";
        public static final String PARAM_INSTALLDIR = "installdir";
        public static final String PARAM_TYPE = "type";
        public static final String PARAM_SYSTEMPROPERTIES = "systemproperties";
        /*public static final String PARAM_SSHPORT = "sshport";
        public static final String PARAM_SSHHOST = "sshhost";
        public static final String PARAM_SSHUSER = "sshuser";
        public static final String PARAM_SSHKEYFILE = "sshkeyfile";
        public static final String PARAM_SSHPASSWORD = "sshpassword";
        public static final String PARAM_SSHKEYPASSPHRASE = "sshkeypassphrase";*/
    }


}
