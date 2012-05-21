rem @echo off

rem  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

rem  Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.

rem  The contents of this file are subject to the terms of either the GNU
rem  General Public License Version 2 only ("GPL") or the Common Development
rem  and Distribution License("CDDL") (collectively, the "License").  You
rem  may not use this file except in compliance with the License.  You can
rem  obtain a copy of the License at
rem  https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
rem  or packager/legal/LICENSE.txt.  See the License for the specific
rem  language governing permissions and limitations under the License.

rem  When distributing the software, include this License Header Notice in each
rem  file and include the License file at packager/legal/LICENSE.txt.

rem  GPL Classpath Exception:
rem  Oracle designates this particular file as subject to the "Classpath"
rem  exception as provided by Oracle in the GPL Version 2 section of the License
rem  file that accompanied this code.

rem  Modifications:
rem  If applicable, add the following below the License Header, with the fields
rem  enclosed by brackets [] replaced by your own identifying information:
rem  "Portions Copyright [year] [name of copyright owner]"

rem  Contributor(s):
rem  If you wish your version of this file to be governed by only the CDDL or
rem  only the GPL Version 2, indicate your decision by adding "[Contributor]
rem  elects to include this software in this distribution under the [CDDL or GPL
rem  Version 2] license."  If you don't indicate a single choice of license, a
rem  recipient has the option to distribute your version of this file under
rem  either the CDDL, the GPL Version 2 or to extend the choice of license to
rem  its licensees as provided above.  However, if you add GPL Version 2 code
rem  and therefore, elected the GPL Version 2 license, then the option applies
rem  only if the new code is made subject to such option by the copyright
rem  holder.

if "%1" == "" goto skip_goto
  goto %1
:skip_goto

if not "%GF_HOME" == "" goto gf_home_ok
 echo You must define GF_HOME (e.g., set GF_HOME = D:\Programs\glassfish3)
 goto end
:gf_home_ok

set PLUGIN

if not "%MODULES_DIR%" == "" modules_dir_ok
 set MODULES_DIR=%GF_HOME%\glassfish\modules
:modules_dir_ok

echo Stopping the server
call asadmin undeploy admin-console
call asadmin stop-domain

:clean

echo Clearing the OSGi cache
rm -R %GF_HOME%\glassfish\domains\domain1\osgi-cache
rm -Rf %GF_HOME%\glassfish\domains\domain1\generated

echo Removing any existing demo plugins from %MODULES_DIR%
rm %MODULES_DIR%/plugin-*.jar

echo Installing modules to %MODULES_DIR%
cp plugin-system\target\plugin-system-*.jar %MODULES_DIR%
for /D %%p in (plugins\*)  do cp %%p\target\plugin-*.jar %MODULES_DIR%

echo Starting the server
call asadmin start-domain --debug=true

:deploy

echo Deploying the application
rem set /P DUMMY="Attach debugger if desired, then press Enter to deploy the web app"
call asadmin deploy webapp\target\admin-console

:end
echo Done.
