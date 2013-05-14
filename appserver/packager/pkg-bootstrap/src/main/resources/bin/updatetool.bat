@echo off
REM
REM  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
REM
REM  Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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

@REM This script is a bootstrap stub. It invokes pkg-bootstrap to download
@REM the actual software.
setlocal
set MY_HOME=%~dp0
set RETURN_CODE=0

@REM We want to set MY_NAME to the name of this file. %~0 may include 
@REM the portion of the path used to invoke the command, for example
@REM ".\foo.bat". We need to trim the leading stuff to get just "foo.bat".
set MY_PATH=%~0
:loop2
for /f "tokens=1 delims=\" %%A in ("%MY_PATH%") do set MY_NAME=%%A
for /f "tokens=1* delims=\" %%A in ("%MY_PATH%") do set MY_PATH=%%B
if defined MY_PATH goto loop2
@REM MY_NAME may be "foo" or "foo.bat" depending on what the user typed.
@REM normalize it to "foo"
if /I %MY_NAME%==updatetool.bat set MY_NAME=updatetool
if /I %MY_NAME%==pkg.bat set MY_NAME=pkg
@REM MY_NAME is now set correctly

@REM Location of bootstrap jar file relative to INSTALL_HOME
set BOOTSTRAPJAR=pkg/lib/pkg-bootstrap.jar
set BOOTSTRAPPROPS=%TEMP%\pkg-bootstrap%RANDOM%.props
set JAVACLIENTJAR=pkg/lib/pkg-client.jar

@REM Go find Java
set MY_JAVA_HOME="none"
set JDK_KEY=HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit
set JRE_KEY=HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Runtime Environment
set JRMT_JDK_KEY=HKEY_LOCAL_MACHINE\SOFTWARE\JRockit\Java Development Kit
set JRMT_JRE_KEY=HKEY_LOCAL_MACHINE\SOFTWARE\JRockit\Java Runtime Environment
set JRRT_KEY=HKEY_LOCAL_MACHINE\SOFTWARE\JRockit\Real Time

@REM Get Java runtime location from the registry. We try 1.6 first, then 1.7
(FOR /F "tokens=1,2*" %%A IN ('reg query "%JDK_KEY%\1.6" /v JavaHome') DO SET MY_JAVA_HOME=%%C) 2>nul
(if not exist "%MY_JAVA_HOME%\bin" FOR /F "tokens=1,2*" %%A IN ('reg query "%JRE_KEY%\1.6" /v JavaHome') DO SET MY_JAVA_HOME=%%C) 2>nul
(if not exist "%MY_JAVA_HOME%\bin" FOR /F "tokens=1,2*" %%A IN ('reg query "%JDK_KEY%\1.7" /v JavaHome') DO SET MY_JAVA_HOME=%%C) 2>nul
(if not exist "%MY_JAVA_HOME%\bin" FOR /F "tokens=1,2*" %%A IN ('reg query "%JRE_KEY%\1.7" /v JavaHome') DO SET MY_JAVA_HOME=%%C) 2>nul

@REM Could not get from registry. See if JAVA_HOME is set
if not exist "%MY_JAVA_HOME%\bin" set MY_JAVA_HOME=%JAVA_HOME%

@REM Can't find a Java runtime. Error.
if not defined MY_JAVA_HOME goto findjavainpath
if not exist "%MY_JAVA_HOME%\bin" goto findjavainpath

@REM MY_JAVA_HOME is set. Use it.
set MY_JAVA_CMD=%MY_JAVA_HOME%\bin\java

:findjavadone

echo.
echo The software needed for this command (%MY_NAME%) is not installed.
if '%MY_NAME%'=='updatetool' (
    echo.
    echo If you choose to install Update Tool, your system will be automatically
    echo configured to periodically check for software updates. If you would like
    echo to configure the tool to not check for updates, you can override the
    echo default behavior via the tool's Preferences facility.

    set PROMPTMSG="Would you like to install Update Tool now (y/n): "
) else (
    set PROMPTMSG="Would you like to install this software now (y/n): "
)
echo.
echo When this tool interacts with package repositories, some system information
echo such as your system's IP address and operating system type and version
echo is sent to the repository server. For more information please see:
echo.
echo http://wikis.oracle.com/display/updatecenter/UsageMetricsUC2
echo.
echo Once installation is complete you may re-run this command.
echo.

:prompt1
set RESPONSE="tbd"
set /p RESPONSE=%PROMPTMSG%
if /I '%RESPONSE%'=='y' goto yes1
if /I '%RESPONSE%'=='n' goto end
goto prompt1

:yes1

@REM pkg-bootstrap takes it's input as a java property file, so we must
@REM write props out to a temporary java props file.
echo # Update Center Bootstrap properties  > "%BOOTSTRAPPROPS%"
echo install.pkg=true>> "%BOOTSTRAPPROPS%"

@REM We pick up proxy settings from HTTP_PROXY and HTTPS_PROXY environment
@REM variables if they are set. Otherwise we get proxy settings from the system
if defined HTTP_PROXY echo proxy.URL=%HTTP_PROXY%>> "%BOOTSTRAPPROPS%"
if defined HTTPS_PROXY echo proxy.secure.URL=%HTTPS_PROXY%>> "%BOOTSTRAPPROPS%"
if not defined HTTP_PROXY echo proxy.use.system=true>>"%BOOTSTRAPPROPS%"
if /I %MY_NAME%==updatetool echo install.updatetool=true>> "%BOOTSTRAPPROPS%"

echo. >> "%BOOTSTRAPPROPS%"

@REM image.path is passed as a system property instead of via the
@REM props file. See bug 376
set MY_IMAGE_PATH=%MY_HOME%\..

@REM Remove Unix bootstub scripts
(if exist "%MY_HOME%\updatetool" del /F "%MY_HOME%\updatetool") 2>nul
(if exist "%MY_HOME%\pkg" del /F "%MY_HOME%\pkg") 2>nul

@REM Finally, run pkg-bootstrap!
echo.
@REM we do refresh to work around UPDATECENTER2-2219
cd "%MY_IMAGE_PATH%"
@echo on
"%MY_JAVA_CMD%" -Dimage.path="%MY_IMAGE_PATH%" -jar "%MY_HOME%\..\%JAVACLIENTJAR%" refresh
"%MY_JAVA_CMD%" -Dimage.path="%MY_IMAGE_PATH%" -jar "%MY_HOME%\..\%BOOTSTRAPJAR%" "%BOOTSTRAPPROPS%"
@echo off

@REM Remember this means if errorlevel 1 or higher...
if errorlevel 1 set RETURN_CODE=%ERRORLEVEL%

@REM An exit code of 3 or 4 means an issue connecting to the repository
if errorlevel 5 goto cleanup
if errorlevel 3 goto proxyerror

:cleanup
@REM Clean up temp file
del "%BOOTSTRAPPROPS%"

@REM Create a temporary script to remove this bat file. If we remove ourselves
@REM then Windows reports an error (see bug 475), so we work around this
@REM by having another script remove us. We just leave mydel.bat as cruft
@REM in %TEMP%
set MY_DEL=%TEMP%\mydel.bat
echo @echo off > "%MY_DEL%"
echo del /F %%1 >> %MY_DEL%"

@REM If the bootstrap process installed *.exe files, then remove the 
@REM cooresponding (bootstub) bat file. Note that as soon as this bat
@REM is removed we terminate execution -- so nothing after this point
@REM will be executed.
(if exist "%MY_HOME%\pkg.exe" "%MY_DEL%" "%MY_HOME%\pkg.bat") 2>nul
(if exist "%MY_HOME%\updatetool.exe" "%MY_DEL%" "%MY_HOME%\updatetool.bat") 2>nul

goto end

:findjavainpath
@REM As a last resort, see if we can find java.exe in the path
@REM This "for" command basically searches the path for java.exe
for /f %%A in ("java.exe") do set MY_JAVA_CMD=%%~$PATH:A
if not defined MY_JAVA_CMD goto nojavaerror
@REM Good, we have a java. Now go back and continue where we left off.
goto findjavadone

:nojavaerror
echo.
echo Failed to locate a Java runtime. Please install Java SE 6 or newer.
echo If you already have one of these installed on your system then either:
echo Set the JAVA_HOME environment variable to the Java SE install location.
echo  or
echo Ensure java.exe is in your PATH.
echo.
set RETURN_CODE=1
goto end

:proxyerror
echo.
echo Could not download application packages. This could be because:
echo   - a proxy server is needed to access the internet. Please ensure that
echo     the system proxy server settings in your Internet Options control panel
echo     (under Connections:LAN Settings) are correct, or set the HTTP_PROXY
echo     environment variable to the full URL of the proxy server.
echo   - the package server or network connection is slow.
echo     If you are getting time out errors you can try setting the
echo     PKG_CLIENT_CONNECT_TIMEOUT and PKG_CLIENT_READ_TIMEOUT
echo     environment variables and try again. For example to increase
echo     the timeouts to 300 seconds set them to 300
echo   - the package server is down or otherwise inaccessible or it is
echo     generating invalid data. Please contact the provider of the package
echo     server.
echo.
set RETURN_CODE=2
goto cleanup

:end
@REM This exit sequence causes the return code to be passed to a calling process
exit /B %RETURN_CODE%

