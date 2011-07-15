/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * This holds the late status of the instance, the commands that are Queued up while the instance was starting
 * etc.
 *
 * @author Vijay Ramachandran
 */
public class InstanceState {
    public enum StateType {
        NO_RESPONSE {
            public String getDescription() {
                return "NO_RESPONSE";
            }

            public String getDisplayString() {
                return " no response";
            }
        },
        NOT_RUNNING {
            public String getDescription() {
                return "NOT_RUNNING";
            }

            public String getDisplayString() {
                return " not running";
            }
        },
        STARTING {
            public String getDescription() {
                return "STARTING";
            }

            public String getDisplayString() {
                return " starting";
            }
        },
        RUNNING {
            public String getDescription() {
                return "RUNNING";
            }

            public String getDisplayString() {
                return " running";
            }
        },
        RESTART_REQUIRED {
            public String getDescription() {
                return "REQUIRES_RESTART";
            }

            public String getDisplayString() {
                return " requires restart";
            }
        },
        NEVER_STARTED {
            public String getDescription() {
                return "NEVER_STARTED";
            }

            public String getDisplayString() {
                return " never started";
            }
        };

        public String getDescription() {
            return null;
        }

        public String getDisplayString() {
            return "NONE";
        }

        public static StateType makeStateType(String s) {
            for (StateType st : StateType.values()) {
                if (s.equals(st.getDescription())) return st;
            }
            return null;
        }
    };

    private StateType currentState;
    private List<String> failedCommands;

    public InstanceState(StateType st) {
        currentState = st;
        failedCommands = new ArrayList<String>();
    }

    public StateType getState() {
        return currentState;
    };

    public void setState(StateType state) {
        currentState = state;
    };

    public List<String> getFailedCommands() {
        return failedCommands;
    }

    public void addFailedCommands(String cmd) {
        if (currentState == StateType.NEVER_STARTED) {
            // do not keep track of failed commands for instances that
            // have never been started
            return;
        }
        failedCommands.add(cmd);
    }

    public void removeFailedCommands() {
        failedCommands.clear();
    }
}
