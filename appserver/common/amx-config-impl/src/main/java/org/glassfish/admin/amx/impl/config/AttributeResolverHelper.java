/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amx.impl.config;

import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.config.AMXConfigProxy;
import org.glassfish.admin.amx.intf.config.SystemProperty;
import org.glassfish.admin.amx.intf.config.SystemPropertiesAccess;

import org.jvnet.hk2.config.TranslationException;
import org.jvnet.hk2.config.VariableResolver;

import java.util.Map;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;


/**
	Helper to resolve attribute configuration values eg ${com.sun.aas.installRoot} once they have
    already been obtained in "raw" form.  If the goal is to fetch the attribute values in
    already-resolved form, do so directly via @{link AttributeResolver#resolveAttribute}.
    <p>
    Values can be resolved into String, boolean or int.  
    <p>
    Example usage:</b>
    <pre>
    HTTPListenerConfig l = ...; // or any AMXConfigProxy sub-interface
    AttributeResolverHelper h = new AttributeResolverHelper( l );
    int port = h.resolveInt( l.getPort() );
    </pre>
    Alternately, the static method form can be used:<br>
    <pre>
    HTTPListenerConfig l = ...; // or any AMXConfigProxy sub-interface
    int port = AttributeResolverHelper.resolveInt( l, value );
    </pre>
    The value can also be pre-resolved by calling
    {@link org.glassfish.admin.amx.intf.config.AttributeResolver#resolveAttribute}
    @see org.glassfish.admin.amx.intf.config.AttributeResolver
 */
@Taxonomy( stability=Stability.UNCOMMITTED )
public class AttributeResolverHelper extends VariableResolver
{
    private static void debug( final String s ) { System.out.println("##### " + s); }
    
    private final AMXConfigProxy mTarget;
    
    public AttributeResolverHelper( final AMXConfigProxy amx)
    {
        mTarget = amx;
    }
    
        protected String
    getVariableValue(final String varName) throws TranslationException
    {
        String result = varName;
        
        // first look for a system property
        final Object value = System.getProperty( varName );
        if ( value != null )
        {
            result = "" + value;
        }
        else
        {
            // Look successively at Containers for SystemProperties
            AMXProxy amx = mTarget;
            while ( amx != null && (amx instanceof AMXConfigProxy) && result == null )
            {
                if ( amx instanceof SystemPropertiesAccess )
                {
                    final Map<String,SystemProperty> props = ((SystemPropertiesAccess)amx).getSystemProperty();
                    
                    // look by calling getName().  We can't just look in the map, because the ObjectName
                    // might not allow some characters that might be allowed in the name field
                    for( final SystemProperty prop : props.values() )
                    {
                        if ( prop.getName().equals( varName ) )
                        {
                            result = prop.getValue();
                            break;
                        }
                    }
                }
                // continue up the containment hierarchy until we run out of config objects
                amx = amx.parent();
            }
        }
        
        return result;
    }
    
    
    /**
        Return true if the string is a template string of the for ${...}
     */
        public static boolean
    needsResolving( final String value )
    {
        return value != null && value.indexOf( "${" ) >= 0;
    }
    
            
    /**
        Resolve the String using the target resolver (MBean).
     */
        public String
    resolve( final String in ) throws TranslationException
    {
        final String result = translate(in);
        
        //debug( "AttributeResolverHelper.resolve(): " + in + " ===> " + result );
        
        return result;
    }
}




