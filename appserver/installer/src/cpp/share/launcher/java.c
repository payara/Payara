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

#define AWT_HEADLESS_OPTION "-Djava.awt.headless=true"
#define CLASSPATH_OPTION "-classpath"
#define ENDORSED_DIRS_OPTION "-Djava.endorsed.dirs="
#define JAVA_IO_TMPDIR_OPTION "-Djava.io.tmpdir="
#define MAX_HEAP_OPTION "-Xmx512m"
#define STATE_OPTION "-DInstallFile="
#define DEBUG_OPTION "-Ddebug=true"
#define DOMAIN_CREATION_OPTION "-Dno.domain=true"
#define PORT_VERIFICATION_OPTION "-Dno.port.verification=true"
#define PASSWORD_VISIBLE_OPTION "-Dpassword.visible=true"
#define TMPDIR_OPTION "-Dtemp.dir="



static const char *progname = NULL;
static JavaVMOption *options = NULL;
static JavaPropsOption *props = NULL;

static int numOptions = 0;
static int numProps = 0;
static int maxOptions = 0;
static int maxProps = 0;

/*
 * Prototypes for functions internal to launcher.
 */
static void AddOption(char *str);
static void AddProp(char *str);
static jboolean ParseArguments(int *pargc, char ***pargv, char **pJavaHome, char **pTempDir, jboolean *pShowConsole, jboolean *pUseAnswerFileOption, char **pAnswerFile, jboolean *pUseAlternateRootOption, char **pAlternateRoot, jboolean *pStoreAnswerFile, char **pAnswerFileLocation, jboolean *pLogLevelOption, jboolean *pUseLogsLocation, char **pLogsLocation, int *pret);

static jboolean CheckJavaVersion(const char *jrepath);
static void DeleteWorkingDirectory(void);
static void PrepareToExit();
#ifdef WIN32
static long WINAPI HandleException(LPEXCEPTION_POINTERS exc);
#else
static void HandleSignalAndExit(int sig);
static void HandleSignalAndAbort(int sig);
#endif
static void PrintUsage(void);
static const char *SetExecName(const char *program);

static char *jdkClassPath[] = JDK_CLASSPATH;
#define NUM_JDK_CLASSPATH (sizeof(jdkClassPath) / sizeof(char *))

static char *applicationHome = NULL;
static char *workdir = NULL;
static jboolean isDebug = JNI_FALSE;
static jboolean noPortVerification = JNI_FALSE;
static jboolean noDomainCreation = JNI_FALSE;
static jboolean visiblePassword = JNI_FALSE;
static jboolean printWizardID = JNI_FALSE;
static jboolean inJava = JNI_FALSE;
static jboolean doCleanup = JNI_FALSE;

/*
 * Entry point.
 */
int
main(int argc, char **argv)
{
    char *s;
    int ret;
    int i;
    int j;
    int size;
    char **original_argv = argv;
    const char *apphome;
    const char *execname;
    char *javahome = NULL;
    char *tmpdir = NULL;
    char *jrepath = NULL;
    char **appclasspath = NULL;
    jboolean showconsole = JNI_FALSE;
    jzfile *zipFile;
    int zipError;
    char *upgradeInvokerName = NULL;

   /* Used for saving answer file. */
    jboolean useAnswerFileOption = JNI_FALSE;
    char *answerFile= NULL;
  
     /* Use Alternate Root */
    jboolean useAlternateRootOption = JNI_FALSE;
    char *alternateRoot = NULL;

     /* Logs Location */
    jboolean useLogsLocation = JNI_FALSE;
    char *logsLocation = NULL;


     /* Dry Run file location */
    jboolean storeAnswerFile = JNI_FALSE;
    char *answerFileLocation = NULL;

    /* Log Level */
    jboolean logLevelOption = JNI_FALSE;

	/* reference to Open Installer Framework zip File */
	char oiZipFile[MAXPATHLEN];

	/* reference to jdk zip File */
	char jdkZipFile[MAXPATHLEN];

    

        char classPath[2048];
#ifdef WIN32
    ULONGLONG requiredDisk;
    unsigned __int64 availableDisk;
    unsigned __int64 freebytestocaller;
    unsigned __int64 totalbytes;
    unsigned __int64 freebytes;
#else
    unsigned long long requiredDisk;
    unsigned long long availableDisk;
    struct statvfs64 buf;
    struct sigaction act;
#endif

    /* Compute the name of the executable */
    if ((execname = SetExecName(*argv)) == NULL)
    {
        char *message = GetLocalizedMessage("no_resolve_exec_path");
        fprintf(stderr, message);
        free(message);
        message = NULL;
        return 1;
    }

    /* Check if another instance is already running */
    ExitIfNotOnlyInstance();

    if ((s = strrchr(execname, FILE_SEPARATOR_CHAR)) != 0)
        progname = s + 1;
    ++argv;
    --argc;

#ifdef WIN32
    SetUnhandledExceptionFilter(HandleException);
#else
    sigfillset(&act.sa_mask);
    act.sa_flags = 0;

    /* Signals to ignore  */
    act.sa_handler = SIG_IGN;
    sigaction(SIGHUP, &act, NULL);
    sigaction(SIGPWR, &act, NULL);
    sigaction(SIGWINCH, &act, NULL);

    /* Signals to run exit handler */
    act.sa_handler = HandleSignalAndExit; 
    sigaction(SIGALRM, &act, NULL);
    sigaction(SIGINT, &act, NULL);
    sigaction(SIGPOLL, &act, NULL);
    sigaction(SIGTERM, &act, NULL);
    sigaction(SIGVTALRM, &act, NULL);

    /* Signals to run exit handler and dump core */
    act.sa_handler = HandleSignalAndAbort; 
    sigaction(SIGABRT, &act, NULL);
    sigaction(SIGBUS, &act, NULL);
    sigaction(SIGFPE, &act, NULL);
    sigaction(SIGILL, &act, NULL);
    sigaction(SIGPIPE, &act, NULL);
    sigaction(SIGQUIT, &act, NULL);
    sigaction(SIGSEGV, &act, NULL);
#endif

    /*
     * Parse command line options and convert any relative paths in the
     * argument list to absolute paths. This function will also remove
     * the program name from argv so that the argument list conforms
     * to the Java argument list format.
     */
    if (!ParseArguments(&argc, &argv, &javahome, &tmpdir, &showconsole, &useAnswerFileOption, &answerFile, &useAlternateRootOption, &alternateRoot, &storeAnswerFile, &answerFileLocation, &logLevelOption, &useLogsLocation, &logsLocation, &ret))
        return ret;

    /* Remove it after console mode support has been turned on */
    showconsole = JNI_FALSE;
    /* Create the temporary directory */
    s = GetLocalizedMessage("checking_disk_space");
    statusf(s);
    s = NULL;
    
    /* if tmpdir explicitly set, override environment variables on relevant platforms */
#ifdef WIN32
    if (tmpdir != NULL)
    {
        char setenvline[1024];
        sprintf(setenvline, "TMP=%s", tmpdir);
        _putenv(setenvline);
    }
#endif
#ifdef LINUX
    if (tmpdir != NULL)
    {
        setenv("TMPDIR", tmpdir, 1);
    }
#endif

    /* if tmpdir has not already been set through command line option get it... */
    
    if (tmpdir == NULL)
    {
        tmpdir = GetTempDir();
    }
    
    if ((tmpdir == NULL) || (workdir = tempnam(tmpdir, progname)) == NULL)
    {
        char *message = GetLocalizedMessage("no_writable_system_tmpdir");
        fprintf(stderr, message, tmpdir);
        free(message);
        message = GetLocalizedMessage("no_writable_system_tmpdir_usage");
        fprintf(stderr, message, progname);
        free(message);
        message = NULL;
        return 1;
    }
  
    /*
     * Create the working directory and protect against the possibility that
     * the working directory may already exist.
     */
#ifdef WIN32
    while (!CreateDirectory(workdir, NULL))
#else
    while (mkdir(workdir, S_IRWXU))
#endif
    {
        free(workdir);
        if ((workdir = tempnam(tmpdir, progname)) == NULL)
        {
            char *message = GetLocalizedMessage("no_writable_system_tmpdir");
            fprintf(stderr, message, tmpdir);
            free(message);
            message = GetLocalizedMessage("no_writable_system_tmpdir_usage");
            fprintf(stderr, message, progname);
            free(message);
            message = NULL;
            return 1;
        }
    }

    /* Check that there enough temporary space */
    requiredDisk = 0;
    if ((zipFile = zipOpen(execname, &zipError)) != NULL)
    {
        jzentry *zipEntry;

        /* Get the uncompressed size of the embedded files */
        i = 0;
        while ((zipEntry = zipGetNextEntry(zipFile, i++)) != NULL)
        {
            requiredDisk += zipEntry->size;
            zipFreeEntry(zipFile, zipEntry);
        }
        zipClose(zipFile);

        /* Reset application home to the working directory */
        free(applicationHome);
        applicationHome = StrDup(workdir);
    }

    /* Add a little extra for the SetupSDK's native executable */
    requiredDisk += 100000;

    ret = 0;
#ifdef WIN32
   if (!GetDiskFreeSpaceEx(workdir, 
		(PULARGE_INTEGER) &freebytestocaller, 
		(PULARGE_INTEGER) &totalbytes,
		(PULARGE_INTEGER) &freebytes))
        ret = 1;
    availableDisk = freebytestocaller;
#else
    if (statvfs64(workdir, &buf))
    {
        if (errno == EOVERFLOW)
        {
            /*
             * If there is an overflow, we can safely assume that there
             * is plenty of free disk space
             */
            availableDisk = requiredDisk + 1;
        }
        else
        {
            ret = 1;
        }
    }
    availableDisk = buf.f_frsize * buf.f_bavail;
#endif
    if (ret)
    {
        char *message = GetLocalizedMessage("no_writable_system_tmpdir");
        fprintf(stderr, message, tmpdir);
        free(message);
        message = GetLocalizedMessage("no_writable_system_tmpdir_usage");
        fprintf(stderr, message, progname);
        free(message);
        message = NULL;
        return 1;
    }
    else if (requiredDisk > availableDisk)
    {
        char *message = GetLocalizedMessage("no_space_system_tmpdir");
        
        /*
         * This is workaround for Windows printf quirk - second lli will
         * be printed with zero value if printed directly 
         */
        char required_buf[32];
        char available_buf[32];
        sprintf(required_buf, "%lli", requiredDisk);
        sprintf(available_buf, "%lli", availableDisk);
        fprintf(stderr, message, tmpdir, required_buf, available_buf);
        free(message);
        message = GetLocalizedMessage("no_writable_system_tmpdir_usage");
        fprintf(stderr, message, progname);
        free(message);
        message = NULL;
        return 1;
    }

    /* Find out where the JRE is that we will be using. */
    s = GetLocalizedMessage("checking_java");
    statusf(s);
    s = NULL;
    ret = 0;
    if (javahome)
    {
        /* Make sure version is OK */
        if ((jrepath = GetJREPath(javahome)) == NULL || !CheckJavaVersion(jrepath))
        {
                char *message = GetLocalizedMessage("no_find_specified_java");
                fprintf(stderr, message, javahome);
                free(message);
                message = NULL;
	            return 1;				
        }
    }
	/* if javahome is not explicitly passed, then check public JREs on the machine. */
    else
    {
	    if ((jrepath = GetPublicJREPath()) == NULL)
        {
			/* If no JDK/JRE is found on the machine, then do not quit yet. 
			Look at the installer bundled to see if there is a bundled JDK. 
			Prior to this the installer bundle itself has to be unzipped 
			in temp dir. */
			ret = 1;
		}
	}

    if ((zipFile = zipOpen(execname, &zipError)) != NULL)
    {
        char *message = GetLocalizedMessage("unzipping_files");
        statusf(message);
        free(message);
        message = NULL;
        if (!UnzipFiles(zipFile, workdir, NULL))
        {
            char *message = GetLocalizedMessage("corrupted_zip_file");
            fprintf(stderr, message, execname);
            free(message);
            message = NULL;
            return 1;
        }
        zipClose(zipFile);
    }

	/* Check to see if there is a bundled jdk.zip in the installer bundle. */
	/* If Java is not found in the environment, then check to see if there is a jdk.zip
	   in package directory. If found one, then unzip it and use it to launch installer.
	   This will only be valid in case of Java EE SDK bundles that includes a jdk in them. */

	if (!javahome && ret == 1) {
			sprintf(jdkZipFile, "%s" JDK_ZIP_PATH, workdir);
			if ((zipFile = zipOpen(jdkZipFile, &zipError)) != NULL)
			{
				char *message = GetLocalizedMessage("unzipping_files_jdk_zip");
				char bundledJavaHome[MAXPATHLEN];
				statusf(message);
				free(message);
				message = NULL;
				if (!UnzipFiles(zipFile, workdir, NULL))
				{
					char *message = GetLocalizedMessage("corrupted_zip_file");
					fprintf(stderr, message, execname);
					free(message);
					message = NULL;
					return 1;
				}
				zipClose(zipFile);
				/* Now check to make sure that the bundled version is okay. */
				/* Make sure version is OK */
				sprintf(bundledJavaHome,"%s\\jdk",workdir);
				if ((jrepath = GetJREPath(bundledJavaHome)) == NULL || !CheckJavaVersion(jrepath))
				{
					char *message = GetLocalizedMessage("no_find_specified_java");
					fprintf(stderr, message, javahome);
					free(message);
					message = NULL;
					ret = 1;
				}
			}
			else 
			{
				char *message = GetLocalizedMessage("no_find_java_usage");
				fprintf(stderr, message, MINIMUM_SUPPORTED_VM_VERSION, SUPPORTED_VM_URL, progname);
				free(message);
				message = NULL;
				return ret;
			}
        }

	/* Could have wrapped this repeated piece of code in a method, but too many arguments to pass, 
	Moreover this is a temp. fix */

	sprintf(oiZipFile, "%s" ENGINE_ZIP_PATH, workdir);
   if ((zipFile = zipOpen(oiZipFile, &zipError)) != NULL)
    {
        char *message = GetLocalizedMessage("unzipping_files_engine_zip");
        statusf(message);
        free(message);
        message = NULL;
        if (!UnzipFiles(zipFile, workdir, NULL))
        {
            char *message = GetLocalizedMessage("corrupted_zip_file");
            fprintf(stderr, message, execname);
            free(message);
            message = NULL;
            return 1;
        }
        zipClose(zipFile);
    }

    sprintf(oiZipFile, "%s" RESOURCES_ZIP_PATH, workdir);
    if ((zipFile = zipOpen(oiZipFile , &zipError)) != NULL)
    {
        char *message = GetLocalizedMessage("unzipping_files_resources_zip");
        statusf(message);
        free(message);
        message = NULL;
        if (!UnzipFiles(zipFile, workdir, NULL))
        {
            char *message = GetLocalizedMessage("corrupted_zip_file");
            fprintf(stderr, message, execname);
            free(message);
            message = NULL;
            return 1;
        }
        zipClose(zipFile);
    }
            
     sprintf(oiZipFile, "%s" METADATA_ZIP_PATH, workdir);
    if ((zipFile = zipOpen(oiZipFile , &zipError)) != NULL)
    {
        char *message = GetLocalizedMessage("unzipping_files_metadata_zip");
        statusf(message);
        free(message);
        message = NULL;
        if (!UnzipFiles(zipFile, workdir, NULL))
        {
            char *message = GetLocalizedMessage("corrupted_zip_file");
            fprintf(stderr, message, execname);
            free(message);
            message = NULL;
            return 1;
        }
        zipClose(zipFile);
    }

    /* Set CLASSPATH */
    if ((apphome = GetApplicationHome()) == NULL)
    {
        char *message = GetLocalizedMessage("no_find_exec_dir");
        fprintf(stderr, message);
        free(message);
        message = NULL;
        return 1;
    }
    size = sizeof(CLASSPATH_OPTION) + strlen(apphome) + 1;
    j = GetJavaClasspathElements(&appclasspath);
    for (i = 0; i < j; i++)
        size += strlen(apphome) + strlen(appclasspath[i]) + 1;
    if (IsJDK(jrepath))
    {
        int jrepathLen = strlen(jrepath);
        for (i = 0; i < NUM_JDK_CLASSPATH; i++)
            size += jrepathLen + strlen(jdkClassPath[i]) + 1;
        s = (char *)MemAlloc(size + 1);
        strcpy(s, CLASSPATH_OPTION);
        for (i = 0; i < NUM_JDK_CLASSPATH; i++)
        {
            strcat(s, jrepath);
            strcat(s, jdkClassPath[i]);
            strcat(s, PATH_SEPARATOR);
        }
    }
    else
    {
        s = (char *)MemAlloc(size + 1);
        strcpy(s, CLASSPATH_OPTION);
    }
    AddOption(StrDup(CLASSPATH_OPTION));
    strcpy(s, apphome);
    for (i = 0; i < j; i++)
    {
        strcat(s, PATH_SEPARATOR);
        strcat(s, apphome);
        strcat(s, appclasspath[i]);
    }
    AddOption(s);

    s = NULL;
    /* Set the "java.io.tmpdir" property */
    s = (char *)MemAlloc(strlen(JAVA_IO_TMPDIR_OPTION) + strlen(workdir) + 1);
    sprintf(s, JAVA_IO_TMPDIR_OPTION "%s", workdir);
    AddOption(s);
    s = NULL;
    
    /* pass the temp dir */
    s = (char *)MemAlloc(strlen(TMPDIR_OPTION) + strlen(tmpdir) + 1);
    sprintf(s, TMPDIR_OPTION "%s", tmpdir);
    AddOption(s);
    s = NULL;
    

    /* Set the "java.endorsed.dir" property to use the default XML parser */
    /* TODO FIX AddOption(StrDup(ENDORSED_DIRS_OPTION)); */

    /* Set the "-Xmx" argument high enough so load the main class */
    AddOption(StrDup(MAX_HEAP_OPTION)); 

    /* Set debug option */
    if (isDebug)
    {
        AddOption(StrDup(DEBUG_OPTION));
    }
    
    /* Set Creator installation options */
    if (noPortVerification)
    {
        AddOption(StrDup(PORT_VERIFICATION_OPTION));
    }
    
    if (noDomainCreation)
    {
        AddOption(StrDup(DOMAIN_CREATION_OPTION));
    }

    if (visiblePassword)
    {
        AddOption(StrDup(PASSWORD_VISIBLE_OPTION));
    }


    /* Set Open Installer Specific parameters */

    /* Setup default options first */
    AddOption(StrDup(CONFIGURATOR_OPTION));
    /* Setup INSTALL_ENGINE Option */
    AddOption(StrDup(INSTALL_ENGINE_OPTION));

    /* Setup INSTALL_RESOURCE */
   s = (char *)MemAlloc(strlen(INSTALL_RESOURCE_OPTION) + strlen(workdir) + 19 + 1);
    sprintf(s, INSTALL_RESOURCE_OPTION "%s" FILE_SEPARATOR "install" FILE_SEPARATOR "metadata",workdir);
    AddOption(s);
    s = NULL;


    /* Setup DEFAULT_RESOURCE */
   s = (char *)MemAlloc(strlen(DEFAULT_RESOURCE_OPTION) + strlen(workdir) + 250 + 1);
    sprintf(s, DEFAULT_RESOURCE_OPTION "%s" FILE_SEPARATOR "install" FILE_SEPARATOR "lib" FILE_SEPARATOR "resources/",workdir);
    AddOption(s); 
    s = NULL;
  

    /* Setup SIMS Native File */
   s = (char *)MemAlloc(strlen(SIMS_NATIVE_OPTION) + strlen(workdir) + 18 + 1);
    sprintf(s, SIMS_NATIVE_OPTION "%s" FILE_SEPARATOR "install" FILE_SEPARATOR "lib", workdir);
    AddOption(s);
    s = NULL;
 

    
/* Set the "DEFAULT_PRODUCTID_PROP" property */
    AddProp(StrDup("-p"));
    AddProp(StrDup(DEFAULT_PRODUCTID_PROP));

/* Set the "PKG_FORMAT_PROP" property */
    AddProp(StrDup("-p"));
    AddProp(StrDup(PKG_FORMAT_PROP));


/* Set the "PLATFORM_PLUGIN_PROP" property */
    s = (char *)MemAlloc(strlen(PLATFORM_PLUGIN_PROP) + strlen(workdir) + strlen(PLATFORM_PLUGIN_PATH) + 10);
#ifdef WIN32
    sprintf(s, PLATFORM_PLUGIN_PROP "%s" PLATFORM_PLUGIN_PATH, workdir);
#else
    sprintf(s, PLATFORM_PLUGIN_PROP "%s" PLATFORM_PLUGIN_PATH, workdir);
#endif
    AddProp(StrDup("-p"));
    AddProp(StrDup(s));
    s = NULL;

/* Set the "PROVIDER_PATH_PROP" property */
    s = (char *)MemAlloc(strlen(PROVIDER_PATH_PROP) + strlen(workdir) + strlen(PROVIDER_PATH) + 10);
#ifdef WIN32
    sprintf(s, PROVIDER_PATH_PROP "%s" PROVIDER_PATH, workdir);
#else
    sprintf(s, PROVIDER_PATH_PROP "file://%s" PROVIDER_PATH, workdir);
#endif
    AddProp(StrDup("-p"));
    AddProp(StrDup(s));
    s = NULL;


/* Set the "DRY_RUN_FILE_PROP" property */
    if (storeAnswerFile) 
    {
    	s = (char *)MemAlloc(strlen(DRY_RUN_FILE_PROP) + strlen(answerFileLocation) + 1);
    	sprintf(s, DRY_RUN_FILE_PROP "%s", answerFileLocation);
    AddProp(StrDup("-p"));
    	AddProp(StrDup(s));
    	s = NULL;
    }

/* CONDITIONAL */
/* Set the "ANSWER_FILE_PROP" property */

    if (useAnswerFileOption) 
    {
    	s = (char *)MemAlloc(strlen(ANSWER_FILE_PROP) + strlen(answerFile) + strlen(workdir) + 100);
#ifdef WIN32
	sprintf(s, ANSWER_FILE_PROP ",file:///%s" FILE_SEPARATOR "install.windows.properties,%s" ,workdir, answerFile);
#else
	sprintf(s, ANSWER_FILE_PROP ",file://%s" FILE_SEPARATOR "install.properties,%s" ,workdir, answerFile);
#endif
    }
    else
    {
	/* assign default */
  	s = (char *)MemAlloc(strlen(ANSWER_FILE_PROP) + strlen(workdir) + 100);
#ifdef WIN32
	sprintf(s, ANSWER_FILE_PROP ",file:///%s" FILE_SEPARATOR "install.windows.properties" , workdir);
#else
	sprintf(s, ANSWER_FILE_PROP ",file://%s" FILE_SEPARATOR "install.properties" , workdir);
#endif
    }
    AddProp(StrDup("-p"));
    AddProp(StrDup(s));
    s = NULL;

/* CONDITIONAL */
/* Set the "LOGS-LOCATION" property */
    if (useLogsLocation) 
    {
    	s = (char *)MemAlloc(strlen(LOGS_LOCATION_PROP) + strlen(logsLocation) + 5);
	sprintf(s, LOGS_LOCATION_PROP "%s", logsLocation);
    }
    else
    {
  	s = (char *)MemAlloc(strlen(LOGS_LOCATION_PROP) + strlen(tmpdir) + 8);
	sprintf(s, LOGS_LOCATION_PROP "%s", tmpdir);
    }

    AddProp(StrDup("-p"));
    AddProp(StrDup(s));
    s = NULL;

/* CONDITIONAL */
/* Set the "ALTERNATE_ROOT_PROP" property */

    if (useAlternateRootOption)
    {
    	s = (char *)MemAlloc(strlen(ALTERNATE_ROOT_PROP) + strlen(alternateRoot) + 15);
    	sprintf(s, ALTERNATE_ROOT_PROP "%s", alternateRoot);
        AddProp(StrDup("-p"));
        AddProp(StrDup(s));
        s = NULL;
    }




/* Set savestate file */

/* Set the "INSTALLABLE_UNIT_PROP" property */
    s = (char *)MemAlloc(strlen(INSTALLABLE_UNIT_PROP) + strlen(workdir) + 35);
    sprintf(s, INSTALLABLE_UNIT_PROP "file:///%s" FILE_SEPARATOR "Product", workdir);
    AddProp(StrDup("-p"));
    AddProp(StrDup(s));
    s = NULL;

/* Set the "MEDIA_LOCATION_PROP" property */
    s = (char *)MemAlloc(strlen(MEDIA_LOCATION_PROP) + strlen(workdir) + strlen(MEDIA_PATH) + 35);
#ifdef WIN32
    sprintf(s, MEDIA_LOCATION_PROP "file:///%s" MEDIA_PATH , workdir);
#else
    sprintf(s, MEDIA_LOCATION_PROP "file://%s" MEDIA_PATH , workdir);
#endif
    AddProp(StrDup("-p"));
    AddProp(StrDup(s));
    s = NULL;


    /*
     * Change the working directory to the directory where the executable
     * is located since the SetupSDK classes expect that. Conversion of
     * any relative paths in the argument list should have already been
     * converted to absolute paths before this step.
     */
    ret = 1;
#ifdef WIN32
    if (SetCurrentDirectory(apphome))
        ret = 0;
#else
    if (!chdir(apphome))
        ret = 0;
#endif
    if (ret)
    {
        char *message = GetLocalizedMessage("no_set_specified_working_dir");
        fprintf(stderr, message, apphome);
        free(message);
        message = NULL;
        return ret;
    }

    /* Create a console if necessary */
    if (showconsole)
        SetPrintToConsole(JNI_TRUE);

    /* Invoke Java */
    s = GetLocalizedMessage("executing_java");
    statusf(s);
    s = NULL;
    inJava = JNI_TRUE;
    ret = ExecuteJava(jrepath, numOptions, options, numProps, props);
    doCleanup = JNI_TRUE;
    inJava = JNI_FALSE;
    doCleanup = JNI_TRUE;
    PrepareToExit();
    return ret;
}

jboolean
IsAbsolutePath(const char *path)
{
#ifdef WIN32
    int len = strlen(path);
    if (len >= 1 && path[0] == FILE_SEPARATOR_CHAR)
        return JNI_TRUE;
    else if (len >= 2 && strchr(path, ':'))
        return JNI_TRUE;
#else
    if (path[0] == FILE_SEPARATOR_CHAR)
        return JNI_TRUE;
#endif

    return JNI_FALSE;
}

#ifndef WIN32
/*
 * Find the absolute path for the executable.
 */
static char *
FindExecName(const char *program)
{
    const char *envvar;
    char *execpath = NULL;

    if (IsAbsolutePath(program))
    {
        execpath = Resolve("", program);
        if (execpath && !IsExecutable(execpath))
        {
            free(execpath);
            execpath = NULL;
        }
        return execpath;
    }
    else if (strchr(program, FILE_SEPARATOR_CHAR) != 0)
    {
        char *cwd = NULL;
        int size = MAXPATHLEN;
        while ((cwd = getcwd(NULL, size)) == NULL)
            size += MAXPATHLEN;
        execpath = Resolve(cwd, program);
        free(cwd);
        cwd = NULL;
        if (execpath && !IsExecutable(execpath))
        {
            free(execpath);
            execpath = NULL;
        }
        return execpath;
    }

    /* Look in PATH environment variable */
    envvar = getenv("PATH");
    if (envvar)
    {
        char *path = NULL;
        char *tokstr;
        char *currentpath;
        jboolean jdkfound = JNI_FALSE;

        path = (char *)MemAlloc(strlen(envvar) + 1);
        strcpy(path, envvar);
        tokstr = path;
        while (!execpath && tokstr)
        {
            /*
             * Look for the Java executable and, if found, find its
             * installation directory
             */
            currentpath = tokstr;
            if ((tokstr = strchr(currentpath, PATH_SEPARATOR_CHAR)) != NULL)
                *tokstr++ = '\0';
            if (strlen(currentpath) == 0)
                currentpath = ".";
            execpath = Resolve(currentpath, program);
            if (execpath && !IsExecutable(execpath))
            {
                free(execpath);
                execpath = NULL;
            }
        }
    }
    return execpath;
}
#endif /* #ifndef WIN32 */

static char *execname = NULL;

/*
 * Compute the name of the executable
 */
static const char *
SetExecName(const char *program)
{
    if (!execname)
    {
#ifdef WIN32
        int size = MAXPATHLEN;
        char *path = NULL;
        int ret;

        path = (char *)MemAlloc(size);
        while ((ret = GetModuleFileName(0, path, size)) != 0 && ret >= size)
        {
            size += MAXPATHLEN;
            free(path);
            path = (char *)MemAlloc(size);
        }
        if (ret)
        {
            execname = path;
        }
        else
        {
            free(path);
            path = NULL;
        }
#else
        execname = FindExecName(program);
#endif
    }
    return execname;
}

/*
 * Return the name of the executable.  Used in java_md.c to find the JRE area.
 */
const char *
GetExecName()
{
    return execname;
}

/*
 * If app is "/foo/x" then return "/foo".
 */
const char *
GetApplicationHome()
{
    if (!applicationHome)
    {
        const char *execname = GetExecName();
        if (execname)
        {
            applicationHome = (char *)MemAlloc(strlen(execname) + 1);
            strcpy(applicationHome, execname);
            *(strrchr(applicationHome, FILE_SEPARATOR_CHAR)) = '\0';
        }
    }
    return applicationHome;
}

/*
 * Adds a new VM option with the given given name and value.
 */
static void
AddOption(char *str)
{
    /*
     * Expand options array if needed to accomodate at least one more
     * VM option.
     */
    if (numOptions >= maxOptions)
    {
        if (options == NULL)
        {
            maxOptions = 4;
            options = MemAlloc(maxOptions * sizeof(JavaVMOption));
        }
        else
        {
            JavaVMOption *tmp;
            maxOptions *= 2;
            tmp = MemAlloc(maxOptions * sizeof(JavaVMOption));
            memcpy(tmp, options, numOptions * sizeof(JavaVMOption));
            free(options);
            options = tmp;
        }
    }
    options[numOptions].optionString = str;
    options[numOptions++].extraInfo = NULL;
}



/*
 * Adds a new proerty with the given given name and value.
 * These properties are to be passed to OpenInstaller's EngineBootstrap Java Class.
 */

static void
AddProp(char *str)
{
    /*
     * Expand Props array if needed to accomodate at least one more
     * VM Props.
     */
    if (numProps >= maxProps)
    {
        if (props == NULL)
        {
            maxProps= 50;
            props = MemAlloc(maxProps * sizeof(JavaPropsOption));
        }
        else
        {
            JavaPropsOption*tmp;
            maxProps *= 2;
            tmp = MemAlloc(maxProps * sizeof(JavaPropsOption));
            memcpy(tmp, props, numProps * sizeof(JavaPropsOption));
            free(props);
            props = tmp;
        }
    }
    props[numProps++].propsString = str;

}
/*
 * Parses command line arguments.
 */
static jboolean
ParseArguments(int *pargc, char ***pargv, char **pJavaHome, char **pTempDir, jboolean *pShowConsole, jboolean *pUseAnswerFileOption, char **pAnswerFile, jboolean *pUseAlternateRootOption, 
char **pAlternateRoot, jboolean *pStoreAnswerFile, char **pAnswerFileLocation, jboolean *pLogLevelOption, jboolean *pUseLogsLocation, char **pLogsLocation, int *pret)
{
    int argc = *pargc;
    char **argv = *pargv;
    char *arg;
    jboolean consoleOption = JNI_TRUE;

    int appArgc;
    char **appArgv = NULL;
    char *cwd = NULL;
    unsigned int size = MAXPATHLEN;

    *pret = 1;
    appArgc = 0;
    *pJavaHome = NULL;
    *pShowConsole = JNI_FALSE;
    while ((arg = *argv) != 0)
    {
        argv++; --argc;
        if (strcmp(arg, "-help") == 0 || strcmp(arg, "-h") == 0)
        {
            *pret = 0;
            PrintUsage();
            goto error;
        }

	/* 
	-Check if -a is passed, if so validate the file and set Answer-Files property.
	if not, then set it to "-p Answer-Files=,file://<InstallDir>/install.properties".
	TODO: Validate the given file name for readability */
	else if (strcmp(arg, "-a") == 0)
        {
            if (argc < 1)
            {
                char *message = GetLocalizedMessage("missing_answerfile_argument");
                fprintf(stderr, message, arg);
                free(message);
                message = NULL;
                PrintUsage();
                goto error;
            }
            *pUseAnswerFileOption = JNI_TRUE;
            
	    /* Convert the path to absolute path, otherwise the file
		gets written to the temp directory that is eventually cleaned up
		immediately after installation. */
            *pAnswerFile = FindAbsolutePath(*argv);
   	    if (access(*pAnswerFile, R_OK) != 0) 
	    {
        	char *message = GetLocalizedMessage("answerfile_not_accessible");
                fprintf(stderr, message, arg);
                free(message);
                message = NULL;
                /* PrintUsage(); */
                goto error;
	    }
            appArgc += 2;
            argv++; --argc;
        }

	/*Check if -l is passed, if so validate the file and set Logs-Location property.
	if not, then set it to "-p Logs-Location=<Directory>".
	TODO: Validate the given directory for readability */
	else if (strcmp(arg, "-l") == 0)
        {
            if (argc < 1)
            {
                char *message = GetLocalizedMessage("missing_log_directory_argument");
                fprintf(stderr, message, arg);
                free(message);
                message = NULL;
                PrintUsage();
                goto error;
            }
            *pUseLogsLocation = JNI_TRUE;
            *pLogsLocation = StrDup(*argv);
    	    if (access(*pLogsLocation, W_OK) != 0) 
	    {
        	char *message = GetLocalizedMessage("log_directory_not_accessible");
                fprintf(stderr, message, arg);
                free(message);
                message = NULL;
                /* PrintUsage(); */
                goto error;
	    }
            appArgc += 2;
            argv++; --argc;
        }
	

	/*-Check if -n is passed, if so validate the file and set Dry-Run-File property.
	if not, then set it to "-p Dry-Run-File=<Non Existing filename>".
	TODO: Validate the given directory for readability */
	else if (strcmp(arg, "-n") == 0)
        {
            if (argc < 1)
            {
                char *message = GetLocalizedMessage("missing_state_file_argument");
                fprintf(stderr, message, arg);
                free(message);
                message = NULL;
                /* PrintUsage(); */
                goto error;
            }
	
            *pStoreAnswerFile = JNI_TRUE;
	    /* Convert the path to absolute path, otherwise the file
		gets written to the temp directory that is eventually cleaned up
		immediately after installation. */
           *pAnswerFileLocation = FindAbsolutePath(*argv);
  	    /* If the file already exists, then bail out */
	    if (access(*pAnswerFileLocation, F_OK) == 0) 
	    {
        	char *message = GetLocalizedMessage("answer_file_already_exists");
                fprintf(stderr, message, arg);
                free(message);
                message = NULL;
                /* PrintUsage(); */
                goto error;
	    }
        
		
            appArgc += 2;
            argv++; --argc;
	    
        }	

	/* Check if -s is passed for silent */
        else if (strcmp(arg, "-s") == 0)
        {
	    AddProp(StrDup("-p"));
            AddProp(StrDup(SILENT_MODE_PROP));
            appArgc++;
        }
	
	/* Check if -q is passed for setting log level to warning */
        else if (strcmp(arg, "-q") == 0)
        {
	    AddProp(StrDup("-p"));
            AddProp(StrDup(WARNING_LOG_LEVEL_PROP));
            *pLogLevelOption = JNI_TRUE;
            appArgc++;
        }

	/* Check if -v is passed for setting log level to verbose/finest */
        else if (strcmp(arg, "-v") == 0)
        {
	    AddProp(StrDup("-p"));
            AddProp(StrDup(FINEST_LOG_LEVEL_PROP));
            *pLogLevelOption = JNI_TRUE;
            appArgc++;
        }

 	/* Check if -j is passed for setting custom javahome */
         else if (strcmp(arg, "-j") == 0)
        {
            if (argc < 1)
            {
                char *message = GetLocalizedMessage("missing_jvm_dir");
                fprintf(stderr, message, arg);
                free(message);
                message = NULL;
                PrintUsage();
                goto error;
            }
            *pJavaHome = StrDup(*argv);
            argv++; --argc;
        }
       
        else
        {
            char *message = GetLocalizedMessage("no_valid_arg");
            fprintf(stderr, message, arg);
            free(message);
            message = NULL;
            PrintUsage();
            goto error;
        }
    }
/* CONDITIONAL */
/* Set the Default Log Level property */
    if (*pLogLevelOption == JNI_FALSE) 
    {
	AddProp(StrDup("-p"));
	AddProp(StrDup(DEFAULT_LOG_LEVEL_PROP));
    }

    *pJavaHome = FindAbsolutePath(*pJavaHome);


#ifdef WIN32
    /* If running in GUI mode, check if there are enough colors */
    if (!consoleOption)
    {
        int colordepth = 0;
        HDC dc = GetDC(NULL);
        if (dc)
        {
            colordepth = GetDeviceCaps(dc, PLANES) * GetDeviceCaps(dc, BITSPIXEL);
            ReleaseDC(NULL, dc);
            dc = NULL;
        }
        /* At least 256 colors are needed */
        if (colordepth < 8)
        {
            char *message = GetLocalizedMessage("need_more_colors");
            if (MessageBox(statusdialog, message, GetTitle(), MB_YESNO | MB_ICONEXCLAMATION | MB_APPLMODAL) == IDYES)
            {
                consoleOption = JNI_TRUE;
                appArgc++;
            }
            free(message);
            message = NULL;
            if (!consoleOption)
            {
                PrintUsage();
                goto error;
            }
        }
    }
#else
#ifndef LINUX
    /* If running in GUI mode, check that GUI is available */
    /* This whole section is being skipped for Linux - Motif is obsoleted there */
    if (!consoleOption)
    {
        const char *envvar = getenv("DISPLAY");
        char *message = GetLocalizedMessage("connecting_x11");
        statusf(message, envvar ? envvar : ":0.0");
        free(message);
        message = NULL;
	SetPrintToConsole(JNI_FALSE);
	if (!LoadX11(JNI_TRUE))
        {
            message = GetLocalizedMessage("no_connect_x11");
            fprintf(stderr, message, envvar ? envvar : ":0.0");
            free(message);
            message = GetLocalizedMessage("no_connect_x11_usage");
            fprintf(stderr, message, progname);
            free(message);
            message = NULL;
            goto error;
        }
    }
#endif
#ifndef ALLOW_AWT_HEADLESS
    /*
     * If running in console or silent mode, check that X11 libraries can be
     * loaded since the AWT libraries are linked to them
     */
    if ((consoleOption) && !LoadX11(JNI_FALSE))
    {
        char *message = GetLocalizedMessage("no_motif_libs");
        fprintf(stderr, message);
        free(message);
        message = GetLocalizedMessage("no_motif_libs_usage");
        fprintf(stderr, message);
        free(message);
        message = NULL;
        goto error;
    }
#endif
#endif

    if (appArgc >= 0)
    {
        int i = 0;
        appArgv = (char **)MemAlloc(sizeof(char**) * (appArgc + 1));
        if (consoleOption)
        {
            /* Defer allocation of and printing to terminal */
            *pShowConsole = JNI_TRUE;
            appArgv[i++] = "-nodisplay";
        }
        appArgv[i++] = NULL;
        *pargc = appArgc;
        *pargv = appArgv;
    }

    return JNI_TRUE;

error:
/*
    if (appArgv)
    {
        free(appArgv);
        appArgv = NULL;
    }
    if (cwd)
    {
        free(cwd);
        cwd = NULL;
    }
    if (*pJavaHome)
    {
        free(*pJavaHome);
        *pJavaHome = NULL;
    }
    if (*pTempDir)
    {
        free(*pTempDir);
        *pTempDir = NULL;
    }
    
   if (*pAnswerFile)
    {
        free(*pAnswerFile);
        *pAnswerFile= NULL;
    }

   if (*pAlternateRoot)
    {
        free(pAlternateRoot);
        *pAlternateRoot= NULL;
    }

   if (*pLogsLocation)
    {
        free(pLogsLocation);
        *pLogsLocation= NULL;
    }

   if (*pAnswerFileLocation)
    {
        free(pAnswerFileLocation);
        *pAnswerFileLocation= NULL;
    }

    *pargv = NULL;
*/
    return JNI_FALSE;
}

/*
 * Returns a pointer to a block of at least 'size' bytes of memory.
 * Prints error message and exits if the memory could not be allocated.
 */
void *
MemAlloc(size_t size)
{
    void *p = malloc(size);
    if (p == NULL)
    {
        perror("Out of memory");
        exit(1);
    }
    return p;
}

/*
 * Returns a pointer to a copy of the specified string.
 * Prints error message and exits if the memory could not be allocated.
 */
char *
StrDup(const char *str)
{
    char *newstr = NULL;
    newstr = (char *)MemAlloc(strlen(str) + 1);
    strcpy(newstr, str);
    return newstr;
}

static char *supported_vm_versions[] = SUPPORTED_VM_VERSIONS;
#define NUM_SUPPORTED_VM_VERSIONS (sizeof(supported_vm_versions) / sizeof(char *))
#define JAVA_VERSION_PROPERTY "java.version"

/*
 * Checks the version information from the java.version property.
 */
jboolean
CheckJavaVersion(const char *jrepath)
{
    jboolean ret = JNI_FALSE;
    char *version = NULL;

    if ((version = CheckJREVersion(jrepath)) != NULL)
    {
        ret = JNI_TRUE;
        free(version);
        version = NULL;
    }

    return ret;
}

/*
 * Checks the version information against the specified string.
 */
jboolean
CheckVersion(const char *version)
{
    int i;

    /* Find a matching supported version */
    for (i = 0; i < NUM_SUPPORTED_VM_VERSIONS; i++)
    {
        if (!strncmp(supported_vm_versions[i], version, strlen(supported_vm_versions[i])))
            return JNI_TRUE;
    }

    return JNI_FALSE;
}

/*
 * Checks that the Java class file exists and is readable
 */
jboolean
CheckJavaClassFile()
{
    jboolean ret = JNI_FALSE;
    char *classfile = NULL;
    const char *apphome = GetApplicationHome();
    const char *class = GetJavaClassName();

    if ((classfile = (char *)malloc(strlen(apphome) + strlen(class) + 8)) != NULL)
    {
        strcpy(classfile, apphome);
        strcat(classfile, FILE_SEPARATOR);
        strcat(classfile, class);
        strcat(classfile, ".class");
        /* If the class file is gone, then the uninstall succeeded */
	/*
        if (access(classfile, R_OK) == 0)
            ret = JNI_TRUE;
	*/
        free(classfile);
        classfile = NULL;
    }

    return ret;
}

/*
 * Custom version of strchr() that properly ignores the specified character
 * if it is part of a multi-byte character.
 */
char
*_strchr(const char *s, int c)
{
    const char *t;

    if (!s)
        return NULL;

    for (t = s; *t; t++)
    {
        if (*t != c || !IsMultiByteChar(s, t - s))
            continue;
        return (char *)t;
    }

    return NULL;
}

/*
 * Custom version of strrchr() that properly ignores the specified character
 * if it is part of a multi-byte character.
 */
char
*_strrchr(const char *s, int c)
{
    const char *t;

    if (!s)
        return NULL;

    for (t = s + strlen(s); t >= s; t--)
    {
        if (*t != c || !IsMultiByteChar(s, t - s))
            continue;
        return (char *)t;
    }

    return NULL;
}

/*
 * Prints default usage message.
 */
static void
PrintUsage()
{
    char *message;
    /*
    char *classname = GetJavaClassName();
    if (strcmp(classname,"appserv_uninstall") ==0)
	message = GetLocalizedMessage("uninstallusage");
    else
	message = GetLocalizedMessage("installusage");
    */
    message = GetLocalizedMessage("usage_text");
    fprintf(stderr, message);
    free(message);
    message = NULL;
}

static void
PrepareToExit()
{
/*
    char *message = GetLocalizedMessage("performing_cleanup");
    statusf(message);
    free(message);
    message = NULL;
*/
#ifdef WIN32
    SetCurrentDirectory("..");
#endif
    if (doCleanup)
        Cleanup();
    DeleteWorkingDirectory(); 
}

#ifdef WIN32

long WINAPI HandleException(LPEXCEPTION_POINTERS exc)
{
    if (exc->ExceptionRecord->ExceptionCode != EXCEPTION_BREAKPOINT)
        PrepareToExit();
    return EXCEPTION_CONTINUE_SEARCH;
}

#else

static void
HandleSignalAndExit(int sig)
{
    PrepareToExit();
    _exit(255);
}

static void
HandleSignalAndAbort(int sig)
{
    struct sigaction act;
    act.sa_handler = SIG_DFL;
    sigemptyset(&act.sa_mask);
    PrepareToExit();
    sigaction(SIGABRT, &act, NULL);
    abort();
}

#endif

static void
DeleteWorkingDirectory()
{
    if (workdir)
        DeleteFilesAndDirectories(workdir, NULL);
    free(workdir);
    workdir = NULL;
}

/*
 * Find and return the absolute path of the given file.
 */
char *
FindAbsolutePath(char *filePath) {
char *absolutePath = NULL;
char *currentDirectory = NULL;
unsigned int size = MAXPATHLEN;
#ifdef WIN32
	currentDirectory = (char *)MemAlloc(size);
	while (GetCurrentDirectory(size, currentDirectory) >= size)
    	{
	       size += MAXPATHLEN;
	       free(currentDirectory);
	       currentDirectory = (char *)MemAlloc(size);
	}
#else
    while ((currentDirectory = getcwd(NULL, size)) == NULL)
        size += MAXPATHLEN;
#endif
    
if (filePath && !IsAbsolutePath(filePath))
{
      absolutePath = (char *)MemAlloc(strlen(currentDirectory ) + strlen(filePath) + 2);
      sprintf(absolutePath, "%s%c%s", currentDirectory, FILE_SEPARATOR_CHAR, filePath);
}
else
{
      absolutePath = filePath;
}

free(currentDirectory);
currentDirectory = NULL;
return absolutePath;
}

/*
 * Unzip the contents of a zip file into the specified directory.
 */
jboolean
UnzipFiles(jzfile *zipFile, const char *outputDir, const char *entrySubdir)
{
    jzentry *zipEntry;
    char *outpath = NULL;
    char *s;
    int i;
    int j;
    int len = MAXPATHLEN;
    int outdirlen = strlen(outputDir);
    struct stat sb;

    if (!zipFile || !outputDir)
        return JNI_FALSE;

    outpath = (char *)MemAlloc(MAXPATHLEN);
    i = 0;
    while ((zipEntry = zipGetNextEntry(zipFile, i++)) != NULL)
    {
        /* Skip any entries that don't match the entry subdirectory filter */
        if (entrySubdir && strncmp(entrySubdir, zipEntry->name, strlen(entrySubdir)))
            goto leave_zip_entry;

        if ((j = outdirlen + strlen(zipEntry->name) + 2) > len)
        {
            len = j;
            free(outpath);
            outpath = (char *)MemAlloc(len);
        }
        /*
         * Important note: we expect that the entry name is not an
         * absolute path and we need to parse the entry name using
         * a Unix file separator character instead of the native
         * file separator
         */
        sprintf(outpath, "%s/%s", outputDir, zipEntry->name);
        s = outpath + outdirlen;
        while ((s = strchr(s, '/')) != NULL)
        {
            *s = '\0';
            if (stat(outpath, &sb) == 0 && (sb.st_mode & S_IFDIR) != S_IFDIR)
                unlink(outpath);
#ifdef WIN32
            CreateDirectory(outpath, NULL);
#else
            mkdir(outpath, S_IRWXU);
#endif
            /* Skip directory entries */
            if (strlen(s + 1) == 0)
                goto leave_zip_entry;
            /* Use the native file separator so that it is a valid path */
            *s++ = FILE_SEPARATOR_CHAR;
        }
        if (!zipReadEntry(zipFile, zipEntry, outpath))
        {
            zipFreeEntry(zipFile, zipEntry);
            free(outpath);
            outpath = NULL;
            return JNI_FALSE;
        }
leave_zip_entry:
        zipFreeEntry(zipFile, zipEntry);
    }
    free(outpath);
    outpath = NULL;

    return JNI_TRUE;
}

