#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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

# version should be in the following format:
# <product-version>,0-<build-number>
#
# We have four types of releases, Major (3.0), Minor (3.1), Update (3.0 UR1) 
# and Patch (3.0 UR1 Patch 1), so we are going to let our version number go 
# upto four digits. However, we will start with 3.0 and add more digits as 
# needed.
#
# As for build-number, we will use three digits. The first two digits will 
# correspond to our well known build numbers with alphabets in build number 
# being translated to the second digit (so build 17 will become 17.0 and 
# build 18a will be 18.1). The third digit will default to 0 and will be 
# used when we start publishing our nightly builds to a package repository. 
# In that case, the number will be incremented by 1 for each nightly build. 
# We will start with 2-digit form and add the third digit when we need it. 
# So, to start with our package versions would look like 3.0,0-18.0.
#
# Now, there are some packages used in GlassFish that have their own 
# well-defined versions (for example, grizzly, Felix, JavaDB etc.) and we 
# will use that. We will not add build numbers for these packages. For 
# example, grizzly version would look like 1.8.2-0,0. 

glassfish_version="4.0.1,0-${build.id}"
felix_version="4.0.2,0-0"
corba_version="4.0.0,0-4"
jsf_version="2.2.7,0-0"
grizzly_version="2.3.13,0-0"
metro_version="2.3.1,0-414"
javahelp_version="2.0.2,0-1"
shoal_version="1.6.18,0-0"

#description
glassfish_description="GlassFish Application Server"
glassfish_description_long="GlassFish Server is a modular and lightweight Java EE 7 compliant application server. Key features include an OSGi runtime, fast startup time, maven support, and rapid iterative development with Active Redeploy.  GlassFish Server also offers feature-rich administration with the web console, command line, and RESTful API."
glassfish_info_classification="Application Servers"
