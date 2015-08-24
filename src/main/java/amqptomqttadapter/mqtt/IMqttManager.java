package amqptomqttadapter.mqtt;

import amqptomqttadapter.avopclient.domain.NotificationMessage;

public interface IMqttManager {

	public void publishOnMqttBroker(NotificationMessage notificationMessage,
			String content);

}
