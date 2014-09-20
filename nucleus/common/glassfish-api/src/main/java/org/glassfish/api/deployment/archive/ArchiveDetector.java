package org.glassfish.api.deployment.archive;
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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


import org.jvnet.hk2.annotations.Contract;

import javax.inject.Singleton;

import java.io.IOException;

/**
 * {@link ArchiveHandler}s are considered part of container implementation, hence are
 * not available until the corresponding container is setup. On the other hand,
 * ArchiveDetectors are pretty light weight and used for selecting the
 * appropriate ArchiveHandler. ArchiveDetectors are supposed to be part of the
 * connector module of a container. Each detector has a rank as returned by {@link #rank()}
 * which can be used to order the detectors as archive detection.
 *
 * <p/>
 * This is a container pluggability interface.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
@Contract
@Singleton
public interface ArchiveDetector {
    // TODO(Sahoo): Should we merge handle & getArchiveHandler methods into one method?

    /**
     * Since archive detection logic is typically executed at a very early stage of deployment,
     * it is mainly heuristic. So some detectors can incorrectly recognize archives that they
     * actually don't support. e.g., take a war file inside an ear file. and asssume that the war file
     * contains some .jsp files. The archive detector responsible for handling the war
     * file could be fooled into thinking the ear file is a war file since it contains
     * jsp files, yet in reality, it only owns one of the sub archive bundled inside
     * the composite ear file.
     * To deal with such situations, each detector can specify a rank which can be used to order
     * the detectors. Since detectors can come from separate authors, rank of a detector must be
     * configurable in an installation.
     *
     * The order in which detectors are used during archive detection is based on the rank. Lower the integer value
     * as returned by this method, earlier it is used during detection.
     *
     * @return the rank of this detector
     */
    int rank();

    /**
     * This method is used to detect the archive type. If this detector can recognize the given archive, then
     * it must return true.
     *
     *
     * @param archive
     * @return
     * @throws IOException
     */
    boolean handles(ReadableArchive archive) throws IOException;

    /**
     * Return a ArchiveHandler that can handle the archive recognised by this ArchiveDetector.
     *
     * @return
     */
    ArchiveHandler getArchiveHandler();

    /**
     * Returns the type of the deployment unit or archive or module whichever way you want to call what's being
     * depoyed. Each archive handler is responsible for only one type of archive and the type of the archive is
     * represented by {@link ArchiveType}.
     *
     * @return the type of the archive or deployment unit that can be detected by this detector
     */
    ArchiveType getArchiveType();
}
