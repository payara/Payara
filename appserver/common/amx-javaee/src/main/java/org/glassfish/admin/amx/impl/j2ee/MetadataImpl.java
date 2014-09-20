/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amx.impl.j2ee;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.management.ObjectName;

public final class MetadataImpl implements Metadata {
    private final ConcurrentMap<String,Object>  mData;
    
    public MetadataImpl(final Map<String,Object> data )
    {
        mData = new ConcurrentHashMap<String,Object>(data);
    }

    public MetadataImpl( final Metadata meta)
    {
        this( meta.getAll() );
    }

    public MetadataImpl()
    {
        mData = new ConcurrentHashMap<String,Object>();
    }

    public Map<String,Object> getAll()
    {
        return Collections.unmodifiableMap(mData);
    }

    public void add( final String key, final Object value)
    {
        mData.put( key, value);
    }
    
    public void remove( final String key)
    {
        mData.remove( key );
    }
    

    public void setCorrespondingConfig( final ObjectName config)
    {
        add( CORRESPONDING_CONFIG, config);
    }
    
    public ObjectName getCorrespondingConfig()
    {
        return getMetadata( CORRESPONDING_CONFIG, ObjectName.class);
    }

    public ObjectName getCorrespondingRef()
    {
        return getMetadata( CORRESPONDING_REF, ObjectName.class);
    }

    public void setCorrespondingRef( final ObjectName config)
    {
        add( CORRESPONDING_REF, config);
    }
    
    public String getDeploymentDescriptor()
    {
        return getMetadata( DEPLOYMENT_DESCRIPTOR, String.class);
    }
    public void setDeploymentDescriptor(final String desc)
    {
        if ( desc == null )
        {
            throw new IllegalArgumentException( "setDeploymentDescriptor: null descriptor" );
        }
        add( DEPLOYMENT_DESCRIPTOR, desc);
    }
    
    public <T> T getMetadata(final String name, final Class<T> clazz)
    {
        final Object value = mData.get(name);
        
        return clazz.cast(value);
    }
}
