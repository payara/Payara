/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.monitor.registry;

/**
 * Provides enumerated constants related to various levels
 * at which monitoring could be set
 * @author  Shreedhar Ganapathy<mailto:shreedhar.ganapathy@sun.com>
 * @author  <a href="mailto:Kedar.Mhaswade@sun.com">Kedar Mhaswade</a>
 */
public class MonitoringLevel {

    public static final MonitoringLevel OFF  = new MonitoringLevel("OFF");
    public static final MonitoringLevel LOW  = new MonitoringLevel("LOW");
    public static final MonitoringLevel HIGH = new MonitoringLevel("HIGH");

    private final String name;
    
    /**
     * Constructor
     */
    private MonitoringLevel(String name ) {
        this.name = name;
    }
    
    public String toString() {
        return ( name );
    }
	
    /**
     * Returns an instance of MonitoringLevel for the given String.
     * The given String has to correspond to one of the public fields declared
     * in this class.
     *
     * @param name String representing the MonitoringLevel
     * @return MonitoringLevel corresponding to given parameter, or null
     * if the parameter is null or does not correspond to any of the
     * Monitoring Levels supported.
     * For $Revision: 1.2 $ of this class, "off", "high" and "low" are
     * supported strings. The comparison is done case insensitively.
     */
    public static MonitoringLevel instance(String name) {
        if (OFF.toString().equalsIgnoreCase(name))
            return ( OFF );
        else if (LOW.toString().equalsIgnoreCase(name))
            return ( LOW );
        else if (HIGH.toString().equalsIgnoreCase(name))
            return ( HIGH );
        return ( null );
    }

    /**
     * Checks two MonitoringLevel objects for equality.
     * 
     * <p>Checks that <i>obj</i> is a MonitoringLevel, and has the same name as
     * this object.
     * 
     * @param obj the object we are testing for equality with this object.
     * @return true if obj is a MonitoringLevel, and has the same name as this
     * MonitoringLevel object.
     */
    public boolean equals(Object obj) {
	if (obj == this)
	    return true;

	if (! (obj instanceof MonitoringLevel))
	    return false;

	MonitoringLevel that = (MonitoringLevel) obj;

	return (this.name.equals(that.name));
    }

    /**
     * Returns the hash code value for this object.
     *
     * <p>The hash code returned is the hash code of the name of this
     * MonitoringLevel object.
     *
     * @return Hash code value for this object.
     */
    public int hashCode() {
	return this.name.hashCode();
    }

}
