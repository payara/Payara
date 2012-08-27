/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.contextpropagation.adaptors;

import org.glassfish.contextpropagation.bootstrap.LoggerAdapter;
import org.junit.Test;

public class MockLoggerAdapter implements LoggerAdapter {
  // TODO TIP: Change the Level constant to control what is logged, use null to reduce output to a minimum
  static final Level LOGGING_LEVEL = null; // Level.WARN; 

  @Override
  public boolean isLoggable(Level level) {
    return _isLoggable(level);
  }

  @Override
  public void log(Level level, MessageID messageID, Object... args) {
    System.out.println(format(messageID.defaultMessage, args));

  }

  private String format(String defaultMessage, Object... args) {
    String formatString = defaultMessage.replaceAll("%([0-9]*)", "%$1\\$s"); // $1 refers to the group %1 is equivalent to %1$s
    return String.format(formatString, args);
  }

  @Override
  public void log(Level level, Throwable t, MessageID messageID, Object... args) {
    log(level, messageID, args);
    t.printStackTrace();
  }
  
  @Test
  public void testFormat() {
    debug(format("arg 1:%1, arg2: %2", "one", "two"));
  }
  
  private static boolean _isLoggable(Level level) {
    return LOGGING_LEVEL != null && level.ordinal() <= LOGGING_LEVEL.ordinal();
  }
  
  public static void debug(String s) {
    if (_isLoggable(Level.DEBUG)) System.out.println(s);
  }

}
