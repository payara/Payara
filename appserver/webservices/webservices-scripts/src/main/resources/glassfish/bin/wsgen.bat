@echo off

REM
REM  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
REM 
REM  Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
REM 
REM  The contents of this file are subject to the terms of either the GNU
REM  General Public License Version 2 only ("GPL") or the Common Development
REM  and Distribution License("CDDL") (collectively, the "License").  You
REM  may not use this file except in compliance with the License.  You can
REM  obtain a copy of the License at
REM  https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
REM  or packager/legal/LICENSE.txt.  See the License for the specific
REM  language governing permissions and limitations under the License.
REM 
REM  When distributing the software, include this License Header Notice in each
REM  file and include the License file at packager/legal/LICENSE.txt.
REM 
REM  GPL Classpath Exception:
REM  Oracle designates this particular file as subject to the "Classpath"
REM  exception as provided by Oracle in the GPL Version 2 section of the License
REM  file that accompanied this code.
REM 
REM  Modifications:
REM  If applicable, add the following below the License Header, with the fields
REM  enclosed by brackets [] replaced by your own identifying information:
REM  "Portions Copyright [year] [name of copyright owner]"
REM 
REM  Contributor(s):
REM  If you wish your version of this file to be governed by only the CDDL or
REM  only the GPL Version 2, indicate your decision by adding "[Contributor]
REM  elects to include this software in this distribution under the [CDDL or GPL
REM  Version 2] license."  If you don't indicate a single choice of license, a
REM  recipient has the option to distribute your version of this file under
REM  either the CDDL, the GPL Version 2 or to extend the choice of license to
REM  its licensees as provided above.  However, if you add GPL Version 2 code
REM  and therefore, elected the GPL Version 2 license, then the option applies
REM  only if the new code is made subject to such option by the copyright
REM  holder.
REM
REM Portions Copyright [2019-2020] Payara Foundation and/or affiliates

VERIFY OTHER 2>nul
setlocal ENABLEEXTENSIONS
if ERRORLEVEL 0 goto ok
echo "Unable to enable extensions"
exit /B 1

:ok
call "%~dp0..\config\asenv.bat"
if "%AS_JAVA%x" == "x" goto UsePath
set JAVA="%AS_JAVA%\bin\java"
goto run

:UsePath
set JAVA=java

:run
CALL %~dp0jdkcheck.bat

if %ENDORSED_AVAILABLE%==true (
    %JAVA% %WSIMPORT_OPTS% -Djava.endorsed.dirs="%~dp0..\modules\endorsed" -cp "%~dp0..\modules\webservices-osgi.jar;%~dp0..\modules\jakarta.xml.rpc-api.jar;%~dp0..\modules\jaxb-osgi.jar" com.sun.tools.ws.WsGen %*
) else (
    %JAVA% %WSIMPORT_OPTS% -cp "%~dp0..\modules\webservices-osgi.jar;%~dp0..\modules\jakarta.xml.rpc-api.jar;%~dp0..\modules\jaxb-osgi.jar;%~dp0..\modules\jakarta.xml.ws-api.jar;%~dp0..\modules\jakarta.jws-api.jar;%~dp0..\modules\webservices-api-osgi.jar;%~dp0..\modules\jakarta.activation-api.jar" com.sun.tools.ws.WsGen %*
)
