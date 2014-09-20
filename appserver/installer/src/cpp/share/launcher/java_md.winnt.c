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

#include <sys/types.h>

#include "java.h"
#include "resource.h"

#define JAVA_DLL "java.dll"

#define PIPE_SIZE 512

#define MIN_TEXT_MARGIN 10

struct jvminfo {
    char *version;
    char *path;
    struct jvminfo *next;
};

typedef const wchar_t *(*message_t)(void);

/*
 * Prototypes.
 */
static void
HideStatusWindow(void);

static INT_PTR CALLBACK
StatusCallback(HWND hWndDlg, UINT uMsg, WPARAM wParam, LPARAM lParam);

const char *
GetArch()
{
#ifdef _WIN64
    return "ia64";
#else
    return "i386";
#endif
}

static HANDLE lockfile = NULL;

static void
DeleteLockFile(void)
{
    if (lockfile)
        UnlockFile(lockfile, 0, 0, 1, 0);
}

void
ExitIfNotOnlyInstance()
{
    const char *execname;

    /*
     * Use an exclusive lock on the executable file to determine if another
     * instance is already running.
     */
    if ((execname = GetExecName()) != NULL && (lockfile = CreateFile(execname, GENERIC_READ, FILE_SHARE_READ, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL)) != NULL)
    {
        if (LockFile(lockfile, 0, 0, 1, 0) == 0)
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
        atexit(DeleteLockFile);
    }
    return;
}

static char *tmpdir = NULL;

jboolean
SetTempDir(const char *path)
{
    char *tmppath = NULL;
    struct stat sb;

    if (path && !IsAbsolutePath(path))
    {
        char *cwd = NULL;
        unsigned int size = MAXPATHLEN;

        cwd = (char *)MemAlloc(size);
        while (GetCurrentDirectory(size, cwd) >= size)
        {
            size += MAXPATHLEN;
            free(cwd);
            cwd = (char *)MemAlloc(size);
        }
        tmppath = (char *)MemAlloc(strlen(cwd) + strlen(path) + 2);
        sprintf(tmppath, "%s" FILE_SEPARATOR "%s", cwd, path);
        free(cwd);
        cwd = NULL;
    }
    else
    {
        tmppath = StrDup(path);
    }
    if (tmppath[strlen(tmppath) - 1] == FILE_SEPARATOR_CHAR)
        tmppath[strlen(tmppath) - 1] = '\0';
    if (access(tmppath, R_OK | W_OK) == 0 && stat(tmppath, &sb) == 0 && (sb.st_mode & S_IFDIR) == S_IFDIR)
    {
        if (tmpdir)
            free(tmpdir);
        tmpdir = tmppath;
        return JNI_TRUE;
    }
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
    char *fullkey = NULL;
    char *lc = NULL;
    message_t sym = NULL;
    const wchar_t *value;
    int size;
    char *ret = NULL;

    lc = StrDup(GetLocale());
    fullkey = (char *)MemAlloc(sizeof(RES_BUNDLE_NAME) + strlen(lc) + strlen(key
) + 3);
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
        if ((sym = (message_t)GetProcAddress(NULL, fullkey)) == NULL)
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
    if ((size = WideCharToMultiByte(GetACP() ? CP_ACP : CP_UTF8, 0, value, -1, NULL, 0, NULL, NULL)) < 0)
        goto error;
    ret = (char *)MemAlloc(size);
    if ((size = WideCharToMultiByte(GetACP() ? CP_ACP : CP_UTF8, 0, value, -1, ret, size, NULL, NULL)) < 0)
        goto error;

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
    {
        char *path = NULL;
        int size = MAXPATHLEN;
        int ret;

        path = (char *)MemAlloc(size);
        while ((ret = GetTempPath(size, path)) != 0 && ret >= size)
        {
            size = ret;
            free(path);
            path = (char *)MemAlloc(size);
        }
        if (ret)
        {
            SetTempDir(path);
        }
        else
        {
            free(path);
            path = NULL;
        }
    }
    return tmpdir;
}

/*
 * Find path to JRE based on a user specified Java installation directory.
 */
char *
GetJREPath(const char *path)
{
    char *libjava = NULL;

    libjava = (char *)MemAlloc(strlen(path) + strlen(JAVA_DLL) + 11);

    /* Is JRE co-located with the application? */
    sprintf(libjava, "%s" FILE_SEPARATOR "bin" FILE_SEPARATOR JAVA_DLL, path);
    if (access(libjava, R_OK) == 0)
    {
        strcpy(libjava, path);
        goto found;
    }

    /* Does the app ship a private JRE in <apphome>/jre directory? */
    sprintf(libjava, "%s" FILE_SEPARATOR "jre" FILE_SEPARATOR "bin" FILE_SEPARATOR JAVA_DLL, path);
    if (access(libjava, R_OK) == 0)
    {
        sprintf(libjava, "%s" FILE_SEPARATOR "jre", path);
        goto found;
    }

    if (libjava)
        free(libjava);
    libjava = NULL;

found:
    if (libjava)
    {
        char *java = NULL;
        char *javaw = NULL;
        char *keytool = NULL;

        /* Check that executables exist */
        java = (char *)MemAlloc(strlen(libjava) + strlen(JAVA_EXE) + 1);
        javaw = (char *)MemAlloc(strlen(libjava) + strlen(JAVAW_EXE) + 1);
        keytool = (char *)MemAlloc(strlen(libjava) + strlen(KEYTOOL_EXE) + 1);
        sprintf(java, "%s" JAVA_EXE, libjava);
        sprintf(javaw, "%s" JAVAW_EXE, libjava);
        sprintf(keytool, "%s" KEYTOOL_EXE, libjava);
        if (access(java, R_OK) != 0 || access(javaw, R_OK) != 0 || access(keytool, R_OK) != 0)
        {
            free(libjava);
            libjava = NULL;
        }
        free(java);
        java = NULL;
        free(javaw);
        javaw = NULL;
        free(keytool);
        keytool = NULL;
    }
    return libjava;
}

/*
 * Helpers to look in the registry for a public JRE.
 */
static char *
GetStringFromRegistry(HKEY key, const char *name)
{
    char *regvalue = NULL;
    DWORD type, size;

    if (RegQueryValueEx(key, name, 0, &type, 0, &size) == 0)
    {
        regvalue = (char *)MemAlloc(size + 1);
        if (RegQueryValueEx(key, name, 0, 0, regvalue, &size) != 0)
        {
            free(regvalue);
            regvalue = NULL;
        }
    }
    return regvalue;
}

HINSTANCE instance = NULL;
HWND statusdialog = NULL;
static char *statusmessage = NULL;
static jboolean dispatch = JNI_FALSE;

static DWORD
WINAPI DispatchWindowsEvents(LPVOID arg)
{
    /* Create the dialog */
    if ((statusdialog = CreateDialog(instance, MAKEINTRESOURCE(IDD_DIALOGBAR), NULL, StatusCallback)) == NULL)
        return 1;

    /* Set title */
    SetWindowText(statusdialog, GetTitle());

    while (dispatch)
    {
        MSG msg;

        if (GetMessage(&msg, statusdialog, 0, 0) != -1)
        {
            TranslateMessage(&msg);
            DispatchMessage(&msg);
        }
        Sleep(0);
    }

    /* Destry the dialog */
    HideStatusWindow();
    DestroyWindow(statusdialog);
    statusdialog = NULL;
    return 0;
}

static INT_PTR CALLBACK
StatusCallback(HWND hWndDlg, UINT uMsg, WPARAM wParam, LPARAM lParam)
{
    HDC dc;
    PAINTSTRUCT ps;
    INT_PTR ret = FALSE;

    switch (uMsg)
    {
        case WM_PAINT:
            InvalidateRect(statusdialog, NULL, TRUE);
            if (hWndDlg && statusmessage && (dc = BeginPaint(hWndDlg, &ps)) != NULL)
            {
                NONCLIENTMETRICS metrics;
                HFONT font = NULL;
                HFONT oldfont = NULL;
                RECT dialogrect;
                RECT textrect;
                RECT oldtextrect;
                int textheight;
                jboolean resize = JNI_FALSE;

                /* Attempt to get the standard message box font */
                metrics.cbSize = sizeof(NONCLIENTMETRICS);
                if (SystemParametersInfo(SPI_GETNONCLIENTMETRICS, sizeof(NONCLIENTMETRICS), &metrics, 0) && (font = CreateFontIndirect(&metrics.lfMessageFont)) != NULL)
                    oldfont = SelectObject(dc, font);

                /*
                 * Calculate how much space we need for the text and resize
                 * and resize dialog if necessary
                 */
                GetWindowRect(hWndDlg, &dialogrect);
                textrect.left = MIN_TEXT_MARGIN;
                textrect.top = MIN_TEXT_MARGIN;
                textrect.right = dialogrect.right - dialogrect.left - MIN_TEXT_MARGIN;
                textrect.bottom = dialogrect.bottom - dialogrect.top - MIN_TEXT_MARGIN;
                CopyRect(&oldtextrect, &textrect);
                textheight = DrawText(dc, statusmessage, -1, &textrect, DT_CALCRECT | DT_CENTER | DT_EXPANDTABS | DT_NOPREFIX);
                if (textrect.right > oldtextrect.right)
                {
                    LONG adjust = ((textrect.right - oldtextrect.right) / 2) + ((textrect.right - oldtextrect.right) % 2);
                    resize = JNI_TRUE;
                    dialogrect.left -= adjust;
                    dialogrect.right += adjust;
                }
                if (textrect.bottom > oldtextrect.bottom)
                {
                    LONG adjust = ((textrect.bottom - oldtextrect.bottom) / 2) + ((textrect.bottom - oldtextrect.bottom) % 2);
                    dialogrect.top -= adjust;
                    dialogrect.bottom += adjust;
                    resize = JNI_TRUE;
                }
                if (resize)
                {
                    SetWindowPos(hWndDlg, HWND_TOP, dialogrect.left, dialogrect.top, dialogrect.right - dialogrect.left, dialogrect.bottom - dialogrect.top, 0);
                    GetWindowRect(hWndDlg, &dialogrect);
                }
                textrect.left = MIN_TEXT_MARGIN;
                textrect.top += (dialogrect.bottom - dialogrect.top - MIN_TEXT_MARGIN - textrect.top - textheight) / 2;
                textrect.right = dialogrect.right - dialogrect.left - MIN_TEXT_MARGIN;
                textrect.bottom = textrect.top + textheight;
                DrawText(dc, statusmessage, -1, &textrect, DT_CENTER | DT_EXPANDTABS | DT_NOPREFIX);
                if (oldfont)
                {
                    SelectObject(dc, oldfont);
                    oldfont = NULL;
                }
                if (font)
                {
                    DeleteObject(font);
                    font = NULL;
                }
                EndPaint(hWndDlg, &ps);
            }
            ret = TRUE;
            break;
        case WM_USER:
            if (lParam)
            {
                char *oldmessage = statusmessage;
                statusmessage = (char *)lParam;
                if (oldmessage)
                    free(oldmessage);
                oldmessage = NULL;
                ShowWindow(statusdialog, SW_SHOW);
                InvalidateRect(statusdialog, NULL, TRUE);
                SetForegroundWindow(statusdialog);
                UpdateWindow(statusdialog);
            }
            ret = TRUE;
            break;
        default:
            ret = FALSE;
            break;
    }
    return ret;
}

static jboolean
LoadWindows()
{
    static jboolean loadingwin = JNI_FALSE;
    HANDLE thrd = NULL;
    DWORD ctid = GetCurrentThreadId();
    DWORD tid;

    if (!instance)
        return JNI_FALSE;

    if (loadingwin)
        return JNI_FALSE;

    if (dispatch)
        return JNI_TRUE;

    loadingwin = JNI_TRUE;

    /* Initialize the lock */
    if (!dispatch)
        dispatch = JNI_TRUE;

    /* Create the event loop thread */
    if ((thrd = CreateThread(NULL, 0, DispatchWindowsEvents, &ctid, 0, &tid)) == NULL)
        goto error;

    /* Raise the thread's priority so that it is guaranteed a time slice */
    if (!SetThreadPriority(thrd, THREAD_PRIORITY_HIGHEST))
        goto error;

    loadingwin = JNI_FALSE;
    return JNI_TRUE;

error:
    if (dispatch)
    {
        dispatch = JNI_FALSE;
        if (thrd)
        {
            WaitForSingleObject(thrd, INFINITE);
            CloseHandle(thrd);
        }
    }
    loadingwin = JNI_FALSE;
    return JNI_FALSE;
}

static int
PrintToStatusWindow(const char *format, va_list args)
{
    int ret;
    int increment = 1024;
    int size = increment;
    char *message = NULL;
    int cachedlen = 0;
    static char *cachedmessage = NULL;

    if (cachedmessage)
        cachedlen = strlen(cachedmessage);
    for (;;)
    {
        message = (char *)MemAlloc(cachedlen + size);
        if (cachedmessage)
            strcpy(message, cachedmessage);

        ret = _vsnprintf(message + cachedlen, size, format, args);
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

    if (LoadWindows() && dispatch)
    {
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
        }

        if (statusdialog)
        {
            ret = strlen(message);
            if (PostMessage(statusdialog, WM_USER, 0, (LPARAM)message))
                message = NULL;
            else
                ret = vprintf(message, NULL);
        }
        else
        {
            ret = vprintf(message, NULL);
        }
    }
    else
    {
        ret = vprintf(message, NULL);
    }

    if (message)
        free(message);
    message = NULL;
    return ret;
}

static void
HideStatusWindow()
{
    if (LoadWindows() && dispatch && statusdialog)
        ShowWindow(statusdialog, SW_HIDE);
}

static int
PrintToMessageBox(UINT type, const char *format, va_list args)
{
    int ret;
    int increment = 1024;
    int size = increment;
    char *message = NULL;
    int cachedlen = 0;
    static char *cachedmessage = NULL;

    if (cachedmessage)
        cachedlen = strlen(cachedmessage);
    for (;;)
    {
        message = (char *)MemAlloc(cachedlen + size);
        if (cachedmessage)
            strcpy(message, cachedmessage);

        ret = _vsnprintf(message + cachedlen, size, format, args);
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

    if (LoadWindows() && dispatch)
    {
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
        }

        if (type == MB_ICONINFORMATION)
            MessageBox(statusdialog, message, GetTitle(), MB_OK | MB_APPLMODAL | MB_ICONINFORMATION);
        else
            MessageBox(statusdialog, message, GetTitle(), MB_OK | MB_APPLMODAL | MB_ICONERROR);
    }
    else
    {
        ret = vprintf(message, NULL);
    }

    if (message)
        free(message);
    message = NULL;
    return ret;
}

#define JDK_KEY "Software" FILE_SEPARATOR "JavaSoft" FILE_SEPARATOR "Java Development Kit"
#define JRE_KEY "Software" FILE_SEPARATOR "JavaSoft" FILE_SEPARATOR "Java Runtime Environment"
#define JAVA_HOME_VALUE "JavaHome"

char *
GetPublicJREPath()
{
    const char *envvar;
    struct jvminfo *bundledjdk = NULL;
    struct jvminfo *bundledjre = NULL;
    struct jvminfo *jdks = NULL;
    struct jvminfo *jres = NULL;
    char *jrefoundpath = NULL;
    HKEY key, subkey;
    int index;
    char subkeyName[MAXPATHLEN];
    int subkeySize;
    FILETIME subkeyTime;

    if ((envvar = GetBundledJREPath()) != NULL)
    {
        struct jvminfo *info = (struct jvminfo *)MemAlloc(sizeof(struct jvminfo)
);
        info->version = NULL;
        info->path = (char *)MemAlloc(strlen(envvar) + 1);
        info->next = NULL;
        strcpy(info->path, envvar);
        if (IsJDK(envvar))
            bundledjdk = info;
        else
            bundledjre = info;
    }

    /* Look for a public JDK in the Windows registry. */
    if (!RegOpenKeyEx(HKEY_LOCAL_MACHINE, JDK_KEY, 0, KEY_ALL_ACCESS, &key))
    {
        jboolean jdkfound = JNI_FALSE;

        index = 0;
        subkeySize = sizeof(subkeyName);
        /* Iterate through each of the JDK version keys */
        while (RegEnumKeyEx(key, index++, subkeyName, &subkeySize, NULL, NULL, NULL, &subkeyTime) == 0)
        {
            char *javapath;
            char *jrepath = NULL;
            subkeySize = sizeof(subkeyName);

            if (RegOpenKeyEx(key, subkeyName, 0, KEY_READ, &subkey) != 0)
                continue;

            /* Find a matching supported version */
            if (!CheckVersion(subkeyName))
            {
                RegCloseKey(subkey);
                continue;
            }

            if ((javapath = GetStringFromRegistry(subkey, JAVA_HOME_VALUE)) == NULL)
            {
                RegCloseKey(subkey);
                continue;
            }

            if ((jrepath = GetJREPath(javapath)) != NULL)
            {
                struct jvminfo *info = (struct jvminfo *)MemAlloc(sizeof(struct jvminfo));
                info->version = (char *)MemAlloc(strlen(subkeyName) + 1);
                strcpy(info->version, subkeyName);
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
                free(jrepath);
                jrepath = NULL;
            }
            free(javapath);
            javapath = NULL;
            RegCloseKey(subkey);
            if (jdkfound)
            {
                RegCloseKey(key);
                goto found;
            }
        }
        RegCloseKey(key);
    }

    /* Look for a public JRE in the Windows registry. */
    if (!RegOpenKeyEx(HKEY_LOCAL_MACHINE, JRE_KEY, 0, KEY_ALL_ACCESS, &key))
    {
        jboolean jdkfound = JNI_FALSE;

        index = 0;
        subkeySize = sizeof(subkeyName);
        /* Iterate through each of the JRE version keys */
        while (RegEnumKeyEx(key, index++, subkeyName, &subkeySize, NULL, NULL, NULL, &subkeyTime) == 0)
        {
            char *javapath;
            char *jrepath = NULL;
            subkeySize = sizeof(subkeyName);

            if (RegOpenKeyEx(key, subkeyName, 0, KEY_READ, &subkey) != 0)
                continue;

            /* Find a matching supported version */
            if (!CheckVersion(subkeyName))
            {
                RegCloseKey(subkey);
                continue;
            }

            if ((javapath = GetStringFromRegistry(subkey, JAVA_HOME_VALUE)) == NULL)
            {
                RegCloseKey(subkey);
                continue;
            }

            if ((jrepath = GetJREPath(javapath)) != NULL)
            {
                struct jvminfo *info = (struct jvminfo *)MemAlloc(sizeof(struct jvminfo));
                info->version = (char *)MemAlloc(strlen(subkeyName) + 1);
                strcpy(info->version, subkeyName);
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
                free(jrepath);
                jrepath = NULL;
            }
            free(javapath);
            javapath = NULL;
            RegCloseKey(subkey);
        }
        RegCloseKey(key);
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
    char *toolsjar = (char *)MemAlloc(strlen(path) + sizeof(TOOLS_JAR_FILE) + 4)
;
    char *dir = strrchr(path, FILE_SEPARATOR_CHAR);

    /* Does the app ship a private JRE in <apphome>/jre directory? */
    if (dir && !strcmp(dir, FILE_SEPARATOR "jre"))
    {
        sprintf(toolsjar, "%s" FILE_SEPARATOR ".." TOOLS_JAR_FILE,  path);
        if (access(toolsjar, R_OK) == 0)
        {
            isJDK = JNI_TRUE;
            goto found;
        }
    }
    /* Is JRE co-located with the application? */
    else
    {
        sprintf(toolsjar, "%s" TOOLS_JAR_FILE,  path);
        if (access(toolsjar, R_OK) == 0)
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

/* Default to GUI mode */
static jboolean printToConsole = JNI_FALSE;

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
    if (printToConsole || !LoadWindows())
        ret = vprintf(format, args);
    else
        ret = PrintToMessageBox(MB_ICONINFORMATION, format, args);
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
    if (printToConsole || !LoadWindows())
    {
        ret = vprintf(format, args);
    }
    else
    {
        if (stream == stderr)
            ret = PrintToMessageBox(MB_ICONERROR, format, args);
        else
            ret = PrintToMessageBox(MB_ICONINFORMATION, format, args);
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
        if (printToConsole || !LoadWindows())
            ret = vprintf(format, args);
        else
            ret = PrintToStatusWindow(format, args);
    }
    else
    {
        HideStatusWindow();
    }
    va_end(args);
    return ret;
}

/*
 * Encodings for Windows language groups. Some locales do not have codepages,
 * and are supported in Windows 2000 solely through Unicode, so we also add
 * the appropriate Unicode variant.
 *
 * Note: this table is copied from the windows/native/common/locale_str.h
 * header in the J2SE 1.4.1 source code and should be updated as new releases
 * of J2SE come out.
 */
static const char *encoding_names[] = {
    "Cp1250",    /*  0:Latin 2  */
    "Cp1251",    /*  1:Cyrillic */
    "Cp1252",    /*  2:Latin 1  */
    "Cp1253",    /*  3:Greek    */
    "Cp1254",    /*  4:Latin 5  */
    "Cp1255",    /*  5:Hebrew   */
    "Cp1256",    /*  6:Arabic   */
    "Cp1257",    /*  7:Baltic   */
    "Cp1258",    /*  8:Viet Nam */
    "MS874",     /*  9:Thai     */
    "MS932",     /* 10:Japanese */
    "GBK",       /* 11:PRC GBK  */
    "MS949",     /* 12:Korean Extended Wansung */
    "MS950",     /* 13:Chinese (Taiwan, Hongkong, Macau) */
    "utf-16le",  /* 14:Unicode  */
    "MS1361",    /* 15:Korean Johab */
};

/*
 * List mapping from LanguageID to Java locale IDs.
 *
 * Note: this table is copied from the windows/native/common/locale_str.h
 * header in the J2SE 1.4.1 source code and should be updated as new releases
 * of J2SE come out.
 */
typedef struct LANGIDtoLocale {
    const WORD    langID;
    const WORD    encoding;
    const char*   javaID;
} LANGIDtoLocale;

static const LANGIDtoLocale langIDMap[] = {
    /* fallback locales to use when the country code doesn't match anything we have */
    0x01,    6, "ar",
    0x02,    1, "bg",
    0x03,    2, "ca",
    0x04,   11, "zh",
    0x05,    0, "cs",
    0x06,    2, "da",
    0x07,    2, "de",
    0x08,    3, "el",
    0x09,    2, "en",
    0x0a,    2, "es",
    0x0b,    2, "fi",
    0x0c,    2, "fr",
    0x0d,    5, "iw",
    0x0e,    0, "hu",
    0x0f,    2, "is",
    0x10,    2, "it",
    0x11,   10, "ja",
    0x12,   12, "ko",
    0x13,    2, "nl",
    0x14,    2, "no",
    0x15,    0, "pl",
    0x16,    2, "pt",
    0x17,    2, "rm",
    0x18,    0, "ro",
    0x19,    1, "ru",
    0x1a,    0, "sh",
    0x1b,    0, "sk",
    0x1c,    0, "sq",
    0x1d,    2, "sv",
    0x1e,    9, "th",
    0x1f,    4, "tr",
    0x20,    2, "ur",
    0x21,    2, "in",
    0x22,    1, "uk",
    0x23,    1, "be",
    0x24,    0, "sl",
    0x25,    7, "et",
    0x26,    7, "lv",
    0x27,    7, "lt",
    0x29,    6, "fa",
    0x2a,    8, "vi",
    0x2b,   14, "hy",
    0x2c,    4, "az",
    0x2d,    2, "eu",
    0x2f,    1, "mk",
    0x31,    2, "ts",
    0x32,    2, "tn",
    0x34,    2, "xh",
    0x35,    2, "zu",
    0x36,    2, "af",
    0x37,   14, "ka",
    0x38,    2, "fo",
    0x39,   14, "hi",
    0x3a,    2, "mt",
    0x3c,    2, "gd",
    0x3d,    2, "yi",
    0x3e,    2, "ms",
    0x3f,    1, "kk",
    0x40,    1, "ky",
    0x41,    2, "sw",
    0x43,    1, "uz",
    0x44,    1, "tt",
    0x46,   14, "pa",
    0x47,   14, "gu",
    0x49,   14, "ta",
    0x4a,   14, "te",
    0x4b,   14, "kn",
    0x4e,   14, "mr",
    0x4f,   14, "sa",
    0x50,    1, "mn",
    0x56,    2, "gl",
    /* mappings for real Windows LCID values */
    0x0401,  6, "ar_SA",
    0x0402,  1, "bg_BG",
    0x0403,  2, "ca_ES",
    0x0404, 13, "zh_TW",
    0x0405,  0, "cs_CZ",
    0x0406,  2, "da_DK",
    0x0407,  2, "de_DE",
    0x0408,  3, "el_GR",
    0x0409,  2, "en_US",
    0x040a,  2, "es_ES",  /* (traditional sort) */
    0x040b,  2, "fi_FI",
    0x040c,  2, "fr_FR",
    0x040d,  5, "iw_IL",
    0x040e,  0, "hu_HU",
    0x040f,  2, "is_IS",
    0x0410,  2, "it_IT",
    0x0411, 10, "ja_JP",
    0x0412, 12, "ko_KR",
    0x0413,  2, "nl_NL",
    0x0414,  2, "no_NO",
    0x0415,  0, "pl_PL",
    0x0416,  2, "pt_BR",
    0x0417,  2, "rm_CH",
    0x0418,  0, "ro_RO",
    0x0419,  1, "ru_RU",
    0x041a,  0, "hr_HR",
    0x041b,  0, "sk_SK",
    0x041c,  0, "sq_AL",
    0x041d,  2, "sv_SE",
    0x041e,  9, "th_TH",
    0x041f,  4, "tr_TR",
    0x0420,  6, "ur_PK",
    0x0421,  2, "in_ID",
    0x0422,  1, "uk_UA",
    0x0423,  1, "be_BY",
    0x0424,  0, "sl_SI",
    0x0425,  7, "et_EE",
    0x0426,  7, "lv_LV",
    0x0427,  7, "lt_LT",
    0x0429,  6, "fa_IR",
    0x042a,  8, "vi_VN",
    0x042b, 14, "hy_AM",  /* Armenian  */
    0x042c,  4, "az_AZ",  /* Azeri_Latin */
    0x042d,  2, "eu_ES",
/*  0x042e,  2, "??",      no ISO-639 abbreviation for Sorbian */
    0x042f,  1, "mk_MK",
/*  0x0430,  2, "??",      no ISO-639 abbreviation for Sutu */
    0x0431,  2, "ts",     /* (country?) */
    0x0432,  2, "tn_BW",
/*  0x0433,  2, "??",      no ISO-639 abbreviation for Venda */
    0x0434,  2, "xh",     /* (country?) */
    0x0435,  2, "zu",     /* (country?) */
    0x0436,  2, "af_ZA",
    0x0437, 14, "ka_GE",  /* Georgian   */
    0x0438,  2, "fo_FO",
    0x0439, 14, "hi_IN",
    0x043a,  2, "mt_MT",
/*  0x043b,  2, "??",      no ISO-639 abbreviation for Sami */
    0x043c,  2, "gd_GB",
    0x043d,  2, "yi",     /* (country?) */
    0x043e,  2, "ms_MY",
    0x043f,  1, "kk_KZ",  /* Kazakh */
    0x0440,  1, "ky_KG",  /* Kyrgyz     */
    0x0441,  2, "sw_KE",
    0x0443,  1, "uz_UZ",  /* Uzbek_Cyrillic*/
    0x0444,  1, "tt",     /* Tatar, no ISO-3166 abbreviation */
    0x0446, 14, "pa_IN",  /* Punjabi   */
    0x0447, 14, "gu_IN",  /* Gujarati  */
    0x0449, 14, "ta_IN",  /* Tamil     */
    0x044a, 14, "te_IN",  /* Telugu    */
    0x044b, 14, "kn_IN",  /* Kannada   */
    0x044e, 14, "mr_IN",  /* Marathi   */
    0x044f, 14, "sa_IN",  /* Sanskrit  */
    0x0450,  1, "mn_MN",  /* Mongolian */
    0x0456,  2, "gl_ES",  /* Galician  */
/*  0x0457, 14, "??_IN",  /* Konkani, no ISO-639 abbreviation*/
/*  0x045a, 14, "??_SY",  /* Syriac, no ISO-639 abbreviation*/
/*  0x0465, 14, "??_MV",  /* Divehi, no ISO-639 abbreviation*/
    0x0801,  6, "ar_IQ",
    0x0804, 11, "zh_CN",
    0x0807,  2, "de_CH",
    0x0809,  2, "en_GB",
    0x080a,  2, "es_MX",
    0x080c,  2, "fr_BE",
    0x0810,  2, "it_CH",
    0x0812, 15, "ko_KR",  /* Korean(Johab)*/
    0x0813,  2, "nl_BE",
    0x0814,  2, "no_NO_NY",
    0x0816,  2, "pt_PT",
    0x0818,  0, "ro_MD",
    0x0819,  1, "ru_MD",
    0x081a,  0, "sh_YU",
    0x081d,  2, "sv_FI",
    0x082c,  1, "az_AZ",  /* Azeri_Cyrillic */
    0x083c,  2, "ga_IE",
    0x083e,  2, "ms_BN",
    0x0843,  4, "uz_UZ",  /* Uzbek_Latin  */
    0x0c01,  6, "ar_EG",
    0x0c04, 13, "zh_HK",
    0x0c07,  2, "de_AT",
    0x0c09,  2, "en_AU",
    0x0c0a,  2, "es_ES",  /* (modern sort) */
    0x0c0c,  2, "fr_CA",
    0x0c1a,  1, "sr_YU",
    0x1001,  6, "ar_LY",
    0x1004, 11, "zh_SG",
    0x1007,  2, "de_LU",
    0x1009,  2, "en_CA",
    0x100a,  2, "es_GT",
    0x100c,  2, "fr_CH",
    0x1401,  6, "ar_DZ",
    0x1404, 13, "zh_MO",
    0x1407,  2, "de_LI",
    0x1409,  2, "en_NZ",
    0x140a,  2, "es_CR",
    0x140c,  2, "fr_LU",
    0x1801,  6, "ar_MA",
    0x1809,  2, "en_IE",
    0x180a,  2, "es_PA",
    0x180c,  2, "fr_MC",
    0x1c01,  6, "ar_TN",
    0x1c09,  2, "en_ZA",
    0x1c0a,  2, "es_DO",
    0x2001,  6, "ar_OM",
    0x2009,  2, "en_JM",
    0x200a,  2, "es_VE",
    0x2401,  6, "ar_YE",
    0x2409,  2, "en",     /* ("Caribbean", which could be any of many countries) */
    0x240a,  2, "es_CO",
    0x2801,  6, "ar_SY",
    0x2809,  2, "en_BZ",
    0x280a,  2, "es_PE",
    0x2c01,  6, "ar_JO",
    0x2c09,  2, "en_TT",
    0x2c0a,  2, "es_AR",
    0x3001,  6, "ar_LB",
    0x3009,  2, "en_ZW",
    0x300a,  2, "es_EC",
    0x3401,  6, "ar_KW",
    0x3409,  2, "en_PH",
    0x340a,  2, "es_CL",
    0x3801,  6, "ar_AE",
    0x380a,  2, "es_UY",
    0x3c01,  6, "ar_BH",
    0x3c0a,  2, "es_PY",
    0x4001,  6, "ar_QA",
    0x400a,  2, "es_BO",
    0x440a,  2, "es_SV",
    0x480a,  2, "es_HN",
    0x4c0a,  2, "es_NI",
    0x500a,  2, "es_PR"
};

static const char *locale = NULL;

const char *
GetLocale()
{
    if (!locale)
    {
        int index = -1;
        int tries = 0;

        /*
         * Query the system for the current system default locale
         * (which is a Windows LCID value),
         */
        LANGID langID = LANGIDFROMLCID(GetThreadLocale());

        /*
         * binary-search our list of LANGID values.  If we don't find the
	 * one we're looking for, mask out the country code and try again
	 * with just the primary language ID
         */
        do
        {
            int lo, hi, mid;
            lo = 0;
            hi = sizeof(langIDMap) / sizeof(LANGIDtoLocale);
            while (index == -1 && lo < hi)
            {
                mid = (lo + hi) / 2;
                if (langIDMap[mid].langID == langID)
                    index = mid;
                else if (langIDMap[mid].langID > langID)
                    hi = mid;
                else
                    lo = mid + 1;
            }
            langID = PRIMARYLANGID(langID);
            ++tries;
        } while (index == -1 && tries < 2);

        /*
         * If we found the LCID, look up the corresponding Java locale ID from
         * the list of Java locale IDs and set up the system properties
         * accordingly. Otherwise, fall back to "en".
         */
        if (index != -1)
            locale = langIDMap[index].javaID;
        else
            locale = "en";
    }

    return locale;
}

jboolean
IsMultiByteChar(const char *s, int pos)
{
    char *t = NULL;
    int lenwith;
    int lenwithout;
    jboolean ret = JNI_FALSE;

    /* Make sure that preceding character is not a multi-byte character */
    t = StrDup(s);
    t[pos + 1] = '\0';
    if ((lenwith = MultiByteToWideChar(GetACP() ? CP_ACP : CP_UTF8, 0, t, -1, NULL, 0)) < 0)
        goto leave;
    t[pos] = '\0';
    if ((lenwithout = MultiByteToWideChar(GetACP() ? CP_ACP : CP_UTF8, 0, t, -1, NULL, 0)) < 0)
        goto leave;
    if (lenwithout + 1 == lenwith)
        ret = JNI_TRUE;

leave:
    free(t);
    t = NULL;
    return ret;
}

char *
CheckJREVersion(const char *path)
{
    char *version = NULL;

    if (access(path, F_OK) == 0)
    {
        char *cmdline = NULL;
        SECURITY_ATTRIBUTES sa;
        PROCESS_INFORMATION pi;
        STARTUPINFO si;
        HANDLE inpipe[2];
        HANDLE outpipe[2];
        HANDLE errpipe[2];

        sa.nLength = sizeof(SECURITY_ATTRIBUTES);
        sa.bInheritHandle = TRUE;
        sa.lpSecurityDescriptor = NULL;
        memset(inpipe, 0, sizeof(inpipe));
        memset(outpipe, 0, sizeof(outpipe));
        memset(errpipe, 0, sizeof(errpipe));
        if (!CreatePipe(&inpipe[0], &inpipe[1], &sa, PIPE_SIZE) || !CreatePipe(&outpipe[0], &outpipe[1], &sa, PIPE_SIZE) || !CreatePipe(&errpipe[0], &errpipe[1], &sa, PIPE_SIZE))
            goto leave;

        /*
         * Determine the amount of memory to allocate assuming the individual
         * components will be quoted and space separated
         */
        cmdline = (char *)MemAlloc(strlen(path) + strlen(JAVAW_EXE) + strlen("-version") + 6);
        strcpy(cmdline, "\"");
        strcat(cmdline, path);
        strcat(cmdline, JAVAW_EXE "\" \"-version\"");

        memset(&pi, 0, sizeof(PROCESS_INFORMATION));
        memset(&si, 0, sizeof(STARTUPINFO));
        si.cb = sizeof(STARTUPINFO);
        si.dwFlags = STARTF_USESTDHANDLES;
        si.hStdInput = inpipe[0];
        si.hStdOutput = outpipe[1];
        si.hStdError = errpipe[1];
        SetHandleInformation(inpipe[1], HANDLE_FLAG_INHERIT, FALSE);
        SetHandleInformation(outpipe[0], HANDLE_FLAG_INHERIT, FALSE);
        SetHandleInformation(errpipe[0], HANDLE_FLAG_INHERIT, FALSE);

        if (CreateProcess(NULL, cmdline, NULL, NULL, TRUE, 0, NULL, NULL, &si, &pi))
        {
            DWORD bytes;
            char *str = NULL;

            WaitForSingleObject(pi.hProcess, INFINITE);
            CloseHandle(pi.hProcess);
            CloseHandle(pi.hThread);
            CloseHandle(inpipe[0]);
            CloseHandle(outpipe[1]);
            CloseHandle(errpipe[1]);
            /*
             * Read the first line of output. We only need to read once since
             * the standard buffer size is far larger than the longest
             * version string.
             */
            version = (char *)MemAlloc(PIPE_SIZE + 1);
            ReadFile(errpipe[0], version, PIPE_SIZE, &bytes, NULL);
            if (bytes <= 0)
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

leave:
        if (cmdline)
            free(cmdline);
        cmdline = NULL;
        CloseHandle(inpipe[0]);
        CloseHandle(inpipe[1]);
        CloseHandle(outpipe[0]);
        CloseHandle(outpipe[1]);
        CloseHandle(errpipe[0]);
        CloseHandle(errpipe[1]);
    }

    return version;
}

/*
 * Execute a Java subprocess with the specified arguments.
 */
int
ExecuteJava(const char *jrepath, int numOptions, const JavaVMOption *options,int numProps, const JavaPropsOption *props)
{
    int i = 0;
    jboolean console = GetPrintToConsole();
    const char* class = GetJavaClassName();
    char *command = NULL;
    char *cmdline = NULL;
    size_t len = 0;
    const char *bundledlibpath = jrepath;
    const char *oldpath = getenv("PATH");
    char *newpath = NULL;
    PROCESS_INFORMATION pi;
    STARTUPINFO si;
    int ret = 1;

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

    if (access(command, R_OK) != 0)
    {
        char *message = GetLocalizedMessage("java_aborted");
        fprintf(stderr, message);
        free(message);
        message = NULL;
        free(command);
        command = NULL;
        return ret;
    }

    /*
     * Determine the amount of memory to allocate assuming the individual
     * components will be quoted and space separated
     */
    len = strlen(command) + 2;
    for (i = 0; i < numOptions; i++)
        len += strlen(options[i].optionString) + 3;
    for (i = 0; i < numProps; i++)
        len += strlen(props[i].propsString) + 3;

    len += strlen(class) + 3;

    cmdline = (char *)MemAlloc(len + 10 + 1);

    strcpy(cmdline, "\"");
    strcat(cmdline, command);
    strcat(cmdline, "\" ");
    /* Add the default Option "-client" here. */
    /*
    strcat(cmdline, " -client ");
    */
    for (i = 0; i < numOptions; i++)
    {
    	strcat(cmdline, "\"");
        strcat(cmdline, options[i].optionString);
    	strcat(cmdline, "\" ");
    }
    strcat(cmdline, " \"");
    strcat(cmdline, class);
    strcat(cmdline, "\" ");

    for (i = 0; i < numProps; i++)
    {
	strcat(cmdline, " ");
        strcat(cmdline, props[i].propsString);
	strcat(cmdline, " ");
    }


    newpath = (char *)MemAlloc(strlen("PATH=") + (bundledlibpath ? strlen(bundledlibpath) + 1 : 0) + (oldpath ? strlen(oldpath) + 1 : 0) + 10);
    strcpy(newpath, "PATH=");
    if (bundledlibpath)
    {
        strcat(newpath, bundledlibpath);
        strcat(newpath, "\\bin");
        strcat(newpath, PATH_SEPARATOR);
    }
    if (oldpath)
    {
        strcat(newpath, oldpath);
        strcat(newpath, PATH_SEPARATOR);
    }
    if (newpath[strlen(newpath) - 1] == PATH_SEPARATOR_CHAR)
        newpath[strlen(newpath) - 1] = '\0';
    putenv(newpath);
    free(newpath);
    newpath = NULL;

    /* Create the console here so that we can set the title */
    if (console)
    {
        HWND consolewindow;

        AllocConsole();
        SetConsoleTitle("");
        consolewindow = FindWindow(NULL, "");
        if (IsIconic(consolewindow))
            ShowWindow(consolewindow, SW_RESTORE);
        SetForegroundWindow(consolewindow);
    }

    memset(&pi, 0, sizeof(PROCESS_INFORMATION));
    memset(&si, 0, sizeof(STARTUPINFO));
    si.cb = sizeof(STARTUPINFO);
    if (CreateProcess(NULL, cmdline, NULL, NULL, FALSE, 0, NULL, NULL, &si, &pi))
    {
        /* Wait a few seconds to display the status dialog */
        Sleep(10000);
        HideStatusWindow();

        WaitForSingleObject(pi.hProcess, INFINITE);
        GetExitCodeProcess(pi.hProcess, &ret);
        CloseHandle(pi.hProcess);
        CloseHandle(pi.hThread);
    }
    else
    {
        char *message = GetLocalizedMessage("java_aborted");
        fprintf(stderr, message);
        free(message);
        message = NULL;
        return ret;
    }

    free(command);
    command = NULL;
    free(cmdline);
    cmdline = NULL;

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
    const char *filename;
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

    if (stat(path, &sb) == 0)
    {
        if ((sb.st_mode & S_IFDIR) == S_IFDIR)
        {
            char *searchpath = NULL;
            if ((searchpath = (char *)malloc(strlen(path) + 5)) != NULL)
            {
                struct _finddata_t filefound;
                long filehandle;
                strcpy(searchpath, path);
                strcat(searchpath, FILE_SEPARATOR "*.*");
                if ((filehandle = _findfirst(searchpath, &filefound)) >= 0)
                {
                    char *entrypath = NULL;
                    do {
                        if ((entrypath = (char *)malloc(strlen(path) + strlen(filefound.name) + 2)) != NULL)
                        {
                            strcpy(entrypath, path);
                            strcat(entrypath, FILE_SEPARATOR);
                            strcat(entrypath, filefound.name);
                            DeleteFilesAndDirectories(entrypath, savelist);
                            free(entrypath);
                            entrypath = NULL;
                        }
                    } while (_findnext(filehandle, &filefound) == 0);
                }
                _findclose(filehandle);
                free(searchpath);
                searchpath = NULL;
            }
            RemoveDirectory(path);
        }
        else
        {
            DeleteFile(path);
        }
    }
}

/* used to invoke the upgrade tool in console mode*/
int
InvokeUpgradeTool(const char *invokerPath)
{

    char *cmdline = NULL;
    int ret = 1;
 
    PROCESS_INFORMATION pi;
    STARTUPINFO si;

    memset(&pi, 0, sizeof(PROCESS_INFORMATION));
    memset(&si, 0, sizeof(STARTUPINFO));
    si.cb = sizeof(STARTUPINFO);

    cmdline = (char *)MemAlloc(150);

    strcpy(cmdline, invokerPath);
 
    if(CreateProcess(NULL, cmdline, NULL, NULL, FALSE, 0, NULL, NULL, &si, &pi))
    {   
		WaitForSingleObject(pi.hProcess, INFINITE);
		GetExitCodeProcess(pi.hProcess, &ret);  
		CloseHandle(pi.hProcess);
		CloseHandle(pi.hThread);
	
		free(cmdline);
		cmdline=NULL;
	
    }
    else
    {
	
	     free(cmdline);
		 cmdline=NULL;
    }

	return ret;
}
	

