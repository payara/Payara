/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.api.admin;

/** Events broker for AdminCommands. It can be used to inform everybody who 
 * listen. Any object can be event. If ReST Provider is registered for particular
 * type, it is also transfered to remote client.
 *
 * @author mmares
 */
public interface AdminCommandEventBroker<T> {
    
    /** Local events are not transfered to remote listener using SSE
     */
    public static final String LOCAL_EVENT_PREFIX = "local/";
    
    /** Fire event under defined name. Any object can be event. 
     * 
     * @param name Event name. Listener is registered to some name.
     * @param event Any object can be event
     */
    public void fireEvent(String name, Object event);
    
    /** Fire event under name of event.getClass.getName().
     * 
     * @param event Any object can be event.
     */
    public void fireEvent(Object event);
    
    /** Register Listener for admin command events. 
     * 
     * @param regexpForName listen to events with name valid to this regular expression.
     * @param listener Listener will be called
     */
    public void registerListener(String regexpForName, AdminCommandListener<T> listener);
    
    /** Remove registered listener.
     * 
     * @param listener Listener to remove
     */
    public void unregisterListener(AdminCommandListener listener);
    
    /** Returns true if exist exists registered listener for given eventName
     */
    public boolean listening(String eventName);
    
    /** Pack of utility methods related to this instance of event broker.
     */
    public EventBrokerUtils getUtils();
    
    /** Place relevant for utility methods
     */
    public interface EventBrokerUtils {
        
        public static final String USER_MESSAGE_NAME = "usermessage";
        
        public void sendMessage(String message);
        
    }
    
    /** Listener for AdminCommand events.
     * 
     * @param <T> Type of event
     */
    public interface AdminCommandListener<T> {
        
        public void onAdminCommandEvent(String name, T event);
        
    }
    
    public static class BrokerListenerRegEvent {
        public static final String EVENT_NAME_LISTENER_REG = LOCAL_EVENT_PREFIX + "listener/register";
        public static final String EVENT_NAME_LISTENER_UNREG = LOCAL_EVENT_PREFIX + "listener/unregister";
        
        private final AdminCommandEventBroker broker;
        private final AdminCommandListener listener;

        public BrokerListenerRegEvent(AdminCommandEventBroker broker, AdminCommandListener listener) {
            this.broker = broker;
            this.listener = listener;
        }

        public AdminCommandEventBroker getBroker() {
            return broker;
        }

        public AdminCommandListener getListener() {
            return listener;
        }
        
    }

}
