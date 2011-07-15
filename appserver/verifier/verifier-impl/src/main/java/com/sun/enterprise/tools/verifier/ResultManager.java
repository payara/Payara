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

package com.sun.enterprise.tools.verifier;


import java.util.Vector;
import java.util.logging.LogRecord;

/**
 * This class is responsible for collecting the result and error data
 *
 * @author Sudipto Ghosh
 */

public class ResultManager {

    public void add(Result result) {
        Result r = result;
        addToResult(r);
    }

    public void log(LogRecord logRecord) {
        error.add(logRecord);
    }

    public int getFailedCount() {
        return (failedResults == null) ? 0 : failedResults.size();
    }

    public int getWarningCount() {
        return (warningResults == null) ? 0 : warningResults.size();
    }

    public int getErrorCount() {
        return (error == null) ? 0 : error.size();
    }

    public Vector getFailedResults() {
        return failedResults;
    }

    public Vector getOkayResults() {
        return okayResults;
    }

    public Vector getWarningResults() {
        return warningResults;
    }

    public Vector getNaResults() {
        return naResults;
    }

    public Vector getError() {
        return error;
    }

    /**
     * add the result object to specific Vector based on the status.
     *
     * @param r
     */
    private void addToResult(Result r) {
        if (r.getStatus() == Result.FAILED) {
            failedResults.add(r);
        } else if (r.getStatus() == Result.PASSED) {
            okayResults.add(r);
        } else if (r.getStatus() == Result.WARNING) {
            warningResults.add(r);
        } else if ((r.getStatus() == Result.NOT_APPLICABLE) ||
                (r.getStatus() == Result.NOT_RUN) ||
                (r.getStatus() == Result.NOT_IMPLEMENTED)) {
            naResults.add(r);
        }
    }

    private Vector<Result> failedResults = new Vector<Result>();
    private Vector<Result> okayResults = new Vector<Result>();
    private Vector<Result> warningResults = new Vector<Result>();
    private Vector<Result> naResults = new Vector<Result>();
    private Vector<LogRecord> error = new Vector<LogRecord>();
}
