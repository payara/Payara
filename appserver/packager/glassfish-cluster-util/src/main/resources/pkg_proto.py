#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

import imp


pkg = {
    "name"          : "glassfish-cluster-util",
    "version"       : "1.0-0.0",
    "attributes"    : {
                        "pkg.summary" : "GlassFish v3 Cluster Administration Utility",
                        "pkg.description" : "The GlassFish v3 Cluster Administration Utility enables users to run a single command to perform the same administrative task on multiple Enterprise Server instances, for example, instances in a cluster where Apache mod_jk is used as load balancer. The Cluster Administration Utility can be used to administer multiple instances in the same way that the asadmin utility can be used to administer a single instance. For information about how to use the Cluster Administration Utility to administer multiple Enterprise Server instances in Amazon Elastic Compute Cloud (EC2), see Sun GlassFish Enterprise Server v3 EC2 Images Guide at http://docs.sun.com/doc/821-1300.",
                        "info.classification" : "Application Servers",
                      },
    "depends"       : { 
                        "pkg:/glassfish-nucleus" : {"type" : "require" },
                      },
    "dirtrees"      : [ "glassfish", "bin" ],
    "licenses"      : {
                        "../../../../CDDL+GPL.txt" : {"license" : "CDDL and GPL v2 with classpath exception"},
                      },
}
