package amqptomqttadapter.amqp.impl;

import java.io.IOException;
import java.text.MessageFormat;


import org.apache.log4j.Logger;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import amqptomqttadapter.amqp.AmqpConfiguration;
import amqptomqttadapter.amqp.AmqpConnectionManager;
import amqptomqttadapter.amqp.MessageListener;
import amqptomqttadapter.avopclient.domain.NotificationMessage;
import amqptomqttadapter.mqtt.impl.MqttManager;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

public class MessageListenerImpl extends Thread implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(MessageListenerImpl.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    private AmqpConnectionManager amqpConnectionManager;
    MqttManager mqttManager = new MqttManager();

    private String hostName;

    /**
     * AMQP channel.
     */
    private Channel channel;

    /**
     * AMQP consumer.
     */
    private QueueingConsumer consumer;

    public MessageListenerImpl(AmqpConnectionManagerImpl amqpConnectionManager) {
        objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        this.amqpConnectionManager = amqpConnectionManager;
    }

    @Override
    public void run() {
        listen();
    }

    public void listen() {
        AmqpConfiguration amqpConfiguration = amqpConnectionManager.getAmqpConfiguration();
        this.hostName = amqpConfiguration.getHostNameString();
        String queue = amqpConfiguration.getReceiveQueueName();
        LOGGER.info(MessageFormat.format("Creating an AMQP consumer for: {0}:{1} (queue \"{2}\", user \"{3}\")",
                hostName, amqpConfiguration.getPortNumber(), queue, amqpConfiguration.getUserName()));

        try {
            startConsumer();
            while (true) {
                QueueingConsumer.Delivery delivery = null;
                try {

                    delivery = consumer.nextDelivery();

                    // retrieve properties of the message
                    BasicProperties properties = delivery.getProperties();

                    String content = new String(delivery.getBody(), properties.getContentEncoding());
                    
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(MessageFormat.format("Received message with content {0}", content));
                    }
                    
                    NotificationMessage notificationMessage = objectMapper.readValue(content, NotificationMessage.class);
                    
                    // Publish on the MQTT broker
                    mqttManager.publishOnMqttBroker(notificationMessage, content);

                    // acknowledge in case of success only
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                } catch (ShutdownSignalException e) {
                    // Auto reconnect
                    startConsumer();
                } catch (Throwable t) {
                    channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);

                    LOGGER.error(MessageFormat.format(
                            "[Listener queue {0}] Exception while listening to the message queue for host {1}", queue,
                            hostName), t);
                }
            }// end while(true) //
        } catch (IOException ioe) {
            LOGGER.error(MessageFormat.format("I/O error while listening to the message queue for host {0}", hostName),
                    ioe);
        }
    }

    private void startConsumer() throws IOException {
        // Open the connection
        Connection connection = amqpConnectionManager.getAmqpConnection(hostName);

        try {
            // Create the channel
            if (connection != null) {
                channel = connection.createChannel();

                // Launch the consumer
                boolean autoAck = false;
                consumer = new QueueingConsumer(channel);
                channel.basicConsume(amqpConnectionManager.getAmqpConfiguration().getReceiveQueueName(), autoAck,
                        consumer);
            } else {
                LOGGER.fatal("Couldn't open amqp connection");
            }
        } catch (Exception ex) {
            LOGGER.debug(ex.getMessage());
        }
    }

    public void cleanupResources() {
        // Destroy
        consumer = null;

        // Close & destroy the channel
        if (channel != null) {
            try {
                channel.close();
            } catch (Exception e) {
                channel = null;
            }
        }
    }

    public void setAmqpConnectionManager(AmqpConnectionManager amqpConnectionManager) {
        this.amqpConnectionManager = amqpConnectionManager;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

}
