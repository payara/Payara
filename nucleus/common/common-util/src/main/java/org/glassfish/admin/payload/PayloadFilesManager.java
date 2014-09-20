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

package org.glassfish.admin.payload;

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.Payload;
import org.glassfish.api.admin.Payload.Part;

/**
 * Manages transferred files delivered via the request or response {@link Payload}.
 * <p>
 * Callers can process the entire payload
 * at once, treating each Part as a file, using the {@link #processParts}
 * method.  Or, the caller can invoke the {@link #processPart}
 * method to work with a single Part as a file.
 * <p>
 * If the caller wants to extract the payload's content as temporary files it should
 * instantiate {@link Temp} which exposes a {@link PayLoadFilesManager.Temp#cleanup}
 * method.  The caller should invoke this method once it has finished with
 * the transferred files, although the finalizer will invoke cleanup just in case.
 * <p>
 * On the other hand, if the caller wants to keep the transferred files it
 * should instantiate {@link Perm}.
 * <p>
 * <code>Temp</code> uses a unique temporary directory, then creates one
 * temp file for each part it is asked to deal with, either from an entire payload
 * ({@link #processParts(org.glassfish.api.admin.Payload.Inbound)}) or a
 * single part ({@link #processPart(org.glassfish.api.admin.Payload.Part)}).  Recall that each part in the
 * payload has a name which is a relative or absolute URI.
 * 
 * @author tjquinn
 */
public abstract class PayloadFilesManager {

    private static final String XFER_DIR_PREFIX = "xfer-";
    public final static LocalStringManagerImpl strings = new LocalStringManagerImpl(PayloadFilesManager.class);

    private final File targetDir;
    protected final Logger logger;
    private final ActionReport report;
    private final ActionReportHandler reportHandler;

    protected final Map<File,Long> dirTimestamps = new HashMap<File,Long>();

    private PayloadFilesManager(
            final File targetDir,
            final ActionReport report,
            final Logger logger,
            final ActionReportHandler reportHandler) {
        this.targetDir = targetDir;
        this.report = report;
        this.logger = logger;
        this.reportHandler = reportHandler;
    }

    private PayloadFilesManager(
            final File targetDir,
            final ActionReport report,
            final Logger logger) {
        this(targetDir, report, logger, null);
    }

    protected File getTargetDir() {
        return targetDir;
    }

    protected URI getParentURI(Part part) throws UnsupportedEncodingException {
            /*
             * parentURI and parentFile start as the target extraction directory for this
             * manager, but will change iff the part specifies a file
             * transfer root.
             */
            File parentFile = getTargetDir();
            URI parentFileURI = parentFile.toURI();

            final Properties partProps = part.getProperties();
            String parentPathFromPart = partProps.getProperty("file-xfer-root");
            if (parentPathFromPart != null) {
                if (! parentPathFromPart.endsWith(File.separator)) {
                    parentPathFromPart = parentPathFromPart + File.separator;
                }
                final File xferRootFile = new File(parentPathFromPart);
                if (xferRootFile.isAbsolute()) {
                    parentFile = xferRootFile;
                } else {
                    parentFile = new File(parentFile, parentPathFromPart);
                }
                /*
                 * If this parent directory does not exist, then the URI from
                 * the File object will lack the trailing slash.  So create
                 * the URI a little oddly to account for that case.
                 */
                parentFileURI = URI.create(
                        parentFile.toURI().toASCIIString() +
                        (parentFile.exists() ? "" : "/"));
            }
            return parentFileURI;
        }

    /**
     * Extracts files from a Payload and leaves them on disk.
     * <p>
     * The Perm manager constructs output file paths this way.  The URI from the
     * manager's targetDir (which the caller passes to the constructor) is the default
     * parent URI for the output file.
     * <p>
     * Next, the Part's properties are checked for a file-xfer-root property.
     * If found, it is used as a URI (either absolute or, if relative, resolved
     * against the targetDir).
     * <p>
     * Finally, the "output name" is either the
     * name from the Payload.Part for the {@link #extractFile(org.glassfish.api.admin.Payload.Part) }
     * method or the caller-provided argument in the {@link #extractFile(org.glassfish.api.admin.Payload.Part, java.lang.String) }
     * method.
     * <p>
     * In either case, the output name is used as a URI
     * string and is resolved against the targetDir combined with (if present) the
     * file-xfer-root property.
     * <p>
     * The net effect of this
     * is that if the output name is an absolute URI then it will override the
     * targetDir and the file-xfer-root setting.  If the output name is
     * relative then it will be resolved
     * against the targetDir plus file-xfer-root URI to derive the URI for the output file.
     */
    public static class Perm extends PayloadFilesManager {

        /**
         * Creates a new PayloadFilesManager for dealing with permanent files that
         * will be anchored at the specified target directory.
         * @param targetDir directory under which the payload's files should be stored
         * @param report result report to which extraction results will be appened
         * @param logger logger to receive messages
         */
        public Perm(final File targetDir, final ActionReport report, final Logger logger) {
            this(targetDir, report, logger, null);
        }

        /**
         * Creates a new PayloadFilesManager for permanent files anchored at
         * the specified target directory.
         * @param targetDir directory under which the payload's files should be stored
         * @param report result report to which extraction results will be appened
         * @param logger logger to receive messages
         * @param reportHandler handler to invoke for each ActionReport in the payload
         */
        public Perm(final File targetDir, final ActionReport report,
                final Logger logger, final ActionReportHandler reportHandler) {
            super(targetDir != null ? targetDir :
		    new File(System.getProperty("user.dir")),
		    report, logger, reportHandler);
        }

        /**
         * Creates a new PayloadFilesManager for permanent files anchored at
         * the caller's current directory.
         * @param report result report to which extraction results will be appended
         * @param logger logger to receive messages
         */
        public Perm(final ActionReport report, final Logger logger) {
            this(report, logger, null);
        }


        /**
         * Creates a new PayloadFilesManager for permanent files anchored at
         * the caller's current directory.
         * @param report result report to which extraction results will be appened
         * @param logger logger to receive messages
         * @param reportHandler handler to invoke for each ActionReport in the payload
         */
        public Perm(final ActionReport report, final Logger logger,
                final ActionReportHandler reportHandler) {
            super(new File(System.getProperty("user.dir")), report, logger, reportHandler);
        }

        /**
         * Creates a new PayloadFilesManager for permanent files anchored at
         * the caller's current directory.
         * @param logger logger to receive messages
         */
        public Perm(final Logger logger) {
            this(null, logger);
        }

        /**
         * Creates a new PayloadFilesManager for permanent files anchored at
         * the caller's current directory.
         */
        public Perm() {
            this((ActionReportHandler) null);
        }

        public Perm(final ActionReportHandler reportHandler) {
            this(null, Logger.getLogger(Perm.class.getName()), reportHandler);
        }

        @Override
        protected void postExtract(File extractedFile) {
            // no-op for permanent files
        }

        @Override
        protected void postProcessParts() {
            final boolean isFine = logger.isLoggable(Level.FINE);
            for (Map.Entry<File,Long> entry : dirTimestamps.entrySet()) {
                if (isFine) {
                    final Date when = new Date(entry.getValue());
                    logger.log(Level.FINER, "Setting lastModified for {0} explicitly to {1}", new Object[]{entry.getKey().getAbsolutePath(), when});
                }
                if ( ! entry.getKey().setLastModified(entry.getValue())) {
                    logger.log(Level.WARNING, strings.getLocalString(
                            "payload.setLatModifiedFailed",
                            "Attempt to set lastModified for {0} failed; no further information is available.  Continuing.",
                            entry.getKey().getAbsoluteFile()));
                }
            }
        }


    }

    /**
     * Extracts files from a payload, treating them as temporary files.
     * The caller should invoke {@link #cleanup} once it is finished with the
     * extracted files, although the finalizer will invoke cleanup if the
     * caller has not.
     */
    public static class Temp extends PayloadFilesManager {

//        /*
//         * regex to match colons and backslashes on Windows and slashes on non-Windows
//         */
//        private static final String DIR_PATH_TO_FLAT_NAME_PATTERN = (File.separatorChar == '\\') ?
//                "[:\\\\]" : "/";

        private boolean isCleanedUp = false;

//        /** maps payload part name paths (excluding name and type) to temp file subdirs */
//        private Map<String,File> pathToTempSubdir = new HashMap<String,File>();

        public Temp(final File parentDir, final ActionReport report,
                final Logger logger) throws IOException {
            super(createTempFolder(
                      parentDir,
                      logger),
                  report,
                  logger);
        }
        /**
         * Creates a new PayloadFilesManager for temporary files.
         * @param report results report to which extraction results will be appended
         * @param logger logger to receive messages
         * @throws java.io.IOException
         */
        public Temp(final ActionReport report, final Logger logger) throws IOException {
            this(new File(System.getProperty("java.io.tmpdir")), report, logger);
        }

        /**
         * Creates a new PayloadFilesManager for temporary files.
         * @param logger logger to receive messages
         * @throws java.io.IOException
         */
        public Temp(final Logger logger) throws IOException {
            this(null, logger);
        }

        /**
         * Deletes the temporary files created by this temp PayloadFilesManager.
         */
        public void cleanup() {
            if ( ! isCleanedUp) {
                FileUtils.whack(super.targetDir);
                isCleanedUp = true;
            }
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            cleanup();
        }

        @Override
        protected void postExtract(File extractedFile) {
            extractedFile.deleteOnExit();
        }

        @Override
        protected void postProcessParts() {
            // no-op
        }


//        private String getParentPath(String partName) {
//            if (partName.endsWith("/")) {
//                partName = partName.substring(0, partName.length() - 1);
//            }
//            int lastSlash = partName.lastIndexOf('/');
//            if (lastSlash != -1) {
//                return partName.substring(0, lastSlash);
//            }
//            return null;
//        }

//        URI getTempSubDirForPath(String path) throws IOException {
//            /*
//             * Convert the path (which is currently in URI form) to
//             * the local file system form.
//             */
//            path = path.replace('/', File.separatorChar);
//            File tempSubDir = pathToTempSubdir.get(path);
//            if (tempSubDir == null) {
//                /*
//                 * Replace slashes (forward or backward) that are directory
//                 * separators and replace colons (from Windows devices) with single
//                 * dashes.  This technique generates unique but flat directory
//                 * names so same-named files in different directories will
//                 * go to different directories.
//                 *
//                 * The extra dashes make sure the prefix meets createTempFile's reqts.
//                 *
//                 */
//                String tempDirPrefix = path.replaceAll(DIR_PATH_TO_FLAT_NAME_PATTERN, "-") + "---";
//                tempSubDir = createTempFolder(getTargetDir(), tempDirPrefix, super.logger);
//                pathToTempSubdir.put(path, tempSubDir);
//            }
//            return tempSubDir.toURI();
//        }

//        private String getNameAndType(String path) {
//            if (path.endsWith("/")) {
//                path = path.substring(0, path.length() - 1);
//            }
//            final int lastSlash = path.lastIndexOf('/');
//            return path.substring(lastSlash + 1);
//        }

    }

    protected abstract void postExtract(final File extractedFile);

    protected URI getOutputFileURI(Part part, String name) throws IOException {
        /*
         * The part name might have path elements using / as the
         * separator, so figure out the full path for the resulting
         * file.
         */
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        URI targetURI = getParentURI(part).resolve(name);
        return targetURI;
    }

    private File removeFile(final Payload.Part part) throws IOException {
        final File result = removeFileWithoutConsumingPartBody(part);
        consumePartBody(part);
        return result;
    }

    private File removeFileWithoutConsumingPartBody(final Payload.Part part) throws IOException {
        final boolean isFine = logger.isLoggable(Level.FINE);
        File targetFile = new File(getOutputFileURI(part, part.getName()));
        if (targetFile.exists()) {
            final boolean isRemovalRecursive = targetFile.isDirectory() && part.isRecursive();
            if (isRemovalRecursive ?
                    FileUtils.whack(targetFile) : targetFile.delete()) {
                if (isFine) {
                    logger.log(Level.FINER, "Deleted {0}{1} as requested", new Object[]{targetFile.getAbsolutePath(), isRemovalRecursive ? " recursively" : ""});
                }
                reportDeletionSuccess();
            } else {
                if (isFine) {
                    logger.log(Level.FINER, "File {0} ({1}) requested for deletion exists but was not able to be deleted", new Object[]{part.getName(), targetFile.getAbsolutePath()});
                }
                reportDeletionFailure(part.getName(),
                        strings.getLocalString("payload.deleteFailedOnFile",
                        "Requested deletion of {0} failed; the file was found but the deletion attempt failed - no reason is available"));
            }
        } else {
            if (isFine) {
                logger.log(Level.FINER, "File {0} ({1}) requested for deletion does not exist.", new Object[]{part.getName(), targetFile.getAbsolutePath()});
            }
            reportDeletionFailure(part.getName(), new FileNotFoundException(targetFile.getAbsolutePath()));
        }
        return targetFile;
    }


    private File replaceFile(final Payload.Part part) throws IOException {
        removeFileWithoutConsumingPartBody(part);
        return extractFile(part, part.getName());
    }

    private void consumePartBody(final Part part) throws FileNotFoundException, IOException {
        InputStream is = null;
        try {
            is = part.getInputStream();
            byte[] buffer = new byte[1024 * 64];
            while (is.read(buffer) != -1) {
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private void processReport(final Payload.Part part) throws Exception {
        if (reportHandler != null) {
            reportHandler.handleReport(part.getInputStream());
        } else {
            consumePartBody(part);
        }
    }

    /**
     * Extracts the contents of the specified Part as a file, specifying
     * the relative or absolute URI to use for creating the extracted file.
     * If outputName is relative it is resolved against the manager's target
     * directory (which the caller passed to the constructor) and the
     * file-xfer-root Part property, if present.
     * @param part the Part containing the file's contents
     * @param outputName absolute or relative URI string to use for the extracted file
     * @return File for the extracted file
     * @throws java.io.IOException
     */
    private File extractFile(final Payload.Part part, final String outputName) throws IOException {
        final boolean isFine = logger.isLoggable(Level.FINE);
        OutputStream os = null;
        InputStream is = null;
        /*
         * Look in the Part's properties first for the URI of the target
         * directory for the file.  If there is none there then use the
         * target directory for this manager.
         */


        try {
            File extractedFile = new File(getOutputFileURI(part, outputName));

            /*
             * Create the required directory tree under the target directory.
             */
            File immediateParent = extractedFile.getParentFile();
            if ( ! immediateParent.exists() && ! immediateParent.mkdirs()) {
                logger.log(Level.WARNING, strings.getLocalString(
                        "payload.mkdirsFailed",
                        "Attempt to create directories for {0} failed; no further information is available. Continuing.",
                        immediateParent.getAbsolutePath()));
            }
            if (extractedFile.exists()) {
                if (!extractedFile.delete() && ! extractedFile.isDirectory()) {
                    /*
                     * Don't warn if we cannot delete the directory - there
                     * are likely to be files in it preventing its removal.
                     */
                      logger.warning(strings.getLocalString(
                            "payload.overwrite",
                            "Overwriting previously-uploaded file because the attempt to delete it failed: {0}",
                            extractedFile.getAbsolutePath()));
                } else if (isFine) {
                    logger.log(Level.FINER, "Deleted pre-existing file {0} before extracting transferred file", extractedFile.getAbsolutePath());
                }
            }

            /*
             * If we are extracting a directory, then we need to consume the
             * Part's body but we won't write anything into the directory
             * file.
             */
            if (outputName.endsWith("/")) {
                if ( ! extractedFile.exists() && ! extractedFile.mkdir()) {
                    logger.log(Level.WARNING, 
                            strings.getLocalString("payload.mkdirsFailed",
                            "Attempt to create directories for {0} failed; no further information is available. Continuing.",
                            extractedFile.getAbsolutePath()));
                }
            }

            final boolean isDir = extractedFile.isDirectory();

            os = isDir ? null :new BufferedOutputStream(new FileOutputStream(extractedFile));
            is = part.getInputStream();
            int bytesRead;
            byte[] buffer = new byte[1024 * 64];
            while ((bytesRead = is.read(buffer)) != -1) {
                if (os != null) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            if (os != null) {
                os.close();
            }
            /* This is because some commands need to process also stream payload
             * parts more then ones. We have to tell that it was extracted to
             * some file
             */
            part.setExtracted(extractedFile);
            
            final String lastModifiedString = part.getProperties().getProperty("last-modified");
            final long lastModified = (lastModifiedString != null ?
                Long.parseLong(lastModifiedString) :
                System.currentTimeMillis());

            if ( ! extractedFile.setLastModified(lastModified)) {
                logger.log(Level.WARNING, strings.getLocalString(
                        "payload.setLatModifiedFailed",
                        "Attempt to set lastModified for {0} failed; no further information is available.  Continuing.",
                        extractedFile.getAbsolutePath()));
            }
            if (extractedFile.isDirectory()) {
                dirTimestamps.put(extractedFile, lastModified);
            }
            postExtract(extractedFile);
            logger.log(Level.FINER, "Extracted transferred entry {0} to {1}", new Object[]{part.getName(), extractedFile.getAbsolutePath()});
            reportExtractionSuccess();
            return extractedFile;
        }
        catch (IOException e) {
            reportExtractionFailure(part.getName(), e);
            throw new IOException(e.getMessage(), e);
        } finally {
            if (os != null) {
                os.close();
                os = null;
            }
        }
    }

    /**
     * Returns all Files extracted from the Payload, treating each Part as a
     * separate file, via a Map from each File to its associated Properties.
     *
     * @param inboundPayload Payload containing file data to be extracted
     * @return map from each extracted File to its corresponding Properties
     * @throws java.io.IOException
     */
    public Map<File,Properties> processPartsExtended(
            final Payload.Inbound inboundPayload) throws Exception {

        if (inboundPayload == null) {
            return Collections.EMPTY_MAP;
        }

        final Map<File,Properties> result = new LinkedHashMap<File,Properties>();

        boolean isReportProcessed = false;
        Part possibleUnrecognizedReportPart = null;

        StringBuilder uploadedEntryNames = new StringBuilder();
        for (Iterator<Payload.Part> partIt = inboundPayload.parts(); partIt.hasNext();) {
            Payload.Part part = partIt.next();
            DataRequestType drt = DataRequestType.getType(part);
            if (drt != null) {
                result.put(drt.processPart(this, part, part.getName()), part.getProperties());
                isReportProcessed |= (drt == DataRequestType.REPORT);
                uploadedEntryNames.append(part.getName()).append(" ");
            } else {
                if ( (! isReportProcessed) && possibleUnrecognizedReportPart == null) {
                    possibleUnrecognizedReportPart = part;
                }
            }
        }
        if ( (! isReportProcessed) && possibleUnrecognizedReportPart != null) {
            DataRequestType.REPORT.processPart(this, possibleUnrecognizedReportPart,
                    possibleUnrecognizedReportPart.getName());
            isReportProcessed = true;
        }
        postProcessParts();
        return result;
    }

    /**
     * Returns all Files extracted from the Payload, treating each Part as a
     * separate file.
     * @param inboundPayload Payload containing file data to be extracted
     * @parma reportHandler invoked for each ActionReport Part in the payload
     * @return the Files corresponding to the content of each extracted file
     * @throws java.io.IOException
     */
    public List<File> processParts(
            final Payload.Inbound inboundPayload) throws Exception {

        return new ArrayList<File>(processPartsExtended(inboundPayload).keySet());
    }

    public static interface ActionReportHandler {
        public void handleReport(final InputStream reportStream) throws Exception;
    }

    protected abstract void postProcessParts();

    private void reportExtractionSuccess() {
        reportSuccess();
    }

    private void reportSuccess() {
        if (report != null) {
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        }
    }

    private void reportDeletionSuccess() {
        reportSuccess();
    }

    private void reportDeletionFailure(final String partName, final String msg) {
        reportFailure(partName, msg, null);
    }

    private void reportDeletionFailure(final String partName, final Exception e) {
        reportFailure(partName, strings.getLocalString(
                    "payload.errDeleting",
                    "Error deleting file {0}",
                    partName), e);
    }

    private void reportFailure(final String partName, final String formattedMessage, final Exception e) {
        if (report != null) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(formattedMessage);
            report.setFailureCause(e);
        }
    }

    private void reportExtractionFailure(final String partName, final Exception e) {
        reportFailure(partName,
                strings.getLocalString(
                    "payload.errExtracting",
                    "Error extracting transferred file {0}",
                    partName),
                    e);
    }

    /**
     * Creates a unique temporary directory within the specified parent.
     * @param parent directory within which to create the temp dir; will be created if absent
     * @return the temporary folder
     * @throws java.io.IOException
     */
    private static File createTempFolder(final File parent, final String prefix, final Logger logger) throws IOException {
        File result = File.createTempFile(prefix, "", parent);
        try {
            if ( ! result.delete()) {
                throw new IOException(
                        strings.getLocalString(
                            "payload.command.errorDeletingTempFile",
                            "Unknown error deleting temporary file {0}",
                            result.getAbsolutePath()));
            }
            if ( ! result.mkdir()) {
                throw new IOException(
                        strings.getLocalString(
                            "payload.command.errorCreatingDir",
                            "Unknown error creating directory {0}",
                            result.getAbsolutePath()));
            }
            logger.log(Level.FINER, "Created temporary upload folder {0}", result.getAbsolutePath());
            return result;
        } catch (Exception e) {
            throw new IOException(strings.getLocalString(
                    "payload.command.errorCreatingXferFolder",
                    "Error creating temporary file transfer folder"), e);
        }
    }

    private static File createTempFolder(final File parent, final Logger logger) throws IOException {
        return createTempFolder(parent, XFER_DIR_PREFIX, logger);
    }

    /**
     * Types of data requests the PayloadFilesManager understands.
     * <p>
     * To add a new type, add a new enum value with the value of the data
     * request type as the constructor argument and implement the processPart
     * method.
     */
    private enum DataRequestType {
        FILE_TRANSFER("file-xfer") {

            @Override
            protected File processPart(
                    final PayloadFilesManager pfm,
                    final Part part,
                    final String partName) throws Exception {
                return pfm.extractFile(part, partName);
            }

        },
        FILE_REMOVAL("file-remove") {
            @Override
            protected File processPart(
                    final PayloadFilesManager pfm,
                    final Part part,
                    final String partName) throws Exception {
                return pfm.removeFile(part);
            }

        },
        FILE_REPLACEMENT("file-replace") {

            @Override
            protected File processPart(
                    final PayloadFilesManager pfm,
                    final Part part,
                    final String partName) throws Exception {
                return pfm.replaceFile(part);
            }
        },
        REPORT("report") {

            @Override
            protected File processPart(
                    final PayloadFilesManager pfm, final
                    Part part,
                    final String partName) throws Exception {
                pfm.processReport(part);
                return null;
            }

        };

        /** data-request-type value for this enum */
        private final String dataRequestType;

        /**
         * Creates a new instance of the enum
         * @param type
         */
        private DataRequestType(final String type) {
            dataRequestType = type;
        }

        /**
         * Processes the specified part by delegating to the right method on
         * the PayloadFilesManager.
         * @param pfm
         * @param part
         * @param partName
         * @return
         * @throws IOException
         */
        protected abstract File processPart(
                final PayloadFilesManager pfm, final Part part, final String partName)
                throws Exception;

        /**
         * Finds the DataRequestType enum which matches the data-request-type
         * in the Part's properties.
         * @param part
         * @return DataRequestType matching the Part's data-request-type; null if no match exists
         */
        private static DataRequestType getType(final Part part) {
            final String targetDataRequestType = part.getProperties().getProperty("data-request-type");
            for (DataRequestType candidateType : values()) {
                if (candidateType.dataRequestType.equals(targetDataRequestType)) {
                    return candidateType;
                }
            }
            return null;
        }
    }
}

