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

package org.glassfish.contextpropagation.weblogic.workarea.spi;

import java.io.IOException;

import org.glassfish.contextpropagation.adaptors.MockLoggerAdapter;
import org.glassfish.contextpropagation.weblogic.workarea.WorkContext;
import org.glassfish.contextpropagation.weblogic.workarea.WorkContextInput;
import org.glassfish.contextpropagation.weblogic.workarea.WorkContextOutput;



/**
 * A basic implementation of <code>WorkAreaContext</code> 
 * @exclude
 */
public final class WorkContextEntryImpl implements WorkContextEntry
{
  public static final String[] PROP_NAMES = new String[] {
    "LOCAL",
    "WORK",
    "RMI",
    "TRANSACTION",
    "JMS_QUEUE",
    "JMS_TOPIC",
    "SOAP",
    "MIME_HEADER",
    "ONEWAY"
  };

  private String name;
  private int propagationMode;
  private WorkContext context;
  private boolean originator;

  @SuppressWarnings("unused")
  private WorkContextEntryImpl() { }
  
  public WorkContextEntryImpl(String name, WorkContext context,
                              int propagationMode) {
    this.name = name;
    this.context = context;
    this.propagationMode = propagationMode;
    this.originator = true;
  }

  private WorkContextEntryImpl(String name, WorkContextInput in)
    throws IOException, ClassNotFoundException 
  {
    this.name = name;
    propagationMode = in.readInt();
    context = in.readContext();
  }
  
  public WorkContext getWorkContext() {
    return context;
  }
  
  public int hashCode() {
    return name.hashCode();
  }

  public boolean equals(Object obj) {
    if (obj instanceof WorkContextEntry) {
      return ((WorkContextEntry)obj).getName().equals(name);
    }
    return false;
  }
  
  public String getName() {
    return name;
  }

  public int getPropagationMode() {
    return propagationMode;
  }

  public boolean isOriginator() {
    return originator;
  }

  public void write(WorkContextOutput out) throws IOException 
  {
    if (this == NULL_CONTEXT) {
      out.writeUTF("");
    }
    else {
      out.writeUTF(name);
      out.writeInt(propagationMode);
      out.writeContext(context);
    }
  }

  public static WorkContextEntry readEntry(WorkContextInput in) 
    throws IOException, ClassNotFoundException 
  {
    String name = in.readUTF();
    MockLoggerAdapter.debug("Read key: " + name);
    if (name.length() == 0) {
      return NULL_CONTEXT;
    }
    else {
      return new WorkContextEntryImpl(name, in);
    }
  }
  
  public String toString() {
    StringBuffer sb = new StringBuffer(name);
    sb.append(", ");
    int p = propagationMode;
    for (int i=0; i<9; i++) {
      if ((p >>>= 1) == 1) {
        sb.append(" | ").append(PROP_NAMES[i]);
      }
    }
    return sb.toString();
  }
}


