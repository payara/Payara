'
' DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
'
' Copyright (c) 2007-2010 Oracle and/or its affiliates. All rights reserved.
'
' The contents of this file are subject to the terms of either the GNU
' General Public License Version 2 only ("GPL") or the Common Development
' and Distribution License("CDDL") (collectively, the "License").  You
' may not use this file except in compliance with the License.  You can
' obtain a copy of the License at
' https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
' or packager/legal/LICENSE.txt.  See the License for the specific
' language governing permissions and limitations under the License.
'
' When distributing the software, include this License Header Notice in each
' file and include the License file at packager/legal/LICENSE.txt.
'
' GPL Classpath Exception:
' Oracle designates this particular file as subject to the "Classpath"
' exception as provided by Oracle in the GPL Version 2 section of the License
' file that accompanied this code.
'
' Modifications:
' If applicable, add the following below the License Header, with the fields
' enclosed by brackets [] replaced by your own identifying information:
' "Portions Copyright [year] [name of copyright owner]"
'
' Contributor(s):
' If you wish your version of this file to be governed by only the CDDL or
' only the GPL Version 2, indicate your decision by adding "[Contributor]
' elects to include this software in this distribution under the [CDDL or GPL
' Version 2] license."  If you don't indicate a single choice of license, a
' recipient has the option to distribute your version of this file under
' either the CDDL, the GPL Version 2 or to extend the choice of license to
' its licensees as provided above.  However, if you add GPL Version 2 code
' and therefore, elected the GPL Version 2 license, then the option applies
' only if the new code is made subject to such option by the copyright
' holder.
'

PRODUCTNAME="glassfish"

Set wShell = CreateObject("WScript.Shell")
gReturnValue = wshell.Run("regsvr32 /s scrrun.dll", 0 ,True)
set gFileSystem = CreateObject("Scripting.FileSystemObject")

MYDIR=trim(Replace(Wscript.scriptFullName, Wscript.scriptName, ""))

ENGINE_DIR=MYDIR+"install"

LOGDIR=gFileSystem.GetSpecialFolder(2)

' this must be changed to your products java installation directory
JAVA_HOME= wShell.ExpandEnvironmentStrings("%JAVA_HOME%")

JAVA_OPTIONS="-Dorg.openinstaller.provider.configurator.class=org.openinstaller.provider.conf.InstallationConfigurator"

'-------------------------------------------------------------------------------
' perform actual operation for the script: install/uninstall
' input(s):  none
' output(s): instCode
'-------------------------------------------------------------------------------
Function perform

ENGINE_OPS="-m " + chr(34) + "file:///"+MYDIR+"Metadata" + chr(34)
ENGINE_OPS=ENGINE_OPS+" -a " + chr(34) + "file:///"+MYDIR+"install.Windows.properties" + chr(34)
ENGINE_OPS=ENGINE_OPS+" -i " + chr(34) + "file:///"+MYDIR + "Product/" + chr(34)
ENGINE_OPS=ENGINE_OPS+" -p Default-Product-ID="+PRODUCTNAME
ENGINE_OPS=ENGINE_OPS+" -p Pkg-Format=zip"


if DRYRUN <> "" Then
    ENGINE_OPS=ENGINE_OPS+" -n "+chr(34)+DRYRUN+chr(34)
end if

if ANSWERFILE <> "" Then
    ENGINE_OPS=ENGINE_OPS+" -a "+chr(34)+ANSWERFILE+chr(34)
end if

if ALTROOT <> "" Then
    ENGINE_OPS=ENGINE_OPS + " -R " + chr(34) + ALTROOT + chr(34)
end if

if LOGLEVEL <> "" Then
    ENGINE_OPS=ENGINE_OPS+" -l "+LOGLEVEL
end if


if LOGDIR <> "" Then
    ENGINE_OPS=ENGINE_OPS+" -p " + chr(34) + "Logs-Location="+LOGDIR+chr(34)
end if

if JAVA_HOME <> ""  Then
    ENGINE_OPS=ENGINE_OPS+" -j "+chr(34)+JAVA_HOME+chr(34)
end if

if JAVA_OPTIONS <> "" Then
    ENGINE_OPS=ENGINE_OPS+" -J " + chr(34) + JAVA_OPTIONS + chr(34)
end if

if INSTALLPROPS <> "" Then
    ENGINE_OPS=ENGINE_OPS+INSTALLPROPS
end if

if INSTALLABLES <> "" Then
    ENGINE_OPS=ENGINE_OPS+" -i " + chr(34) + INSTALLABLES + chr(34)
end if

'wscript.echo "wscript //nologo " & chr(34) & ENGINE_DIR & "\bin\Engine-wrapper.vbs" & chr(34) & " " &  ENGINE_OPS

wShell.exec "wscript //nologo " & chr(34) & ENGINE_DIR & "\bin\Engine-wrapper.vbs" & chr(34) & " " &  ENGINE_OPS


End Function

'-------------------------------------------------------------------------------
' retrieve bundled JVM from Media based on os and platfo${RM}
' input(s):  none
' output(s): JAVAMEDIAPATH
'-------------------------------------------------------------------------------
Function getBundledJvm

  JAVAMEDIAPATH="usr\jdk\instances\jdk1.5.0"

End Function

Function useBundledJvm
  getBundledJvm
  JAVA_HOME=BUNDLED_JAVA_JRE_LOC+"\"+JAVAMEDIAPATH
  if Not gFileSystem.FolderExists(JAVA_HOME) Then
       WScript.Echo JAVA_HOME+" must be the root directory of a valid JVM installation"
       WScript.Echo "Please provide JAVA_HOME as argument with -j option and proceed."
       WScript.Quit(1)
  end if
End Function


'-------------------------------------------------------------------------------
' usage only: define what parameters are available here
' input(s):  exitCode
'-------------------------------------------------------------------------------
Function usage
WScript.echo "GlassFish V3 Installer based on openInstaller"

WScript.Quit(1)

End Function



'-------------------------------------------------------------------------------
' ****************************** MAIN THREAD ***********************************
'-------------------------------------------------------------------------------

' check arguments

Set args = WScript.Arguments
argumentCounter=0

do while argumentCounter < args.Length
argName=args.Item(argumentCounter)

select case argName

case "-a"
  if argumentCounter + 1 < args.Length Then
    ANSWERFILE=trim(args.Item(argumentCounter+1))
    argumentCounter=argumentCounter+2
  Else
    usage
  End if

case "-R"
  if argumentCounter + 1 < args.Length Then
    ALTROOT=trim(args.Item(argumentCounter+1))
    argumentCounter=argumentCounter+2

	    if Not gFileSystem.FolderExists(ALTROOT) Then
		WScript.Echo ALTROOT+" is not a valid alternate root"
                WScript.Quit(1)
            end if
  Else
    usage
  End if

case "-l"
  if argumentCounter + 1 < args.Length Then
    LOGDIR=trim(args.Item(argumentCounter+1))
    argumentCounter=argumentCounter+2

	    if Not gFileSystem.FolderExists(LOGDIR) Then
		WScript.Echo LOGDIR+" is not a directory or is not writable"
                WScript.Quit(1)
	    end if
  Else
    usage
  End if
case "-q"
  LOGLEVEL="WARNING"
  argumentCounter=argumentCounter+1

case "-v"
  LOGLEVEL="FINEST"
  argumentCounter=argumentCounter+1

case "-t"
  WScript.Echo "TextUI is not supported for Windows."
  argumentCounter=argumentCounter+1

case "-n"
  if argumentCounter + 1 < args.Length Then
    DRYRUN=trim(args.Item(argumentCounter+1))
    argumentCounter=argumentCounter+2
  end if
case "-j"

  if argumentCounter + 1 < args.Length Then
    JAVA_HOME=trim(args.Item(argumentCounter+1))
    argumentCounter=argumentCounter+2

    if Not gFileSystem.FolderExists(JAVA_HOME) Then
	WScript.Echo JAVA_HOME+" must be the root directory of a valid JVM installation"
               WScript.Quit(1)
    end if
  Else
    usage
  End if

case "-J"
  if argumentCounter + 1 < args.Length Then
    JAVAOPTIONS=trim(args.Item(argumentCounter+1))
    argumentCounter=argumentCounter+2
  Else
    usage
  End if

case "-p"
 if argumentCounter + 1 < args.Length Then
   INSTALLPROPS=INSTALLPROPS+" -p "+trim(args.Item(argumentCounter+1))
   argumentCounter=argumentCounter+2
  Else
    usage
  End if

case "-h"
 usage
 argumentCounter=argumentCounter+1

case Else
 usage

end select

Loop


'WScript.Echo "Welcome to GlassFish V3 installation based on openInstaller.  Press OK to begin."

' overwrite check if user specify javahome to use
if JAVA_HOME = "" Then
    useBundledJvm
end if

perform


