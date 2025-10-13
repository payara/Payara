/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.admin.servermgmt.stringsubs.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.io.Writer;

import com.sun.enterprise.admin.servermgmt.xml.stringsubs.MemberEntry;

/**
 * Handles the creation of {@link Reader} and {@link Writer} for a
 * {@link MemberEntry} of an archive.
 *
 * @see FileSubstitutionHandler
 */
public class ArchiveMemberHandler implements ArchiveMember
{
    /** Reference to the parent archive wrapper. */
    private final ArchiveEntryWrapper archiveWrapper;
    private final FileSubstitutionHandler handler;

    /**
     * Constructs the {@link ArchiveMemberHandler} for the given input file.
     *
     * @param file Member entry that has to undergo string substitution.
     * @param wrapper Parent archive of the input file.
     * @throws FileNotFoundException If file is not found.
     */
    public ArchiveMemberHandler(File file, ArchiveEntryWrapper wrapper)
            throws FileNotFoundException {
        handler = file.length() > SubstitutionFileUtil.getInMemorySubstitutionFileSizeInBytes() ?
                new LargeFileSubstitutionHandler(file) : new SmallFileSubstitutionHandler(file);
        archiveWrapper = wrapper;
    }

    @Override
    public void finish() {
        handler.finish();
        getParent().notifyCompletion();
    }

    @Override
    public ArchiveEntryWrapper getParent() {
        return archiveWrapper;
    }

    @Override
    public String getName() {
        return handler.getName();
    }

    @Override
    public Reader getReader() {
        return handler.getReader();
    }

    @Override
    public Writer getWriter() {
        return handler.getWriter();
    }
}