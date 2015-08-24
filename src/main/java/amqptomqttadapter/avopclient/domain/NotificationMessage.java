package amqptomqttadapter.avopclient.domain;


/**
 * The main of a published message.
 */
public class NotificationMessage {

	/**
	 * The name.
	 */
	public String name;

	/**
	 * The reception date.
	 */
	public Long date;

	/**
	 * The data message content.
	 */
	public Object content;

	public String getName() {
		return name;
	}

	public Long getDate() {
		return date;
	}

	public Object getContent() {
		return content;
	}

}
