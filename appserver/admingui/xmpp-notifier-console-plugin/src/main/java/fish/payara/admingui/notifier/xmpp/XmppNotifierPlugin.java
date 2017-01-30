/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.admingui.notifier.xmpp;

import java.net.URL;
import org.glassfish.api.admingui.ConsoleProvider;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Andrew Pielage
 */
@Service
public class XmppNotifierPlugin implements ConsoleProvider {
    
    @Override
    public URL getConfiguration() {
        return null;
    }
}
