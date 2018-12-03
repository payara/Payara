/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;

import com.google.common.net.MediaType;
import java.io.*;
import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.inject.Inject;

import org.glassfish.api.admin.*;
import org.jvnet.hk2.annotations.Optional;

import org.jvnet.hk2.annotations.Service;

import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.util.cluster.SyncRequest;
import com.sun.enterprise.util.cluster.SyncRequest.ModTime;
import com.sun.enterprise.security.auth.realm.file.FileRealm;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.deployment.versioning.VersioningUtils;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.PostConstruct;

/**
 * The core server synchronization logic.  Given a request from
 * the client, it fills the payload with the files the client needs.
 *
 * The list of files in the config directory to synchronize is in
 * META-INF/config-files in this module, or in config/config-files
 * in the domain directory.
 *
 * @author Bill Shannon
 */
@Service
@PerLookup
public final class ServerSynchronizer implements PostConstruct {

    @Inject
    private ServerEnvironment env;

    @Inject
    private Domain domain;

    @Inject @Optional
    private Applications applications;

    private static boolean syncArchive = false;

    private URI domainRootUri;  // URI of the domain's root directory

    private Logger logger;

    private enum SyncLevel { TOP, DIRECTORY, RECURSIVE }

    private static final LocalStringManagerImpl STRINGS = new LocalStringManagerImpl(ServerSynchronizer.class);
    private static final String DOMAIN_XML = "domain.xml";

    @Override
    public void postConstruct() {
        domainRootUri = env.getInstanceRoot().toURI();
    }

    /**
     * Handle a single syncrhonization request for the given server
     * by adding the needed files to the payload.
     */
    public void synchronize(Server server, SyncRequest sr,
                            Payload.Outbound payload,
                            ActionReport report, Logger logger) {
        this.logger = logger;
        try {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "ServerSynchronizer: synchronization request for server {0}, directory {1}", new Object[]{server.getName(), sr.dir});
            }
            // handle the request appropriately based on the directory
            switch (sr.dir) {
                case "config":
                    synchronizeConfig(payload, server, sr);
                    break;
                case "applications":
                    synchronizeApplications(payload, server, sr);
                    break;
                case "lib":
                    synchronizeLib(payload, server, sr);
                    break;
                case "docroot":
                    synchronizeDocroot(payload, server, sr);
                    break;
                case "config-specific":
                    synchronizeConfigSpecificDir(payload, server, sr);
                    break;
                default:
                    report.setActionExitCode(ExitCode.FAILURE);
                    report.setMessage(
                            STRINGS.getLocalString("serversync.unknown.dir",
                                    "Unknown directory: {0}", sr.dir));
                    return;
            }
            report.setActionExitCode(ExitCode.SUCCESS);
        } catch (URISyntaxException ex) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("ServerSynchronizer: Exception processing request");
                logger.fine(ex.toString());
            }
            report.setActionExitCode(ExitCode.FAILURE);
            report.setMessage(
                    STRINGS.getLocalString("serversync.exception.processing",
                        "ServerSynchronizer: Exception processing request"));
            report.setFailureCause(ex);
        }
    }

    /**
     * Synchronize files in the config directory.
     * If the domain.xml file is up to date, don't worry
     * about any of the other files.
     */
    private void synchronizeConfig(Payload.Outbound payload, Server server,
                                    SyncRequest sr) throws URISyntaxException {
        logger.finer("ServerSynchronizer: synchronize config");
        // find the domain.xml entry
        ModTime domainXmlMT = null;
        for (ModTime mt : sr.files) {
            if (mt.name.equals(DOMAIN_XML)) {
                domainXmlMT = mt;
                break;
            }
        }
        if (domainXmlMT == null)        // couldn't find it, fake it
            domainXmlMT = new ModTime(DOMAIN_XML, 0);

        File configDir = env.getConfigDirPath();
        if (!syncFile(domainRootUri, configDir, domainXmlMT, payload)) {
            logger.fine("ServerSynchronizer: domain.xml HAS NOT CHANGED, " +
                        "thus no files will be synchronized");
            return;
        }

        // get the set of all the config files we need to consider
        Set<String> configFileSet = getConfigFileNames();
        configFileSet.remove(DOMAIN_XML);     // already handled it

        // add the list of file realm files
        getRealmFileNames(server, configFileSet);

        for (ModTime mt : sr.files) {
            if (mt.name.equals(DOMAIN_XML))   // did domain.xml above
                continue;
            if (configFileSet.contains(mt.name)) {
                // if client has file, remove it from set
                configFileSet.remove(mt.name);
                syncFile(domainRootUri, configDir, mt, payload);
            } else
                removeFile(domainRootUri, configDir, mt, payload);
        }

        // now do all the remaining files the client doesn't have
        for (String name : configFileSet)
            syncFile(domainRootUri, configDir, new ModTime(name, 0), payload);
    }

    /**
     * Return the names of the config files we need to consider.
     * Names are all relative to the config directory.
     */
    private Set<String> getConfigFileNames() {
        Set<String> files = new LinkedHashSet<String>();
        BufferedReader in = null;
        try {
            File configDir = env.getConfigDirPath();
            File f = new File(configDir, "config-files");
            if (f.exists()) {
                in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            } else {
                InputStream res = getClass().getResourceAsStream("/META-INF/config-files");
                if (res != null) {
                    in = new BufferedReader(new InputStreamReader(res));
                } else {
                    logger.severe("ServerSynchronizer: can't find list of config files to synchronize!");
                }
            }
            String line;
            if (in != null) {
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("#")) {  // ignore comment lines
                        continue;
                    }
                    line = line.trim();
                    if (line.length() == 0) {    // ignore blank lines
                        continue;
                    }
                    files.add(line);
                }
            }
        } catch (IOException ex) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("ServerSynchronizer: IOException in getConfigFileNames");
                logger.fine(ex.toString());
            }
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException cex) {
            }
        }
        return files;
    }

    /**
     * Get the names of any realm files in the config directory
     * and add them to the set of file names.  This will normally
     * find at least the "admin-keyfile" and "keyfile" files.
     */
    private void getRealmFileNames(Server server, Set<String> files) {
        File configDir = env.getConfigDirPath();
        URI configURI = configDir.toURI();
        Config config = domain.getConfigNamed(server.getConfigRef());
        for (String file : FileRealm.getRealmFileNames(config)) {
            File rfile = new File(file);
            if (!rfile.exists())        // skip if file doesn't exist
                continue;
            URI f = configURI.relativize(rfile.toURI());
            if (!f.isAbsolute())        // if file is in config dir, add it
                files.add(f.toString());
        }
    }

    /**
     * Sync an individual file.  Return true if the file changed.
     * The file is named by mt.name, relative to base.  The name
     * used in the response will be relative to root.  In case the
     * file is a directory, tell the payload to include it recursively,
     * and replace the entire contents of the directory in case any
     * files were removed.
     */
    private boolean syncFile(URI root, File base, ModTime mt,
                            Payload.Outbound payload)
                            throws URISyntaxException {
        File f = fileOf(base, mt.name);
        if (!f.exists())
            return false;
        if (mt.time != 0 && f.lastModified() == mt.time)
            return false;     // success, nothing to do
        if (logger.isLoggable(Level.FINEST))
            logger.log(Level.FINEST, "ServerSynchronizer: file {0} out of date, time {1}", new Object[]{mt.name, f.lastModified()});
        try {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "ServerSynchronizer: sending file {0}{1}", new Object[]{f, mt.time == 0 ?
                        " because it doesn't exist on the instance":
                        " because it was out of date"});
            payload.requestFileReplacement(MediaType.OCTET_STREAM.toString(), root.relativize(f.toURI()), "configChange", null, f, true);
        } catch (IOException ioex) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "ServerSynchronizer: IOException attaching file: {0}", f);
                logger.fine(ioex.toString());
            }
        }
        return true;
    }

    /**
     * Send a request to the client to remove the specified file.
     * The file is named by mt.name, relative to base.  The name
     * used in the response will be relative to root.
     */
    private void removeFile(URI root, File base, ModTime mt,
                            Payload.Outbound payload)
                            throws URISyntaxException {
        File f = fileOf(base, mt.name);
        if (logger.isLoggable(Level.FINEST))
            logger.log(Level.FINEST, "ServerSynchronizer: file {0} removed from client", mt.name);
        try {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "ServerSynchronizer: removing file {0} because it does not exist on the DAS", f);
            payload.requestFileRemoval(
                root.relativize(f.toURI()),
                "configChange", null);
        } catch (IOException ioex) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "ServerSynchronizer: IOException removing file: {0}", f);
                logger.fine(ioex.toString());
            }
        }
    }

    /**
     * Synchronize all the applications in the applications directory.
     * We use the mod time of the application directory to decide if
     * the application has changed.  If it has changed, we also send
     * any of the generated content.
     */
    private void synchronizeApplications(Payload.Outbound payload,
                                    Server server, SyncRequest sr)
                                    throws URISyntaxException {
        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "ServerSynchronizer: synchronize application instance {0}", sr.instance);
        Map<String, Application> apps = getApps(server);
        File appsDir = env.getApplicationRepositoryPath();

        for (ModTime mt : sr.files) {
            if (apps.containsKey(mt.name)) {
                syncApp(apps.get(mt.name), appsDir, mt, payload);
                // if client has app, remove it from set
                apps.remove(mt.name);
            } else
                removeApp(apps.get(mt.name), appsDir, mt, payload);
        }

        // now do all the remaining apps the client doesn't have
        for (Map.Entry<String, Application> e : apps.entrySet())
            syncApp(e.getValue(), appsDir, new ModTime(e.getKey(), 0),
                                                        payload);
    }

    /**
     * Get the applications that should be
     * available to the specified server instance.
     */
    private Map<String, Application> getApps(Server server) {

        Map<String, Application> apps = new HashMap<String, Application>();
        if (applications == null)
            return apps;        // no apps

        // all apps are under <server>, even in a cluster
        for (ApplicationRef ref : server.getApplicationRef()) {
            Application app = applications.getApplication(ref.getRef());
            if (app != null) {
                if (logger.isLoggable(Level.FINEST))
                    logger.log(Level.FINEST, "ServerSynchronizer: got app {0}", app.getName());
                if (Boolean.parseBoolean(app.getDirectoryDeployed())) {
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST, "ServerSynchronizer: skipping directory deployed app: {0}", app.getName());
                    }
                } else {
                    apps.put(VersioningUtils.getRepositoryName(app.getName()), app);
                }
            }
        }
        return apps;
    }

    /**
     * Synchronize the application named by mt.name in the
     * base directory.  If the application is out of date,
     * add the application files to the payload, including
     * the generated files.
     */
    private boolean syncApp(Application app, File base, ModTime mt,
                            Payload.Outbound payload)
                            throws URISyntaxException {
        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "ServerSynchronizer: sync app {0}", mt.name);
        try {
            File appDir = fileOf(base, mt.name);
            if (syncArchive) {
                File archive = app.application();
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "ServerSynchronizer: check archive {0}", archive);
                }
                if (mt.time != 0 && archive.lastModified() == mt.time) {
                    return false;     // success, nothing to do
                }

                // attach the archive file
                attachAppArchive(archive, payload);
                /*
                 * Note that we don't need the deployment plan because
                 * we're not going to actually deploy it on the server
                 * instance, we're just going to unzip it.
                 */
            } else {
                if (logger.isLoggable(Level.FINEST))
                    logger.log(Level.FINEST, "ServerSynchronizer: check app dir {0}", appDir);
                if (mt.time != 0 && appDir.lastModified() == mt.time)
                    return false;     // success, nothing to do

                /*
                 * Recursively attach the application directory and
                 * all the generated directories.  The client will
                 * remove the old versions before installing the new ones.
                 */
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "ServerSynchronizer: sending files for application {0}{1}", new Object[]{mt.name, mt.time == 0 ?
                            " because it doesn't exist on the instance":
                            " because it was out of date"});
                attachAppDir(appDir, payload);
            }

            // in either case, we attach the generated artifacts
            File gdir;
            gdir = env.getApplicationCompileJspPath();
            attachAppDir(fileOf(gdir, mt.name), payload);
            gdir = env.getApplicationGeneratedXMLPath();
            attachAppDir(fileOf(gdir, mt.name), payload);
            gdir = env.getApplicationEJBStubPath();
            attachAppDir(fileOf(gdir, mt.name), payload);
            gdir = new File(env.getApplicationStubPath(), "policy");
            attachAppDir(fileOf(gdir, mt.name), payload);
            // and also the altdd dir
            gdir = env.getApplicationAltDDPath();
            attachAppDir(fileOf(gdir, mt.name), payload);

        } catch (IOException ioex) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "ServerSynchronizer: IOException syncing app {0}", mt.name);
                logger.fine(ioex.toString());
            }
        }
        return true;
    }

    /**
     * Synchronize the lib directory.
     */
    private void synchronizeLib(Payload.Outbound payload,
                                    Server server, SyncRequest sr)
                                    throws URISyntaxException {
        List<String> skip = new ArrayList<String>();
        skip.add("databases");
        synchronizeDirectory(payload, server, sr,
                                env.getLibPath(), skip, SyncLevel.RECURSIVE);
    }

    /**
     * Synchronize the docroot directory.
     */
    private void synchronizeDocroot(Payload.Outbound payload,
                                    Server server, SyncRequest sr)
                                    throws URISyntaxException {
        synchronizeDirectory(payload, server, sr,
                                new File(env.getInstanceRoot(), "docroot"), null,
                                SyncLevel.DIRECTORY);
    }

    /**
     * Synchronize a directory.
     */
    private void synchronizeDirectory(Payload.Outbound payload,
            Server server, SyncRequest sr, File dir, List<String> skip,
            SyncLevel level) throws URISyntaxException {
        if (logger.isLoggable(Level.FINEST))
            logger.log(Level.FINEST, "ServerSynchronizer: directory is {0}", dir);
        List<String> fileSet = getFileNames(dir, skip, level);
        synchronizeDirectory(payload, server, sr, dir, fileSet);
    }

    private void synchronizeDirectory(Payload.Outbound payload,
            Server server, SyncRequest sr, File dir, List<String> fileSet)
            throws URISyntaxException {

        for (ModTime mt : sr.files) {
            if (fileSet.contains(mt.name)) {
                // if client has file, remove it from set
                fileSet.remove(mt.name);
                syncFile(domainRootUri, dir, mt, payload);
            } else
                removeFile(domainRootUri, dir, mt, payload);
        }

        // now do all the remaining files the client doesn't have
        for (String name : fileSet)
            syncFile(domainRootUri, dir, new ModTime(name, 0), payload);
    }

    /**
     * Synchronize the config-specific directory.
     * The directory for the instance is in the instance-config-specific
     * config directory, which is in the main config directory.
     * The instance-config-specific config directory is named
     * <config-name>.
     */
    private void synchronizeConfigSpecificDir(Payload.Outbound payload,
            Server server, SyncRequest sr)
            throws URISyntaxException {
        String configDirName = server.getConfigRef();
        File configDir = env.getConfigDirPath();
        File configSpecificDir = new File(configDir, configDirName);
        if (logger.isLoggable(Level.FINEST))
            logger.finest("ServerSynchronizer: " +
                        "config-specific directory is " + configSpecificDir);
        if (!configSpecificDir.exists()) {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "ServerSynchronizer: no config-specific directory to synchronize: {0}", configSpecificDir);
            return;             // nothing to do
        }

        List<String> fileSet = new ArrayList<String>();
        getFileNames(configSpecificDir, configDir, null, fileSet,
                                                        SyncLevel.DIRECTORY);
        synchronizeDirectory(payload, server, sr, configDir, fileSet);
    }

    /**
     * Return a list with the names of all the
     * files in the specified directory.
     */
    private List<String> getFileNames(File dir, List<String> skip,
                                        SyncLevel level) {
        List<String> names = new ArrayList<String>();
        if (dir.exists())
            getFileNames(dir, dir, skip, names, level);
        else {
            if (logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST, "ServerSynchronizer: directory doesn''t exist: {0}", dir);
        }
        return names;
    }

    /**
     * Get the mod times for the entries in dir and add them to the
     * SyncRequest, using names relative to baseDir.  If level is
     * RECURSIVE, check subdirectories and only include times for files,
     * and empty directories.
     */
    private int getFileNames(File dir, File baseDir, List<String> skip,
                                List<String> names, SyncLevel level) {
        if (level == SyncLevel.TOP) {
            String name = baseDir.toURI().relativize(dir.toURI()).getPath();
            // if name is a directory, it will end with "/"
            if (name.endsWith("/"))
                name = name.substring(0, name.length() - 1);
            names.add(name);
            return 1;     // nothing else
        }
        int cnt = 0;
        for (String file : dir.list()) {
            File f = new File(dir, file);
            String name = baseDir.toURI().relativize(f.toURI()).getPath();
            // if name is a directory, it will end with "/"
            if (name.endsWith("/"))
                name = name.substring(0, name.length() - 1);
            if (skip != null && skip.contains(name))
                continue;
            if (f.isDirectory() && level == SyncLevel.RECURSIVE) {
                int subFileCnt = getFileNames(f, baseDir, skip, names, level);
                if (subFileCnt == 0) {
                    names.add(name);
                    cnt++;
                } else {
                    cnt += subFileCnt;
                }
            } else {
                names.add(name);
                cnt++;
            }
        }
        return cnt;
    }

    /**
     * Attach the application archive file to the payload.
     */
    private void attachAppArchive(File file, Payload.Outbound payload)
                                throws IOException {
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "ServerSynchronizer: domainRootUri {0}", domainRootUri);
            logger.log(Level.FINER, "ServerSynchronizer: file.toURI() {0}", file.toURI());
            logger.log(Level.FINER, "ServerSynchronizer: attach file {0}", domainRootUri.relativize(file.toURI()));
        }
        payload.attachFile("application/octet-stream",
            domainRootUri.relativize(file.toURI()),
            "configChange", file, true);
    }

    /**
     * Attach the application directory and all its contents to the payload.
     */
    private void attachAppDir(File dir, Payload.Outbound payload)
                                throws IOException {
        if (logger.isLoggable(Level.FINER))
            logger.log(Level.FINER, "ServerSynchronizer: attach directory {0}", domainRootUri.relativize(dir.toURI()));
        if (!dir.exists()) {
            logger.finer("ServerSynchronizer: nothing to attach");
            return;
        }
        payload.requestFileReplacement("application/octet-stream",
            domainRootUri.relativize(dir.toURI()),
            "configChange", null, dir, true);
    }

    /**
     * Send requests to the client to remove the specified app directory
     * and all the generated directories.
     */
    private void removeApp(Application app, File base, ModTime mt,
                            Payload.Outbound payload)
                            throws URISyntaxException {
        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "ServerSynchronizer: removing files for application {0} because it is no longer deployed to this instance", mt.name);
        try {
            File dir = fileOf(base, mt.name);
            removeDir(dir, payload);
            dir = env.getApplicationCompileJspPath();
            removeDir(fileOf(dir, mt.name), payload);
            dir = env.getApplicationGeneratedXMLPath();
            removeDir(fileOf(dir, mt.name), payload);
            dir = env.getApplicationEJBStubPath();
            removeDir(fileOf(dir, mt.name), payload);
            dir = new File(env.getApplicationStubPath(), "policy");
            removeDir(fileOf(dir, mt.name), payload);
        } catch (IOException ioex) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "ServerSynchronizer: IOException removing app {0}", mt.name);
                logger.fine(ioex.toString());
            }
        }
    }

    /**
     * Request recursive removal of the specified directory.
     */
    private void removeDir(File file, Payload.Outbound payload)
                                throws IOException {
        payload.requestFileRemoval(
            domainRootUri.relativize(file.toURI()),
            "configChange", null, true);    // recursive removal
    }

    /**
     * Return a File representing the URI relative to the base directory.
     */
    private File fileOf(File base, String uri) throws URISyntaxException {
        // have to use string concatenation to combine the relative URI
        // with the base URI because URI.resolve() ignores the last
        // component of the base URI
        return new File(new URI(base.toURI().toString() + "/" + uri));
    }
}
