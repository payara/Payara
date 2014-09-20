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

#include "java.h"
#include <iconv.h>
#include <langinfo.h>
#include <pthread.h>

#ifndef P_tmpdir
#define P_tmpdir "/var/tmp"
#endif

#define JAVA_DLL "libjava.so"

#ifdef SOLARIS
#define GUILIBDIR "/usr/dt/lib"
#elif LINUX
#define GUILIBDIR "/usr/X11R6/lib"
#endif
#define GUI4DLL "libXm.so.4"
#define GUI3DLL "libXm.so.3"
#define GUI2DLL "libXm.so.2"
#define GUI1DLL "libXm.so.1"
#define GUIDLL "libXm.so"

#define PIPE_SIZE 512

struct jvminfo {
    char *version;
    char *path;
    struct jvminfo *next;
};

typedef const char *(*message_t)(void);

/*
 * Prototypes.
 */
static const char *
UpdateLocale();

const char *
GetArch()
{
#ifdef JNI_MD_SYSNAME
    return JNI_MD_SYSNAME;
#else
#error JNI_MD_SYSNAME must be defined for this platform
#endif
}

static char *lockfile = NULL;
static int lockfd = -1;

static void
DeleteLockFile()
{
    if (lockfd >= 0)
       close(lockfd);
    if (lockfile)
    {
        unlink(lockfile);
        free(lockfile);
        lockfile = NULL;
    }
}

void
ExitIfNotOnlyInstance()
{
    const char *tmpdir;
    const char *execname;
    struct stat sb;

    /*
     * Create a file unique to this executable and the effective user running
     * this program and put an exclusive lock on it. Note that if the default
     * temporary directory is not writable (a very unusual case which causes
     * many other problems for many other applications), there is no checking.
     */
    if ((tmpdir = GetTempDir()) != NULL && (execname = GetExecName()) != NULL && stat(execname, &sb) == 0)
    {
        const char *title = GetTitle();

        lockfile = (char *)MemAlloc(strlen(tmpdir) + strlen(title) + 45);
        sprintf(lockfile, "%s" FILE_SEPARATOR "%s.%lu.%llu.%lu", tmpdir, title, (unsigned long)sb.st_dev, (unsigned long long)sb.st_ino, (unsigned long)geteuid());
        if ((lockfd = creat(lockfile, S_IRUSR | S_IWUSR)) >= 0)
        {
            struct flock lck;

            lck.l_type = F_WRLCK;
            lck.l_whence = 0;
            lck.l_start = 0;
            lck.l_len = 0;
            if (fcntl(lockfd, F_SETLK, &lck) < 0 && errno == EAGAIN)
            {
                char *key = NULL;
                char *message = NULL;

                key = GetMessageKeyPrefix("already_started");
                message = GetLocalizedMessage(key);
                free(key);
                key = NULL;
                fprintf(stderr, message);
                free(message);
                message = NULL;
                exit(1);
            }
        }
        atexit(DeleteLockFile);
    }
    return;
}

static char *tmpdir = NULL;

jboolean
SetTempDir(const char *path)
{
    char *tmppath = NULL;
    char *cwd = NULL;
    int size = MAXPATHLEN;
    struct stat sb;

    if (path && !IsAbsolutePath(path))
    {
        while ((cwd = getcwd(NULL, size)) == NULL)
            size += MAXPATHLEN;
    }
    tmppath = Resolve(cwd, path);
    if (cwd)
        free(cwd);
    cwd = NULL;
    if (tmppath && access(tmppath, R_OK | W_OK | X_OK) == 0 && stat(tmppath, &sb) == 0 && (sb.st_mode & S_IFDIR) == S_IFDIR)
    {
        if (tmpdir)
            free(tmpdir);
        tmpdir = tmppath;
        return JNI_TRUE;
    }
    if (tmppath)
        free(tmppath);
    tmppath = NULL;
    return JNI_FALSE;
}

#ifndef RES_BUNDLE_NAME
#error RES_BUNDLE_NAME macro must be set
#endif

/*
 * Retrieve a localized message from the resource bundle compiled into the
 * executable.
 */
char *
GetLocalizedMessage(const char *key)
{
    const char *encoding;
    iconv_t cd;
    char *fullkey = NULL;
    char *lc = NULL;
    message_t sym = NULL;
    const char *value;
    char *ret = NULL;

    lc = StrDup(GetLocale());
    fullkey = (char *)MemAlloc(sizeof(RES_BUNDLE_NAME) + strlen(lc) + strlen(key) + 3);
    while (!sym)
    {
        strcpy(fullkey, RES_BUNDLE_NAME);
        if (strlen(lc) > 0)
        {
            strcat(fullkey, "_");
            strcat(fullkey, lc);
        }
        strcat(fullkey, "_");
        strcat(fullkey, key);
        if ((sym = (message_t)dlsym(dlopen(NULL, RTLD_NOW + RTLD_GLOBAL), fullkey)) == NULL)
        {
            if (strlen(lc) > 0)
            {
                char *s;
                if ((s = strrchr(lc, '_')) != NULL)
                    *s = '\0';
                else
                    *lc = '\0';
            }
            else
            {
                goto error;
            }
        }
        else
        {
            break;
        }
    }
    free(lc);
    lc = NULL;
    free(fullkey);
    fullkey = NULL;
    if ((value = sym()) == NULL)
        goto error;
    /* Convert from UTF-8 to local encoding */
    if ((encoding = nl_langinfo(CODESET)) != NULL && (cd = iconv_open(encoding, "UTF-8")) != (iconv_t)-1)
    {
        size_t size = MAXPATHLEN;

        ret = (char *)MemAlloc(size + 1);
        for (;;)
        {
            const char *inbuf = value;
            size_t inleft = strlen(value);
            char *outbuf = ret;
            size_t outleft = size;

            if (!iconv(cd, &inbuf, &inleft, &outbuf, &outleft))
            {
                /* NULL terminate the converted string */
                ret[size - outleft] = '\0';
                iconv_close(cd);
                break;
            }
            else
            {
                if (errno == E2BIG)
                {
                    size += MAXPATHLEN;
                    free(ret);
                    ret = (char *)MemAlloc(size + 1);
                }
                else
                {
                    iconv_close(cd);
                    goto error;
                }
            }
        }
    }
    else
    {
        /* Pass it through unconverted and hope for the best */
        ret = StrDup(value);
    }
    return ret;

error:
    /* Always return a valid string */
    if (lc)
        free(lc);
    lc = NULL;
    if (fullkey)
        free(fullkey);
    fullkey = NULL;
    if (ret)
        free(ret);
    ret = (char *)MemAlloc(1);
    ret[0] = '\0';
    return ret;
}

const char *
GetTempDir()
{
    if (!tmpdir)
        SetTempDir(P_tmpdir);
    return tmpdir;
}

/*
 * Find path to JRE based on a user specified Java installation directory.
 */
char *
GetJREPath(const char *path)
{
    char *libjava;
    const char *arch = GetArch();

    libjava = (char *)MemAlloc(strlen(path) + strlen(arch) + 1 + strlen(JAVA_DLL) + 11);

    /* Is JRE co-located with the application? */
    sprintf(libjava, "%s" FILE_SEPARATOR "lib" FILE_SEPARATOR "%s" FILE_SEPARATOR JAVA_DLL, path, arch);

    if (access(libjava, F_OK) == 0)
    {
        strcpy(libjava, path);
        goto found;
    }

    /* Does the app ship a private JRE in <apphome>/jre directory? */
    memset(libjava,0,strlen(path) + strlen(arch) + strlen(JAVA_DLL) + 11);
    sprintf(libjava, "%s" FILE_SEPARATOR "jre" FILE_SEPARATOR "lib" FILE_SEPARATOR "%s" FILE_SEPARATOR JAVA_DLL, path, arch);
    if (access(libjava, F_OK) == 0)
    {
        sprintf(libjava, "%s" FILE_SEPARATOR "jre", path);
        goto found;
    }

#ifdef LINUX
    /* for 64-bit JDK, must try a different directory */
    memset(libjava,0,strlen(path) + strlen(arch) + 1 + strlen(JAVA_DLL) + 11);
    sprintf(libjava, "%s" FILE_SEPARATOR "jre" FILE_SEPARATOR "lib" FILE_SEPARATOR "%s" FILE_SEPARATOR JAVA_DLL, path, X64);
    if (access(libjava, F_OK) == 0)
    {
        sprintf(libjava, "%s" FILE_SEPARATOR "jre", path);
        goto found;
    }
#endif

    free(libjava);
    return NULL;

found:
       {
        char *java = NULL;
        char *javaw = NULL;

        /* Check that executables exist */
        java = (char *)MemAlloc(strlen(libjava) + strlen(JAVA_EXE) + 1);
        javaw = (char *)MemAlloc(strlen(libjava) + strlen(JAVAW_EXE) + 1);
        sprintf(java, "%s" JAVA_EXE, libjava);
        sprintf(javaw, "%s" JAVAW_EXE, libjava);
        if (!IsExecutable(java) || !IsExecutable(javaw))
        {
  	    libjava = NULL;
        }
        free(java);
        java = NULL;
        free(javaw);
        javaw = NULL;
     }
    return libjava;  
}

static void *libgui = NULL;
static X11Functions *x11funcs = NULL;
static XtAppContext appcontext = NULL;
static Widget appshell = NULL;
static Widget statusdialog = NULL;
static Display *display = NULL;
static jboolean dispatch = JNI_FALSE;
static pthread_mutexattr_t lockattributes;
static pthread_mutex_t lock;

static int
HandleX11Error(Display *display, XErrorEvent *error)
{
    /* 
     * Sometimes errors happen, especially when xhosting. When that happens,
     * switch to console mode.
     */
    SetPrintToConsole(JNI_TRUE);
    return 0;
}

static int
HandleX11IOError(Display *display)
{
    /* Switch to console mode before Xlib invokes exit() */
    SetPrintToConsole(JNI_TRUE);
    return 0;
}

static void
HandleX11Warning()
{
}

static void *
DispatchX11Events(void *arg)
{
    struct timespec ts;
    ts.tv_sec = 0;
    ts.tv_nsec = 100000000;

    while (dispatch && x11funcs && appcontext)
    {
        if (dispatch && !pthread_mutex_lock(&lock))
        {
            while (x11funcs->XtAppPending(appcontext))
            {
                XEvent event;
                x11funcs->XtAppNextEvent(appcontext, &event);
                x11funcs->XtDispatchEvent(&event);
            }
            pthread_mutex_unlock(&lock);
        }
        /* Need to use nanosleep so that lower priority threads can run */
        nanosleep(&ts, NULL);
    }
}

const X11Functions *
LoadX11(jboolean initdisplay)
{
    static loadingx11 = JNI_FALSE;
    static nox11 = JNI_FALSE;
    int mode = RTLD_NOW + RTLD_GLOBAL;
    int argc = 0;
    pthread_t tid;
    int policy;
    struct sched_param priority;
    char *s;

    /* Handle the rare cases where this function is recursively called */
    if (loadingx11)
        return NULL;

    loadingx11 = JNI_TRUE;

    /* Don't bother trying to load libraries if a prior attempt failed */
    if (nox11)
        return NULL;

    /* Try to load Motif 2.1 or 1.2 libraries */
    if (!libgui
        && (libgui = dlopen(GUILIBDIR FILE_SEPARATOR GUI4DLL, mode)) == NULL
        && (libgui = dlopen(GUI4DLL, mode)) == NULL
        && (libgui = dlopen(GUILIBDIR FILE_SEPARATOR GUI3DLL, mode)) == NULL
        && (libgui = dlopen(GUI3DLL, mode)) == NULL
        && (libgui = dlopen(GUILIBDIR FILE_SEPARATOR GUI2DLL, mode)) == NULL
        && (libgui = dlopen(GUI2DLL, mode)) == NULL
        && (libgui = dlopen(GUILIBDIR FILE_SEPARATOR GUI1DLL, mode)) == NULL
        && (libgui = dlopen(GUI1DLL, mode)) == NULL
        && (libgui = dlopen(GUILIBDIR FILE_SEPARATOR GUIDLL, mode)) == NULL
        && (libgui = dlopen(GUIDLL, mode)) == NULL)
    {
        nox11 = JNI_TRUE;
        goto error;
    }

    if (!x11funcs)
    {
        x11funcs = (X11Functions *)MemAlloc(sizeof(X11Functions));
        if ((x11funcs->XCloseDisplay = (XCloseDisplay_t)dlsym(libgui, "XCloseDisplay")) == NULL) goto error;
        if ((x11funcs->XOpenDisplay = (XOpenDisplay_t)dlsym(libgui, "XOpenDisplay")) == NULL) goto error;
        if ((x11funcs->XRaiseWindow = (XRaiseWindow_t)dlsym(libgui, "XRaiseWindow")) == NULL) goto error;
        if ((x11funcs->XSetErrorHandler = (XSetErrorHandler_t)dlsym(libgui, "XSetErrorHandler")) == NULL) goto error;
        if ((x11funcs->XSetIOErrorHandler = (XSetIOErrorHandler_t)dlsym(libgui, "XSetIOErrorHandler")) == NULL) goto error;
        if ((x11funcs->XSetLocaleModifiers = (XSetLocaleModifiers_t)dlsym(libgui, "XSetLocaleModifiers")) == NULL) goto error;
        if ((x11funcs->XSupportsLocale = (XSupportsLocale_t)dlsym(libgui, "XSupportsLocale")) == NULL) goto error;
        if ((x11funcs->XmCreateErrorDialog = (XmCreateErrorDialog_t)dlsym(libgui, "XmCreateErrorDialog")) == NULL) goto error;
        if ((x11funcs->XmCreateInformationDialog = (XmCreateInformationDialog_t)dlsym(libgui, "XmCreateInformationDialog")) == NULL) goto error;
        if ((x11funcs->XmCreateTemplateDialog = (XmCreateTemplateDialog_t)dlsym(libgui, "XmCreateTemplateDialog")) == NULL) goto error;
        if ((x11funcs->XmMessageBoxGetChild = (XmMessageBoxGetChild_t)dlsym(libgui, "XmMessageBoxGetChild")) == NULL) goto error;
        if ((x11funcs->XmStringCreateLtoR = (XmStringCreateLtoR_t)dlsym(libgui, "XmStringCreateLtoR")) == NULL) goto error;
        if ((x11funcs->XmStringFree = (XmStringFree_t)dlsym(libgui, "XmStringFree")) == NULL) goto error;
        if ((x11funcs->XtAppNextEvent = (XtAppNextEvent_t)dlsym(libgui, "XtAppNextEvent")) == NULL) goto error;
        if ((x11funcs->XtAppPending = (XtAppPending_t)dlsym(libgui, "XtAppPending")) == NULL) goto error;
        if ((x11funcs->XtAppSetErrorHandler = (XtAppSetErrorHandler_t)dlsym(libgui, "XtAppSetErrorHandler")) == NULL) goto error;
        if ((x11funcs->XtAppSetWarningHandler = (XtAppSetWarningHandler_t)dlsym(libgui, "XtAppSetWarningHandler")) == NULL) goto error;
        if ((x11funcs->XtDestroyWidget = (XtDestroyWidget_t)dlsym(libgui, "XtDestroyWidget")) == NULL) goto error;
        if ((x11funcs->XtDispatchEvent = (XtDispatchEvent_t)dlsym(libgui, "XtDispatchEvent")) == NULL) goto error;
        if ((x11funcs->XtGetValues = (XtGetValues_t)dlsym(libgui, "XtGetValues")) == NULL) goto error;
        if ((x11funcs->XtIsManaged = (XtIsManaged_t)dlsym(libgui, "XtIsManaged")) == NULL) goto error;
        if ((x11funcs->XtManageChild = (XtManageChild_t)dlsym(libgui, "XtManageChild")) == NULL) goto error;
        if ((x11funcs->XtRealizeWidget = (XtRealizeWidget_t)dlsym(libgui, "XtRealizeWidget")) == NULL) goto error;
        if ((x11funcs->XtScreenOfObject = (XtScreenOfObject_t)dlsym(libgui, "XtScreenOfObject")) == NULL) goto error;
        if ((x11funcs->XtUnmanageChild = (XtUnmanageChild_t)dlsym(libgui, "XtUnmanageChild")) == NULL) goto error;
        if ((x11funcs->XtVaAppInitialize = (XtVaAppInitialize_t)dlsym(libgui, "XtVaAppInitialize")) == NULL) goto error;
        if ((x11funcs->XtVaSetValues = (XtVaSetValues_t)dlsym(libgui, "XtVaSetValues")) == NULL) goto error;
        if ((x11funcs->XtWindowOfObject = (XtWindowOfObject_t)dlsym(libgui, "XtWindowOfObject")) == NULL) goto error;
    }

    /* Return if the initialize display flag is false */
    if (!initdisplay)
        return x11funcs;

    if (appshell)
    {
        loadingx11 = JNI_FALSE;
        return x11funcs;
    }

    if ((display = x11funcs->XOpenDisplay(NULL)) == NULL)
        goto error;

    x11funcs->XSetErrorHandler(HandleX11Error);
    x11funcs->XSetIOErrorHandler(HandleX11IOError);

    /* Try to make the X11 server's locale match our locale */
    if (!x11funcs->XSupportsLocale())
    {
        setlocale(LC_ALL, "C");
        /* Reset cached locale */
        UpdateLocale();
    }
    x11funcs->XSetLocaleModifiers("");

    /*
     * Don't bother checking return value since this function will terminate
     * the application if it fails
     */
    appshell = x11funcs->XtVaAppInitialize(&appcontext, (char *)GetTitle(), NULL, 0, &argc, NULL, NULL, NULL);
    x11funcs->XtVaSetValues(appshell,
        XmNmappedWhenManaged, False,
        NULL);
    x11funcs->XtAppSetErrorHandler(appcontext, (XtErrorHandler)HandleX11Error);
    x11funcs->XtAppSetWarningHandler(appcontext, (XtErrorHandler)HandleX11Warning);
    x11funcs->XtRealizeWidget(appshell);

    /* Initialize the lock */
    if (!dispatch)
    {
        if (pthread_mutexattr_init(&lockattributes))
            goto error;
        if (pthread_mutexattr_settype(&lockattributes, PTHREAD_MUTEX_RECURSIVE) || pthread_mutex_init(&lock, &lockattributes))
        {
            pthread_mutexattr_destroy(&lockattributes);
            goto error;
        }
        dispatch = JNI_TRUE;
    }

    /* Create the event loop thread */
    if (pthread_create(&tid, NULL, DispatchX11Events, NULL))
        goto error;

    /* Raise the thread's priority so that it is guaranteed a time slice */
    if (pthread_getschedparam(tid, &policy, &priority) || (priority.sched_priority = sched_get_priority_max(policy)) == -1)
        goto error;
    if (pthread_setschedparam(tid, policy, &priority))
        goto error;

    loadingx11 = JNI_FALSE;
    return x11funcs;

error:
    SetPrintToConsole(JNI_TRUE);
    if (dispatch)
    {
        dispatch = JNI_FALSE;
        pthread_join(tid, NULL);
        pthread_mutex_destroy(&lock);
        pthread_mutexattr_destroy(&lockattributes);
    }
    /* Close the display and all associated resources */
    if (x11funcs)
    {
        if (display)
             x11funcs->XCloseDisplay(display);
        display = NULL;
        free(x11funcs);
        x11funcs = NULL;
    }
    appcontext = NULL;
    appshell = NULL;
    statusdialog = NULL;
    /* Unload the library */
    if (libgui)
        dlclose(libgui);
    libgui = NULL;
    loadingx11 = JNI_FALSE;
    return NULL;
}

static void
CenterAndManageChild(Widget w)
{
    const X11Functions *funcs;

    if ((funcs = LoadX11(JNI_TRUE)) != NULL && dispatch && !pthread_mutex_lock(&lock))
    {
        Screen *screen;
        Dimension screenwidth = 0;
        Dimension screenheight = 0;
        Dimension width = 0;
        Dimension height = 0;
        Position x = 0;
        Position y = 0;
        Arg xargs[2];

        screen = funcs->XtScreenOfObject(w);
        screenwidth = WidthOfScreen(screen);
        screenheight = HeightOfScreen(screen);
        XtSetArg(xargs[0], XmNwidth, &width);
        XtSetArg(xargs[1], XmNheight, &height);
        if (funcs->XtIsManaged(w))
            funcs->XRaiseWindow(display, funcs->XtWindowOfObject(w));
        else
            funcs->XtManageChild(w);
        funcs->XtGetValues(w, xargs, XtNumber(xargs));
        if (screenwidth > width)
            x = (screenwidth - width) / 2;
        if (screenheight > height)
            y = (screenheight - height) / 2;
        funcs->XtVaSetValues(w,
            XmNx, x,
            XmNy, y,
            NULL);
        pthread_mutex_unlock(&lock);
    }
}

static int
PrintToStatusWindow(const char *format, va_list args)
{
    int ret;
    int increment = 1024;
    int size = increment;
    char *message;
    const X11Functions *funcs;
    int cachedlen = 0;
    static char *cachedmessage = NULL;

    if (cachedmessage)
        cachedlen = strlen(cachedmessage);
    for (;;)
    {
        message = (char *)MemAlloc(cachedlen + size);
        if (cachedmessage)
            strcpy(message, cachedmessage);

        ret = vsnprintf(message + cachedlen, size, format, args);
        if (ret < 0 || ret >= size)
        {
            size += increment;
            free(message);
            message = NULL;
            continue;
        }
        break;
    }
    if (cachedmessage)
        free(cachedmessage);
    cachedmessage = NULL;

    if ((funcs = LoadX11(JNI_TRUE)) != NULL && dispatch && !pthread_mutex_lock(&lock))
    {
        XmString xtitle = NULL;
        XmString xmessage = NULL;
        jboolean err = JNI_FALSE;

        /* Create the status window */
        if (!statusdialog)
        {
            Arg xargs[8];

            if ((xtitle = funcs->XmStringCreateLtoR((char *)GetTitle(), XmFONTLIST_DEFAULT_TAG)) == NULL)
            {
                err = JNI_TRUE;
                goto cleanup;
            }
            XtSetArg(xargs[0], XmNdialogTitle, xtitle);
            XtSetArg(xargs[1], XmNmwmDecorations, 0);
            XtSetArg(xargs[2], XmNmwmFunctions, 0);
            XtSetArg(xargs[3], XmNwidth, 400);
            XtSetArg(xargs[4], XmNheight, 100);
            XtSetArg(xargs[5], XmNresizePolicy, XmRESIZE_GROW);
            XtSetArg(xargs[6], XmNmessageAlignment, XmALIGNMENT_CENTER);
            XtSetArg(xargs[7], XmNdialogStyle, XmDIALOG_MODELESS);
            if ((statusdialog = funcs->XmCreateTemplateDialog(appshell, "Status", xargs, XtNumber(xargs))) == NULL)
            {
                err = JNI_TRUE;
                goto cleanup;
            }
            funcs->XtUnmanageChild(funcs->XmMessageBoxGetChild(statusdialog, XmDIALOG_SEPARATOR));
        }

        if (message[strlen(message) - 1] == '\n')
        {
            /* Chop off trailing newline */
            message[strlen(message) - 1] = '\0';
        }
        else
        {
            /* If there is no newline, cache message and print later */
            cachedmessage = message;
            message = NULL;
            ret = 0;
            goto cleanup;
        }

        /* Update and show the status window */
        if ((xmessage = funcs->XmStringCreateLtoR((char *)message, XmFONTLIST_DEFAULT_TAG)) == NULL)
        {
            err = JNI_TRUE;
            goto cleanup;
        }
        funcs->XtVaSetValues(statusdialog,
            XmNmessageString, xmessage,
            NULL);
        CenterAndManageChild(statusdialog);

cleanup:
        if (xtitle)
            funcs->XmStringFree(xtitle);
        xtitle = NULL;
        if (xmessage)
            funcs->XmStringFree(xmessage);
        xmessage = NULL;
        if (err)
            ret = vprintf(message, NULL);
        pthread_mutex_unlock(&lock);
    }
    else
    {
       ret = vprintf(message, NULL);
    }

    free(message);
    message = NULL;
    return ret;
}

static void
HideStatusWindow()
{
    const X11Functions *funcs;

    if ((funcs = LoadX11(JNI_TRUE)) != NULL && dispatch && !pthread_mutex_lock(&lock))
    {
        if (statusdialog)
        {
            funcs->XtUnmanageChild(statusdialog);
            funcs->XtDestroyWidget(statusdialog);
        }
        pthread_mutex_unlock(&lock);
    }
    statusdialog = NULL;
}

static int
PrintToMessageBox(unsigned char type, const char *format, va_list args)
{
    int ret;
    int increment = 1024;
    int size = increment;
    char *message;
    const X11Functions *funcs;
    int cachedlen = 0;
    static char *cachedmessage = NULL;

    if (cachedmessage)
        cachedlen = strlen(cachedmessage);
    for (;;)
    {
        message = (char *)MemAlloc(cachedlen + size);
        if (cachedmessage)
            strcpy(message, cachedmessage);

        ret = vsnprintf(message + cachedlen, size, format, args);
        if (ret < 0 || ret >= size)
        {
            size += increment;
            free(message);
            message = NULL;
            continue;
        }
        break;
    }
    if (cachedmessage)
        free(cachedmessage);
    cachedmessage = NULL;

    if ((funcs = LoadX11(JNI_TRUE)) != NULL && dispatch && !pthread_mutex_lock(&lock))
    {
        Widget dialog = NULL;
        char *title = NULL;
        XmString xtitle = NULL;
        XmString xmessage = NULL;
        XmString xok = NULL;
        char *ok = GetLocalizedMessage("ok");
        Arg xargs[7];
        jboolean err = JNI_FALSE;

        if (message[strlen(message) - 1] == '\n')
        {
            /* Chop off trailing newline */
            message[strlen(message) - 1] = '\0';
        }
        else
        {
            /* If there is no newline, cache message and print later */
            cachedmessage = message;
            message = NULL;
            goto cleanup;
        }

        /* Create the dialog */
        if ((xtitle = funcs->XmStringCreateLtoR((char *)GetTitle(), XmFONTLIST_DEFAULT_TAG)) == NULL)
        {
            err = JNI_TRUE;
            goto cleanup;
        }
        if ((xmessage = funcs->XmStringCreateLtoR(message, XmFONTLIST_DEFAULT_TAG)) == NULL)
        {
            err = JNI_TRUE;
            goto cleanup;
        }
        if ((xok = funcs->XmStringCreateLtoR(ok, XmFONTLIST_DEFAULT_TAG)) == NULL)
        {
            err = JNI_TRUE;
            goto cleanup;
        }
        XtSetArg(xargs[0], XmNdialogTitle, xtitle);
        XtSetArg(xargs[1], XmNmessageString, xmessage);
        XtSetArg(xargs[2], XmNdefaultButtonType, XmDIALOG_OK_BUTTON);
        XtSetArg(xargs[3], XmNdialogStyle, XmDIALOG_APPLICATION_MODAL);
        XtSetArg(xargs[4], XmNmwmDecorations, MWM_DECOR_ALL | MWM_DECOR_MINIMIZE | MWM_DECOR_MAXIMIZE | MWM_DECOR_RESIZEH);
        XtSetArg(xargs[5], XmNmwmFunctions, MWM_FUNC_ALL | MWM_FUNC_MINIMIZE | MWM_FUNC_MAXIMIZE | MWM_FUNC_RESIZE);
        XtSetArg(xargs[6], XmNokLabelString, xok);
        if (type == XmDIALOG_INFORMATION)
        {
            if ((dialog = funcs->XmCreateInformationDialog(appshell, "Information", xargs, XtNumber(xargs))) == NULL)
            {
                err = JNI_TRUE;
                goto cleanup;
            }
        }
        else
        {
            if ((dialog = funcs->XmCreateErrorDialog(appshell, "Error", xargs, XtNumber(xargs))) == NULL)
            {
                err = JNI_TRUE;
                goto cleanup;
            }
        }

        /* Display the dialog */
        funcs->XtUnmanageChild(funcs->XmMessageBoxGetChild(dialog, XmDIALOG_CANCEL_BUTTON));
        funcs->XtUnmanageChild(funcs->XmMessageBoxGetChild(dialog, XmDIALOG_HELP_BUTTON));
        CenterAndManageChild(dialog);

        if (!appcontext)
        {
            err = JNI_TRUE;
            goto cleanup;
        }

        /* Wait for the user to close the dialog */
        while (funcs->XtIsManaged(dialog))
        {
            XEvent event;
            funcs->XtAppNextEvent(appcontext, &event);
            funcs->XtDispatchEvent(&event);
        }

cleanup:
        if (xtitle)
            funcs->XmStringFree(xtitle);
        xtitle = NULL;
        if (xmessage)
            funcs->XmStringFree(xmessage);
        xmessage = NULL;
        if (xok)
            funcs->XmStringFree(xok);
        xok = NULL;
        free(ok);
        ok = NULL;
        if (dialog)
            funcs->XtDestroyWidget(dialog);
        dialog = NULL;
        if (err)
            ret = vprintf(message, NULL);
        pthread_mutex_unlock(&lock);
    }
    else
    {
        ret = vfprintf(stderr, message, NULL);
    }

    free(message);
    message = NULL;
    return ret;
}

/*
 * Search for a JRE giving priority to JDK installations
 */
char *
GetPublicJREPath()
{
    struct stat sb;
    const char *envvar;
    const char *envvar2;
    struct jvminfo *bundledjdk = NULL;
    struct jvminfo *bundledjre = NULL;
    struct jvminfo *jdks = NULL;
    struct jvminfo *jres = NULL;
    char *jrefoundpath = NULL;

    if ((envvar = GetBundledJREPath()) != NULL)
    {
        struct jvminfo *info = (struct jvminfo *)MemAlloc(sizeof(struct jvminfo));
        info->version = NULL;
        info->path = (char *)MemAlloc(strlen(envvar) + 1);
        info->next = NULL;
        strcpy(info->path, envvar);
        if (IsJDK(envvar))
            bundledjdk = info;
        else
            bundledjre = info;
    }

#ifdef SOLARIS
    /* Look in standard Solaris package installation location */
    envvar = "/usr/jdk/latest";
    if (stat(envvar, &sb) == 0)
    {
        char *jrepath = NULL;
        if ((jrepath = GetJREPath(envvar)) != NULL)
        {
            char *version = NULL;
            if ((version = CheckJREVersion(jrepath)) != NULL)
            {
                struct jvminfo *info = (struct jvminfo *)MemAlloc(sizeof(struct jvminfo));
                info->version = version;
                info->path = jrepath;
                if (IsJDK(jrepath))
                {
                    info->next = jdks;
                    jdks = info;
                }
                else
                {
                    info->next = jres;
                    jres = info;
                }
            }
            else
            {
                jrepath = NULL;
            }
        }
    }
#elif LINUX
    /* Look in standard Linux RPM installation locations */
    envvar = "/usr/java";
    if (stat(envvar, &sb) == 0)
    {
        if ((sb.st_mode & S_IFDIR) == S_IFDIR)
        {
            DIR *dir;
            if ((dir = opendir(envvar)) != NULL)
            {
                struct dirent *entry;
                char *javapath = NULL;
                while ((entry = readdir(dir)) != NULL)
                {
                    if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0)
                        continue;
                    if ((javapath = (char *)malloc(strlen(envvar) + strlen(entry->d_name) + 2)) != NULL)
                    {
                        char *jrepath = NULL;
                        char *version = NULL;
                        strcpy(javapath, envvar);
                        strcat(javapath, FILE_SEPARATOR);
                        strcat(javapath, entry->d_name);
                        if ((jrepath = GetJREPath(javapath)) != NULL)
                        {
                            if ((version = CheckJREVersion(jrepath)) != NULL)
                            {
                                struct jvminfo *info = (struct jvminfo *)MemAlloc(sizeof(struct jvminfo));
                                info->version = version;
                                info->path = jrepath;
                                if (IsJDK(jrepath))
                                {
                                    info->next = jdks;
                                    jdks = info;
                                }
                                else
                                {
                                    info->next = jres;
                                    jres = info;
                                }
                            }
                            else
                            {
                                jrepath = NULL;
                            }
                        }
                        free(javapath);
                        javapath = NULL;
                    }
                }
                closedir(dir);
            }
        }
    }
#endif

    /* Look in the JAVA_HOME and PATH environment variables */
    envvar = getenv("JAVA_HOME");
    envvar2 = getenv("PATH");
    if (envvar || envvar2)
    {
        char *path = NULL;
        char *currentpath;
        char *tokstr = (char *)MemAlloc((envvar ? strlen(envvar) + 5 : 0) + (envvar2 ? strlen(envvar2) : 0) + 1);

        path = (char *)MemAlloc((envvar ? strlen(envvar) + 5 : 0) + (envvar2 ? strlen(envvar2) : 0) + 1);
        *path = '\0';
        if (envvar)
        {
            strcat(path, envvar);
            strcat(path, FILE_SEPARATOR "bin");
            strcat(path, PATH_SEPARATOR);
        }
        if (envvar2)
        {
            strcat(path, envvar2);
            strcat(path, PATH_SEPARATOR);
        }
        *strrchr(path, PATH_SEPARATOR_CHAR) = '\0';
        tokstr = path;
        while (tokstr)
        {
            char *javapath = (char *)malloc(MAXPATHLEN);
            /*
             * Look for the Java executable and, if found, find its
             * installation directory
             */
            currentpath = tokstr;
            if ((tokstr = strchr(currentpath, PATH_SEPARATOR_CHAR)) != NULL)
                *tokstr++ = '\0';
            if (strlen(currentpath) == 0)
                currentpath = ".";
           javapath = Resolve(currentpath, "java");
            if (javapath && access(javapath, X_OK) == 0)
            {
                char *jrepath = NULL;
                char *version = NULL;
                /* Chop off executable name */
                *strrchr(javapath, FILE_SEPARATOR_CHAR) = '\0';
                /* Chop off last directory name */
                *strrchr(javapath, FILE_SEPARATOR_CHAR) = '\0';
                if ((jrepath = GetJREPath(javapath))!= NULL)
                {
                    if ((version = CheckJREVersion(jrepath)) != NULL)
                    {
                        struct jvminfo *info = (struct jvminfo *)MemAlloc(sizeof(struct jvminfo));
                        info->version = version;
                        info->path = jrepath;
                        if (IsJDK(jrepath))
                        {
                            info->next = jdks;
                            jdks = info;
                        }
                        else
                        {
                            info->next = jres;
                            jres = info;
                        }
                    }
                }
            }
         free(javapath);
        }
        free(path);
        path = NULL;
    }

    /* Make bundled JDK first choice */
    if (bundledjdk)
    {
        if (!jrefoundpath)
            jrefoundpath = bundledjdk->path;
        else
            free(bundledjdk->path);
        free(bundledjdk);
        bundledjdk = NULL;
    }

    /* Iterate through JDKs and use the one with the highest version */
    while (jdks && jdks->next)
    {
        struct jvminfo *info;
        if (jrefoundpath || strcmp(jdks->next->version, jdks->version) > 0)
        {
            info = jdks;
            jdks = jdks->next;
        }
        else
        {
            info = jdks->next;
            jdks->next = jdks->next->next;
        }
        free(info->version);
        free(info->path);
        free(info);
        info = NULL;
    }
    if (jdks)
    {
        if (!jrefoundpath)
            jrefoundpath = jdks->path;
        else
            free(jdks->path);
        free(jdks->version);
        free(jdks);
        jdks = NULL;
    }

    /* Make bundled JRE the next choice */
    if (bundledjre)
    {
        if (!jrefoundpath)
            jrefoundpath = bundledjre->path;
        else
            free(bundledjre->path);
        free(bundledjre);
        bundledjre = NULL;
    }

    /* Iterate through JREs and use the one with the highest version */
    while (jres && jres->next)
    {
        struct jvminfo *info;
        if (jrefoundpath || strcmp(jres->next->version, jres->version) > 0)
        {
            info = jres;
            jres = jres->next;
        }
        else
        {
            info = jres->next;
            jres->next = jres->next->next;
        }
        free(info->version);
        free(info->path);
        free(info);
        info = NULL;
    }
    if (jres)
    {
        if (!jrefoundpath)
            jrefoundpath = jres->path;
        else
            free(jres->path);
        free(jres->version);
        free(jres);
        jres = NULL;
    }

found:
    return jrefoundpath;
}

jboolean
IsJDK(const char* path)
{
    jboolean isJDK = JNI_FALSE;
    char *toolsjar = (char *)MemAlloc(strlen(path) + sizeof(TOOLS_JAR_FILE) + 4);
    char *dir = strrchr(path, FILE_SEPARATOR_CHAR);

    /* Does the app ship a private JRE in <apphome>/jre directory? */
    if (dir && !strcmp(dir, FILE_SEPARATOR "jre"))
    {
        sprintf(toolsjar, "%s" FILE_SEPARATOR ".." TOOLS_JAR_FILE,  path);
        if (access(toolsjar, F_OK) == 0)
        {
            isJDK = JNI_TRUE;
            goto found;
        }
    }
    /* Is JRE co-located with the application? */
    else
    {
        sprintf(toolsjar, "%s" TOOLS_JAR_FILE,  path);
        if (access(toolsjar, F_OK) == 0)
        {
            isJDK = JNI_TRUE;
            goto found;
        }
    }

found:
    free(toolsjar);
    toolsjar = NULL;
    return isJDK;
}

/* Default to console mode */
static jboolean printToConsole = JNI_TRUE;

jboolean
GetPrintToConsole()
{
    return printToConsole;
}

void
SetPrintToConsole(jboolean mode)
{
    printToConsole = mode;
}

int
_printf(const char *format, ...)
{
    int ret;
    va_list args;
    va_start(args, format);
    if (printToConsole || !LoadX11(JNI_TRUE))
       ret = vprintf(format, args);
    else
       ret = PrintToMessageBox(XmDIALOG_INFORMATION, format, args);
    va_end(args);
    fflush(stdout);
    return ret;
}

int
_fprintf(FILE *stream, const char *format, ...)
{
    int ret;
    va_list args;
    va_start(args, format);
    if (printToConsole || !LoadX11(JNI_TRUE))
    {
       ret = vfprintf(stream, format, args);
    }
    else
    {
        if (stream == stderr)
            ret = PrintToMessageBox(XmDIALOG_ERROR, format, args);
        else
            ret = PrintToMessageBox(XmDIALOG_INFORMATION, format, args);
    }
    va_end(args);
    fflush(stream);
    return ret;
}

int
statusf(const char *format, ...)
{
    int ret;
    va_list args;
    va_start(args, format);
    if (format)
    {
        if (printToConsole || !LoadX11(JNI_TRUE))
            ret = vprintf(format, args);
        else
            ret = PrintToStatusWindow(format, args);
    }
    va_end(args);
    return ret;
}

/*
 * Mappings from partial locale names to full locale names.
 *
 * Note: this table is copied from the solaris/native/java/lang/locale_str.h
 * header in the J2SE 1.4.1 source code and should be updated as new releases
 * of J2SE come out.
 */
static const char *locale_aliases[] = {
    "ar", "ar_EG",
    "be", "be_BY",
    "bg", "bg_BG",
    "br", "br_FR",
    "ca", "ca_ES",
    "cs", "cs_CZ",
    "cz", "cs_CZ",
    "da", "da_DK",
    "de", "de_DE",
    "el", "el_GR",
    "en", "en_US",
    "eo", "eo",    /* no country for Esperanto */
    "es", "es_ES",
    "et", "et_EE",
    "eu", "eu_ES",
    "fi", "fi_FI",
    "fr", "fr_FR",
    "ga", "ga_IE",
    "gl", "gl_ES",
    "he", "iw_IL",
    "hr", "hr_HR",
#ifdef LINUX
    "hs", "en_US", // used on Linux, not clear what it stands for
#endif
    "hu", "hu_HU",
    "id", "in_ID",
    "in", "in_ID",
    "is", "is_IS",
    "it", "it_IT",
    "iw", "iw_IL",
    "ja", "ja_JP",
    "kl", "kl_GL",
    "ko", "ko_KR",
    "lt", "lt_LT",
    "lv", "lv_LV",
    "mk", "mk_MK",
    "nl", "nl_NL",
    "no", "no_NO",
    "pl", "pl_PL",
    "pt", "pt_PT",
    "ro", "ro_RO",
    "ru", "ru_RU",
#ifdef LINUX
    "se", "en_US", // used on Linux, not clear what it stands for
#endif
    "sh", "sh_YU",
    "sk", "sk_SK",
    "sl", "sl_SI",
    "sq", "sq_AL",
    "sr", "sr_YU",
    "su", "fi_FI",
    "sv", "sv_SE",
    "th", "th_TH",
    "tr", "tr_TR",
#ifdef LINUX
    "ua", "en_US", // used on Linux, not clear what it stands for
#endif
    "uk", "uk_UA",
#ifdef LINUX
    "wa", "en_US", // used on Linux, not clear what it stands for
#endif
    "zh", "zh_CN",
#ifdef LINUX
    "catalan", "ca_ES",
    "croatian", "hr_HR",
    "czech", "cs_CZ",
    "danish", "da_DK",
    "dansk", "da_DK",
    "deutsch", "de_DE",
    "dutch", "nl_NL",
    "finnish", "fi_FI",
    "fran\xE7\x61is", "fr_FR",
    "french", "fr_FR",
    "german", "de_DE",
    "greek", "el_GR",
    "hebrew", "iw_IL",
    "hrvatski", "hr_HR",
    "hungarian", "hu_HU",
    "icelandic", "is_IS",
    "italian", "it_IT",
    "japanese", "ja_JP",
    "norwegian", "no_NO",
    "polish", "pl_PL",
    "portuguese", "pt_PT",
    "romanian", "ro_RO",
    "russian", "ru_RU",
    "slovak", "sk_SK",
    "slovene", "sl_SI",
    "slovenian", "sl_SI",
    "spanish", "es_ES",
    "swedish", "sv_SE",
    "turkish", "tr_TR",
#else
    "big5", "zh_TW.Big5",
    "chinese", "zh_CN",
    "japanese", "ja_JP",
    "tchinese", "zh_TW",
#endif
    ""
 };

/*
 * Linux/Solaris language string to ISO639 string mapping table.
 *
 * Note: this table is copied from the solaris/native/java/lang/locale_str.h
 * header in the J2SE 1.4.1 source code and should be updated as new releases
 * of J2SE come out.
 */
static const char *language_names[] = {
    "C", "en",
    "POSIX", "en",
    "ar", "ar",
    "be", "be",
    "bg", "bg",
    "br", "br",
    "ca", "ca",
    "cs", "cs",
    "cz", "cs",
    "da", "da",
    "de", "de",
    "el", "el",
    "en", "en",
    "eo", "eo",
    "es", "es",
    "et", "et",
    "eu", "eu",
    "fi", "fi",
    "fo", "fo",
    "fr", "fr",
    "ga", "ga",
    "gl", "gl",
    "hi", "hi",
    "he", "iw",
    "hr", "hr",
#ifdef LINUX
    "hs", "en", // used on Linux, not clear what it stands for
#endif
    "hu", "hu",
    "id", "in",
    "in", "in",
    "is", "is",
    "it", "it",
    "iw", "iw",
    "ja", "ja",
    "kl", "kl",
    "ko", "ko",
    "lt", "lt",
    "lv", "lv",
    "mk", "mk",
    "nl", "nl",
    "no", "no",
    "nr", "nr",
    "pl", "pl",
    "pt", "pt",
    "ro", "ro",
    "ru", "ru",
#ifdef LINUX
    "se", "en", // used on Linux, not clear what it stands for
#endif
    "sh", "sh",
    "sk", "sk",
    "sl", "sl",
    "sq", "sq",
    "sr", "sr",
    "su", "fi",
    "sv", "sv",
    "th", "th",
    "tr", "tr",
#ifdef LINUX
    "ua", "en", // used on Linux, not clear what it stands for
#endif
    "uk", "uk",
#ifdef LINUX
    "wa", "en", // used on Linux, not clear what it stands for
#endif
    "zh", "zh",
#ifdef LINUX
    "catalan", "ca",
    "croatian", "hr",
    "czech", "cs",
    "danish", "da",
    "dansk", "da",
    "deutsch", "de",
    "dutch", "nl",
    "finnish", "fi",
    "fran\xE7\x61is", "fr",
    "french", "fr",
    "german", "de",
    "greek", "el",
    "hebrew", "iw",
    "hrvatski", "hr",
    "hungarian", "hu",
    "icelandic", "is",
    "italian", "it",
    "japanese", "ja",
    "norwegian", "no",
    "polish", "pl",
    "portuguese", "pt",
    "romanian", "ro",
    "russian", "ru",
    "slovak", "sk",
    "slovene", "sl",
    "slovenian", "sl",
    "spanish", "es",
    "swedish", "sv",
    "turkish", "tr",
#else
    "chinese", "zh",
    "japanese", "ja",
    "korean", "ko",
#endif
    "",
};

/*
 * Linux/Solaris country string to ISO3166 string mapping table.
 *
 * Note: this table is copied from the solaris/native/java/lang/locale_str.h
 * header in the J2SE 1.4.1 source code and should be updated as new releases
 * of J2SE come out.
 */
static const char *country_names[] = {
    "AT", "AT",
    "AU", "AU",
    "AR", "AR",
    "BE", "BE",
    "BR", "BR",
    "BO", "BO",
    "CA", "CA",
    "CH", "CH",
    "CL", "CL",
    "CN", "CN",
    "CO", "CO",
    "CR", "CR",
    "CZ", "CZ",
    "DE", "DE",
    "DK", "DK",
    "DO", "DO",
    "EC", "EC",
    "EE", "EE",
    "ES", "ES",
    "FI", "FI",
    "FO", "FO",
    "FR", "FR",
    "GB", "GB",
    "GR", "GR",
    "GT", "GT",
    "HN", "HN",
    "HR", "HR",
    "HU", "HU",
    "ID", "ID",
    "IE", "IE",
    "IL", "IL",
    "IN", "IN",
    "IS", "IS",
    "IT", "IT",
    "JP", "JP",
    "KR", "KR",
    "LT", "LT",
    "LU", "LU",
    "LV", "LV",
    "MX", "MX",
    "NI", "NI",
    "NL", "NL",
    "NO", "NO",
    "NZ", "NZ",
    "PA", "PA",
    "PE", "PE",
    "PL", "PL",
    "PT", "PT",
    "PY", "PY",
#ifdef LINUX
    "RN", "US", // used on Linux, not clear what it stands for
#endif
    "RO", "RO",
    "RU", "RU",
    "SE", "SE",
    "SI", "SI",
    "SK", "SK",
    "SV", "SV",
    "TH", "TH",
    "TR", "TR",
    "UA", "UA",
    "UK", "GB",
    "US", "US",
    "UY", "UY",
    "VE", "VE",
    "TW", "TW",
    "YU", "YU",
    "",
};

/*
 * Linux/Solaris variant string to Java variant name mapping table.
 *
 * Note: this table is copied from the solaris/native/java/lang/locale_str.h
 * header in the J2SE 1.4.1 source code and should be updated as new releases
 * of J2SE come out.
 */
static const char *variant_names[] = {
#ifdef LINUX
    "nynorsk", "NY",
#endif
    "",
};

/*
 * Take an array of string pairs (map of key->value) and a string (key).
 * Examine each pair in the map to see if the first string (key) matches the
 * string.  If so, store the second string of the pair (value) in the value and
 * return 1.  Otherwise do nothing and return 0.  The end of the map is
 * indicated by an empty string at the start of a pair (key of "").
 */
static int
MapLookup(const char **map, const char *key, const char **value) {
    int i;
    for (i = 0; strcmp(map[i], ""); i += 2)
    {
        if (!strcmp(key, map[i]))
        {
            *value = map[i + 1];
            return 1;
        }
    }
    return 0;
}

static char *locale = NULL;

static const char * 
UpdateLocale()
{
    if (locale)
        free(locale);
    return GetLocale();
}

/* Use native strrchr to avoid recursive calls to this function */
const char *
GetLocale()
{
/* Use native strrchr to avoid recursive calls to this function */
#define __strrchr strrchr
#undef strrchr
    if (!locale)
    {
        const char *lc;
        char *s;
        char *t;
        const char *language = "en";
        const char *country = NULL;
        const char *variant = NULL; 

        /* Set the locale to the users current locale */
        if ((lc = setlocale(LC_ALL, "")) == NULL)
            lc = setlocale(LC_ALL, "C");

        /*
         * Using the UTF-8 or utf8 encodings is not handled well when xhosting
         * so chop it off.
         */
        t = StrDup(lc);
        if ((s = strstr(t, ".utf8")) != NULL)
            *s = '\0';
        if ((s = strstr(t, ".UTF-8")) != NULL)
            *s = '\0';
        if ((lc = setlocale(LC_ALL, t)) == NULL)
            lc = setlocale(LC_ALL, "C");
        s = NULL;
        free(t);
        t = NULL;

#ifdef SOLARIS
        /*
         * Xlib on Solaris does not handle the "@euro" variant very well so
         * chop it off.
         */
        t = StrDup(lc);
        if ((s = strstr(t, "@euro")) != NULL)
            *s = '\0';
        if ((lc = setlocale(LC_ALL, t)) == NULL)
            lc = setlocale(LC_ALL, "C");
        s = NULL;
        free(t);
        t = NULL;
#endif

        /* Parse the language, country, encoding, and variant from the
         * locale.  Any of the elements may be missing, but they must occur
         * in the order language_country.encoding@variant, and must be
         * preceded by their delimiter (except for language).
         *
         * If the locale name (without .encoding@variant, if any) matches
         * any of the names in the locale_aliases list, map it to the
         * corresponding full locale name.  Most of the entries in the
         * locale_aliases list are locales that include a language name but
         * no country name, and this facility is used to map each language
         * to a default country if that's possible.  It's also used to map
         * the Solaris locale aliases to their proper Java locale IDs.
         */
        s = StrDup(lc);

        /* Get and normalize the variant */
        if ((t = strrchr(s, '@')) != NULL)
        {
            *t++ = '\0';
            MapLookup(variant_names, t, &variant);
        }

        /* Skip the encoding */
        if ((t = strrchr(s, '.')) != NULL)
            *t++ = '\0';

        /* Get and normalize the country */
        if (MapLookup(locale_aliases, s, &lc))
        {
            free(s);
            s = StrDup(lc);
        }
        if ((t = strrchr(s, '_')) != NULL)
        {
            *t++ = '\0';
            MapLookup(country_names, t, &country);
        }

        /* Get and normalize the language */
        MapLookup(language_names, s, &language);
        free(s);
        s = NULL;

        /* Create the full locale string */
        locale = (char *)MemAlloc((language ? strlen(language) : 0) + (country ? strlen(country) : 0) + (variant ? strlen(variant) : 0) + 3);
        strcpy(locale, language);
        if (country)
        {
            strcat(locale, "_");
            strcat(locale, country);
        }
        if (variant)
        {
            if (!country)
                strcat(locale, "_");
            strcat(locale, "_");
            strcat(locale, variant);
        }
    }

    return locale;
#define strrchr __strrchr
}

jboolean
IsMultiByteChar(const char *s, int pos)
{
    int len = strlen(s);
    int curpos = 0;

    if (!pos)
        return JNI_TRUE;

    /* Make sure charset is set */
    GetLocale();

    /* Make sure that preceding character is not a multi-byte character */
    while (curpos + 1 < pos)
    {
        int charsize;
        if ((charsize = mblen(s + curpos, len)) == -1)
            return JNI_FALSE;
        curpos += charsize;
    }
    if (curpos + 1 == pos)
        return JNI_TRUE;
    else
        return JNI_FALSE;
}

/*
 * Check if the path is executable.
 */
jboolean
IsExecutable(const char *path)
{
    struct stat sb;

    if (access(path, R_OK | X_OK) == 0 && stat(path, &sb) == 0 && (sb.st_mode & S_IFDIR) == 0)
        return JNI_TRUE;
    else
        return JNI_FALSE;
}



/*
 * Find a command in a directory, returning the path.
 */
char *
Resolve(const char *parent, const char *child)
{
    char *path = NULL;
    char *real = NULL;


    if (parent)
    {
        path = (char *)malloc(strlen(parent) + strlen(child) + 2); 
        sprintf(path, "%s" FILE_SEPARATOR "%s", parent, child);
    }
    else
    {
       path = (char *)MemAlloc(strlen(child) + 2); 
       path = StrDup(child);
    }
    real = (char *)MemAlloc(MAXPATHLEN);
    if (realpath(path, real))
    {
       free(path);
       path = real;
    }
    else
    {
      free(real);
     }
   
    return path;
}


char *
CheckJREVersion(const char *path)
{
    char *version = NULL;

    if (access(path, F_OK) == 0)
    {
        pid_t pid;
        int fdout[2];
        struct stat sb;
        int info;
        int i;

        if (pipe(fdout) < 0)
            return NULL;
        /* Flush all open streams before forking */
        fflush(NULL);

        /* Child process */
        if ((pid = fork()) == 0)
        {
            char *command = NULL;
            char *cmdv[3];

            command = (char *)MemAlloc(strlen(path) + strlen(JAVAW_EXE) + 1);
            sprintf(command, "%s" JAVAW_EXE, path);
            cmdv[0] = command;
            cmdv[1] = "-version";
            cmdv[2] = NULL;

            /* Connect output stream to pipe */
            setbuf(stdout, NULL);
            dup2(fdout[1], fileno(stderr));
            /* Close input and output streams to avoid hanging */
            fclose(stdin);
            fclose(stdout);
            /*
             * Make sure debug statements are suppressed by passing an empty
             * environment
             */
            execve(cmdv[0], cmdv, NULL);
        }

        /* If child exited with a 0 exit status, the check passed */
#ifdef LINUX
	if (pid > 0 && waitpid(pid, &info, 0) >= 0 && WIFEXITED(info) && fstat(fdout[0], &sb) == 0)
#else
	if (pid > 0 && waitpid(pid, &info, 0) >= 0 && WIFEXITED(info) && fstat(fdout[0], &sb) == 0 && sb.st_size > 0)
#endif    
        {
            ssize_t bytes;
            char *str = NULL;
            /*
             * Read the first line of output. We only need to read once since
             * the standard pipe buffer size is far larger than the longest
             * version string.
             */
            version = (char *)MemAlloc(PIPE_SIZE + 1);
            bytes = read(fdout[0], version, PIPE_SIZE);
            if (bytes < 0)
                bytes = 0;
            version[bytes] = '\0';
            /* Chop off any garbage after the first newline */
            if ((str = strchr(version, '\n')) != NULL)
                *str = '\0';
            /* Parse version string */
            if ((str = strrchr(version, '"')) != NULL)
                *str = '\0';
            if ((str = strrchr(version, '"')) != NULL)
            {
                *str++ = '\0';
                str = StrDup(str);
                free(version);
                version = str;
            }
            if (!CheckVersion(version))
            {
                free(version);
                version = NULL;
            } 
        }

        close(fdout[0]);
        close(fdout[1]);
    }

    return version;
}

/*
 * Execute a Java subprocess with the specified arguments.
 */
int
ExecuteJava(const char *jrepath, int numOptions, const JavaVMOption *options,int numProps, const JavaPropsOption *props)
{
    int i;
    int j;
    jboolean console = GetPrintToConsole();
    const char* class = GetJavaClassName();
    char *command = NULL;
    char **cmdv = NULL;
    struct sigaction act;
    pid_t pid;
    int info;
    int ret = 1;
        unsigned int sec = 10;

   /*
    if (!CheckJavaClassFile(class))
    {
        char *key = NULL;
        char *message = NULL;

        key = GetMessageKeyPrefix("no_files");
        message = GetLocalizedMessage(key);
        free(key);
        key = NULL;
        fprintf(stderr, message);
        free(message);
        message = NULL;
        return ret;
    }

   */
    command = (char *)MemAlloc(strlen(jrepath) + (console ? strlen(JAVA_EXE) : strlen(JAVAW_EXE)) + 1);
    strcpy(command, jrepath);
    strcat(command, console ? JAVA_EXE : JAVAW_EXE);
    if (!IsExecutable(command))
    {
        char *message = GetLocalizedMessage("java_aborted");
        fprintf(stderr, message);
        free(message);
        message = NULL;
        free(command);
        command = NULL;
        return ret;
    }

    cmdv = (char **)MemAlloc((numOptions + 3) * sizeof(char*) + 30);
    j = 0;
    cmdv[j++] = command;
    /*
    cmdv[j++] = "-client";
    */
    for (i = 0; i < numOptions; i++) {
        cmdv[j++] = options[i].optionString;
     }
    cmdv[j++] = (char *)class;
    for (i = 0; i < numProps; i++) {
        cmdv[j++] = props[i].propsString;
     }
    cmdv[j++] = NULL;

    /* Flush all open streams before forking */
    fflush(NULL);

    /* Child process */
    if ((pid = fork()) == 0)
    {
        execv(cmdv[0], cmdv);
    }

    free(cmdv);
    cmdv = NULL;
    free(command);
    command = NULL;

   /*
     Grab child's exit status 
   */
    if (pid > 0)
    {

        /*Wait a few seconds to display the status dialog */
        while ((sec = sleep(sec)) > 0)
            ;
        /*
        if (!console)
        {
            HideStatusWindow();
        }
        */

        if (waitpid(pid, &info, 0) >= 0 && WIFEXITED(info))
            ret = WEXITSTATUS(info);
    }
    else
    {
        char *message = GetLocalizedMessage("java_aborted");
        fprintf(stderr, message);
        free(message);
        message = NULL;
    }

    return ret;
}

/*
 * Perform recursive deletion of a file or directory. Note: don't use MemAlloc
 * function here since this is called from an exit handler.
 */
void
DeleteFilesAndDirectories(const char *path, char **savelist)
{
    struct stat sb;
    const char *filename = NULL;
    int i;

    if (!path || !IsAbsolutePath(path) || (filename = strrchr(path, FILE_SEPARATOR_CHAR)) == 0)
        return;
    filename++;

    /* Ignore special directory entries */
    if (strcmp(filename, ".") == 0 || strcmp(filename, "..") == 0)
    {
        return;
    }
    /* Chop off any trailing file separator */
    else if (strlen(filename) == 0)
    {
        char *newpath = NULL;
        if ((newpath = (char *)malloc(strlen(path) + 1)) != NULL)
        {
            strcpy(newpath, path);
            *strrchr(newpath, FILE_SEPARATOR_CHAR) = '\0';
            DeleteFilesAndDirectories(newpath, savelist);
	    if (newpath)
            	free(newpath);
            newpath = NULL;
        }
    }

    /* Ignore files in the save list */
    for (i = 0; savelist && savelist[i] != NULL; i++)
    {
        if (strcmp(path, savelist[i]) == 0)
            return;
    }

    /* Use lstat so that we don't recurse into softlinks to diretories */
    if (lstat(path, &sb) == 0)
    {
        if ((sb.st_mode & S_IFDIR) == S_IFDIR)
        {
            DIR *dir;
            if ((dir = opendir(path)) != NULL)
            {
                struct dirent *entry;
                char entrypath[2048];
                while ((entry = readdir(dir)) != NULL)
                {
                        strcpy(entrypath, path);
                        strcat(entrypath, FILE_SEPARATOR);
                        strcat(entrypath, entry->d_name);
                        DeleteFilesAndDirectories(entrypath, savelist);
                }
                closedir(dir);
            }
        }
        remove(path);
    }
}

/* used to invoke the upgrade tool in console mode*/
int
InvokeUpgradeTool(const char *invokerPath)
{

	int pid,info;
	int ret = -1;
	char * cmdline = NULL;
	char * cmdv[2];

	cmdline = (char *)MemAlloc(150);
	strcpy(cmdline, invokerPath);
	
	cmdv[0]=(char *)MemAlloc(150);
	strcpy(cmdv[0],cmdline);
	cmdv[1]=(char *)MemAlloc(150);
	cmdv[1]=NULL;

	if( ( pid = fork()) == 0 ){
		execve(cmdv[0], cmdv, NULL);
	}
	free(cmdline);
	cmdline=NULL;
	free(cmdv[0]);
	cmdv[0]=NULL;
	
	
	if ( pid > 0) {
		if ( waitpid(pid, &info, 0) >= 0 && WIFEXITED(info)) {
			ret = WEXITSTATUS(info);
		}
	} 
	return ret;
}
