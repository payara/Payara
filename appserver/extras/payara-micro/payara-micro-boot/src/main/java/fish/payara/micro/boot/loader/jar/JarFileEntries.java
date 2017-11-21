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

package fish.payara.micro.boot.loader.jar;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;

import fish.payara.micro.boot.loader.data.RandomAccessData;
import fish.payara.micro.boot.loader.data.RandomAccessData.ResourceAccess;

/**
 * Provides access to entries from a {@link JarFile}. In order to reduce memory
 * consumption entry details are stored using int arrays. The {@code hashCodes} array
 * stores the hash code of the entry name, the {@code centralDirectoryOffsets} provides
 * the offset to the central directory record and {@code positions} provides the original
 * order position of the entry. The arrays are stored in hashCode order so that a binary
 * search can be used to find a name.
 * <p>
 * A typical Spring Boot application will have somewhere in the region of 10,500 entries
 * which should consume about 122K.
 *
 * @author Phillip Webb
 */
class JarFileEntries implements CentralDirectoryVisitor, Iterable<JarEntry> {

	private static final long LOCAL_FILE_HEADER_SIZE = 30;

	private static final String SLASH = "/";

	private static final String NO_SUFFIX = "";

	protected static final int ENTRY_CACHE_SIZE = 25;

	private final JarFile jarFile;

	private final JarEntryFilter filter;

	private RandomAccessData centralDirectoryData;

	private int size;

	private int[] hashCodes;

	private int[] centralDirectoryOffsets;

	private int[] positions;

	private final Map<Integer, FileHeader> entriesCache = Collections
			.synchronizedMap(new LinkedHashMap<Integer, FileHeader>(16, 0.75f, true) {

				@Override
				protected boolean removeEldestEntry(
						Map.Entry<Integer, FileHeader> eldest) {
					if (JarFileEntries.this.jarFile.isSigned()) {
						return false;
					}
					return size() >= ENTRY_CACHE_SIZE;
				}

			});

	JarFileEntries(JarFile jarFile, JarEntryFilter filter) {
		this.jarFile = jarFile;
		this.filter = filter;
	}

	@Override
	public void visitStart(CentralDirectoryEndRecord endRecord,
			RandomAccessData centralDirectoryData) {
		int maxSize = endRecord.getNumberOfRecords();
		this.centralDirectoryData = centralDirectoryData;
		this.hashCodes = new int[maxSize];
		this.centralDirectoryOffsets = new int[maxSize];
		this.positions = new int[maxSize];
	}

	@Override
	public void visitFileHeader(CentralDirectoryFileHeader fileHeader, int dataOffset) {
		AsciiBytes name = applyFilter(fileHeader.getName());
		if (name != null) {
			add(name, fileHeader, dataOffset);
		}
	}

	private void add(AsciiBytes name, CentralDirectoryFileHeader fileHeader,
			int dataOffset) {
		this.hashCodes[this.size] = name.hashCode();
		this.centralDirectoryOffsets[this.size] = dataOffset;
		this.positions[this.size] = this.size;
		this.size++;
	}

	@Override
	public void visitEnd() {
		sort(0, this.size - 1);
		int[] positions = this.positions;
		this.positions = new int[positions.length];
		for (int i = 0; i < this.size; i++) {
			this.positions[positions[i]] = i;
		}
	}

	private void sort(int left, int right) {
		// Quick sort algorithm, uses hashCodes as the source but sorts all arrays
		if (left < right) {
			int pivot = this.hashCodes[left + (right - left) / 2];
			int i = left;
			int j = right;
			while (i <= j) {
				while (this.hashCodes[i] < pivot) {
					i++;
				}
				while (this.hashCodes[j] > pivot) {
					j--;
				}
				if (i <= j) {
					swap(i, j);
					i++;
					j--;
				}
			}
			if (left < j) {
				sort(left, j);
			}
			if (right > i) {
				sort(i, right);
			}
		}
	}

	private void swap(int i, int j) {
		swap(this.hashCodes, i, j);
		swap(this.centralDirectoryOffsets, i, j);
		swap(this.positions, i, j);
	}

	private void swap(int[] array, int i, int j) {
		int temp = array[i];
		array[i] = array[j];
		array[j] = temp;
	}

	@Override
	public Iterator<JarEntry> iterator() {
		return new EntryIterator();
	}

	public boolean containsEntry(String name) {
		return getEntry(name, FileHeader.class, true) != null;
	}

	public JarEntry getEntry(String name) {
		return getEntry(name, JarEntry.class, true);
	}

	public InputStream getInputStream(String name, ResourceAccess access)
			throws IOException {
		FileHeader entry = getEntry(name, FileHeader.class, false);
		return getInputStream(entry, access);
	}

	public InputStream getInputStream(FileHeader entry, ResourceAccess access)
			throws IOException {
		if (entry == null) {
			return null;
		}
		InputStream inputStream = getEntryData(entry).getInputStream(access);
		if (entry.getMethod() == ZipEntry.DEFLATED) {
			inputStream = new ZipInflaterInputStream(inputStream, (int) entry.getSize());
		}
		return inputStream;
	}

	public RandomAccessData getEntryData(String name) throws IOException {
		FileHeader entry = getEntry(name, FileHeader.class, false);
		if (entry == null) {
			return null;
		}
		return getEntryData(entry);
	}

	private RandomAccessData getEntryData(FileHeader entry) throws IOException {
		// aspectjrt-1.7.4.jar has a different ext bytes length in the
		// local directory to the central directory. We need to re-read
		// here to skip them
		RandomAccessData data = this.jarFile.getData();
		byte[] localHeader = Bytes.get(
				data.getSubsection(entry.getLocalHeaderOffset(), LOCAL_FILE_HEADER_SIZE));
		long nameLength = Bytes.littleEndianValue(localHeader, 26, 2);
		long extraLength = Bytes.littleEndianValue(localHeader, 28, 2);
		return data.getSubsection(entry.getLocalHeaderOffset() + LOCAL_FILE_HEADER_SIZE
				+ nameLength + extraLength, entry.getCompressedSize());
	}

	private <T extends FileHeader> T getEntry(String name, Class<T> type,
			boolean cacheEntry) {
		int hashCode = AsciiBytes.hashCode(name);
		T entry = getEntry(hashCode, name, NO_SUFFIX, type, cacheEntry);
		if (entry == null) {
			hashCode = AsciiBytes.hashCode(hashCode, SLASH);
			entry = getEntry(hashCode, name, SLASH, type, cacheEntry);
		}
		return entry;
	}

	private <T extends FileHeader> T getEntry(int hashCode, String name, String suffix,
			Class<T> type, boolean cacheEntry) {
		int index = getFirstIndex(hashCode);
		while (index >= 0 && index < this.size && this.hashCodes[index] == hashCode) {
			T entry = getEntry(index, type, cacheEntry);
			if (entry.hasName(name, suffix)) {
				return entry;
			}
			index++;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T extends FileHeader> T getEntry(int index, Class<T> type,
			boolean cacheEntry) {
		try {
			FileHeader cached = this.entriesCache.get(index);
			FileHeader entry = (cached != null ? cached
					: CentralDirectoryFileHeader.fromRandomAccessData(
							this.centralDirectoryData,
							this.centralDirectoryOffsets[index], this.filter));
			if (CentralDirectoryFileHeader.class.equals(entry.getClass())
					&& type.equals(JarEntry.class)) {
				entry = new JarEntry(this.jarFile, (CentralDirectoryFileHeader) entry);
			}
			if (cacheEntry && cached != entry) {
				this.entriesCache.put(index, entry);
			}
			return (T) entry;
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private int getFirstIndex(int hashCode) {
		int index = Arrays.binarySearch(this.hashCodes, 0, this.size, hashCode);
		if (index < 0) {
			return -1;
		}
		while (index > 0 && this.hashCodes[index - 1] == hashCode) {
			index--;
		}
		return index;
	}

	public void clearCache() {
		this.entriesCache.clear();
	}

	private AsciiBytes applyFilter(AsciiBytes name) {
		return (this.filter == null ? name : this.filter.apply(name));
	}

	/**
	 * Iterator for contained entries.
	 */
	private class EntryIterator implements Iterator<JarEntry> {

		private int index = 0;

		@Override
		public boolean hasNext() {
			return this.index < JarFileEntries.this.size;
		}

		@Override
		public JarEntry next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			int entryIndex = JarFileEntries.this.positions[this.index];
			this.index++;
			return getEntry(entryIndex, JarEntry.class, true);
		}
                
                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

	}

}
