/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package com.sun.enterprise.tools.classmodel;

public class Constants {

  /**
   * This is the target inhabitants file built.
   * <p>
   * Passed as a system property.
   */
  public static final String PARAM_INHABITANT_TARGET_FILE = "inhabitants.target.file";
  
  /**
   * This is the source inhabitants file read.
   * <p>
   * Passed as a system property.
   */
  public static final String PARAM_INHABITANT_SOURCE_FILE = "inhabitants.source.file";
  
  /**
   * This is the source files (jars | directories) to introspect and build a habitat for. 
   * <p>
   * Passed as a system property.
   */
  public static final String PARAM_INHABITANTS_SOURCE_FILES = "inhabitants.source.files";
  
  /**
   * This is the working classpath the introspection machinery will use to resolve
   * referenced contracts and annotations.  <b>Without this you may see a bogus
   * inhabitants file being generated.</b>  The indicator for this is a habitat with
   * only class names and missing indicies.
   * <p>
   * Passed as a system property.
   */
  public static final String PARAM_INHABITANTS_CLASSPATH = "inhabitants.classpath";

  /**
   * Set to true if the inhabitants should be sorted
   * <p>
   * Passed as a system property.
   */
  public static final String PARAM_INHABITANTS_SORTED = "inhabitants.sorted";
  
  /**
   * This is the optionally provided Advisor for pruning and/or caching the {@link #PARAM_INHABITANTS_CLASSPATH}.
   * <p>
   * Passed as a system property.
   */
  public static final String PARAM_INHABITANTS_CLASSPATH_ADVISOR = "inhabitants.classpath.advisor";
}
