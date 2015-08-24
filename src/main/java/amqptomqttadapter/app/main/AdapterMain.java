package amqptomqttadapter.app.main;


import org.apache.log4j.Logger;

import amqptomqttadapter.amqp.impl.AmqpConnectionManagerImpl;


public class AdapterMain {

    private static final Logger LOG = Logger.getLogger(AdapterMain.class);

    /**
     * This application will transform AMQP messages into MQTT ones and will publish them into an MQTT broker.
     * 
     * @param args
     */
    @SuppressWarnings("unused")
    public static void main(String[] args) {
        LOG.info("AMQP to MQTT adapter - Starting");
        AmqpConnectionManagerImpl amqpConnectionManager = new AmqpConnectionManagerImpl();

    }

}
