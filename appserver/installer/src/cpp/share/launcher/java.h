/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

#ifndef _JAVA_H_
#define _JAVA_H_

/*
 * Get system specific defines.
 */
#include <jni.h>
#include <limits.h>
#include <stdlib.h>
#include <sys/stat.h>
#define CONFIGURATOR_OPTION "-Dorg.openinstaller.provider.configurator.class=org.openinstaller.provider.conf.InstallationConfigurator"
#ifdef WIN32
#include "java_md.winnt.h"
#define INSTALL_ENGINE_OPTION "-Dinstall.engine=.\\install\\lib\\engine.jar;.\\install\\lib\\sims.jar;.\\install\\lib\\config.jar"
#define INSTALL_RESOURCE_OPTION "-Dinstaller.resources.url=file:///"
#define DEFAULT_RESOURCE_OPTION "-Ddefault.resources.url=file:///"
#else
#define INSTALL_ENGINE_OPTION "-Dinstall.engine=./install/lib/engine.jar:./install/lib/sims.jar:./install/lib/config.jar"
#define INSTALL_RESOURCE_OPTION "-Dinstaller.resources.url=file://"
#define DEFAULT_RESOURCE_OPTION "-Ddefault.resources.url=file://"
#include "java_md.unix.h"
#endif
#include "zip_util.h"

/*
 * Enable headless installation
 */
 
#define ALLOW_AWT_HEADLESS

/* Open Installer Options */

#define PLATFORM_PLUGIN_PATH FILE_SEPARATOR "install" FILE_SEPARATOR "lib" FILE_SEPARATOR "platforms"
#define PLATFORM_PLUGIN_PROP "Platform-Plugin-Path=" 

#define PROVIDER_PATH FILE_SEPARATOR "install" FILE_SEPARATOR "lib" FILE_SEPARATOR "providers"
#define PROVIDER_PATH_PROP "Provider-Path=" 

#define DRY_RUN_FILE_PROP "Dry-Run-File="
#define CONFIG_STATE_PROP "Config-State="
#define ANSWER_FILE_PROP "Answer-Files="
#define ALTERNATE_ROOT_PROP "Alternate-Root="

#define WARNING_LOG_LEVEL_PROP "Log-Level=WARNING"
#define FINEST_LOG_LEVEL_PROP "Log-Level=FINEST"
#define DEFAULT_LOG_LEVEL_PROP "Log-Level=INFO"

#define MEDIA_PATH FILE_SEPARATOR "install" FILE_SEPARATOR "metadata"
#define MEDIA_LOCATION_PROP "Media-Location="


#define LOGS_LOCATION_PROP "Logs-Location="

#define INSTALLABLE_UNIT_PATH FILE_SEPARATOR "Product/"
#define INSTALLABLE_UNIT_PROP "Installable-Unit-Path="

#define DEFAULT_PRODUCTID_PROP "Default-Product-ID=glassfish"
#define CONSOLE_MODE_PROP "Display-Mode=CUI"
#define SILENT_MODE_PROP "Display-Mode=SILENT"

#define PKG_FORMAT_PROP "Pkg-Format=zip"

#define SIMS_NATIVE_OPTION "-Dsims.native-file-dir="

/* Open installer zip files to be extracted instead of bundling the extracted ones.
Saves about 4-5 MB to the download size. */
#define ENGINE_ZIP_PATH FILE_SEPARATOR "Product" FILE_SEPARATOR "Packages" FILE_SEPARATOR "Engine.zip"
#define RESOURCES_ZIP_PATH FILE_SEPARATOR "Product" FILE_SEPARATOR "Packages" FILE_SEPARATOR "Resources.zip"
#define METADATA_ZIP_PATH FILE_SEPARATOR "Product" FILE_SEPARATOR "Packages" FILE_SEPARATOR "metadata.zip"

/* Path to JDK.zip, only applicable to Java EE SDKs with JDK bundle. */
#define JDK_ZIP_PATH FILE_SEPARATOR "Product" FILE_SEPARATOR "Packages" FILE_SEPARATOR "jdk.zip"


/*
 * Each of these entries are added onto the classpath by the launcher.
 */
#define TOOLS_JAR_FILE FILE_SEPARATOR "lib" FILE_SEPARATOR "tools.jar"
#define JDK_CLASSPATH { TOOLS_JAR_FILE, FILE_SEPARATOR "classes" }

/*
 * List of the Java versions that are supported. These strings are evaluated
 * by the CheckJavaVersion() function in the launcher. That function performs
 * a comparison of each string in the list against the version string obtained
 * from the JVM.
 *
 * pluby: I have all possible future releases of Java in the list so that we
 * only need to remove versions and we won't get stuck with an installer that
 * won't work with a Java release that is released after the installer.
 */
#define MINIMUM_SUPPORTED_VM_VERSION "1.6"

#define SUPPORTED_VM_URL "http://java.sun.com/j2se"

#define SUPPORTED_VM_VERSIONS { \
    "1.6", \
    "1.7", \
    "1.8", \
    "1.9" \
}

/*
 * Pointers to the needed JNI invocation API, initialized by LoadJavaVM.
 */
typedef jint (JNICALL *CreateJavaVM_t)(JavaVM **pvm, void **env, void *args);
typedef jint (JNICALL *GetDefaultJavaVMInitArgs_t)(void *args);

typedef struct {
    CreateJavaVM_t CreateJavaVM;
    GetDefaultJavaVMInitArgs_t GetDefaultJavaVMInitArgs;
} InvocationFunctions;

/*
 * Protoypes for launcher functions in the system specific java_md.c.
 */
char *
CheckJREVersion(const char *path);

void
DeleteFilesAndDirectories(const char *path, char **savelist);

/* Struct to Hold -p Properties to be passed to JVM */
typedef struct JavaPropsOptionStruct {
	char *propsString;
}JavaPropsOption;

int
ExecuteJava(const char *jrepath, int numOptions, const JavaVMOption *options, int numProps, const JavaPropsOption *);

void
ExitIfNotOnlyInstance(void);

char *
GetJREPath(const char *path);

char *
GetPublicJREPath(void);

const char *
GetArch(void);

const char *
GetLocale(void);

char *
GetLocalizedMessage(const char *key);

jboolean
GetPrintToConsole(void);

const char *
GetTempDir(void);

jboolean
IsJDK(const char *path);

jboolean
IsMultiByteChar(const char *s, int pos);

void
SetPrintToConsole(jboolean mode);

jboolean
SetTempDir(const char *path);

/*
 * Defined in java.c; used in java_md.*.c.
 */
extern jboolean
CheckJavaClassFile();

extern jboolean
CheckVersion(const char *version);

extern const char *
GetApplicationHome(void);

extern const char *
GetExecName(void);

extern char *
FindAbsolutePath(char *filePath);

extern jboolean
IsAbsolutePath(const char *path);

#ifndef WIN32
extern jboolean
IsExecutable(const char *path);
#endif

extern void *
MemAlloc(size_t size);

#ifndef WIN32
extern char *
Resolve(const char *parent, const char *child);
#endif

extern char *
StrDup(const char *str);

extern jboolean
UnzipFiles(jzfile *zipFile, const char *outputDir, const char *entrySubdir);

/*
 * Defined in setup.c or uninstall.c; used in java.c and java_md.*.c.
 */
extern void
Cleanup(void);

extern const char *
GetBundledJREPath(void);

extern const char *
GetBundledLibraryPath(void);

extern const char *
GetJavaClassName(void);

extern int
GetJavaClasspathElements(char ***elements);

extern char *
GetMessageKeyPrefix(const char *key);

extern const char *
GetTitle(void);

extern jboolean
IsStateFileRequired(void);

/*
 * Swap in our own custom printf() and fprintf() funtions depending on whether
 * we are are using console or GUI mode.
 */
extern int _printf(const char *format, ...);
#define printf _printf
extern int _fprintf(FILE *stream, const char *format, ...);
#define fprintf _fprintf

/*
 * Swap in out own string handling functions that handle mulit-byte character
 * strings.
 */
char *_strchr(const char *s, int c);
#define strchr _strchr
char *_strrchr(const char *s, int c);
#define strrchr _strrchr

#endif /* _JAVA_H_ */

