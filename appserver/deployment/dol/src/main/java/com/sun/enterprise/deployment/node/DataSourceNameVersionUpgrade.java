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

package com.sun.enterprise.deployment.node;

import java.util.HashMap;
import java.util.Map;

/**
 * This interface defines the processing used to upgrade
 * data-source-name to the latest version
 *
 * One element name is matched:
 * "weblogic-application/jdbc-connection-pool/data-source-name" is replaced by
 * "weblogic-application/jdbc-connection-pool/data-source-jndi-name".
 * @author  Gerald Ingalls
 * @version 
 */
public class DataSourceNameVersionUpgrade implements VersionUpgrade {
  private static String DATA_SOURCE_NAME =
    "weblogic-application/jdbc-connection-pool/data-source-name";
  private static String DATA_SOURCE_JNDI_NAME =
    "weblogic-application/jdbc-connection-pool/data-source-jndi-name";
  private Map<String,String> matches;
  public DataSourceNameVersionUpgrade() {
    matches = new HashMap<String,String>();
    init();
  }

  /**
   * Return the kind of processing to do
   * @return the kind of processing to do
   */
  public UpgradeType getUpgradeType() {
    return UpgradeType.REPLACE_ELEMENT;
  }

  /**
   * Initialize
   */
  public void init() {
    matches.put(DATA_SOURCE_NAME, null);
  }

  /**
   * Return the map of xml element to match
   * @return the map of xml element to match
   */
  public Map<String,String> getMatchXPath() {
    return matches;
  }

  /**
   * Return the replacement element name
   * @return the replacement element name
   */
  public String getReplacementElementName() {
    return DATA_SOURCE_JNDI_NAME;
  }

  /**
   * Return the replacement element value
   * @return the replacement element value
   */
  public String getReplacementElementValue() {
    return matches.get(DATA_SOURCE_NAME);
  }

  /**
   * Return whether the matched items are valid.
   * @return whether the matched items are valid.
   */
  public boolean isValid() {
    return true;
  }
}
