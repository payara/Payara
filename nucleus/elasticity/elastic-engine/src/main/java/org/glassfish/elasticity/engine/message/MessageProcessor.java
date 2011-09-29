package org.glassfish.elasticity.engine.message;

import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.elasticity.engine.container.ElasticServiceContainer;
import org.glassfish.elasticity.engine.util.EngineUtil;
import org.glassfish.elasticity.expression.ElasticExpressionEvaluator;
import org.glassfish.elasticity.expression.ExpressionEvaluationException;
import org.glassfish.elasticity.expression.ExpressionNode;
import org.glassfish.elasticity.group.ElasticMessageHandler;
import org.glassfish.elasticity.group.GroupMemberEventListener;
import org.glassfish.elasticity.util.NotEnoughMetricDataException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mahesh Kannan
 */
public class MessageProcessor
        implements GroupMemberEventListener, ElasticMessageHandler {

    private static final Logger logger = EngineUtil.getLogger();

    private ElasticServiceContainer container;

    private ServerEnvironment serverEnvironment;

    private AtomicReference<String[]> currentMembers = new AtomicReference<String[]>();

    private String remoteExpHandlerToken;

    private AtomicInteger messageIdCounter = new AtomicInteger();

    private String serviceName;

    private ConcurrentHashMap<String, ExpressionResponse> responses
            = new ConcurrentHashMap<String, ExpressionResponse>();

    public MessageProcessor(ElasticServiceContainer container, ServerEnvironment serverEnvironment) {
        this.container = container;
        this.serverEnvironment = serverEnvironment;

        this.serviceName = container.getElasticService().getName();
        this.remoteExpHandlerToken = serviceName + ":ElasticExpressionEvaluator";
    }

    public ElasticMessage createElasticMessage(String targetInstanceName) {
        ElasticMessage message = new ElasticMessage();
        message.setMessageId("" + messageIdCounter.incrementAndGet())
                .setServiceName(container.getElasticService().getName())
                .setSourceMemberName(serverEnvironment.getInstanceName())
                .setSubComponentName(targetInstanceName)
                .setSubComponentName(remoteExpHandlerToken);

        return message;
    }

    @Override
    public void onViewChange(String memberName, Collection<String> currentAliveAndReadyMembers, Collection<String> previousView, boolean isJoinEvent) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "ElasticEvent[service=" + serviceName + "]: Member " + memberName
                + (isJoinEvent ? " JOINED" : " LEFT") + " the cluster"
                + "; currentView: " + currentAliveAndReadyMembers);
        }
        Set<String> members = new HashSet<String>();
        members.addAll(currentAliveAndReadyMembers);

        currentMembers.set(members.toArray(new String[0]));
    }

    public int getCurrentMemberCount() {
        return currentMembers.get().length;
    }

    public ExpressionResponse sendMessage(ElasticMessage message) {
        if (currentMembers.get().length == 0) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "No member in the cluster is alive. currentSize: " + currentMembers.get().length);
            }
            throw new NotEnoughMetricDataException("Not enough data. Reason: No instances other than the master is running");
        }

        byte[] data = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        ExpressionResponse resp = null;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(message);
            oos.close();
            data = bos.toByteArray();

            String[] targets = currentMembers.get();

            if (message.isResponseRequired()) {
                resp = new ExpressionResponse(message.getMessageId(), targets);
                responses.put(message.getMessageId(), resp);
            }

            if (message.getTargetMemberName() == null) {
                for (String member : currentMembers.get()) {
                    container.getGroupServiceProvider().sendMessage(member, serviceName, data);
                }
            } else {
                container.getGroupServiceProvider().sendMessage(message.getTargetMemberName(), serviceName, data);
            }

            if (resp != null) {
                resp.await(10, TimeUnit.SECONDS);
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Exception during message sending", ex);
        } finally {
            try {
                oos.close();
            } catch (Exception ex) {
            }
        }

        return resp;
    }

    @Override
    public void handleMessage(String senderName, String messageToken, byte[] data) {

        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(bis);
            ElasticMessage message = (ElasticMessage) ois.readObject();
            if (remoteExpHandlerToken.equals(message.getSubComponentName())) {
                if (message.isResponseMessage()) {
                    List<Object> result = (List<Object>) message.getData();
                    String respId = message.getInResponseToMessageId();
                    ExpressionResponse resp = responses.get(respId);
                    if (resp != null) {
                        if (message.isValidData()) {
//                            System.out.println("EXPR RESPONSE: Got response from " + message.getSourceMemberName() + " result = " + result.get(0));
                            resp.addResponse(message.getSourceMemberName(), result.get(0));
                        } else {
                            resp.addException(message.getSourceMemberName(), message.getException());
                        }
                    }
                } else {
                    //First prepare the response message
                    ElasticMessage responseMessage = new ElasticMessage();
                    responseMessage.setMessageId("" + messageIdCounter.incrementAndGet())
                            .setTargetMemberName(senderName)
                            .setServiceName(serviceName)
                            .setSourceMemberName(serverEnvironment.getInstanceName())
                            .setSubComponentName(message.getSubComponentName())
                            .setInResponseToMessageId(message.getMessageId())
                            .setIsResponseMessage(true);
//                    System.out.println(" received message "+message.toString());
                    List<Object> resultList = new ArrayList<Object>();
                    try {
                        List<ExpressionNode> list = (List<ExpressionNode>) message.getData();
                        for (ExpressionNode node : list) {

                            ElasticExpressionEvaluator evaluator = new ElasticExpressionEvaluator(container.getHabitat(), container);
                            Object answer = evaluator.evaluate(node);
                            resultList.add(answer);

                            responseMessage.setData(resultList);
                        }
                    } catch (ExpressionEvaluationException ex) {
                        responseMessage.setException(ex);
                    }

                    if(logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Sending an elastic RESPONSE message: " + responseMessage.getData());
                    }
                    sendMessage(responseMessage);
                }
            } else {
                logger.warning("Received a generic message: " + message);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                ois.close();
            } catch (Exception ex) {
            }
            try {
                bis.close();
            } catch (Exception ex) {
            }
        }
    }

    public static class ExpressionResponse {

        private String responseId;

        private HashMap map = new HashMap();

        private CountDownLatch latch;

        private boolean gotExceptions;

        public ExpressionResponse(String responseId, String[] targetNames) {
            this.responseId = responseId;
            latch = new CountDownLatch(targetNames.length);
        }

        public void addResponse(String respondingInstanceName, Object value) {
            map.put(respondingInstanceName, value);
            latch.countDown();
        }

        public void addException(String respondingInstanceName, Exception ex) {
            map.put(respondingInstanceName, ex);
            latch.countDown();
            gotExceptions = true;
        }

        public long getResponseCount() {
            return latch.getCount();
        }

        public void await(long time, TimeUnit unit)
            throws InterruptedException {
            latch.await(time, unit);
        }

        public boolean hasExceptions() {
            return gotExceptions;
        }

        public Collection values() {
            return map.values();
        }
    }
}
