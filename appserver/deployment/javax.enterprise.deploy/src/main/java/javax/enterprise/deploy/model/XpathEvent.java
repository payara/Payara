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

package javax.enterprise.deploy.model;

import java.beans.PropertyChangeEvent;

/**
 * An Event class describing ConfigBeans being added/subtracted
 * from a server configuration.
 */
public final class XpathEvent {

   private final DDBean bean;
   private final Object typ;
   private PropertyChangeEvent changeEvent;

   /**
    * Adding a DDBean
    */   
   public static final Object BEAN_ADDED = new Object();
   /**
    * Removing a DDBean
    */   
   public static final Object BEAN_REMOVED = new Object();
   /**
    * Changing a DDBean
    */
   public static final Object BEAN_CHANGED = new Object();

   /**
    * A description of a change in the ConfigBean tree.
    * @param bean The ConfigBean being added/removed.
    * @param typ Indicates an add/remove event.
    */   
   public XpathEvent(DDBean bean, Object typ) {
       this.bean = bean; this.typ = typ;
       }

   public PropertyChangeEvent getChangeEvent() {
       if(typ == BEAN_CHANGED) return changeEvent;
       return null;
   }
   
   public void setChangeEvent(PropertyChangeEvent pce) {
       changeEvent = pce;
   }
   
       /**
        * The bean being added/removed/changed.
        * @return The bean being added/removed/changed.
        */       
   public DDBean getBean() {return bean;}

   /** Is this an add event?
    * @return true if this is an add event.
    */   
   public boolean isAddEvent() {return typ == BEAN_ADDED;}

   /** Is this a remove event?
    * @return true if this is a remove event.
    */   
   public boolean isRemoveEvent() {return typ == BEAN_REMOVED;}

   /** Is this a change event?
    * @return true if this is a change event.
    */   
   public boolean isChangeEvent() {return typ == BEAN_CHANGED;}

   }
