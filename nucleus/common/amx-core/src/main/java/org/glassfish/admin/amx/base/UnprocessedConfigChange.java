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

package org.glassfish.admin.amx.base;

import java.beans.PropertyChangeEvent;
import org.glassfish.admin.amx.util.ObjectUtil;
import org.glassfish.admin.amx.util.StringUtil;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;

/**
<em>Note: this API is highly volatile and subject to change<em>.
<p>
Class representing a change to a configuration attribute.
A PropertyChangeEvent is unsuitable, as its 'source' is transient.
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
public final class UnprocessedConfigChange
{
    private final String mName;

    private final String mOldValue;

    private final String mNewValue;

    private final Object mSource;

    private final String mReason;

    /** indicates that the change represents more than one property. The old/new values are arbitrary */
    public static final String MULTI = "*";

    public Object[] toArray()
    {
        return new Object[]
                {
                    mName, mOldValue, mNewValue, mSource, mReason
                };
    }

    /** must match the order in {@link #toArray} */
    public UnprocessedConfigChange(final Object[] data)
    {
        this((String) data[0], (String) data[1], (String) data[2], data[3], (String) data[4]);

        // nice to do this first, but compiler won't allow it!
        if (data.length != 5)
        {
            throw new IllegalArgumentException();
        }
    }

    public UnprocessedConfigChange(
            final String name,
            final String oldValue,
            final String newValue,
            final Object source,
            final String reason)
    {
        mReason = reason == null ? "unspecified" : reason;
        mName = name;
        mSource = source;
        mOldValue = oldValue;
        mNewValue = newValue;
    }

    public UnprocessedConfigChange(final String reason, final PropertyChangeEvent e)
    {
        this(e.getPropertyName(), "" + e.getOldValue(), "" + e.getNewValue(), e.getSource(), reason);
    }

    /** The (human readable) reason the change could not be made. */
    public String getReason()
    {
        return mReason;
    }

    /** name of the property.  Can be null */
    public String getPropertyName()
    {
        return mName;
    }

    /**
    Preferred value is an ObjectName, otherwise a String suitable for a user to understand
    what might have been affected.  Can be null.
     */
    public Object getSource()
    {
        return mSource;
    }

    /** Old value of the property.  Can be null */
    public String getOldValue()
    {
        return mOldValue;
    }

    /** New value of the property.  Can be null */
    public String getNewValue()
    {
        return mNewValue;
    }

    @Override
    public String toString()
    {
        return "UnprocessedConfigChange: name = " + getPropertyName() +
               ", source = " + getSource() +
               ", oldValue = " + StringUtil.quote( getOldValue() ) +
               ", newValue = " + StringUtil.quote( getNewValue() ) +
               ", reason = " + StringUtil.quote("" + getReason());
    }

    private boolean eq(final Object lhs, final Object rhs)
    {
        if (lhs == rhs)
        {
            return true;
        }

        return lhs != null ? lhs.equals(rhs) : false;

    }

    @Override
    public boolean equals(final Object rhs)
    {
        if (!(rhs instanceof UnprocessedConfigChange))
        {
            return false;
        }

        final UnprocessedConfigChange x = (UnprocessedConfigChange) rhs;

        return eq(mName, x.mName) &&
               eq(mOldValue, x.mOldValue) &&
               eq(mNewValue, x.mNewValue) &&
               eq(mSource, x.mSource) &&
               eq(mReason, x.mReason);
    }

    @Override
    public int hashCode()
    {
        return ObjectUtil.hashCode(mName, mOldValue, mNewValue, mSource, mReason);
    }

}



























