/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

var fso = WScript.CreateObject("Scripting.FileSystemObject");

var wshShell = WScript.CreateObject("WScript.Shell");
var envVars = wshShell.Environment("PROCESS");

var pathSep = ";";

var prelim_AS_INSTALL = new String(envVars("_AS_INSTALL"));
var javaProgram = new String(quoteStringIfNeeded(envVars("JAVA")));
var driveLetter = prelim_AS_INSTALL.substring(0,1).toUpperCase();
var AS_INSTALL = driveLetter + prelim_AS_INSTALL.substring(1);

var AS_INSTALL_MOD = AS_INSTALL + "\\modules";

var builtinEndorsedDirSetting = quoteMultiStringIfNeeded(
    AS_INSTALL +
    "\\lib\\endorsed" +
    pathSep +
    AS_INSTALL_MOD + 
    "\\endorsed", pathSep);

var mainClassIdentRequired = 1;

var appcPath = envVars("APPCPATH");

var accJar=quoteStringIfNeeded(AS_INSTALL + "\\lib\\gf-client.jar");

var jvmArgs="-Dcom.sun.aas.installRoot=" + quoteStringIfNeeded(AS_INSTALL) +
    " -Djava.security.policy=" + quoteStringIfNeeded(AS_INSTALL + "\\lib\\appclient\\client.policy") +
    " -Djava.system.class.loader=org.glassfish.appclient.client.acc.agent.ACCAgentClassLoader" +
    " -Djava.security.auth.login.config=" + quoteStringIfNeeded(AS_INSTALL + "\\lib\\appclient\\appclientlogin.conf");
var VMARGS = envVars("VMARGS");
if (VMARGS != "") {
    jvmArgs += " " + VMARGS;
}

var inputArgs = new String(envVars("inputArgs"));

var accArgs;
var appArgs;
var jvmMainArgs;
var userEndorsedDirSetting;
var accMainArgs;
var mainClassIdent;

var expecting;

var ACCArgType;
var JVMArgType;

var matched = false;
if (inputArgs.length == 0) {
    inputArgs="-usage";
}
processArgs();

if (appcPath != "") {
    accArgs += ",appcpath=" + appcPath;
}
if (jvmMainArgs == "") {
    if (mainClassIdentRequired == 1) {
        inputArgs="-usage";
        processArgs();
        //recordACCArg("usage");
        //accMainArgs = "-usage";
        //jvmMainArgs = "-jar " + accJar;
    }
}
/*
 * The next statement emits a "set" command which assigns the java command to be
 * run to the env. variable "javaCmd."  The calling script then
 * simply includes a line %javaCmd% to execute the generated
 * command.  Defining the env. variable from this
 * script does not work; the scope is not right.
 */
var javaCmd = javaProgram + " " + finishJVMArgs() + " -javaagent:" +
    accJar + accArgs + "," + accMainArgs + " " +
    jvmMainArgs + " " + appArgs;

WScript.StdOut.WriteLine("set javaCmd=" + javaCmd);

// return to shell here

function prepareArgProcessing() {
    accArgs = "=mode=acscript";
    appArgs = "";
    jvmMainArgs = "";
    userEndorsedDirSetting = null;
    accMainArgs = "";
    mainClassIdent = null;
    expecting=null;

    ACCArgType="ACC";
    JVMArgType="JVM";
    recordACCArg("-configxml", quoteStringIfNeeded(AS_INSTALL + "\\domains\\domain1\\config\\sun-acc.xml"));

    //matched = false;

}

function processArgs() {
    prepareArgProcessing();
    var jvmValuedOptions = ["-classpath", "-cp", "-jar"];
    var accValuedOptions = ["-client", "-mainclass", "-name", "-xml",
        "-configxml", "-user", "-password", "-passwordfile",
        "-targetserver"];
    var accNonvaluedOptions = ["-textauth", "-noappinvoke", "-usage", "-help"];
    //var re = new RegExp("\"([^\"]+)\"|[^\"\\s]+)","g");
    var re = /"([^"]+)"|([^"\s]+)/g;
    
    // var tokens = inputArgs.split(" ");
    // var tokens = re.exec(inputArgs);
    var tokens = inputArgs.match(re);
    for (tokenIndex = 0; tokenIndex < tokens.length; tokenIndex++) {
        var token = tokens[tokenIndex];
        
        var matched=null;
        matched = matchToWithType(token, jvmValuedOptions, "JVM");
        if (matched == null) {
            matched = matchToWithType(token, accValuedOptions, "ACC");
        }
        if (matched == null) {
            matched = matchTo(token, accNonvaluedOptions);
            if (matched != null) {
                recordACCArg(token);
                if ((token == "-usage") || (token == "-help")) {
                    mainClassIdentRequired = 0;
                }
            }
        }
        if (matched == null) {
            var tokenString = new String(token);
            if (tokenString.charAt(0) == "-") {
                matched = token;
                recordNonACCOption(token);
                expecting = null;
            }
        }
        
        if (matched == null) {
            if (expecting != null) {
                recordArg(expectingArgType, expecting, token);
            } else {
                recordLoneArg(token);
            }
            expecting = null;
        }
    }
}

function matchToWithType(token, targets, argType) {
    var result = matchTo(token, targets);
    if (result != null) {
        expectingArgType = argType;
        expecting = result;
    }
    return result;
}

function matchTo(token, targets) {
    for (var candidate in targets) {
        if (token == targets[candidate]) {
            expecting = null;
            return token;
        }
    }
    return null;
}

function recordArg(argType, expecting, arg) {
    if (argType == "APP") {
        recordAPPArg(argType);
        recordAPPArg(arg);
    } else if (argType == "ACC") {
        recordACCArg(expecting, arg);
    } else if (argType == "JVM") {
        recordJVMArg(expecting, arg);
    }
}

function recordAPPArg(arg1, arg2) {
    appArgs += " " + arg1;
    if (arg2 != null) {
        appArgs += " " + arg2;
    }
}

function jvmMainArgsFor(value) {
    var valueStr = new String(value);
    var earEnding = /.ear$/
    if (valueStr.match(earEnding)) {
        return "-jar " + accJar;
    } else {
        return "-jar " + value;
    }
}

function recordClientArg(value) {
    if (mainClassIdent == "final") {
        recordAPPArg("-client");
        recordAPPArg(value);
    } else {
        // See if the client value is a folder.
        var objFSO = new ActiveXObject("Scripting.FileSystemObject");
        if (objFSO.FolderExists(value)) {
            jvmMainArgs = "-jar " + accJar;
            accMainArgs = "client=dir=" + value;
        } else {
            jvmMainArgs = jvmMainArgsFor(value);
            accMainArgs = "client=jar="  + value;
        }
        mainClassIdent = "tentative";
        mainClassIdentRequired = 0;
    }
}


function recordACCArg(arg1, arg2) {
    if (ACCArgType == "APP") {
        recordAPPArg(arg1);
    } else {
        if (arg1 == "-client") {
            recordClientArg(arg2);
        } else {
            accArgs += ",arg=" + arg1;
            if (arg2 != null) {
                accArgs += ",arg=" + arg2;
            }
        }
    }
}


function recordMainClass(arg1, arg2) {
    if (arg1 == "-jar") {
        jvmMainArgs = jvmMainArgsFor(arg2);
        accMainArgs = "client=jar=" + arg2;
        mainClassIdent = "final";
    } else if (arg1 == "-client") {
        jvmMainArgs = jvmMainArgsFor(arg2);
        accMainArgs = "client=jar=" + arg2;
        mainClassIdent = "tentative";
    } else {
        var _tmp = new String(arg2);
        if (_tmp.lastIndexOf(".class") != -1) {
            jvmMainArgs = "-jar " + accJar;
            accMainArgs = "client=classfile=" + arg2;
            mainClassIdent = "final";
        } else {
            jvmMainArgs = arg1;
            accMainArgs = "client=class=" + arg1;
            mainClassIdent = "final";
        }
    }
    ACCArgType = "APP";
    JVMArgType = "APP";
    mainClassIdentRequired = 0;
}

function recordJVMArg(arg1, arg2) {
    if (JVMArgType == "APP") {
        recordAppArg(arg1, arg2);
    } else {
        if (arg1 == "-jar") {
            recordMainClass(arg1, arg2);
        } else {
            if (arg1.match("-Djava\\.endorsed\\.dirs=") != null) {
                /*
                 * We need to merge the user's setting with the path to the
                 * app server's endorsed dir.  So for now
                 * just remember the user's setting
                 * and do not add it to the jvm args...yet.
                 */
                userEndorsedDirSetting = arg2;
            }
            jvmArgs += " " + arg1;
            if (arg2 != null) {
                jvmArgs += " " + arg2;
            }
        }
    }
}

function finishJVMArgs() {
    /*
     * Note that the user's setting, if present, was stored including the
     * -Djava.endorsed.dirs= part.  So we just add on if the user specified
     * anything but we must supply that part if the user did not specify anything.
     */
    var endorsedDirSetting;
    if (userEndorsedDirSetting != null) {
        endorsedDirSetting = userEndorsedDirSetting + pathSep + builtinEndorsedDirSetting;
    } else {
        endorsedDirSetting = "-Djava.endorsed.dirs=" +
            builtinEndorsedDirSetting + pathSep + jreEndorsedDirValue();
    }
    return jvmArgs + " " + endorsedDirSetting;
}

function recordNonACCOption(value) {
    if (mainClassIdent == null) {
        recordJVMArg(value);
    } else {
        recordAPPArg(value);
    }
}

function quoteStringIfNeeded(s) {
    var result = s;
    if (s.indexOf(" ") != -1) {
        result = "\"" + s + "\"";
    }
    return result;
}

function quoteMultiStringIfNeeded(s, sep) {
    var result = new String();
    var sepLoc;
    var scanStart = 0;
    var parts = s.split(sep);
    for (var part in parts) {
        if (result.length > 0) {
            result = result + sep;
        }
        result = result + quoteStringIfNeeded(parts[part]);
    }
    return result;
}

function recordLoneArg(token) {
    if (mainClassIdent == null) {
        recordMainClass(token);
    } else {
        recordAPPArg(token);
    }
}

function jreEndorsedDirValue() {
    var osPath = envVars("PATH");
    var jreHomePath = jreHome(osPath);
    var jreEndorsedDir = null;
    if (jreHomePath != null) {
        var endorsedPath = jreHomePath + "\\lib\\endorsed";
        if (fso.FolderExists(endorsedPath)) {
            jreEndorsedDir = fso.getFolder(endorsedPath).Path;
        }
    }
    return quoteMultiStringIfNeeded(jreEndorsedDir, pathSep);
}

function jreHome(osPath) {
    var osPathElts = osPath.split(pathSep);
    var jreHome = null;
    for (i in osPathElts) {
        var osPathElt = osPathElts[i];
        var javaExe = osPathElt + "\\java.exe";
        if (fso.FileExists(javaExe)) {
            if (fso.FolderExists(osPathElt + "\\..\\jre")) {
                // This looks like a JDK installation.
                jreHome = osPathElt + "\\..\\jre";
                break;
            } else {
                // Doesn't look like a JDK; maybe it's a JRE installation.
                var jrePath = osPathElt + "\\..\\..\\jre";
                if (fso.FolderExists(jrePath)) {
                    // This looks like a JRE.
                    jreHome = osPathElt + "\\..";
                    break;
                } else {
                    // This path element looks like neither a JDK nor a JRE.
                    continue;
                }
            }
        }
    }
    return jreHome;
}
