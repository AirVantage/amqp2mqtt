package amqptomqttadapter.mqtt.impl;

import java.io.FileNotFoundException;
import java.text.MessageFormat;
import java.util.Properties;

import org.apache.log4j.Logger;

import amqptomqttadapter.avopclient.domain.NotificationMessage;
import amqptomqttadapter.avopclient.utils.PropertyLoader;
import amqptomqttadapter.mqtt.IMqttManager;

public class MqttManager implements IMqttManager {

	private static final Logger LOG = Logger.getLogger(MqttManager.class);

	private PublisherMqtt publisher;

	private String hostname;
	private String clientId;
	private String protocol;
	private String topicData;
	private String topicState;

	private static String BROKER_SEPARATION = "://";

	public MqttManager() {
		super();
		loadProperties();
		publisher = new PublisherMqtt(protocol + BROKER_SEPARATION + hostname,
				clientId);
	}

	public void publishOnMqttBroker(NotificationMessage notificationMessage,
			String content) {

		String notificationName = notificationMessage.getName();
		if (notificationName.equals("event.system.incoming.communication")) {
			// Incoming Communication message
			LOG.info("Received Incoming communication message");
			publisher.produce(content, topicData);
		} else if (notificationName.equals("event.operation.state.change")) {
			// Operation State Change message
			LOG.info("Received Operation State Change message.");
			publisher.produce(content, topicState);
		} else {
			LOG.warn(MessageFormat.format("Received message with unknown name : {0} - Skip it", notificationName));
		}

	}

	/**
	 * Initialization of static parameters from conf.properties
	 * 
	 * @return true if successful
	 */
	private boolean loadProperties() {
		boolean res = false;
		boolean error = true;
		Properties prop = null;
		try {
			// loading properties
			prop = PropertyLoader.load("conf.properties");
			error = false;
		} catch (FileNotFoundException f) {
			try {
				// Only works in a development environment (i.e eclipse)
				prop = PropertyLoader
						.load("src/main/resources/conf.properties");
				error = false;
			} catch (Exception e) {
				LOG.debug("Could not find conf.properties in the development directory. "
						+ e.getMessage());
			}

		} catch (Exception e) {
			LOG.error("Could not find any conf.properties. " + e.getMessage());
		}
		if (!error) {
			this.hostname = prop.getProperty("mqtt.hostname", "");
			this.clientId = prop.getProperty("mqtt.clientId", "");
			this.protocol = prop.getProperty("mqtt.protocol", "");
			this.topicData = prop.getProperty("mqtt.topic.data", "");
			this.topicState = prop.getProperty("mqtt.topic.state", "");

			res = true;
			LOG.debug("Configuration file for MqttManager has been successfully loaded.");
		}
		return res;
	}

}
