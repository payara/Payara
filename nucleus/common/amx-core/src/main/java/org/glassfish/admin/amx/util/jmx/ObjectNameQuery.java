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

package org.glassfish.admin.amx.util.jmx;

import javax.management.ObjectName;
import java.util.Set;

public interface ObjectNameQuery
{
    /**
    Return the ObjectNames of all MBeans whose properties match all the specified
    regular expressions.  Both property names and values may be searched.

    A starting set may be specified by using an ObjectName pattern.
    This can greatly improve the performance of the search by restricting the
    set of MBeans which are examined; otherwise all registered MBeans must be examined.

    The regexNames[ i ] pattern corresponds to regexValues[ i ].  A value of null
    for any item is taken to mean "match anything".  Thus specifing null for
    'regexNames' means "match any name" and specifying regexNames[ i ] = null means
    to match only based on regexValues[ i ] (and vice versa).

    @param startingSet 			optional ObjectName pattern for starting set to search
    @param regexNames			optional series of regular expressions for Property names
    @param regexValues			optional series of regular expressions for Property values
    @return 					array of ObjectName (may be of zero length)
     */
    Set<ObjectName> matchAll(Set<ObjectName> startingSet, String[] regexNames, String[] regexValues);

    /**
    Return the ObjectNames of all MBeans whose properties match any of the specified
    regular expressions.  Both property names and values may be searched.

    A starting set may be specified by using an ObjectName pattern.
    This can greatly improve the performance of the search by restricting the
    set of MBeans which are examined; otherwise all registered MBeans must be examined.


    The regexNames[ i ] pattern corresponds to regexValues[ i ].  A value of null
    for any item is taken to mean "match anything".  Thus specifing null for
    'regexNames' means "match any name" and specifying regexNames[ i ] = null means
    to match only based on regexValues[ i ] (and vice versa).

    @param startingSet 			optional ObjectName pattern for starting set to search
    @param regexNames			optional series of regular expressions for Property names
    @param regexValues			optional series of regular expressions for Property values
    @return 					array of ObjectName (may be of zero length)
     */
    Set<ObjectName> matchAny(Set<ObjectName> startingSet, String[] regexNames, String[] regexValues);

}






