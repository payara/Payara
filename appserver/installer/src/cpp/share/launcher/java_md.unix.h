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

#ifndef JAVA_MD_H
#define JAVA_MD_H

#include <unistd.h>
#include <string.h>
#include <stdio.h>
#include <sys/types.h>
#include <dirent.h>
#include <dlfcn.h>
#include <locale.h>
#include <sys/wait.h>
#include <sys/statvfs.h>
#include <fcntl.h>
#include <signal.h>
#include <errno.h>

#define XTSTRINGDEFINES
#define XMSTRINGDEFINES
#include <Xm/MessageB.h>
#include <Xm/MwmUtil.h>

#define PATH_SEPARATOR_CHAR ':'
#define FILE_SEPARATOR_CHAR '/'
#define PATH_SEPARATOR ":"
#define FILE_SEPARATOR "/"
#define MAXPATHLEN PATH_MAX + 1

#define JAVA_EXE FILE_SEPARATOR "bin" FILE_SEPARATOR "java"
#define JAVAW_EXE JAVA_EXE

/* define architecture for 64-bit AMD systems */
#define X64 "amd64"

/*
 * Define a function for printing out status messages
 */
extern int statusf(const char* format, ...);

/* Utility functions for checking the GUI display */
typedef void (*XCloseDisplay_t)(Display *);
typedef Display *(*XOpenDisplay_t)(char *);
typedef void (*XRaiseWindow_t)(Display *, Window);
typedef void (*XSetErrorHandler_t)(int (*handler)(Display *, XErrorEvent *));
typedef void (*XSetIOErrorHandler_t)(int (*handler)(Display *));
typedef char* (*XSetLocaleModifiers_t)(char *);
typedef Boolean (*XSupportsLocale_t)();
typedef Widget (*XmCreateErrorDialog_t)(Widget, String, ArgList, Cardinal);
typedef Widget (*XmCreateInformationDialog_t)(Widget, String, ArgList, Cardinal);
typedef Widget (*XmCreateTemplateDialog_t)(Widget, String, ArgList, Cardinal);
typedef Widget (*XmMessageBoxGetChild_t)(Widget, unsigned char);
typedef XmString (*XmStringCreateLtoR_t)(char *, char *);
typedef void (*XmStringFree_t)(XmString);
typedef void (*XtAppNextEvent_t)(XtAppContext, XEvent *);
typedef XtInputMask (*XtAppPending_t)(XtAppContext);
typedef void (*XtAppSetErrorHandler_t)(XtAppContext, XtErrorHandler);
typedef void (*XtAppSetWarningHandler_t)(XtAppContext, XtErrorHandler);
typedef void (*XtDestroyWidget_t)(Widget);
typedef Boolean (*XtDispatchEvent_t)(XEvent *);
typedef void (*XtGetValues_t)(Widget, ArgList, Cardinal);
typedef Boolean (*XtIsManaged_t)(Widget);
typedef void (*XtManageChild_t)(Widget);
typedef void (*XtRealizeWidget_t)(Widget);
typedef Screen *(*XtScreenOfObject_t)(Widget);
typedef void (*XtUnmanageChild_t)(Widget);
typedef Widget (*XtVaAppInitialize_t)(XtAppContext *, String, XrmOptionDescRec *, Cardinal, int *, String *, String *, ...);
typedef void (*XtVaSetValues_t)(Widget, ...);
typedef Window (*XtWindowOfObject_t)(Widget);

typedef struct {
    XCloseDisplay_t XCloseDisplay;
    XOpenDisplay_t XOpenDisplay;
    XRaiseWindow_t XRaiseWindow;
    XSetErrorHandler_t XSetErrorHandler;
    XSetIOErrorHandler_t XSetIOErrorHandler;
    XSetLocaleModifiers_t XSetLocaleModifiers;
    XSupportsLocale_t XSupportsLocale;
    XmCreateErrorDialog_t XmCreateErrorDialog;
    XmCreateInformationDialog_t XmCreateInformationDialog;
    XmCreateTemplateDialog_t XmCreateTemplateDialog;
    XmMessageBoxGetChild_t XmMessageBoxGetChild;
    XmStringCreateLtoR_t XmStringCreateLtoR;
    XmStringFree_t XmStringFree;
    XtAppNextEvent_t XtAppNextEvent;
    XtAppPending_t XtAppPending;
    XtAppSetErrorHandler_t XtAppSetErrorHandler;
    XtAppSetWarningHandler_t XtAppSetWarningHandler;
    XtDestroyWidget_t XtDestroyWidget;
    XtDispatchEvent_t XtDispatchEvent;
    XtGetValues_t XtGetValues;
    XtIsManaged_t XtIsManaged;
    XtManageChild_t XtManageChild;
    XtRealizeWidget_t XtRealizeWidget;
    XtScreenOfObject_t XtScreenOfObject;
    XtUnmanageChild_t XtUnmanageChild;
    XtVaAppInitialize_t XtVaAppInitialize;
    XtVaSetValues_t XtVaSetValues;
    XtWindowOfObject_t XtWindowOfObject;
} X11Functions;

/* Utility functions for displaying the main status window */
extern const X11Functions *LoadX11(jboolean initdisplay);

#endif

