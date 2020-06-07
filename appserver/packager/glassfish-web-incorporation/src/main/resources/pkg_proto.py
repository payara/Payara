#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
#
# The contents of this file are subject to the terms of either the GNU
# General Public License Version 2 only ("GPL") or the Common Development
# and Distribution License("CDDL") (collectively, the "License").  You
# may not use this file except in compliance with the License.  You can
# obtain a copy of the License at
# https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
# or packager/legal/LICENSE.txt.  See the License for the specific
# language governing permissions and limitations under the License.
#
# When distributing the software, include this License Header Notice in each
# file and include the License file at packager/legal/LICENSE.txt.
#
# GPL Classpath Exception:
# Oracle designates this particular file as subject to the "Classpath"
# exception as provided by Oracle in the GPL Version 2 section of the License
# file that accompanied this code.
#
# Modifications:
# If applicable, add the following below the License Header, with the fields
# enclosed by brackets [] replaced by your own identifying information:
# "Portions Copyright [year] [name of copyright owner]"
#
# Contributor(s):
# If you wish your version of this file to be governed by only the CDDL or
# only the GPL Version 2, indicate your decision by adding "[Contributor]
# elects to include this software in this distribution under the [CDDL or GPL
# Version 2] license."  If you don't indicate a single choice of license, a
# recipient has the option to distribute your version of this file under
# either the CDDL, the GPL Version 2 or to extend the choice of license to
# its licensees as provided above.  However, if you add GPL Version 2 code
# and therefore, elected the GPL Version 2 license, then the option applies
# only if the new code is made subject to such option by the copyright
# holder.
#
# Portions Copyright [2019-2020] [Payara Foundation and/or its affiliates]

import imp

conf = imp.load_source("pkg_conf", "../pkg_conf.py")

pkg = {
    "name"          : "glassfish-web-incorporation",
    "version"       : conf.glassfish_version,
    "attributes"    : {
                        "pkg.summary" : "GlassFish Web Profile Incorporation",
                        "pkg.description" : "GlassFish Web Profile Incorporation Package.  "+conf.glassfish_description_long,
                        "info.classification" : conf.glassfish_info_classification,
                      },
    "depends"       : { 
	    		"pkg:/h2db@" + conf.h2db_version : {"type" : "incorporate" },
			"pkg:/pkg-java@1" : {"type" : "incorporate" },
			"pkg:/felix@" + conf.felix_version : {"type" : "incorporate" },
			"pkg:/glassfish-hk2@" + conf.glassfish_version : {"type" : "incorporate" },
		 	"pkg:/glassfish-grizzly@" + conf.grizzly_version : {"type" : "incorporate" },
			"pkg:/glassfish-nucleus@" +conf.glassfish_version : {"type" : "incorporate" },
			"pkg:/glassfish-grizzly-full@" + conf.grizzly_version : {"type" : "incorporate" },
			"pkg:/glassfish-common@" + conf.glassfish_version : {"type" : "incorporate" },
		 	"pkg:/shoal@" + conf.shoal_version : {"type" : "incorporate" },
			"pkg:/glassfish-cluster@" + conf.glassfish_version : {"type" : "incorporate" },
			"pkg:/glassfish-ha@" + conf.glassfish_version : {"type" : "incorporate" },
			"pkg:/jersey@2" : {"type" : "incorporate" },
			"pkg:/glassfish-management@" + conf.glassfish_version : {"type" : "incorporate" },
                        "pkg:/glassfish-commons-web@" + conf.glassfish_version : {"type" : "incorporate" },
			"pkg:/glassfish-jca@" + conf.glassfish_version : {"type" : "incorporate" },
			"pkg:/glassfish-jpa@" + conf.glassfish_version : {"type" : "incorporate" },
			"pkg:/glassfish-jta@" + conf.glassfish_version : {"type" : "incorporate" },
			"pkg:/glassfish-corba-base@" + conf.corba_version : {"type" : "incorporate" },
			"pkg:/glassfish-jts@" + conf.glassfish_version : {"type" : "incorporate" },
			"pkg:/glassfish-ejb-lite@" + conf.glassfish_version : {"type" : "incorporate" },
			"pkg:/glassfish-jsf@" + conf.jsf_version : {"type" : "incorporate" },
			"pkg:/glassfish-web@" + conf.glassfish_version : {"type" : "incorporate" },
                        "pkg:/glassfish-osgi-http@" + conf.glassfish_version : {"type" : "incorporate" },
			"pkg:/glassfish-jcdi@" + conf.glassfish_version : {"type" : "incorporate" },
			"pkg:/glassfish-jdbc@" + conf.glassfish_version : {"type" : "incorporate" },
			"pkg:/glassfish-gui@" + conf.glassfish_version : {"type" : "incorporate" },	
			"pkg:/glassfish-scripting@3.1" : {"type" : "incorporate" },	
                      },
    "licenses"      : {
                        "../../../../CDDL+GPL.txt" : {"license" : "CDDL and GPL v2 with classpath exception"},
                      },
}
