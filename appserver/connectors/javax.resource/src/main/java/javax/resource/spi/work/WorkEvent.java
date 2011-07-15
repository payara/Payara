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

package javax.resource.spi.work;

import java.lang.Object;
import java.lang.Runnable;
import java.lang.Exception;
import java.lang.Throwable;
import java.util.EventObject;

/**
 * This class models the various events that occur during the processing of
 * a <code>Work</code> instance.
 *
 * @version 1.0
 * @author  Ram Jeyaraman
 */
public class WorkEvent extends EventObject {

    /**
     * Indicates <code>Work</code> instance has been accepted.
     */
    public static final int WORK_ACCEPTED = 1;

    /**
     * Indicates <code>Work</code> instance has been rejected.
     */
    public static final int WORK_REJECTED = 2;

    /**
     * Indicates <code>Work</code> instance has started execution.
     */
    public static final int WORK_STARTED = 3;

    /**
     * Indicates <code>Work</code> instance has completed execution.
     */
    public static final int WORK_COMPLETED = 4;

    /**
     * The event type.
     */
    private int type;

    /**
     * The <code>Work</code> object on which the event occured.
     */
    private Work work;

    /**
     * The exception that occured during <code>Work</code> processing.
     */
    private WorkException exc;

    /**
     * The start delay duration (in milliseconds).
     */
    private long startDuration = WorkManager.UNKNOWN;

    /**
     * Constructor.
     *
     * @param source The object on which the event initially 
     * occurred.
     *
     * @param type The event type.
     *
     * @param work The <code>Work</code> object on which 
     * the event occured.
     *
     * @param exc The exception that occured during 
     * <code>Work</code> processing.

    */
    public WorkEvent(Object source, int type, Work work, WorkException exc) {
	super(source);
	this.type = type;
	this.work =  work;
	this.exc = exc;
    }

    /**
     * Constructor.
     *
     * @param source The object on which the event initially 
     * occurred.
     *
     * @param type The event type.
     *
     * @param work The <code>Work</code> object on which 
     * the event occured.
     *
     * @param exc The exception that occured during 
     * <code>Work</code> processing.
     *
     * @param startDuration The start delay duration 
     * (in milliseconds).
     */
    public WorkEvent(Object source, int type, Work work, WorkException exc,
            long startDuration) {
	this(source, type, work, exc);
	this.startDuration = startDuration;
    }

    /**
     * Return the type of this event.
     *
     * @return the event type.
     */
    public int getType() { return this.type; }

    /**
     * Return the <code>Work</code> instance which is the cause of the event.
     *
     * @return the <code>Work</code> instance.
     */
    public Work getWork() { return this.work; }

    /**
     * Return the start interval duration.
     *
     * @return the time elapsed (in milliseconds) since the <code>Work</code>
     * was accepted, until the <code>Work</code> execution started. Note, 
     * this does not offer real-time guarantees. It is valid to return -1, if
     * the actual start interval duration is unknown.
     */
    public long getStartDuration() { return this.startDuration; }

    /**
     * Return the <code>WorkException</code>. The actual 
     * <code>WorkException</code> subtype returned depends on the type of the
     * event.
     *
     * @return a <code>WorkRejectedException</code> or a 
     * <code>WorkCompletedException</code>, if any.
     */
    public WorkException getException() { return this.exc; }
}
