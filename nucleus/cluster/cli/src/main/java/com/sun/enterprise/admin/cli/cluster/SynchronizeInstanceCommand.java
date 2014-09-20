/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.cli.cluster;

import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import java.io.*;
import java.net.ConnectException;
import java.text.DateFormat;
import java.util.*;
import java.util.logging.*;
import java.util.zip.*;
import javax.xml.bind.*;


import org.jvnet.hk2.annotations.Service;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;

import com.sun.enterprise.util.cluster.SyncRequest;
import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.common.util.admin.AuthTokenManager;
import org.glassfish.hk2.api.PerLookup;

/**
 * Synchronize a local server instance.
 */
@Service(name = "_synchronize-instance")
@PerLookup
public class SynchronizeInstanceCommand extends LocalInstanceCommand {

    @Param(name = "instance_name", primary = true, optional = true)
    private String instanceName0;

    @Param(name = "sync", optional = true, defaultValue = "normal",
        acceptableValues = "none, normal, full")
    protected String sync="normal";

    private RemoteCLICommand syncCmd = null;

    private static enum SyncLevel { TOP, FILES, DIRECTORY, RECURSIVE };

    // the name of the sync state file, relative to the instance directory
    private static final String SYNC_STATE_FILE = ".syncstate";

    @Override
    protected void validate() throws CommandException {
        if (ok(instanceName0))
            instanceName = instanceName0;
        super.validate();
    }

    /**
     */
    @Override
    protected int executeCommand() throws CommandException {
        
        if (synchronizeInstance())
            return SUCCESS;
        else {
            logger.info(Strings.get("Sync.failed",
                                    programOpts.getHost(),
                                    Integer.toString(programOpts.getPort())));
            return ERROR;
        }
    }

    /**
     * Synchronize this server instance.  Return true if server is synchronized. 
     * Return false if synchronization failed, but no files were changed
     * (meaning that it is ok to bring the server up).
     * Throw a CommandException if synchronization failed in such a way that 
     * instance startup should not be attempted.
     */
    protected boolean synchronizeInstance() throws CommandException {

        File dasProperties = getServerDirs().getDasPropertiesFile();
        if (logger.isLoggable(Level.FINER))
            logger.finer("das.properties: " + dasProperties);

        if (!dasProperties.exists()) {
            logger.info(
                Strings.get("Sync.noDASConfigured", dasProperties.toString()));
            return false;
        }
        setDasDefaults(dasProperties);

        /*
         * Create the remote command object that we'll reuse for each request.
         */

        /*
         * Because we reuse the command, we also need to reuse the auth token
         * (if one is present).
         */
        final String origAuthToken = programOpts.getAuthToken();
        if (origAuthToken != null) {
            programOpts.setAuthToken(AuthTokenManager.markTokenForReuse(origAuthToken));
        }

        syncCmd = new RemoteCLICommand("_synchronize-files", programOpts, env);
        syncCmd.setFileOutputDirectory(instanceDir);

        /*
         * The sync state file records the fact that we're in the middle
         * of a synchronization attempt.  When we're done, we remove it.
         * If we crash, it will be left behind, and the next sync attempt
         * will notice it and force a full sync.
         */
        File syncState = new File(instanceDir, SYNC_STATE_FILE);

        boolean doFullSync = false;
        if (sync.equals("normal") && syncState.exists()) {
            String lastSync = DateFormat.getDateTimeInstance().format(
                                new Date(syncState.lastModified()));
            logger.info(Strings.get("Sync.fullRequired", lastSync));
            doFullSync = true;
        }

        /*
         * Create the sync state file to indicate that
         * we've started synchronization.  If the file
         * already exists (e.g., from a previous failed
         * synchronization attempt), that's fine.
         */
        try {
            syncState.createNewFile();
        } catch (IOException ex) {
            logger.warning(
                Strings.get("Sync.cantCreateSyncState", syncState));
        }

        /*
         * If --sync full, we remove all local state related to the instance,
         * then do a sync.  We only remove the local directories that are
         * synchronized from the DAS; any other local directories (logs,
         * instance-private state) are left alone.
         */
        if (sync.equals("full") || doFullSync) {
            if (logger.isLoggable(Level.FINE))
                logger.fine(Strings.get("Instance.fullsync", instanceName));
            removeSubdirectory("config");
            removeSubdirectory("applications");
            removeSubdirectory("generated");
            removeSubdirectory("lib");
            removeSubdirectory("docroot");
        }

        File domainXml =
                    new File(new File(instanceDir, "config"), "domain.xml");
        long dtime = domainXml.exists() ? domainXml.lastModified() : -1;
        File docroot = new File(instanceDir, "docroot");

        CommandException exc = null;
        try {
            /*
             * First, synchronize the config directory.
             */
            SyncRequest sr = getModTimes("config", SyncLevel.FILES);
            synchronizeFiles(sr);

            /*
             * Was domain.xml updated?
             * If not, we're all done.
             */
            if (domainXml.lastModified() == dtime) {
                if (logger.isLoggable(Level.FINE))
                    logger.fine(Strings.get("Sync.alreadySynced"));
                if (!syncState.delete())
                    logger.warning(
                        Strings.get("Sync.cantDeleteSyncState", syncState));
                /*
                 * Note that we earlier marked the token for reuse.  It's OK
                 * to return immediately here with the DAS still willing to
                 * accept the same token again.  The token will expire and be
                 * cleaned up in a little while and it was never exposed in a
                 * way that could be intercepted and used illicitly.
                 */
                return true;
            }

            /*
             * Now synchronize the applications.
             */
            sr = getModTimes("applications", SyncLevel.DIRECTORY);
            synchronizeFiles(sr);

            /*
             * Did we get any archive files?  If so,
             * have to unzip them in the applications
             * directory.
             */
            File appsDir = new File(instanceDir, "applications");
            File archiveDir = new File(appsDir, "__internal");
            for (File adir : FileUtils.listFiles(archiveDir)) {
                File[] af = FileUtils.listFiles(adir);
                if (af.length != 1) {
                    if (logger.isLoggable(Level.FINER))
                        logger.finer("IGNORING " + adir + ", # files " +
                                                                    af.length);
                    continue;
                }
                File archive = af[0];
                File appDir = new File(appsDir, adir.getName());
                if (logger.isLoggable(Level.FINER))
                    logger.finer("UNZIP " + archive + " TO " + appDir);
                try {
                    expand(appDir, archive);
                } catch (Exception ex) { }
            }

            FileUtils.whack(archiveDir);

            /*
             * Next, the libraries.
             * We assume there's usually very few files in the
             * "lib" directory so we check them all individually.
             */
            sr = getModTimes("lib", SyncLevel.RECURSIVE);
            synchronizeFiles(sr);

            /*
             * Next, the docroot.
             * The docroot could be full of files, so we only check
             * one level.
             */
            sr = getModTimes("docroot", SyncLevel.DIRECTORY);
            synchronizeFiles(sr);

            /*
             * Check any subdirectories of the instance config directory.
             * We only expect one - the config-specific directory,
             * but since we don't have an easy way of knowing the
             * name of that directory, we include them all.  The
             * DAS will tell us to remove anything that shouldn't
             * be there.
             */
            sr = new SyncRequest();
            sr.instance = instanceName;
            sr.dir = "config-specific";
            File configDir = new File(instanceDir, "config");
            for (File f : configDir.listFiles()) {
                if (!f.isDirectory())
                    continue;
                getFileModTimes(f, configDir, sr, SyncLevel.DIRECTORY);
            }
            /*
             * Before sending the last sync request revert to using the original
             * auth token, if one is present.  The token would be retired
             * later when it expires anyway, but this is just a little cleaner.
             */
            if (origAuthToken != null) {
                syncCmd.getProgramOptions().setAuthToken(origAuthToken);
            }
            synchronizeFiles(sr);
        } catch (ConnectException cex) {
            if (logger.isLoggable(Level.FINER))
                logger.finer("Couldn't connect to DAS: " + cex);
            /*
             * Don't chain the exception, otherwise asadmin will think it
             * it was a connect failure and will list the closest matching
             * local command.  Not what we want here.
             */
            exc = new CommandException(
                        Strings.get("Sync.connectFailed", cex.getMessage()));
        } catch (CommandException ex) {
            if (logger.isLoggable(Level.FINER))
                logger.finer("Exception during synchronization: " + ex);
            exc = ex;
        }
        
        if (exc != null) {
            /*
             * Some unexpected failure.  If the domain.xml hasn't
             * changed, assume no local state has changed and it's safe
             * to remove the sync state file.  Otherwise, something has
             * changed, and we don't know how much has changed, so leave
             * the sync state file so we'll do a full sync the next time.
             * If nothing has changed, allow the server to come up.
             */
            if (domainXml.exists() && domainXml.lastModified() == dtime &&
                    docroot.isDirectory()) {
                // nothing changed and sync has completed at least once
                if (!syncState.delete())
                    logger.warning(
                        Strings.get("Sync.cantDeleteSyncState", syncState));
                return false;
            }
            throw exc;
        }

        /*
         * Success!  Remove sync state file.
         */
        if (!syncState.delete())
            logger.warning(Strings.get("Sync.cantDeleteSyncState", syncState));
        return true;
    }

    /**
     * Return a SyncRequest with the mod times for all the
     * files in the specified directory.
     */
    private SyncRequest getModTimes(String dir, SyncLevel level) {
        SyncRequest sr = new SyncRequest();
        sr.instance = instanceName;
        sr.dir = dir;
        File fdir = new File(instanceDir, dir);
        if (!fdir.exists())
            return sr;
        getFileModTimes(fdir, fdir, sr, level);
        return sr;
    }

    /**
     * Get the mod times for the entries in dir and add them to the
     * SyncRequest, using names relative to baseDir.  If level is
     * RECURSIVE, check subdirectories and only include times for files,
     * not directories.
     */
    private void getFileModTimes(File dir, File baseDir, SyncRequest sr,
                                    SyncLevel level) {
        if (level == SyncLevel.TOP) {
            long time = dir.lastModified();
            SyncRequest.ModTime mt = new SyncRequest.ModTime(".", time);
            sr.files.add(mt);
            return;
        }
        for (String file : dir.list()) {
            File f = new File(dir, file);
            long time = f.lastModified();
            if (time == 0)
                continue;
            if (f.isDirectory()) {
                if (level == SyncLevel.RECURSIVE) {
                    getFileModTimes(f, baseDir, sr, level);
                    continue;
                } else if (level == SyncLevel.FILES)
                    continue;
            }
            String name = baseDir.toURI().relativize(f.toURI()).getPath();
            // if name is a directory, it will end with "/"
            if (name.endsWith("/"))
                name = name.substring(0, name.length() - 1);
            SyncRequest.ModTime mt = new SyncRequest.ModTime(name, time);
            sr.files.add(mt);
            if (logger.isLoggable(Level.FINER))
                logger.finer(f + ": mod time " + mt.time);
        }
    }

    /**
     * Ask the server to synchronize the files in the SyncRequest.
     */
    private void synchronizeFiles(SyncRequest sr)
                                throws CommandException, ConnectException {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("mt.", ".xml");
            tempFile.deleteOnExit();

            JAXBContext context = JAXBContext.newInstance(SyncRequest.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty("jaxb.formatted.output", Boolean.TRUE);
            marshaller.marshal(sr, tempFile);
            if (logger.isLoggable(Level.FINER))
                marshaller.marshal(sr, System.out);

            File syncdir = new File(instanceDir, sr.dir);
            if (logger.isLoggable(Level.FINER))
                logger.finer("Sync directory: " + syncdir);
            // _synchronize-files takes a single operand of type File
            // Note: we throw the output away to avoid printing a blank line
            syncCmd.executeAndReturnOutput("_synchronize-files",
                tempFile.getPath());

            // the returned files are automatically saved by the command
        } catch (IOException ex) {
            if (logger.isLoggable(Level.FINER))
                logger.finer("Got exception: " + ex);
            throw new CommandException(
                Strings.get("Sync.dirFailed", sr.dir, ex.toString()), ex);
        } catch (JAXBException jex) {
            if (logger.isLoggable(Level.FINER))
                logger.finer("Got exception: " + jex);
            throw new CommandException(
                Strings.get("Sync.dirFailed", sr.dir, jex.toString()), jex);
        } catch (CommandException cex) {
            Throwable cause = cex.getCause();
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Got exception: " + cex);
                logger.finer("  cause: " + cause);
            }
            if (cause instanceof ConnectException)
                throw (ConnectException)cause;
            throw new CommandException(
                Strings.get("Sync.dirFailed", sr.dir, cex.getMessage()), cex);
        } finally {
            // remove tempFile
            if (tempFile != null) {
                if (!tempFile.delete())
                    logger.warning(
                        Strings.get("Sync.cantDeleteTempFile", tempFile));
            }
        }
    }

    /**
     * Remove the named subdirectory of the instance directory.
     */
    private void removeSubdirectory(String name) {
        File subdir = new File(instanceDir, name);
        if (logger.isLoggable(Level.FINER))
            logger.finer("Removing: " + subdir);
        FileUtils.whack(subdir);
    }

    /**
     * Expand the archive to the specified directory.
     * XXX - this doesn't handle all the cases required for a Java EE app,
     * but it's good enough for now for some performance testing
     */
    private static void expand(File dir, File archive) throws Exception {
        if (!dir.mkdir())
            logger.warning(
                Strings.get("Sync.cantCreateDirectory", dir));
        long modtime = archive.lastModified();
        ZipFile zf = new ZipFile(archive);
	try {
	    Enumeration<? extends ZipEntry> e = zf.entries();
	    while (e.hasMoreElements()) {
		ZipEntry ze = e.nextElement();
		File entry = new File(dir, ze.getName());
		if (ze.isDirectory()) {
		    if (!entry.mkdir())
			logger.warning(
			    Strings.get("Sync.cantCreateDirectory", dir));
		} else {
		    FileUtils.copy(zf.getInputStream(ze),
				    new FileOutputStream(entry), 0);
		}
	    }
	} finally {
	    try {
		zf.close();
	    } catch (IOException ex) { }
	}
        if (!dir.setLastModified(modtime))
            logger.warning(
                Strings.get("Sync.cantSetModTime", dir));
    }
}
