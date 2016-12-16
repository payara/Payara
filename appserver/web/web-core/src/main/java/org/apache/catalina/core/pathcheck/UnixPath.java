/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
// Portions Copyright [2016] [Payara Foundation and/or its affiliates]

package org.apache.catalina.core.pathcheck;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Iterator;

/**
 * bare minimum taken from JDK to normalize file names
 *
 * @author lprimak
 */
class UnixPath implements Path {
    private final UnixFilesystem fs;

    // internal representation
    private final byte[] path;

    private static ThreadLocal<SoftReference<CharsetEncoder>> encoder
            = new ThreadLocal<>();

    // array of offsets of elements in path (created lazily)
    private volatile int[] offsets;
    // String representation (created lazily)
    private volatile String stringValue;


    public UnixPath(UnixFilesystem fs, String input) {
        // removes redundant slashes and checks for invalid characters
        this(fs, encode(normalizeAndCheck(input)));
    }

    UnixPath(UnixFilesystem fs, byte[] path) {
        this.fs = fs;
        this.path = path;
    }

    // create offset list if not already created
    private void initOffsets() {
        if (offsets == null) {
            int count, index;

            // count names
            count = 0;
            index = 0;
            if (isEmpty()) {
                // empty path has one name
                count = 1;
            } else {
                while (index < path.length) {
                    byte c = path[index++];
                    if (c != '/' && c != '\\') {
                        count++;
                        while (index < path.length && path[index] != '/' && path[index] != '\\') {
                            index++;
                        }
                    }
                }
            }

            // populate offsets
            int[] result = new int[count];
            count = 0;
            index = 0;
            while (index < path.length) {
                byte c = path[index];
                if (c == '/' || c == '\\') {
                    index++;
                } else {
                    result[count++] = index++;
                    while (index < path.length && path[index] != '/' && path[index] != '\\') {
                        index++;
                    }
                }
            }
            synchronized (this) {
                if (offsets == null) {
                    offsets = result;
                }
            }
        }
    }

    // package-private
    // removes redundant slashes and check input for invalid characters
    static String normalizeAndCheck(String input) {
        int n = input.length();
        char prevChar = 0;
        for (int i = 0; i < n; i++) {
            char c = input.charAt(i);
            if ((c == '/' || c == '\\') && (prevChar == '/' || prevChar == '\\')) {
                return normalize(input, n, i - 1);
            }
            checkNotNul(input, c);
            prevChar = c;
        }
        if (prevChar == '/' || prevChar == '\\') {
            return normalize(input, n, n - 1);
        }
        return input;
    }

    private static String normalize(String input, int len, int off) {
        if (len == 0) {
            return input;
        }
        int n = len;
        while ((n > 0) && (input.charAt(n - 1) == '/' || input.charAt(n - 1) == '\\')) {
            n--;
        }
        if (n == 0) {
            return "/";
        }
        StringBuilder sb = new StringBuilder(input.length());
        if (off > 0) {
            sb.append(input.substring(0, off));
        }
        char prevChar = 0;
        for (int i = off; i < n; i++) {
            char c = input.charAt(i);
            if ((c == '/' || c == '\\') && (prevChar == '/' || prevChar == '\\')) {
                continue;
            }
            checkNotNul(input, c);
            sb.append(c);
            prevChar = c;
        }
        return sb.toString();
    }

    private static void checkNotNul(String input, char c) {
        if (c == '\u0000') {
            throw new InvalidPathException(input, "Nul character not allowed");
        }
    }

    // encodes the given path-string into a sequence of bytes
    private static byte[] encode(String input) {
        SoftReference<CharsetEncoder> ref = encoder.get();
        CharsetEncoder ce = (ref != null) ? ref.get() : null;
        if (ce == null) {
            ce = Charset.defaultCharset().newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            encoder.set(new SoftReference<>(ce));
        }

        char[] ca = input.toCharArray();

        // size output buffer for worse-case size
        byte[] ba = new byte[(int) (ca.length * (double) ce.maxBytesPerChar())];

        // encode
        ByteBuffer bb = ByteBuffer.wrap(ba);
        CharBuffer cb = CharBuffer.wrap(ca);
        ce.reset();
        CoderResult cr = ce.encode(cb, bb, true);
        boolean error;
        if (!cr.isUnderflow()) {
            error = true;
        } else {
            cr = ce.flush(bb);
            error = !cr.isUnderflow();
        }
        if (error) {
            throw new InvalidPathException(input,
                    "Malformed input or input contains unmappable chacraters");
        }

        // trim result to actual length if required
        int len = bb.position();
        if (len != ba.length) {
            ba = Arrays.copyOf(ba, len);
        }

        return ba;
    }

    // returns {@code true} if this path is an empty path
    private boolean isEmpty() {
        return path.length == 0;
    }

    // returns an empty path
    private UnixPath emptyPath() {
        return new UnixPath(fs, new byte[0]);
    }

    @Override
    public FileSystem getFileSystem() {
        return fs;
    }

    @Override
    public boolean isAbsolute() {
        return (path.length > 0 && (path[0] == '/' || path[0] == '\\'));
    }

    @Override
    public int getNameCount() {
        initOffsets();
        return offsets.length;
    }

    @Override
    public Path normalize() {
        final int count = getNameCount();
        if (count == 0) {
            return this;
        }

        boolean[] ignore = new boolean[count];      // true => ignore name
        int[] size = new int[count];                // length of name
        int remaining = count;                      // number of names remaining
        boolean hasDotDot = false;                  // has at least one ..
        boolean isAbsolute = isAbsolute();

        // first pass:
        //   1. compute length of names
        //   2. mark all occurences of "." to ignore
        //   3. and look for any occurences of ".."
        for (int i = 0; i < count; i++) {
            int begin = offsets[i];
            int len;
            if (i == (offsets.length - 1)) {
                len = path.length - begin;
            } else {
                len = offsets[i + 1] - begin - 1;
            }
            size[i] = len;

            if (path[begin] == '.') {
                if (len == 1) {
                    ignore[i] = true;  // ignore  "."
                    remaining--;
                } else if (path[begin + 1] == '.') // ".." found
                {
                    hasDotDot = true;
                }
            }
        }

        // multiple passes to eliminate all occurences of name/..
        if (hasDotDot) {
            int prevRemaining;
            do {
                prevRemaining = remaining;
                int prevName = -1;
                for (int i = 0; i < count; i++) {
                    if (ignore[i]) {
                        continue;
                    }

                    // not a ".."
                    if (size[i] != 2) {
                        prevName = i;
                        continue;
                    }

                    int begin = offsets[i];
                    if (path[begin] != '.' || path[begin + 1] != '.') {
                        prevName = i;
                        continue;
                    }

                    // ".." found
                    if (prevName >= 0) {
                        // name/<ignored>/.. found so mark name and ".." to be
                        // ignored
                        ignore[prevName] = true;
                        ignore[i] = true;
                        remaining = remaining - 2;
                        prevName = -1;
                    } else // Case: /<ignored>/.. so mark ".." as ignored
                    {
                        if (isAbsolute) {
                            boolean hasPrevious = false;
                            for (int j = 0; j < i; j++) {
                                if (!ignore[j]) {
                                    hasPrevious = true;
                                    break;
                                }
                            }
                            if (!hasPrevious) {
                                // all proceeding names are ignored
                                ignore[i] = true;
                                remaining--;
                            }
                        }
                    }
                }
            } while (prevRemaining > remaining);
        }

        // no redundant names
        if (remaining == count) {
            return this;
        }

        // corner case - all names removed
        if (remaining == 0) {
            return isAbsolute ? new UnixPath(fs, File.separator) : emptyPath();
        }

        // compute length of result
        int len = remaining - 1;
        if (isAbsolute) {
            len++;
        }

        for (int i = 0; i < count; i++) {
            if (!ignore[i]) {
                len += size[i];
            }
        }
        byte[] result = new byte[len];

        // copy names into result
        int pos = 0;
        if (isAbsolute) {
            result[pos++] = (byte) File.separatorChar;
        }
        for (int i = 0; i < count; i++) {
            if (!ignore[i]) {
                System.arraycopy(path, offsets[i], result, pos, size[i]);
                pos += size[i];
                if (--remaining > 0) {
                    result[pos++] = (byte) File.separatorChar;
                }
            }
        }
        return new UnixPath(fs, result);
    }

    @Override
    public String toString() {
        // OK if two or more threads create a String
        if (stringValue == null) {
            stringValue = new String(path);     // platform encoding
        }
        return stringValue;
    }

    @Override
    public Path getRoot() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Path getFileName() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Path getParent() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Path getName(int index) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean startsWith(Path other) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean startsWith(String other) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean endsWith(Path other) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean endsWith(String other) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Path resolve(Path other) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Path resolve(String other) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Path resolveSibling(Path other) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Path resolveSibling(String other) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Path relativize(Path other) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public URI toUri() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Path toAbsolutePath() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public File toFile() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Iterator<Path> iterator() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int compareTo(Path other) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
