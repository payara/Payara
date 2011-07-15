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

/*
 * DatabaseOutputStream.java
 *
 * Created on Jan 14, 2003
 */


package com.sun.jdo.spi.persistence.generator.database;

import java.io.*;
import java.sql.*;
import java.util.ResourceBundle;
import org.glassfish.persistence.common.I18NHelper;
import com.sun.jdo.spi.persistence.utility.logging.Logger;


/*
 * Represents a database connection as an output stream.
 *
 * @author Jie Leng 
 */
public class DatabaseOutputStream extends OutputStream {
     /** The logger */
    private static final Logger logger =
            LogHelperDatabaseGenerator.getLogger();

    /** I18N message handler */
    private final static ResourceBundle messages =
            I18NHelper.loadBundle(DatabaseOutputStream.class);

    /** Connection to the database. */
    // XXX FIXME S/b final; make it so if we can get rid of setConnection.
    private Connection conn_ = null;

    // XXX FIXME Assert conn != null and directly set the value of conn;
    // remove setConnection (below)
    public DatabaseOutputStream(Connection conn) {
        super();
        setConnection(conn);
    }

    // XXX FIXME I think this is not needed.
    public DatabaseOutputStream() {
        super();
    }

    /**
     * Closes the database connection.
     */
    public void close() {
        try {
            // XXX test is not necessary once we assert not null in constructor
            if (conn_ != null) {
                conn_.commit();
                // Close the connection
                conn_.close();
            }

        } catch (SQLException e) {
        if (logger.isLoggable(Logger.FINE))
            logger.fine("Exception in cleanup", e); // NOI18N
        }
    }

    /**
     * Commits the database connection.
     */
    public void flush() {
        try {
            // XXX test is not necessary once we assert not null in constructor
            if (conn_ != null) {
                conn_.commit();
            }
        } catch (SQLException e) {
            if (logger.isLoggable(Logger.FINE))
               logger.fine("Exception in cleanup", e); // NOI18N
        }
    }

    /**
     * This method is not supported in DatabaseOutputStream because it
     * doesn't make sense to write a single int to a database stream.  So
     * always throws UnsupportedOperationException.
     * @throws UnsupportedOperationException
     */ 
    public void write(int b) {
        throw new UnsupportedOperationException(); 
    }

    /**
     * Executes the given statement in the database.
     * @param stmt SQL to be executed
     * @throws SQLException Thrown if there is a problem preparing stmt as a
     * statement, or in executing it.
     */
    public void write(String stmt) throws SQLException {
	// Check if stmt is empty (null), and abort if so.
        if (stmt == null || stmt.trim().length() == 0) {
            return;
        }

        PreparedStatement pstmt = conn_.prepareStatement(stmt);
        pstmt.execute();
    }

    // XXX FIXME Is this really necessary?  Delete if possible.
    public void setConnection(Connection conn) {
        conn_ = conn;
    }
}
