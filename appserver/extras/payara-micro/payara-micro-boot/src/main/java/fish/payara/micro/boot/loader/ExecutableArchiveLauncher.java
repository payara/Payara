/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Portions copyright 2016 Payara Foundation and/or its affiliates

package fish.payara.micro.boot.loader;

import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;

import fish.payara.micro.boot.loader.archive.Archive;
import fish.payara.micro.boot.loader.archive.Archive.Entry;
import fish.payara.micro.boot.loader.archive.Archive.EntryFilter;

/**
 * Base class for executable archive {@link Launcher}s.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public abstract class ExecutableArchiveLauncher extends Launcher {

	private final Archive archive;

	public ExecutableArchiveLauncher() {
		try {
			this.archive = createArchive();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	protected ExecutableArchiveLauncher(Archive archive) {
		this.archive = archive;
	}

	protected final Archive getArchive() {
		return this.archive;
	}

	@Override
	protected String getMainClass() throws Exception {
		Manifest manifest = this.archive.getManifest();
		String mainClass = null;
		if (manifest != null) {
			mainClass = manifest.getMainAttributes().getValue("Start-Class");
		}
		if (mainClass == null) {
			throw new IllegalStateException(
					"No 'Start-Class' manifest entry specified in " + this);
		}
		return mainClass;
	}

	@Override
	protected List<Archive> getClassPathArchives() throws Exception {
		List<Archive> archives = new ArrayList<Archive>(
				this.archive.getNestedArchives(new EntryFilter() {

					@Override
					public boolean matches(Entry entry) {
						return isNestedArchive(entry);
					}

				}));
		postProcessClassPathArchives(archives);
		return archives;
	}

	/**
	 * Determine if the specified {@link JarEntry} is a nested item that should be added
	 * to the classpath. The method is called once for each entry.
	 * @param entry the jar entry
	 * @return {@code true} if the entry is a nested item (jar or folder)
	 */
	protected abstract boolean isNestedArchive(Archive.Entry entry);

	/**
	 * Called to post-process archive entries before they are used. Implementations can
	 * add and remove entries.
	 * @param archives the archives
	 * @throws Exception if the post processing fails
	 */
	protected void postProcessClassPathArchives(List<Archive> archives) throws Exception {
	}

}
