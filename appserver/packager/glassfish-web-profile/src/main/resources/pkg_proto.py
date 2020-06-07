#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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
    "name"          : "glassfish-web-profile",
    "version"       : conf.glassfish_version,
    "attributes"    : {
                        "pkg.summary" : "GlassFish Web Profile",
                        "pkg.description" : "GlassFish Web Profile Metapackage.  "+conf.glassfish_description_long,
                        "info.classification" : conf.glassfish_info_classification,
                      },
    "depends"       : { 
                        "pkg:/h2db" : {"type" : "require" },
			"pkg:/pkg-java" : {"type" : "require" },
			"pkg:/felix" : {"type" : "require" },
                        "pkg:/appserver-core" : {"type" : "require" },
			"pkg:/glassfish-hk2" : {"type" : "require" },
		 	"pkg:/glassfish-grizzly" : {"type" : "require" },
			"pkg:/glassfish-nucleus" : {"type" : "require" },
			"pkg:/glassfish-grizzly-full" : {"type" : "require" },
			"pkg:/glassfish-common" : {"type" : "require" },
			"pkg:/shoal" : {"type" : "require" },
			"pkg:/glassfish-cluster" : {"type" : "require" },
			"pkg:/glassfish-ha" : {"type" : "require" },
			"pkg:/jersey@1.8" : {"type" : "require" },
			"pkg:/glassfish-management" : {"type" : "require" },
                        "pkg:/glassfish-common-web" : {"type" : "require" },
			"pkg:/glassfish-jca" : {"type" : "require" },
			"pkg:/glassfish-jpa" : {"type" : "require" },
			"pkg:/glassfish-jta" : {"type" : "require" },
			"pkg:/glassfish-corba-base" : {"type" : "require" },
			"pkg:/glassfish-jts" : {"type" : "require" },
			"pkg:/glassfish-ejb-lite" : {"type" : "require" },
			"pkg:/glassfish-jsf" : {"type" : "require" },
			"pkg:/glassfish-web" : {"type" : "require" },
                        "pkg:/glassfish-osgi-http" : {"type" : "require" },
			"pkg:/glassfish-jcdi" : {"type" : "require" },
			"pkg:/glassfish-jdbc" : {"type" : "require" },
			"pkg:/glassfish-gui" : {"type" : "require" },
			"pkg:/glassfish-web-incorporation" : {"type" : "require" },
                      },
    "licenses"      : {
                        "../../../../CDDL+GPL.txt" : {"license" : "CDDL and GPL v2 with classpath exception"},
                      },
}
