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

package org.glassfish.internal.logging;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Loggable objects are used when there is a need to collect log entry data
 * without actually logging the message until a later time. Type casting is weak
 * for the log parameters provided when constructing. The user must ensure that
 * the arguments associated with a particular log message are of the appropriate
 * type and in the correct order.
 */
public class Loggable {

  private static final String FORMAT_PREFIX = "[{0}:{1}]";

  private String id;
  private Object[] args;
  private Throwable thrown;
  private Logger logger;
  private Level level;

  private static Throwable getThrowable(Object[] args) {
    if (args == null)
      return null;
    int candidateIndex = args.length - 1;
    if (candidateIndex >= 0) {
      Object throwableCandidate = args[candidateIndex];
      if (throwableCandidate instanceof Throwable) {
        return (Throwable) throwableCandidate;
      }
    }
    return null;
  }

  /**
   * Constructor
   * @exclude
   */
  public Loggable(Level level, String id, Object[] args, Logger logger) {
    this.level = level;
    this.id = id;
    this.args = args;
    this.thrown = getThrowable(this.args);
    this.logger = logger;
  }

  /**
   * Log the message.
   */
  public String log() {
    LogRecord rec = new LogRecord(level, getMessage(false,false));
    if (thrown != null) {
      rec.setThrown(thrown);
    }
    logger.log(rec);// [i18n ok]
    return id;
  }

  /**
   * Gets the contents of the message body without appending a stack trace. This
   * is particularly useful when using the value of a loggables message as the
   * value when creating an exception.
   * 
   */
  public String getMessageBody() {
    return getMessage(true, false);
  }

  /**
   * Get the message in specified locale.
   */
  private String getMessage(boolean prefix, boolean addTrace) {

    StringBuffer sb = new StringBuffer();

    if (prefix) {
      Object[] preArgs = { getSubSystem(), id };
      sb.append(MessageFormat.format(FORMAT_PREFIX, preArgs));
    }
    sb.append(MessageFormat.format(getBody(), args));

    // if last arg was a throwable and addTrace is set, stick exception on
    // end
    if (addTrace && (thrown != null)) {
      sb.append("\n");
      sb.append(throwable2StackTrace(thrown));
    }

    return sb.toString();
  }

  private Object throwable2StackTrace(Throwable th) {
    ByteArrayOutputStream ostr = new ByteArrayOutputStream();
    th.printStackTrace(new PrintStream(ostr));
    return ostr.toString();
  }

  private String getBody() {
    return logger.getResourceBundle().getString(id);
  }

  private String getSubSystem() {
    return logger.getName();
  }

  /**
   * Get the message in current locale with [subsytem:id] prefix.
   */
  public String getMessage() {
    return getMessage(true, true);
  }

  /**
   * Get the message in current locale, no prefix.
   */
  public String getMessageText() {
    return getMessage(false, true);
  }

  /**
   * Get the message id.
   */
  public String getId() {
    return id;
  }

}
