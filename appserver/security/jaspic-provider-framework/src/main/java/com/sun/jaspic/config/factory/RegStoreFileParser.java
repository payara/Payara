/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package com.sun.jaspic.config.factory;

import static com.sun.jaspic.config.helper.JASPICLogManager.JASPIC_LOGGER;
import static com.sun.jaspic.config.helper.JASPICLogManager.RES_BUNDLE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.security.auth.message.config.AuthConfigFactory.RegistrationContext;

/**
 * Used by ServerConfigProvider to parse the configuration file. If a file does not exist originally, the default
 * providers are not used. A file is only created if needed, which happens if providers are registered or unregistered
 * through the store() or delete() methods.
 *
 * @author Bobby Bissett
 */
public final class RegStoreFileParser {

    private static final Logger logger = Logger.getLogger(JASPIC_LOGGER, RES_BUNDLE);

    private static final String SEP = ":";
    private static final String CON_ENTRY = "con-entry";
    private static final String REG_ENTRY = "reg-entry";
    private static final String REG_CTX = "reg-ctx";
    private static final String LAYER = "layer";
    private static final String APP_CTX = "app-ctx";
    private static final String DESCRIPTION = "description";
    private static final String[] INDENT = { "", "  ", "    " };

    private final File configurationFile;
    private List<EntryInfo> entries;
    private List<EntryInfo> defaultEntries;

    /**
     * Loads the configuration file from the given filename. If a file is not found, then the default entries are used.
     * Otherwise the file is parsed to load the entries.
     *
     */
    public RegStoreFileParser(String pathParent, String pathChild, List<EntryInfo> defaultEntries) {
        configurationFile = new File(pathParent, pathChild);
        this.defaultEntries = defaultEntries == null ? new ArrayList<EntryInfo>() : defaultEntries;

        try {
            loadEntries();
        } catch (IOException ioe) {
            logWarningDefault(ioe);
        } catch (IllegalArgumentException iae) {
            logWarningDefault(iae);
        }
    }

    /**
     * Returns the in-memory list of entries. MUST Hold exclusive lock on calling factory while processing entries
     */
    List<EntryInfo> getPersistedEntries() {
        return entries;
    }

    /**
     * Adds the provider to the entry list if it is not already present, creates the configuration file if necessary, and
     * writes the entries to the file.
     */
    void store(String className, RegistrationContext registrationContext, Map<String, String> properties) {
        synchronized (configurationFile) {
            if (checkAndAddToList(className, registrationContext, properties)) {
                try {
                    writeEntries();
                } catch (IOException ioe) {
                    logWarningUpdated(ioe);
                }
            }
        }
    }

    /**
     * Removes the provider from the entry list if it is already present, creates the configuration file if necessary, and
     * writes the entries to the file.
     */
    void delete(RegistrationContext registrationContext) {
        synchronized (configurationFile) {
            if (checkAndRemoveFromList(registrationContext)) {
                try {
                    writeEntries();
                } catch (IOException ioe) {
                    logWarningUpdated(ioe);
                }
            }
        }
    }

    /**
     * If this entry does not exist, this method stores it in the entries list and returns true to indicate that the
     * configuration file should be written.
     */
    private boolean checkAndAddToList(String className, RegistrationContext registrationContext, Map<String, String> properties) {

        // Convention is to use null for empty properties
        if (properties != null && properties.isEmpty()) {
            properties = null;
        }

        EntryInfo newEntry = new EntryInfo(className, properties, registrationContext);
        EntryInfo entry = getMatchingRegistrationEntry(newEntry);

        // There is no matching entry, so add to list
        if (entry == null) {
            entries.add(newEntry);
            return true;
        }

        // Otherwise, check reg contexts to see if there is a match
        if (entry.getRegistrationContexts().contains(registrationContext)) {
            return false;
        }

        // No matching context in existing entry, so add to existing entry
        entry.getRegistrationContexts().add(new RegistrationContextImpl(registrationContext));

        return true;
    }

    /**
     * If this registration context does not exist, this method returns false. Otherwise it removes the entry and returns
     * true to indicate that the configuration file should be written.
     *
     * This only makes sense for registry entries.
     */
    private boolean checkAndRemoveFromList(RegistrationContext target) {
        boolean retValue = false;
        try {
            ListIterator<EntryInfo> lit = entries.listIterator();
            while (lit.hasNext()) {

                EntryInfo info = lit.next();
                if (info.isConstructorEntry()) {
                    continue;
                }

                Iterator<RegistrationContext> iter = info.getRegistrationContexts().iterator();
                while (iter.hasNext()) {
                    RegistrationContext ctx = iter.next();
                    if (ctx.equals(target)) {
                        iter.remove();
                        if (info.getRegistrationContexts().isEmpty()) {
                            lit.remove();
                        }
                        retValue = true;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return retValue;
    }

    /**
     * Used to find a matching registration entry in the 'entries' list without including registration contexts. If there is
     * not a matching entry, return null.
     */
    private EntryInfo getMatchingRegistrationEntry(EntryInfo target) {
        for (EntryInfo info : entries) {
            if (!info.isConstructorEntry() && info.matchConstructors(target)) {
                return info;
            }
        }

        return null;
    }

    /**
     * This method overwrites the existing file with the current entries.
     */
    private void writeEntries() throws IOException {
        if (configurationFile.exists() && !configurationFile.canWrite() && logger.isLoggable(WARNING)) {
            logger.log(WARNING, "jaspic.factory_cannot_write_file", configurationFile.getPath());
        }

        clearExistingFile();

        PrintWriter out = new PrintWriter(configurationFile);
        int indent = 0;
        for (EntryInfo info : entries) {
            if (info.isConstructorEntry()) {
                writeConEntry(info, out, indent);
            } else {
                writeRegEntry(info, out, indent);
            }
        }
        out.close();
    }

    /**
     * Writes constructor entry output of the form:
     * 
     * <pre>
     *  con-entry { className key:value key:value }
     * </pre>
     * 
     * The first appearance of a colon ":" separates the key and value of the property (so a value may contain a colon as
     * part of the string). For instance: "mydir:c:foo" would have key "mydir" and value "c:foo".
     */
    private void writeConEntry(EntryInfo info, PrintWriter out, int i) {
        out.println(INDENT[i++] + CON_ENTRY + " {");
        out.println(INDENT[i] + info.getClassName());

        Map<String, String> properties = info.getProperties();
        if (properties != null) {
            for (Map.Entry<String, String> val : properties.entrySet()) {
                out.println(INDENT[i] + val.getKey() + SEP + val.getValue());
            }
        }

        out.println(INDENT[--i] + "}");
    }

    /*
     * Write registration entry output of the form: <pre> reg-entry { con-entry { see writeConEntry() for detail } reg-ctx {
     * layer:HttpServlet app-ctx:security-jaspic-https description:My provider } } </pre>
     */
    private void writeRegEntry(EntryInfo info, PrintWriter out, int i) {
        out.println(INDENT[i++] + REG_ENTRY + " {");
        if (info.getClassName() != null) {
            writeConEntry(info, out, i);
        }

        for (RegistrationContext registrationContext : info.getRegistrationContexts()) {
            out.println(INDENT[i++] + REG_CTX + " {");
            if (registrationContext.getMessageLayer() != null) {
                out.println(INDENT[i] + LAYER + SEP + registrationContext.getMessageLayer());
            }

            if (registrationContext.getAppContext() != null) {
                out.println(INDENT[i] + APP_CTX + SEP + registrationContext.getAppContext());
            }

            if (registrationContext.getDescription() != null) {
                out.println(INDENT[i] + DESCRIPTION + SEP + registrationContext.getDescription());
            }

            out.println(INDENT[--i] + "}");
        }

        out.println(INDENT[--i] + "}");
    }

    private void clearExistingFile() throws IOException {
        boolean newCreation = !configurationFile.exists();

        if (!newCreation) {
            if (!configurationFile.delete()) {
                throw new IOException();
            }
        }

        if (newCreation) {
            logger.log(INFO, "jaspic.factory_creating_conf_file", configurationFile.getPath());
        }

        if (!configurationFile.createNewFile()) {
            throw new IOException();
        }
    }

    /**
     * Called from the constructor. This is the only time the file is read, though it is written when new entries are stored
     * or deleted.
     */
    private void loadEntries() throws IOException {
        synchronized (configurationFile) {
            entries = new ArrayList<EntryInfo>();
            if (configurationFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(configurationFile))) {
                    String line = reader.readLine();
                    while (line != null) {
                        String trimLine = line.trim(); // can't trim readLine() result
                        if (trimLine.startsWith(CON_ENTRY)) {
                            entries.add(readConEntry(reader));
                        } else if (trimLine.startsWith(REG_ENTRY)) {
                            entries.add(readRegEntry(reader));
                        }
                        line = reader.readLine();
                    }
                }
            } else {
                if (logger.isLoggable(FINER)) {
                    logger.log(FINER, "jaspic.factory_file_not_found",
                            configurationFile.getParent() + File.pathSeparator + configurationFile.getPath());
                }

                for (EntryInfo entry : defaultEntries) {
                    entries.add(new EntryInfo(entry));
                }
            }
        }
    }

    private EntryInfo readConEntry(BufferedReader reader) throws IOException {
        // Entry must contain class name as next line
        String className = reader.readLine();
        if (className != null) {
            className = className.trim();
        }

        return new EntryInfo(className, readProperties(reader));
    }

    /**
     * Properties must be of the form "key:value." While the key String cannot contain a ":" character, the value can. The
     * line will be broken into key and value based on the first appearance of the ":" character.
     */
    private Map<String, String> readProperties(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line != null) {
            line = line.trim();
        }

        if ("}".equals(line)) {
            return null;
        }

        Map<String, String> properties = new HashMap<String, String>();
        while (!"}".equals(line)) {
            properties.put(line.substring(0, line.indexOf(SEP)), line.substring(line.indexOf(SEP) + 1, line.length()));
            line = reader.readLine();
            if (line != null) {
                line = line.trim();
            }
        }

        return properties;
    }

    private EntryInfo readRegEntry(BufferedReader reader) throws IOException {
        String className = null;
        Map<String, String> properties = null;
        List<RegistrationContext> ctxs = new ArrayList<RegistrationContext>();
        String line = reader.readLine();
        if (line != null) {
            line = line.trim();
        }
        while (!"}".equals(line)) {
            if (line.startsWith(CON_ENTRY)) {
                EntryInfo conEntry = readConEntry(reader);
                className = conEntry.getClassName();
                properties = conEntry.getProperties();
            } else if (line.startsWith(REG_CTX)) {
                ctxs.add(readRegContext(reader));
            }
            line = reader.readLine();
            if (line != null) {
                line = line.trim();
            }

        }
        return new EntryInfo(className, properties, ctxs);
    }

    private RegistrationContext readRegContext(BufferedReader reader) throws IOException {
        String layer = null;
        String appCtx = null;
        String description = null;
        String line = reader.readLine();
        if (line != null) {
            line = line.trim();
        }

        while (!"}".equals(line)) {
            String value = line.substring(line.indexOf(SEP) + 1, line.length());
            if (line.startsWith(LAYER)) {
                layer = value;
            } else if (line.startsWith(APP_CTX)) {
                appCtx = value;
            } else if (line.startsWith(DESCRIPTION)) {
                description = value;
            }

            line = reader.readLine();
            if (line != null) {
                line = line.trim();
            }
        }

        return new RegistrationContextImpl(layer, appCtx, description, true);
    }

    private void logWarningUpdated(Exception exception) {
        if (logger.isLoggable(WARNING)) {
            logger.log(WARNING, "jaspic.factory_could_not_persist", exception.toString());
        }
    }

    private void logWarningDefault(Exception exception) {
        if (logger.isLoggable(WARNING)) {
            logger.log(WARNING, "jaspic.factory_could_not_read", exception.toString());
        }
    }

}
