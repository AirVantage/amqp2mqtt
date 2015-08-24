package amqptomqttadapter.mqtt.impl;


import java.text.MessageFormat;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import amqptomqttadapter.mqtt.IPublisherMqtt;


public class PublisherMqtt implements IPublisherMqtt, MqttCallback {

    private static final Logger LOG = Logger.getLogger(PublisherMqtt.class);

    private static final long TIME_BEFORE_RETRY = 30000L;

    private String broker;
    private String clientId;
    private int qos = 2;

    @SuppressWarnings("unused")
    private PublisherMqtt() {
    }

    public PublisherMqtt(String broker, String clientId) {
        this.clientId = clientId;
        this.broker = broker;
    }
    
    public void produce(String content, String topicName) {
    	
    	LOG.debug(MessageFormat.format("Ready to publish MQTT message {0}", content));
    	
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setWill("adapteur/clienterrors", "crashed".getBytes(), 2, true);

            MqttClient sampleClient = null;
            // Connect to Broker
            LOG.debug("Connecting to broker: " + broker);
            while (sampleClient == null) {
                sampleClient = connectToBroker(persistence, connOpts);
                if (sampleClient == null) {
                    LOG.debug("Waiting to connect to the broker...");
                    Thread.sleep(TIME_BEFORE_RETRY);
                }
            }

            LOG.debug("Connected to " + broker);

            MqttMessage message = new MqttMessage(String.valueOf(content).getBytes());
            message.setQos(qos);
            message.setRetained(true);

            LOG.info("Publishing to topic \"" + topicName + "\" qos " + qos);
            MqttDeliveryToken token = null;

            MqttTopic topic = sampleClient.getTopic(topicName);
            // publish message to broker
            token = topic.publish(message);
            // Wait until the message has been delivered to the broker
            LOG.debug("Waiting acknowledgment from the broker...");
            token.waitForCompletion(TIME_BEFORE_RETRY);
            LOG.debug("Waiting done.");

            LOG.info("Message published");

            // disconnect
            try {
                sampleClient.disconnect();
            } catch (Exception e) {
                LOG.error("Error disconnecting. Reason : " + e.getMessage());
            }
            LOG.info("Disconnected");

        } catch (Exception e) {
            LOG.error("Exception when publishing the notification to the topic " + topicName + ". Reason : "
                    + e.getMessage());
        }

    }

    private MqttClient connectToBroker(MemoryPersistence persistence, MqttConnectOptions connOpts) {
        MqttClient res = null;
        try {
            res = new MqttClient(broker, clientId, persistence);
            res.setCallback(this);
            res.connect(connOpts);
        } catch (MqttException e) {
            LOG.error("Error connecting: " + e.getMessage());
            return null;
        }
        return res;
    }

    public String getBroker() {
        return broker;
    }

    public void setBroker(String broker) {
        this.broker = broker;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    /**
     * This callback is invoked upon losing the MQTT connection.
     */
    public void connectionLost(Throwable arg0) {
        LOG.debug("Connection lost.");
        // TODO reconnect here

    }

    /**
     * 
     * deliveryComplete This callback is invoked when a message published by this client is successfully received by the
     * broker.
     * 
     */
    public void deliveryComplete(IMqttDeliveryToken token) {
        try {
            if (token.getMessage() != null) {
                LOG.info("Publication complete" + new String(token.getMessage().getPayload()));
            } else {
                LOG.info("Delivery done. ");
            }
        } catch (MqttException e) {
            LOG.error(e.getMessage());
        }
    }

    /**
     * This callback is invoked when a message is received on a subscribed topic. Unused.
     */
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        LOG.info("-------------------------------------------------");
        LOG.info("| Topic:" + topic);
        LOG.info("| Message: " + new String(message.getPayload()));
        LOG.info("-------------------------------------------------");

    }

}
