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

package org.glassfish.admingui.common.handlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;
import org.glassfish.admingui.common.util.GuiUtil;

/**
 *  <p>	This class provides API support for managing {@link Tag}s.</p>
 */
public class TagSupport implements Serializable{

    /**
     *	<p> This method adds a tag for the given <code>tagViewId</code> and
     *	    <code>user</code>.  If the tagViewId is new, it will store the
     *	    given <code>displayName</code>.</p>
     */
    public static void addTag(String tagName, String tagViewId, String displayName, String user) {
	// Normalize tagViewId, but not Tag name (only normalize the Tag-name
	// when getting/storing it as a key to a Map)
	if (tagName == null) {
	    throw new IllegalArgumentException("You cannot add a tag with a null name!");
	}
	if (tagViewId == null) {
	    throw new IllegalArgumentException("You cannot tag a page which does not have an ID!");
	}
	tagViewId = normalizeTagViewId(tagViewId);

	// Give user a reasonable value if it is null
	if (user == null) {
	    // user = getExternalContext().getUserPrincipal().getName();
	    user = "anonymous";
	}

	// See if we already have a Tag for this...
	Tag theTag = null;
	List<Tag> tags = queryTags(tagName, tagViewId, (String) null);
	// Check to see if this is already tagged...
	if ((tags != null) && (tags.size() > 0)) {
	    // There should only be 1 for a unique tagName/tagViewId...
	    theTag = tags.get(0);
	    if (theTag.containsUser(user)) {
		// Already tagged by this user... nothing to do
		return;
	    }

	    // Add a new user to the existing tag...
	    theTag.addUser(user);
	} else {
	    // Create a new one...
	    theTag = new Tag(tagName, tagViewId, displayName, user);
	}

	// Store it...
	setTag(theTag);
    }

    /**
     *	<p> This method stores a single {@link Tag}.</p>
     */
    private static void setTag(Tag tag) {
	// First get the 2 Tags Maps...
	Map<String, List<Tag>>[] maps = getTagMaps();

	if ((tag.getUsers() != null) && (tag.getUsers().size() > 0)) {
	    // Set in the by-tag-name Map...
	    setTagInMap(maps[TAG_NAME_MAP_IDX],
		    normalizeTagName(tag.getTagName()), tag);

	    // Set in the by-page Map...
	    setTagInMap(maps[PAGE_MAP_IDX], tag.getTagViewId(), tag);
	} else {
	    // Delete mode...
	    // Tags by name map...
	    List<Tag> tags = maps[TAG_NAME_MAP_IDX].get(
		normalizeTagName(tag.getTagName()));
	    tags.remove(tag);

	    // Tags by viewId
	    tags = maps[PAGE_MAP_IDX].get(tag.getTagViewId());
	    tags.remove(tag);
	}

	// Save the data...
	setTagMaps(maps);
    }

    /**
     *	<p> This method sets a <code>Tag</code> in the given
     *	    <code>Map</code> by the given <code>key</code>.  The key is
     *	    expected to be normalized already.</p>
     */
    private static void setTagInMap(Map<String, List<Tag>> map, String key, Tag tag) {
	List<Tag> tagList = map.get(key);
	if (tagList != null) {
	    // We already have this Tag, see if we have a hit on the page too
	    // equals() compares tagName / page
	    int tagIdx = tagList.indexOf(tag);
	    if (tagIdx != -1) {
		// Need to remove this tag so its doesn't exist 2x
		// (it is important to update due to possible user changes)
		tagList.remove(tagIdx);
	    }
	} else {
	    // We need to create a List and add it to the Map
	    tagList = new ArrayList<Tag>(1);
	    map.put(key, tagList);
	}

	// Now just add tag to the List
	tagList.add(tag);
    }

    /**
     *	<p> This method stores the given array of maps via the
     *	    <code>Preferences API</code>.</p>
     */
    private static void setTagMaps(Map<String, List<Tag>>[] maps) {
	ByteArrayOutputStream buf = new ByteArrayOutputStream();
	try {
	    // Prepare the data...
	    ObjectOutputStream out = new ObjectOutputStream(buf);
	    out.writeObject(maps);

	    // Store it via the Preferences API...
	    Preferences prefs = Preferences.userRoot().node(BASE_NODE);
	    prefs.putByteArray(TAG_DATA_KEY, buf.toByteArray());
	} catch (Exception ex) {
	    throw new RuntimeException("Unable to store preference!", ex);
	}
	// FIXME: I need to be able to store larger amounts of DATA!
	// FIXME: I should make the data I'm storing smaller...
    }

    /**
     *	<p> This method accesses the Tag Map.</p>
     */
    @SuppressWarnings("unchecked") 
    private static Map<String, List<Tag>>[] getTagMaps() {
	Map<String, List<Tag>>[] result = null;
	Preferences prefs = Preferences.userRoot().node(BASE_NODE);
	byte tagData[] = prefs.getByteArray(TAG_DATA_KEY, null);
	if (tagData == null) {
	    // Initialize it...
	    result = (Map<String, List<Tag>>[]) new Map[] {
		    new HashMap<String, List<Tag>>(),	// By Tag Name
		    new HashMap<String, List<Tag>>()	// By Page ID
		};
	} else {
	    try {
		ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(tagData));
		result = (Map<String, List<Tag>>[]) stream.readObject();
	    } catch (java.io.InvalidClassException ex) {
		throw new IllegalStateException(
		    "Perhaps you have an old Tag storage format?", ex);
	    } catch (Exception ex) {
		throw new IllegalStateException(
		    "Unable to read Tag information!", ex);
	    }
	}

	return result;
    }

    /**
     *	<p> This method searches the tags based on the given criteria.  Any of
     *	    the criteria may be null, meaning not to filter by that
     *	    criterion.</p>
     *
     *	@param  tagName     Name of the tag to find, not required. May be null
     *			    if tagViewId, or user is supplied -- or to return
     *			    all tags.
     *	@param  tagViewId   Unique id for the page to search for tags. May be
     *			    null (for all pages).
     *	@param  user        User id. From getUserPrincipal() in some cases?
     *			    Allow seeing tags created by specific users. May be
     *			    null (for all users).
     *
     *	@return	Returns the search results, or <code>null</code> if nothing
     *		is found.
     */
    public static List<Tag> queryTags(String tagName, String tagViewId, String user) {
	Map<String, List<Tag>>[] maps = getTagMaps();
	List<Tag> results = null;
	Tag testTag = null;

	// Make sure this is normalized...
	tagViewId = normalizeTagViewId(tagViewId);

	// Check out we should search...
	if (tagName != null) {
	    // We'll search first by TagName
	    results = maps[TAG_NAME_MAP_IDX].get(normalizeTagName(tagName));
	    if (results == null) {
		return null;
	    }

	    if (tagViewId != null) {
		// Now filter by tagViewId...
		Iterator<Tag> it = results.iterator();
		while (it.hasNext()) {
		    testTag = it.next();
		    if (!testTag.getTagViewId().equals(tagViewId)) {
			// Is not for the page, remove it from the result set
			it.remove();
		    }
		}
	    }
	} else if (tagViewId != null) {
	    // Search by tagViewId and maybe user (if !null)
	    results = maps[PAGE_MAP_IDX].get(tagViewId);
	} else {
	    // Include everything...
	    results = new ArrayList<Tag>();
	    Map<String, List<Tag>> map = maps[TAG_NAME_MAP_IDX];
            for(Map.Entry<String,List<Tag>> e : map.entrySet()){
		results.addAll(e.getValue());
	    }
	}

	// Finally filter out unwanted users, if applicable...
	if ((user != null) && (results != null)) {
	    Iterator<Tag> it = results.iterator();
	    while (it.hasNext()) {
		testTag = it.next();
		if (!testTag.containsUser(user)) {
		    // Does not contain the user, remove it from the result set
		    it.remove();
		}
	    }
	}

	// Make sure we have something to return...
	if ((results != null) && (results.size() == 0)) {
	    // We don't have anything to return!
	    results = null;
	}

	// Return the results of the search...
	return results;
    }

    /**
     *	<p> This method removes a Tag.</p>
     */
    public static void removeTag(String tagName, String tagViewId, String user) {
	if ((tagName == null) || (tagViewId == null) || (user == null)) {
	    throw new IllegalArgumentException("To remove a Tag, you "
		+ "must specify the tagName, tagViewId, and tag owner!");
	}
	tagName = normalizeTagName(tagName);
	tagViewId = normalizeTagViewId(tagViewId);

	// Find it...
	List<Tag> results = TagSupport.queryTags(tagName, tagViewId, user);

	// Should be at most 1 match, however, there may be multiple users.
	if (results.size() > 0) {
	    // Remove the 1st (and only) Tag...
	    Tag targetTag = results.get(0);
	    targetTag.removeUser(user);
	    // Save tag (if no more users, it will be deleted)
	    setTag(targetTag);
	}
    }

    /**
     *	<p> This method ensure that tags are compared w/o taking into account
     *	    case or whitespace.</p>
     */
    private static String normalizeTagName(String tagName) {
	if (tagName == null) {
	    throw new IllegalArgumentException("Tag name cannot be null!");
	}
	return tagName.replaceAll("\\s", "").toLowerCase(GuiUtil.guiLocale);
    }

    /**
     *	<p> tagViewId's are expected to be context relative paths.  These
     *	    tagViewId's may include QUERY_STRING parameters if they are used to
     *	    determine the content of the page.  This means, we must take extra
     *	    special care to normalize the order of important QUERY_STRING
     *	    properties.  We also need to ensure leading (or intermediate) '/'
     *	    characters are normalized, and that the extension is normalized
     *	    (this method will ensure all tagViewId's end in .jsf).</p>
     *
     *	<p> Case will be preserved.</p>
     */
    public static String normalizeTagViewId(String tagViewId) {
	if (tagViewId == null) {
	    return null;
	}

	// Split off the base/QS...
	tagViewId = tagViewId.trim();
	int idx = tagViewId.indexOf('?');
	String baseName = (idx == -1) ? tagViewId : tagViewId.substring(0, idx);
	String queryString = (idx == -1) ? "" : tagViewId.substring(idx+1);

	// Get rid of leading and extra '/' characters...
	StringTokenizer tokenizer = new StringTokenizer(baseName, "/");
	StringBuilder builder = new StringBuilder(tokenizer.nextToken());
	while (tokenizer.hasMoreTokens()) {
	    builder.append('/');
	    builder.append(tokenizer.nextToken());
	}
	baseName = builder.toString();

	// Normalize Extension...
	if (!baseName.endsWith(".jsf")) {
	    idx = baseName.lastIndexOf('.');
	    if (idx != -1) {
		// Replace existing extension with .jsf...
		baseName = baseName.substring(0, idx) + ".jsf";
	    }
	}

	// Split & sort the NVPs...
	if (queryString.length() > 0) {
	    tokenizer = new StringTokenizer(queryString, "&");
	    List<String> nvps = new ArrayList<String>();
	    while (tokenizer.hasMoreTokens()) {
		nvps.add(tokenizer.nextToken());
	    }

	    // Sort them...
	    Collections.sort(nvps);

	    // Rebuild the QS, now ordered...
	    builder = new StringBuilder(nvps.remove(0));
	    for (String nvp : nvps) {
		// Add the rest...
		builder.append('&');
		builder.append(nvp);
	    }
	    queryString = builder.toString();
	}

	// Reassemble the String...
	tagViewId = baseName
		+ ((queryString.length() > 0) ? ("?" + queryString) : "");
	return tagViewId;
    }

    /**
     *
     */
    public static void main(String args[]) {
	/*
	// Write
	ByteArrayOutputStream buf = new ByteArrayOutputStream();
	byte[] data = null;
	try {
	    ObjectOutputStream out = new ObjectOutputStream(buf);
	    out.writeObject("This is a test!");
	    data = buf.toByteArray();
	} catch (Exception ex) {
	    ex.printStackTrace();
	    System.exit(-1);
	}

	// Read
	//BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
	try {
	    ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(data));
	    System.out.println("Line == " + stream.readObject());
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
	*/

	TagSupport.addTag("bar", "deploy.jsf?a=z&c=e", "Display Name", "jane");
	TagSupport.addTag("bar", "/index.jsf?a=z&c=f", "Display Name", "jane");
	TagSupport.addTag("bar", "/index.jsf?a=z&c=e", "Display Name", "jane");
	TagSupport.addTag("bar", "deploy.jsf?a=z&c=g", "Display Name", "jane");
	TagSupport.addTag("bar", "indexdeployjsf?a=z&c=h", "Display Name", "jack");
	TagSupport.addTag("bar", "/index.jsf?deploy=z&c=i", "Display Name", "jack");
	TagSupport.addTag("foo", "/index.jsf?a=z&c=j", "Display Name", "jack");
	TagSupport.addTag("bar", "/index.jsf?a=z&c=e", "Display Name", "jane");
	TagSupport.addTag("bar", "/index.jsf?a=z&c=f", "Display Name", "jane");
	TagSupport.addTag("bar", "deploy.jsf?a=z&c=e", "Display Name", "jane");
	TagSupport.addTag("bar", "/index.jsf?a=z&c=g", "Display Name", "jane");
	TagSupport.addTag("bar", "deploy.jsf?a=z&c=h", "Display Name", "jack");
	TagSupport.addTag("bar", "/index.jsf?a=z&c=i", "Display Name", "jack");
	TagSupport.addTag("foo", "deploy.jsf?a=z&c=j", "Display Name", "jack");
	TagSupport.addTag("foo", "deploy.jsf?a=z&c=k", "Display Name", "jack");
	TagSupport.addTag("bat", "deploy.jsf?a=z&c=l", "Display Name", "jack");
	TagSupport.addTag("foo", "/index.jsf?a=z&c=m", "Display Name", "jack");
	TagSupport.addTag("foo", "deploy.jsf?a=z&c=k", "Display Name", "jack");
	TagSupport.addTag("bat", "deploy.jsf?a=z&c=l", "Display Name", "jack");
	TagSupport.addTag("foo", "deploy.jsf?a=z&c=m", "Display Name", "jack");
	TagSupport.addTag("bat", "deploy.jsf?a=z&c=n", "Display Name", "jack");
	TagSupport.addTag("bat", "/index.jsf?a=z&c=o", "Display Name", "bill");
	TagSupport.addTag("bat", "deploy.jsf?a=z&c=p", "Display Name", "bill");
	TagSupport.addTag("bat", "deploy.jsf?a=z&c=q", "Display Name", "bill");
	TagSupport.addTag("foo", "deploy.jsf?a=z&c=s", "Display Name", "bill");
	TagSupport.addTag("foo", "deploy.jsf?a=z&c=t", "Display Name", "jane");
	TagSupport.addTag("bat", "deploy.jsf?a=z&c=u", "Display Name", "jane");
	TagSupport.addTag("bat", "deploy.jsf?a=z&c=v", "Display Name", "jane");
	List<Tag> results = TagSupport.queryTags(null, null, null);
//	List<Tag> results = TagSupport.queryTags(null, null, "anonymous");
//	List<Tag> results = TagSupport.queryTags(null, null, "admin");
//	List<Tag> results = TagSupport.queryTags("bar", null, null);
//	List<Tag> results = TagSupport.queryTags("foo", null, null);
//	List<Tag> results = TagSupport.queryTags("foo", null, "anonymous");
//	List<Tag> results = TagSupport.queryTags("foo", null, "admin");
//	List<Tag> results = TagSupport.queryTags("foo", "/index.jsf?a=b&c=d", "admin");
//	List<Tag> results = TagSupport.queryTags("foo", "/index.jsf?a=b&c=e", "anonymous");
//	List<Tag> results = TagSupport.queryTags("bar", "/index.jsf?a=b&c=e", "admin");
//	List<Tag> results = TagSupport.queryTags(null, "/index.jsf?a=b&c=e", "admin");
//	List<Tag> results = TagSupport.queryTags(null, "/index.jsf?a=b&c=e", "anonymous");
//	List<Tag> results = TagSupport.queryTags("foo", "/index.jsf?a=b&c=d", null);
//	List<Tag> results = TagSupport.queryTags("bar", "/index.jsf?a=b&c=e", null);
//	List<Tag> results = TagSupport.queryTags(null, "/index.jsf?a=b&c=d", null);
	if (results != null) {
	    for (Tag tag : results) {
		System.out.println("Found==> " + tag);
	    }
	}
    }


    /**
     *	The array index for the Map of Tags by tag name.
     */
    private static final int TAG_NAME_MAP_IDX	= 0;

    /**
     *	The array index for the Map of Tags by page.
     */
    private static final int PAGE_MAP_IDX	= 1;


    /**
     *	<p> This is the base <em>Preferences</em> node for tags.</p>
     */
    public static final String BASE_NODE    = "/glassfish/tags";

    /**
     *	<p> This is the key used to access the tag data under the
     *	    {@link #BASE_NODE} <code>Java Preferences API</code> node.</p>
     */
    public static final String TAG_DATA_KEY = "tagData";
}
