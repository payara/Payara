package org.glassfish.elasticity.expression;

import org.glassfish.elasticity.group.ElasticMessageHandler;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.List;

/**
 * @author Mahesh.Kannan@Oracle.Com
 */
public class RemoteExpressionHandler
    implements ElasticMessageHandler {

    private String token;

    public RemoteExpressionHandler(String token) {
        this.token = token;
    }

    @Override
    public void handleMessage(String senderName, String messageToken, byte[] data) {


        System.out.println("RemoteExpressionHandler[" + token + "]: Received a message from " + senderName + "; messageToken=" + messageToken
                + "; data= " + data.length);

        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(bis);
            List<List<ExpressionNode>> nodes = (List<List<ExpressionNode>>) ois.readObject();
            for (List<ExpressionNode> list : nodes) {
                for (ExpressionNode node : list) {
                     System.out.println("RemoteExpressionHandler[" + token + "] Received: " + node);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { ois.close(); } catch (Exception ex) {}
            try { bis.close(); } catch (Exception ex) {}
        }
    }

}
