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

#ifndef _UNINSTALL_H_
#define _UNINSTALL_H_

#include "java.h"

#define LIB_DIR FILE_SEPARATOR "jdk"
#define BUNDLED_JVM_DIR LIB_DIR

#ifdef WIN32
#define DELETE_INCLUDE_ITEMS { FILE_SEPARATOR "domains",FILE_SEPARATOR "samples",FILE_SEPARATOR "jdk",FILE_SEPARATOR "hadb",FILE_SEPARATOR "lib" FILE_SEPARATOR "install",FILE_SEPARATOR "config", FILE_SEPARATOR "lib" FILE_SEPARATOR "certutil.exe",FILE_SEPARATOR "lib" FILE_SEPARATOR "pk12util.exe",FILE_SEPARATOR "bin" FILE_SEPARATOR "cliutil.dll"}
#else
#define DELETE_INCLUDE_ITEMS { FILE_SEPARATOR "uninstall", FILE_SEPARATOR "domains", FILE_SEPARATOR "jdk", FILE_SEPARATOR "hadb", FILE_SEPARATOR "config", FILE_SEPARATOR "samples"}
#endif

#define CLASS_NAME "appserv_uninstall"
#define MESSAGE_KEY_PREFIX "uninstall"

static char *delete_include_items[] = DELETE_INCLUDE_ITEMS;
#define NUM_DELETE_INCLUDE_ITEMS (sizeof(delete_include_items) / sizeof(char *))

/*
 * Perform any necessary cleanup.
 * Note: don't use MemAlloc function here since we are in an exit handler.
 */
void Cleanup()
{
    const char *apphome = GetApplicationHome();
    const char *class = GetJavaClassName();

    if (apphome)
    {
        char *classfile = NULL;
        if ((classfile = (char *)malloc(strlen(apphome) + strlen(class) + 8)) != NULL)
        {
            strcpy(classfile, apphome);
            strcat(classfile, FILE_SEPARATOR);
            strcat(classfile, class);
            strcat(classfile, ".class");
            /* If the class file is gone, then the uninstall succeeded */
            if (!CheckJavaClassFile())
            {
                char *deletelist[32];
                
                    int i;
                    jboolean iserror = JNI_FALSE;
                    for (i = 0; i < NUM_DELETE_INCLUDE_ITEMS; i++)
                    {
                        if ((deletelist[i] = (char *)malloc(strlen(apphome) + strlen(delete_include_items[i]) + 1)) == NULL)
                            break;
                        strcpy(deletelist[i], apphome);
                        strcat(deletelist[i], delete_include_items[i]);
                    }
                    deletelist[i] = NULL;
                    
                    for (i = 0; deletelist[i] != NULL; i++)   
                        DeleteFilesAndDirectories(deletelist[i], NULL); 
                    for (i = 0; deletelist[i] != NULL; i++)
                        free(deletelist[i]);
            }
            free(classfile);
            classfile = NULL;
        }
    }
}

static char *bundledJREPath = NULL;

/*
 * Returns the bundled JVM path if it exists.
 */
const char *
GetBundledJREPath()
{
    if (!bundledJREPath)
    {
        const char *apphome;
        char *jrepath = NULL;
        if ((apphome = GetApplicationHome()) != NULL)
        {
            jrepath = (char *)MemAlloc(strlen(apphome) + sizeof(BUNDLED_JVM_DIR) + 1);
            sprintf(jrepath, "%s" BUNDLED_JVM_DIR, apphome);
            bundledJREPath = GetJREPath(jrepath);
            free(jrepath);
            jrepath = NULL;
        }
    }
    return bundledJREPath;
}

static char *bundledLibPath = NULL;

/*
 * Returns the bundled library path.
 */
const char *
GetBundledLibraryPath()
{
    if (!bundledLibPath)
    {
        const char *apphome;
        if ((apphome = GetApplicationHome()) != NULL)
        {
            bundledLibPath = (char *)MemAlloc(strlen(apphome) + sizeof(LIB_DIR) + 1);
            sprintf(bundledLibPath, "%s" LIB_DIR, apphome);
        }
    }
    return bundledLibPath;
}

/*
 * Returns the Java class name to load.
 */
const char *
GetJavaClassName()
{
    return CLASS_NAME;
}

#ifdef CLASSPATH_ELEMENTS
static char *classpath_elements[] = CLASSPATH_ELEMENTS;
#define NUM_CLASSPATH_ELEMENTS (sizeof(classpath_elements) / sizeof(char *))
#endif

/*
 * Returns the Java class path elements to load.
 */
int
GetJavaClasspathElements(char ***elements)
{
#ifdef CLASSPATH_ELEMENTS
    *elements = classpath_elements;
    return NUM_CLASSPATH_ELEMENTS;
#else
    *elements = NULL;
    return 0;
#endif
}

/*
 * Returns the prefix to use for looking for localized messages.
 */
char *
GetMessageKeyPrefix(const char *key)
{
    char *ret = NULL;
    ret = (char *)MemAlloc(sizeof(MESSAGE_KEY_PREFIX) + strlen(key) + 2);
    sprintf(ret, MESSAGE_KEY_PREFIX "_%s", key);
    return ret;
}

static char *title = NULL;

/*
 * Returns the title to use on status windows.
 */
const char *
GetTitle()
{
    if (!title)
        title = GetLocalizedMessage(MESSAGE_KEY_PREFIX "_title");
    return title;
}

/*
 * Returns whether state file argument is required when the "-silent"
 * option is used.
 */
jboolean
IsStateFileRequired()
{
    return JNI_FALSE;
}

#endif /* _UNINSTALL_H_ */

