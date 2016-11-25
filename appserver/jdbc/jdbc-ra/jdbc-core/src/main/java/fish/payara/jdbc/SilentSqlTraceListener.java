/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.jdbc;

import org.glassfish.api.jdbc.SQLTraceListener;
import org.glassfish.api.jdbc.SQLTraceRecord;

/**
 *
 * @author Andrew Pielage
 */
public class SilentSqlTraceListener implements SQLTraceListener {
    
    @Override
    public void sqlTrace(SQLTraceRecord record) {
        // Do nothing, we want to be silent
    }
}
