rem @echo off

if "%1" == "" goto skip_goto
  goto %1
:skip_goto

if not "%GF_HOME" == "" goto gf_home_ok
 echo You must define GF_HOME (e.g., set GF_HOME = D:\Programs\glassfish3)
 goto end
:gf_home_ok

set PLUGIN

if not "%MODULES_DIR%" == "" modules_dir_ok
 set MODULES_DIR=%GF_HOME%\glassfish\modules
:modules_dir_ok

echo Stopping the server
call asadmin undeploy admin-console
call asadmin stop-domain

:clean

echo Clearing the OSGi cache
rm -R %GF_HOME%\glassfish\domains\domain1\osgi-cache
rm -Rf %GF_HOME%\glassfish\domains\domain1\generated

echo Removing any existing demo plugins from %MODULES_DIR%
rm %MODULES_DIR%/plugin-*.jar

echo Installing modules to %MODULES_DIR%
cp plugin-system\target\plugin-system-*.jar %MODULES_DIR%
for /D %%p in (plugins\*)  do cp %%p\target\plugin-*.jar %MODULES_DIR%

echo Starting the server
call asadmin start-domain --debug=true

:deploy

echo Deploying the application
rem set /P DUMMY="Attach debugger if desired, then press Enter to deploy the web app"
call asadmin deploy webapp\target\admin-console

:end
echo Done.
