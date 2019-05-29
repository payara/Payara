@echo off
REM
REM  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
REM
REM  Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

setlocal
goto :main

:seekJavaOnPath
for /f %%J in ("java.exe") do set JAVA=%%~$PATH:J
goto :EOF

:chooseJava
rem
rem Looks for Java at AS_JAVA, JAVA_HOME or in the path.
rem Sets javaSearchType to tell which was used to look for java,
rem javaSearchTarget to the location (or path), and
rem JAVA to the found java executable.

    if "%AS_JAVA%x" == "x" goto :checkJAVA_HOME
       set javaSearchType=AS_JAVA
       set javaSearchTarget="%AS_JAVA%"
       set JAVA=%AS_JAVA%\bin\java.exe
       for %%a in ("%AS_JAVA%") do set ACCJavaHome=%%~sa
       goto :verifyJava

:checkJAVA_HOME
    if "%JAVA_HOME%x" == "x" goto :checkPATH
       set javaSearchType=JAVA_HOME
       set javaSearchTarget="%JAVA_HOME%"
       set JAVA=%JAVA_HOME%\bin\java.exe
       for %%a in ("%JAVA_HOME%") do set ACCJavaHome=%%~sa
       goto :verifyJava

:checkPATH
    set JAVA=java
    call :seekJavaOnPath
    set javaSearchType=PATH
    set javaSearchTarget="%PATH%"

:verifyJava
rem
rem Make sure java really exists where we were told to look.  If not
rem display how we tried to find it and then try to run it, letting the shell
rem issue the error so we don't have to do i18n of our own message from the script.
    if EXIST "%JAVA%" goto :EOF
    echo
    echo %javaSearchType%=%javaSearchTarget%
    echo
    "%JAVA%"
    exit/b %ERRORLEVEL%
goto :EOF

:main
set _AS_INSTALL=%~dp0..
call "%_AS_INSTALL%\config\asenv.bat"
call :chooseJava

set inputArgs=%*
rem
rem Convert the java.exe path and the classpath path to
rem Windows "short" versions - with no spaces - so the
rem for /F statement below will work correctly.  Spaces cause
rem it great troubles.
rem
for %%a in ("%JAVA%") do set ACCJava=%%~sa%
for %%a in ("%_AS_INSTALL%/lib/gf-client.jar") do set XCLASSPATH=%%~sa
for /F "usebackq tokens=*" %%a in (`%ACCJava% -classpath %XCLASSPATH% org.glassfish.appclient.client.CLIBootstrap`) do set javaCmd=%%a
%javaCmd%
