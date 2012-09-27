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

package org.glassfish.diagnostics.context;

import org.glassfish.contextpropagation.Location;

/**
 * The diagnostics Context is the object through which
 * diagnostics data relevant to the current task are
 * read and written.
 *
 * A task is a unit of work being executed by the java
 * process. Examples include
 * <ul>
 *      <li>responding to an external stimulus such
 * as a simple http request or web-service request</li>
 *     <li>executing a scheduled job</li>
 * </ul>
 * A parent task can create sub-tasks the completion of which
 * may or may not affect the execution of the parent task.
 * The diagnostics Context of the parent task will propagate
 * to the child sub-tasks.
 *
 * Diagnostics data include:
 * <ul>
 *     <li>Location: {@link org.glassfish.contextpropagation.Location}
 *     provides correlation between a task and its sub-task(s)</li>
 *     <li>Name-value pairs: Arbitrary name-value pairs that may
 *     be reported in diagnostics features such as logging,
 *     flight recordings, request sampling and tracing and so on.
 *     <ul>
 *         <li>Name: The name should use the standard java naming
 *         convention including package name. The name should
 *         be sufficiently clear that consumers of the data
 *         (e.g. the readers of log files, i.e. developers!)
 *         have some good starting point when interpreting the
 *         diagnostics data.</li>
 *         <li>Value: The value should be as concise as possible.</li>
 *     </ul>
 *     Only those name-value pairs marked for propagation will
 *     propagate to the diagnostics Contexts of sub-tasks.
 *     It is generally the case that data associated with a
 *     particular name will either always propagate or always
 *     not propagate - i.e. it is either usefully shared with
 *     child Contexts or only makes sense if kept private
 *     to one Context.
 *     </li>
 * </ul>
 *
 * The diagnostics Context of the currently executing task can
 * be obtained from the {@link ContextManager}.
 *
 * @see org.glassfish.contextpropagation.Location
 */
public interface Context
{
 /**
  * Get the Location of this Context.
  */
  public Location getLocation();

 /**
  * Put a named value in this Context.
  *
  * @param name The name of the item of data.
  * @param value The value of item of data.
  * @param propagates If true then the data item will be propagated.
  * @return The previous value associated with this name (if there is one).
  */
  public <T> T put(String name, String value, boolean propagates);

  /**
   * Put a named value in this Context.
   *
   * @param name The name of the item of data.
   * @param value The value of item of data.
   * @param propagates If true then the data item will be propagated.
   * @return The previous value associated with this name (if there is one).
   */
  public <T> T put(String name, Number value, boolean propagates);

  /**
   * Remove the named value from this Context.
   *
   * @param  name The name of the item of data.
   * @return The value being removed if there is one, otherwise null.
   */
  public <T> T remove(String name);

  /**
  * Get a named value from this Context.
  *
  * @param  name The name of the item of data.
  * @return The value associated with this name if there is one,
  *         otherwise null.
  */
  public <T> T get(String name);
}
