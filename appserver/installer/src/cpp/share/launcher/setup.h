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

#ifndef _SETUP_H_
#define _SETUP_H_

#include "java.h"

#ifdef WIN32
#define CLASSPATH_ELEMENTS { ";.\\registration-api.jar;.\\registration-impl.jar;.\\commons-codec-1.3.jar;.\\pkg-bootstrap.jar;.\\pkg-client.jar;.\\sysnet-registration.jar;.\\install\\lib\\bootstrap.jar;.\\install\\lib\\bindings.jar;.\\install\\lib\\external\\beanshell\\bsh.jar;.\\install\\lib\\external\\chaxml\\chaxml.jar;.\\install\\lib\\external\\charva\\charva.jar;.\\install\\lib\\external\\swixml\\swixml.jar;.\\install\\lib\\external\\swixml\\j2h.jar;.\\install\\lib\\external\\swixml\\ui.jar;.\\install\\lib\\external\\jdom\\jdom.jar;.\\install\\lib\\external\\freemarker\\freemarker.jar;.\\install\\lib\\external\\jaxb\\jaxb-impl.jar;.\\install\\lib\\external\\jaxb\\jaxb-jsr173_1.0_api.jar;.\\install\\lib\\external\\jaxb\\jaxb-api.jar;.\\install\\lib\\external\\jaxb\\activation.jar;.\\install\\lib\\external\\apache\\commons-logging.jar;" }
#else
#define CLASSPATH_ELEMENTS { ":./registration-api.jar:./registration-impl.jar:./commons-codec-1.3.jar:./sysnet-registration.jar:./install/lib/bootstrap.jar:./install/lib/bindings.jar:./install/lib/external/beanshell/bsh.jar:./install/lib/external/chaxml/chaxml.jar:./install/lib/external/charva/charva.jar:./install/lib/external/swixml/swixml.jar:./install/lib/external/swixml/j2h.jar:./install/lib/external/swixml/ui.jar:./install/lib/external/jdom/jdom.jar:./install/lib/external/freemarker/freemarker.jar:./install/lib/external/jaxb/jaxb-impl.jar:./install/lib/external/jaxb/jaxb-jsr173_1.0_api.jar:./install/lib/external/jaxb/jaxb-api.jar:./install/lib/external/jaxb/activation.jar:./install/lib/external/apache/commons-logging.jar:" }
#endif



#define LIB_DIR FILE_SEPARATOR "package"
#define BUNDLED_JVM_DIR LIB_DIR FILE_SEPARATOR "jre"

#define CLASS_NAME "org.openinstaller.core.EngineBootstrap"
#define MESSAGE_KEY_PREFIX "setup"

/*
 * Perform any necessary cleanup.
 */
void Cleanup()
{
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
            int apphomelen = strlen(apphome);

            jrepath = (char *)MemAlloc(apphomelen + sizeof(BUNDLED_JVM_DIR) + 1);
            sprintf(jrepath, "%s" BUNDLED_JVM_DIR, apphome);

            /* If not found, look for it in the embedded zip files */
            if ((bundledJREPath = GetJREPath(jrepath)) == NULL)
            {
                jzfile *zipFile;
                int zipError;
                const char *execname = GetExecName();
                char *s;

                if (execname && (zipFile = zipOpen(execname, &zipError)) != NULL)
                {
                    s = GetLocalizedMessage("unzipping_java_files");
                    statusf(s);
                    free(s);
                    /* Replace native file separators in entry name */
                    s = jrepath + apphomelen;
                    while ((s = strchr(s, FILE_SEPARATOR_CHAR)) != NULL)
                        *s++ = '/';
                    if (UnzipFiles(zipFile, apphome, jrepath + apphomelen + 1))
                    {
                        sprintf(jrepath, "%s" BUNDLED_JVM_DIR, apphome);
                        bundledJREPath = GetJREPath(jrepath);
                    }
                    zipClose(zipFile);
                }
            }
            
            free(jrepath);
            jrepath = NULL;
        }
    }
    return bundledJREPath;
}

static char *bundledLibPath;

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
            bundledLibPath = (char *)MemAlloc(1048);
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
    return JNI_TRUE;
}

#endif /* _SETUP_H_ */

