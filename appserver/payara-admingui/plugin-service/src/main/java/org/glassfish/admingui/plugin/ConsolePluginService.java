/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admingui.plugin;

import org.glassfish.api.admingui.ConsoleProvider;

import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.MultiMap;
import org.jvnet.hk2.config.ConfigParser;
import org.jvnet.hk2.config.DomDocument;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.Module;

import org.glassfish.admingui.connector.TOC;
import org.glassfish.admingui.connector.TOCItem;
import org.glassfish.admingui.connector.Index;
import org.glassfish.admingui.connector.IndexItem;
import org.glassfish.admingui.connector.IntegrationPoint;
import org.glassfish.admingui.connector.ConsoleConfig;

import javax.inject.Inject;


/**
 *  <p>	This class provides access to {@link IntegrationPoint}s.</p>
 *
 *  @author Ken Paulsen	(ken.paulsen@sun.com)
 */
@Service
public class ConsolePluginService {
    @Inject Logger logger;
    @Inject ServiceLocator habitat;
    @Inject IterableProvider<ConsoleProvider> providers;

/*
    @Inject ModulesRegistry modulesRegistry;
        for(Module m : modulesRegistry.getModules()) {
            url = m.getClassLoader().getResource(ConsoleProvider.DEFAULT_CONFIG_FILENAME);
            if(url!=null)
                ; // TODO: parse url
        }
*/

    /**
     *	<p> Default constructor.</p>
     */
    public ConsolePluginService() {
    }

    /**
     *	<p> Initialize the available {@link IntegrationPoint}s.</p>
     */
    protected synchronized void init() {
	if (initialized) {
	    return;
	}
	initialized = true;

	// First find the parser
	if ((providers != null) && (providers.iterator().hasNext())) {
	    // Get our parser...
	    ConfigParser parser = new ConfigParser(habitat);
	    URL url = null;
	    String id = null;

	    // Loop through the configs and add them all
	    for (ConsoleProvider provider : providers) {
		// Read the contents from the URL
		url = provider.getConfiguration();
		if (url == null) {
		    url = provider.getClass().getClassLoader().getResource(
			ConsoleProvider.DEFAULT_CONFIG_FILENAME);
		}
		if (url == null) {
		    if (logger.isLoggable(Level.INFO)) {
			logger.info("Unable to find "
			    + ConsoleProvider.DEFAULT_CONFIG_FILENAME
			    + " file for provider '"
			    + provider.getClass().getName() + "'");
		    }
		    continue;
		}
//System.out.println("Provider *"+provider+"* : url=*"+url+"*");
		DomDocument doc = parser.parse(url);

		// Get the New IntegrationPoints
		ConsoleConfig config = (ConsoleConfig) doc.getRoot().get();

		// Save the ClassLoader for later
//System.out.println("Storing: " + config.getId() + " : " + provider.getClass().getClassLoader());
		id = config.getId();
		moduleClassLoaderMap.put(id, provider.getClass().getClassLoader());
		classLoaderModuleMap.put(provider.getClass().getClassLoader(), id);

		// Add the new IntegrationPoints
		addIntegrationPoints(config.getIntegrationPoints(), id);
	    }
	}

//System.out.println("IP Map: " + pointsByType.toString());

	// Log some trace messages
	if (logger.isLoggable(Level.FINE)) {
	    logger.fine("Console Plugin Service has been Initialized!");
	    if (logger.isLoggable(Level.FINEST)) {
		logger.finest(pointsByType.toString());
	    }
	}
    }

    /**
     *	<p> This method returns a merged Table Of Contents for all found help
     *	    sets for the given locale.</p>
     */
    public synchronized TOC getHelpTOC(String locale) {
	if (locale == null) {
	    locale = "en"; // Use this as the default...
	}
	TOC mergedTOC = helpSetMap.get(locale);
	if (mergedTOC != null) {
	    // Already calculated...
	    return mergedTOC;
	}

	// TOC
	Map<String, List<URL>> mapUrls = getResources(locale + "/help/toc.xml");

	// Get our parser...
	ConfigParser parser = new ConfigParser(habitat);

	// Setup a new "merged" TOC...
	mergedTOC = new TOC();
	mergedTOC.setTOCItems(new ArrayList<TOCItem>());
	mergedTOC.setVersion("2.0");

	// Loop through the urls and add them all
	String id = null; // module id
	String prefix = "/" + locale + "/help/";  // prefix (minus module id)
	List<URL> urls = null; // URLs to TOC files w/i each plugin module
	for (Map.Entry<String, List<URL>> entry : mapUrls.entrySet()) {
	    id = entry.getKey();
	    urls = entry.getValue();
	    for (URL url : urls) {
		DomDocument doc = parser.parse(url);

		// Merge all the TOC's...
		TOC toc = (TOC) doc.getRoot().get();
		for (TOCItem item : toc.getTOCItems()) {
		    insertTOCItem(mergedTOC.getTOCItems(), item, id + prefix);
		}
	    }
	}

// FIXME: Sort?
	return mergedTOC;
    }

    /**
     *	<p> This method inserts the given <code>item</code> into the
     *	    <code>dest</code> list.</p>
     */
    private void insertTOCItem(List<TOCItem> dest, TOCItem item, String prefix) {
	int idx = dest.indexOf(item);
	if (idx == -1) {
	    // Fix target path...
	    fixTargetPath(item, prefix);

	    // Not there yet, just add it...
	    dest.add(item);
	} else {
	    // Already there, insert children of item...
	    TOCItem parent = dest.get(idx);
	    for (TOCItem child : item.getTOCItems()) {
		insertTOCItem(parent.getTOCItems(), child, prefix);
	    }
	}
    }

    /**
     *	<p> This method returns a merged Table Of Contents for all found help
     *	    sets for the given locale.</p>
     */
    public synchronized Index getHelpIndex(String locale) {
	if (locale == null) {
	    locale = "en"; // Use this as the default...
	}
	Index mergedIndex = helpSetIndexMap.get(locale);
	if (mergedIndex != null) {
	    // Already calculated...
	    return mergedIndex;
	}

	// TOC
	Map<String, List<URL>> mapUrls = getResources(locale + "/help/index.xml");

	// Get our parser...
	ConfigParser parser = new ConfigParser(habitat);

	// Setup a new "merged" TOC...
	mergedIndex = new Index();
	mergedIndex.setIndexItems(new ArrayList<IndexItem>());
	mergedIndex.setVersion("2.0");

	// Loop through the urls and add them all
	String id = null; // module id
	String prefix = "/" + locale + "/help/";  // prefix (minus module id)
	List<URL> urls = null; // URLs to TOC files w/i each plugin module
	for (Map.Entry<String, List<URL>> entry : mapUrls.entrySet()) {
	    id = entry.getKey();
	    urls = entry.getValue();
	    for (URL url : urls) {
		DomDocument doc = parser.parse(url);

		// Merge all the TOC's...
		Index index = (Index) doc.getRoot().get();
		for (IndexItem item : index.getIndexItems()) {
		    insertIndexItem(mergedIndex.getIndexItems(), item, id + prefix);
		}
	    }
	}

// FIXME: Sort?
	return mergedIndex;
    }

    /**
     *	<p> This method inserts the given <code>item</code> into the
     *	    <code>dest</code> list.</p>
     */
    private void insertIndexItem(List<IndexItem> dest, IndexItem item, String prefix) {
	int idx = dest.indexOf(item);
	if (idx == -1) {
	    // Fix target path...
	    fixHtmlFileForIndexItem(item, prefix);

	    // Not there yet, just add it...
	    dest.add(item);
	} else {
	    // Already there, insert children of item...
	    IndexItem parent = dest.get(idx);
	    for (IndexItem child : item.getIndexItems()) {
		insertIndexItem(parent.getIndexItems(), child, prefix);
	    }
	}
    }


    /**
     *
     */
    private void fixTargetPath(TOCItem parent, String prefix) {
	parent.setTargetPath(prefix + parent.getTarget() + ".html");
	for (TOCItem item : parent.getTOCItems()) {
	    fixTargetPath(item, prefix);
	}
    }

    /**
     *
     */
    private void fixHtmlFileForIndexItem(IndexItem parent, String prefix) {
        String target = null;
        if (null != (target = parent.getTarget())) {
            parent.setHtmlFileForTarget(prefix + target + ".html");
        }
	for (IndexItem item : parent.getIndexItems()) {
	    fixHtmlFileForIndexItem(item, prefix);
	}
    }

    /**
     *	<p> This method searches the classpath of all plugins for the requested
     *	    resource and returns all instances of it (if any).  This method
     *	    will NOT return <code>null</code>, but may return an empty
     *	    <code>List</code>.</p>
     */
    public Map<String, List<URL>> getResources(String name) {
	Map<String, List<URL>> result = new HashMap<String, List<URL>>();
	if ((providers != null) && (providers.iterator().hasNext())) {
	    // Get our parser...
	    Enumeration<URL> urls = null;
	    URL url = null;

	    // Loop through the configs and add them all
	    for (ConsoleProvider provider : providers) {
		// Read the contents from the URL
		ClassLoader loader = provider.getClass().getClassLoader();
		try {
		    urls = loader.getResources(name);
		} catch (IOException ex) {
		    if (logger.isLoggable(Level.INFO)) {
			logger.log(Level.INFO, "Error getting resource '"
			    + name + "' from provider: '"
			    + provider.getClass().getName() + "'. Skipping...",
			    ex);
		    }
		    continue;
		}
		List<URL> providerURLs = new ArrayList<URL>();
		while (urls.hasMoreElements()) {
		    // Found one... add it.
		    url = urls.nextElement();
		    try {
			providerURLs.add(new URL(url, ""));
		    } catch (Exception ex) {
			// Ignore b/c this should not ever happen, we're not
			// changing the URL
			System.out.println(
			    "ConsolePluginService: URL Copy Failed!");
		    }
		}

		// Put the URLs into the Map by module id...
		if (providerURLs.size() > 0) {
		    result.put(classLoaderModuleMap.get(loader), providerURLs);
		}
	    }
	}
	return result;
    }

    /***********************************************************
    public static byte[] readFromURL(URL url) throws IOException {
        byte buffer[] = new byte[10000];
        byte result[] = new byte[0];

	int count = 0;
	int offset = 0;
	java.io.InputStream in = url.openStream();

	// Attempt to read up to 10K bytes.
	count = in.read(buffer);
	while (count != -1) {
	    // Make room for new content...
	    //result = Arrays.copyOf(result, offset + count);  Java 6 only...
	    // When I can depend on Java 6... replace the following 3 lines
	    // with the line above.
	    byte oldResult[] = result;
	    result = new byte[offset + count];
	    System.arraycopy(oldResult, 0, result, 0, offset);

	    // Copy in new content...
	    System.arraycopy(buffer, 0, result, offset, count);

	    // Increment the offset
	    offset += count;

	    // Attempt to read up to 10K more bytes...
	    count = in.read(buffer);
	}
        return result;
    }
    ***********************************************************/

    /**
     *	<p> This method allows new {@link IntegrationPoint}s to be added to
     *	    the known {@link IntegrationPoint}s.</p>
     */
    public void addIntegrationPoints(List<IntegrationPoint> points, String id) {
	// Add them all...
	for (IntegrationPoint point : points) {
	    addIntegrationPoint(point, id);
	}
    }

    /**
     *	<p> This method allows a new {@link IntegrationPoint} to be added to
     *	    the known {@link IntegrationPoint}s.</p>
     */
    public void addIntegrationPoint(IntegrationPoint point, String id) {
	// Associate the Provider with this IntegrationPoint so we
	// have a way to get the correct classloader
	point.setConsoleConfigId(id);

	// Add it
	pointsByType.add(point.getType(), point);
    }

    /**
     *	<p> This method returns the {@link IntegrationPoint}s associated with
     *	    the given type.</p>
     *
     *	@param	type	The type of {@link IntegrationPoint}s to retrieve.
     */
    public List<IntegrationPoint> getIntegrationPoints(String type) {
	init();	// Ensure it is initialized.
	return pointsByType.get(type);
    }

    /**
     *	<p> This method returns the <code>ClassLoader</code> associated with
     *	    the requested module.  If the requested module does not exist, has
     *	    not been initialized, or does not contain any admin console
     *	    extensions, this method will return <code>null</code>.</p>
     *
     *	@param	moduleName	The name of the module.
     *
     *	@return	<code>null</code>, or the module's <code>ClassLoader</code>.
     */
    public ClassLoader getModuleClassLoader(String moduleName) {
	return moduleClassLoaderMap.get(moduleName);
    }

    /**
     *	<p> Flag indicating intialization has already occured.</p>
     */
    private boolean initialized	    = false;

    /**
     *	<p> This <code>Map</code> contains the {@link IntegrationPoint}s keyed
     *	    by the <code>type</code> of integration.</p>
     */
    private MultiMap<String, IntegrationPoint> pointsByType =
	    new MultiMap<String, IntegrationPoint>();

    /**
     *	<p> This <code>Map</code> keeps track of the <code>ClassLoader</code>
     *	    for each module that provides GUI {@link IntegrationPoint}s.  It
     *	    is keyed by the id specified in the <code>console-config.xml</code>
     *	    file from the module.</p>
     */
    private Map<String, ClassLoader> moduleClassLoaderMap =
	    new HashMap<String, ClassLoader>();

    /**
     *	<p> This <code>Map</code> keeps track of the module id for each
     *	    <code>ClassLoader</code> that provides {@link IntegrationPoint}s.
     *	    It is keyed by the classloader and returns the module id as
     *	    specified in the module's <code>console-config.xml</code> file.</p>
     */
    private Map<ClassLoader, String> classLoaderModuleMap =
	    new HashMap<ClassLoader, String>();

    /**
     *
     */
    private Map<String, TOC> helpSetMap = new HashMap<String, TOC>();

    private Map<String, Index> helpSetIndexMap = new HashMap<String, Index>();

}
