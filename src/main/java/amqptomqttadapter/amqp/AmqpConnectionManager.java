package amqptomqttadapter.amqp;

import java.io.IOException;

import com.rabbitmq.client.Connection;

public interface AmqpConnectionManager {

	/**
	 * Heartbeat (in seconds) of the AMQP connection.
	 */
	public static final int AMQP_HEARTBEAT = 30;

	/**
	 * Retrieves any of the available AMQP connections.
	 * 
	 * @return An active <code>Connection</code> object.
	 * @throws IOException
	 *             If a problem is detected while retrieving the connection.
	 */
	public Connection getAnyAmqpConnection() throws IOException;

	/**
	 * Returns an AMQP connection, creating it if necessary.
	 * 
	 * @param hostName
	 * @return A Connection object.
	 * @throws IOException
	 *             If a problem is detected while opening a connection.
	 */
	public Connection getAmqpConnection(String hostName) throws IOException;

	/**
	 * Retrieves the associated AMQP configuration.
	 * 
	 * @return
	 */
	public AmqpConfiguration getAmqpConfiguration();

	/**
	 * Check if there is an active amqp connection.
	 * 
	 * @return boolean
	 */
	public boolean hasActiveConnection();
}
