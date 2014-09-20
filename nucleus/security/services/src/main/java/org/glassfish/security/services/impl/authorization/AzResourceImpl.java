/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.security.services.impl.authorization;

import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.security.services.api.authorization.AzResource;
import org.glassfish.security.services.api.common.Attributes;
import org.glassfish.security.services.impl.ServiceLogging;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AzResourceImpl extends AzAttributesImpl implements AzResource {


    private static final Logger logger = Logger.getLogger(ServiceLogging.SEC_SVCS_LOGGER,ServiceLogging.SHARED_LOGMESSAGE_RESOURCE);

    private static final boolean REPLACE = true;


    private final URI uri;


    /**
     * Constructor
     *
     * @param resource The represented resource
     * @throws IllegalArgumentException Given resource was null
     */
    public AzResourceImpl( URI resource )  {
        super(NAME);

        if ( null == resource ) {
            throw new IllegalArgumentException("Illegal null resource URI.");
        }
        this.uri = resource;

        // Dump the query parameters into the attribute map.
        addAttributesFromUriQuery( uri, this, !REPLACE );
    }


    /**
     * Determines the URI representing this resource.
     * @return The URI representing this resource., never null.
     */
    @Override
    public URI getUri() {
        return uri;
    }


    /**
     * Determines the URI used to initialize this resource.
     * @return The URI used to initialize this resource.
     */
    @Override
    public String toString() {
        return uri.toString();
    }


    /**
     * Yet another URI query parser, but this one knows how to populate
     * <code>{@link org.glassfish.security.services.api.common.Attributes}</code>.
     *
     * @param uri The URI from which the query will be derived.
     * @param attributes Attributes collection to populate
     * @param replace true to replace entire attribute, false to append value to attribute.
     * See <code>{@link org.glassfish.security.services.api.common.Attributes#addAttribute}</code>.
     * @throws IllegalArgumentException URI or Attributes is null.
     */
    static void addAttributesFromUriQuery( URI uri, Attributes attributes, boolean replace ) {
        if ( null == uri ) {
            throw new IllegalArgumentException( "Illegal null URI." );
        }
        if ( null == attributes ) {
            throw new IllegalArgumentException( "Illegal null Attributes." );
        }

        String query = uri.getRawQuery();
        if ( ( null != query ) && ( query.length() > 0 ) ) {
            String[] params = query.split( "&" );
            if ( ( null != params ) && ( params.length > 0 ) ) {
                for ( String nv : params ) {
                    if ( (null == nv) || (nv.length() <= 0) )  {
                        continue;
                    }

                    String name, value;
                    int equalsPos = nv.indexOf( "=" );
                    if ( -1 == equalsPos ) {
                        name = decodeURI( nv );
                        value = "";
                    } else {
                        name = decodeURI( nv.substring( 0, equalsPos ) );
                        value = decodeURI( nv.substring( equalsPos + 1 ) );
                    }

                    attributes.addAttribute( name, value, replace );
                }
            }
        }
    }


    /**
     * URI decode the input, assumes UTF-8 encoding.
     *
     * @param input The input to decode.
     * @return The decoded input, null returns null.
     */
    static String decodeURI( String input ) {
        if ( null == input ) {
            return null;
        }

        String output = input;
        try {
            output = URLDecoder.decode(input, "UTF-8");
        } catch ( UnsupportedEncodingException e ) {
            if ( logger.isLoggable( Level.WARNING ) ) {
                logger.log( Level.WARNING, URI_DECODING_ERROR, e.getLocalizedMessage() );
            }
        }

        return output;
    }
    
	@LogMessageInfo(
			message = "Unable to decode URI: {0}.",
			level = "WARNING")
	private static final String URI_DECODING_ERROR = "SEC-SVCS-00102";
}
