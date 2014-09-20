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

package org.glassfish.admin.amx.util.jmx.stringifier;

import org.glassfish.admin.amx.util.stringifier.Stringifier;

import javax.management.MBeanOperationInfo;

public class MBeanOperationInfoStringifier
        extends MBeanFeatureInfoStringifier implements Stringifier
{
    public static final MBeanOperationInfoStringifier DEFAULT = new MBeanOperationInfoStringifier();

    public MBeanOperationInfoStringifier()
    {
        super();
    }

    public MBeanOperationInfoStringifier(MBeanFeatureInfoStringifierOptions options)
    {
        super(options);
    }

    public static String getImpact(MBeanOperationInfo info)
    {
        String impactStr = null;

        switch (info.getImpact())
        {
            default:
                impactStr = "unknown";
                break;
            case MBeanOperationInfo.INFO:
                impactStr = "info";
                break;
            case MBeanOperationInfo.ACTION:
                impactStr = "action";
                break;
            case MBeanOperationInfo.ACTION_INFO:
                impactStr = "action-info";
                break;
        }

        return (impactStr);
    }

    public static String getSignature(MBeanOperationInfo info)
    {
        return (getSignature(info, MBeanFeatureInfoStringifierOptions.DEFAULT));
    }

    public static String getSignature(MBeanOperationInfo info, MBeanFeatureInfoStringifierOptions options)
    {
        return (paramsToString(info.getSignature(), options));
    }

    public static String getDescription(MBeanOperationInfo info)
    {
        return (sOperationDelimiter + "\"" + info.getDescription() + "\"");
    }

    public String stringify(Object o)
    {
        assert (o != null);
        final MBeanOperationInfo op = (MBeanOperationInfo) o;

        String result = getPresentationTypeString(op.getReturnType()) + " " + op.getName() + "(";

        // create the signature string
        result = result + getSignature(op, mOptions) + ")";

        String impactStr = getImpact(op);

        result = result + sOperationDelimiter + "impact=" + impactStr;

        if (mOptions.mIncludeDescription)
        {
            result = result + getDescription(op);
        }

        return (result);
    }

}
