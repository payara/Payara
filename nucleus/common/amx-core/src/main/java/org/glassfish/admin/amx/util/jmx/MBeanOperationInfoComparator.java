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

package org.glassfish.admin.amx.util.jmx;

import java.io.Serializable;
import javax.management.MBeanOperationInfo;
import org.glassfish.admin.amx.util.jmx.stringifier.MBeanFeatureInfoStringifierOptions;
import org.glassfish.admin.amx.util.jmx.stringifier.MBeanOperationInfoStringifier;

/**
Caution: this Comparator may be inconsistent with equals() because it ignores the description.
 */
public final class MBeanOperationInfoComparator
        implements java.util.Comparator<MBeanOperationInfo>, Serializable
{
    private static final MBeanOperationInfoStringifier OPERATION_INFO_STRINGIFIER =
            new MBeanOperationInfoStringifier(new MBeanFeatureInfoStringifierOptions(false, ","));

    public static final MBeanOperationInfoComparator INSTANCE = new MBeanOperationInfoComparator();

    private MBeanOperationInfoComparator()
    {
    }

    @Override
    public int compare(final MBeanOperationInfo info1, final MBeanOperationInfo info2)
    {
        // we just want to sort based on name and signature; there can't be two operations with the
        // same name and same signature, so as long as we include the name and signature the
        // sorting will always be consistent.
        int c = info1.getName().compareTo(info2.getName());
        if (c == 0)
        {
            // names the same, subsort on signature, first by number of params
            c = info1.getSignature().length - info2.getSignature().length;
            if (c == 0)
            {
                // names the same, subsort on signature, first by number of params
                c = MBeanOperationInfoStringifier.getSignature(info1).compareTo(
                        MBeanOperationInfoStringifier.getSignature(info2));
            }

        }

        return (c);
    }

    @Override
    public boolean equals(Object other)
    {
        return (other instanceof MBeanOperationInfoComparator);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        return hash;
    }

}
