package amqptomqttadapter.amqp.impl;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;


import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import amqptomqttadapter.amqp.AmqpConfiguration;
import amqptomqttadapter.amqp.AmqpConnectionManager;
import amqptomqttadapter.amqp.MessageListener;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * Manager for methods around the AMQP connections towards the infrastructure.
 * 
 */
public class AmqpConnectionManagerImpl implements AmqpConnectionManager,
		InitializingBean, DisposableBean {

	/**
	 * Logger for this class.
	 */
	private final Logger LOGGER = Logger.getLogger(AmqpConnectionManager.class);

	/**
	 * Associated AMQP configuration.
	 */
	private AmqpConfiguration amqpConfiguration = AmqpConfiguration
			.getInstance();
	/**
	 * Stored connections to the Message Broker nodes.
	 */
	private Map<String, Connection> connections;

	/**
	 * Stored message listeners over the existing AMQP connections.
	 */
	private Map<String, MessageListener> messageListeners;

	/**
	 * Stored shutdown listeners over the existing AMQP connections.
	 */
	public Map<String, ShutdownListener> shutdownListeners;

	public AmqpConnectionManagerImpl() {
		connections = new HashMap<String, Connection>();
		messageListeners = new HashMap<String, MessageListener>();
		shutdownListeners = new HashMap<String, ShutdownListener>();
		openConnections();

		return;
	}

	public void destroy() throws Exception {
		closeConnections();
	}

	/**
	 * Returns an AMQP connection to the Message Broker network, creating it if
	 * necessary. This is typically used to send a message towards (it will be
	 * routed on their side towards the correct broker node).
	 * 
	 * @return A Connection object.
	 * @throws IOException
	 *             If a problem is detected while opening a connection.
	 */
	public Connection getAnyAmqpConnection() throws IOException {
		// If there are active connections, return one
		if (connections.size() > 0) {
			String[] activeHosts = new String[0];
			activeHosts = connections.keySet().toArray(activeHosts);
			if (activeHosts.length > 0) {
				return connections.get(activeHosts[0]);
			}
		}

		// Otherwise, try the configuration file host names
		for (String hostName : amqpConfiguration.getHostNames()) {
			try {
				return getAmqpConnection(hostName);
			} catch (IOException ioe) {
				LOGGER.error(
						MessageFormat
								.format("Error while trying to open a connection to host {0}, trying the next one...",
										hostName), ioe);
			}
		}

		// If this fails, throw an IOException.
		throw new IOException(
				"Error while trying to open an AMQP connection to any of the Message Broker hosts");
	}

	/**
	 * Returns an AMQP connection to a specific node of the Message Broker
	 * network, creating it if necessary.
	 * 
	 * @param hostName
	 * @return A Connection object.
	 * @throws IOException
	 *             If a problem is detected while opening a connection.
	 */
	public Connection getAmqpConnection(String hostName) throws IOException {
		Connection connection = connections.get(hostName);
		LOGGER.info(MessageFormat.format(
				"Searching cached connection for host {0}", hostName));
		if (connection == null) {
			LOGGER.info(MessageFormat.format(
					"No connection for host {0}, creating one", hostName));
			try {
				openConnection(hostName);
				return connection;
			} catch (Exception e) {
				LOGGER.error(MessageFormat.format(
						"Couldn't open connection for host {0}", hostName), e);
			}
			return null;
		} else {
			LOGGER.info(MessageFormat.format("Found connection for host {0}",
					hostName));
			if (connection.isOpen())
				return connection;
			else {
				return connection;
			}
		}
	}

	public boolean hasActiveConnection() {
		boolean res = false;
		for (Connection connection : connections.values()) {
			if (connection.isOpen()) {
				res = true;
				break;
			}
		}
		return res;
	}

	private void openConnection(final String hostName)
			throws KeyManagementException, NoSuchAlgorithmException {
		LOGGER.info(MessageFormat.format("Opening AMQP connection to host {0}",
				hostName));

		// Open the connection
		ConnectionFactory connectionFactory = new ConnectionFactory();
		connectionFactory.setVirtualHost(amqpConfiguration.getVirtualHost());
		connectionFactory.setUsername(amqpConfiguration.getUserName());
		connectionFactory.setPassword(amqpConfiguration.getPassword());
		connectionFactory.setRequestedHeartbeat(AMQP_HEARTBEAT);

		if (amqpConfiguration.isUseSsl()) {
			connectionFactory.useSslProtocol();
		}

		Address[] addresses = new Address[] { new Address(hostName,
				amqpConfiguration.getPortNumber()) };

		Connection connection = null;
		Boolean connected = false;
		while (!connected) {
			try {
				connection = connectionFactory.newConnection(addresses);
				connected = true;
			} catch (UnknownHostException unk) {
				sleepBeforeReconnection(hostName);
				LOGGER.info("UnknownHostException : Still not connected");
			} catch (IOException e) {
				sleepBeforeReconnection(hostName);
				LOGGER.error("Error while opening ampq connexion", e);
			}
		}

		// Add a shutdown listener to automatically reconnect
		ShutdownListener shutdownListener = new ShutdownListener() {

			public void shutdownCompleted(ShutdownSignalException cause) {
				LOGGER.warn(
						MessageFormat
								.format("AMQP connection to host {0} is down, trying to reconnect in {1} seconds",
										hostName, amqpConfiguration
												.getReconnectionDelay()), cause);
				sleepBeforeReconnection(hostName);
				try {
					openConnection(hostName);
				} catch (KeyManagementException e) {
					LOGGER.error("Couldn't open connection", e);
				} catch (NoSuchAlgorithmException e) {
					LOGGER.error("Couldn't open connection", e);
				}
			}
		};

		shutdownListeners.put(amqpConfiguration.getHostNameString(),
				shutdownListener);
		connection.addShutdownListener(shutdownListener);

		// Create a listener for the new connection
		createMessageListener(hostName);

		// Store the connection
		connections.put(hostName, connection);
	}

	private final void sleepBeforeReconnection(String hostName) {
		// Sleep
		try {
			Thread.sleep(amqpConfiguration.getReconnectionDelay() * 1000);
		} catch (InterruptedException ie) {
			LOGGER.warn(MessageFormat.format(
					"Reconnection sleep interrupted for host {0}", hostName),
					ie);
		}
	}

	private void openConnections() {
		LOGGER.info(MessageFormat.format(
				"Opening AMQP connections to hosts {0}",
				amqpConfiguration.getHostNameString()));

		if (amqpConfiguration.isEnabled()) {

			for (final String hostName : amqpConfiguration.getHostNames()) {
				new Thread() {
					@Override
					public void run() {
						try {
							getAmqpConnection(hostName);
						} catch (IOException ioe) {
							LOGGER.error(
									MessageFormat
											.format("Exception while opening connection to host {0}",
													hostName), ioe);
						}
					}
				}.start();
			}
		} else {
			LOGGER.info("AMQP is not activated");
		}
	}

	public void afterPropertiesSet() throws Exception {
	}

	private void closeConnections() {
		LOGGER.info(MessageFormat.format(
				"Closing AMQP connections to hosts {0}",
				amqpConfiguration.getHostNameString()));

		// Close & destroy the connections
		if (connections != null) {
			for (Connection connection : connections.values()) {
				if (connection != null) {
					try {
						Object host = connection.getClientProperties().get(
								"host");
						if (host != null) {
							destroyMessageListener(host.toString());
							// Properly remove the shutdown listener before
							// closing the connection, in order to avoid
							// automatic reconnection attempts
							destroyShutdownListener(connection, host.toString());
						}

						if (connection.isOpen()) {
							connection.close();
						}

					} catch (IOException ioe) {
						LOGGER.error("Error while closing AMQP connection", ioe);
						connection = null;
					}
				}
			}

			// Clear the connection map
			connections.clear();
		}
	}

	/**
	 * Creates the message listener for the selected host.
	 * 
	 * @param host
	 *            AMQP host - <code>null</code> not permitted.
	 */
	private void createMessageListener(String host) {
		LOGGER.info(MessageFormat.format(
				"Creating AMQP message listener for host {0}", host));

		// Destroy the listener, if it exists
		destroyMessageListener(host);

		// Create and store the listener
		MessageListener listener = new MessageListenerImpl(this);
		listener.setAmqpConnectionManager(this);
		listener.setHostName(host);
		messageListeners.put(host, listener);

		// Launch the listening process
		listener.start();
	}

	/**
	 * Destroys the message listener for the selected host.
	 * 
	 * @param host
	 *            AMQP host - <code>null</code> not permitted.
	 */
	private void destroyMessageListener(String host) {
		// Destroy the listener, if it exists
		if (messageListeners.containsKey(host)) {
			LOGGER.info(MessageFormat.format(
					"Destroying AMQP message listener for host {0}", host));
			MessageListener listener = messageListeners.get(host);
			listener.cleanupResources();
			messageListeners.remove(listener);
		}
	}

	/**
	 * Destroys the shutdown listener for the selected host.
	 * 
	 * @param host
	 *            AMQP host - <code>null</code> not permitted.
	 */
	private void destroyShutdownListener(Connection connection, String host) {
		// Destroy the listener, if it exists
		if (shutdownListeners.containsKey(host)) {
			LOGGER.info(MessageFormat.format(
					"Destroying AMQP shutdown listener for host {0}", host));
			ShutdownListener listener = shutdownListeners.get(host);
			connection.removeShutdownListener(listener);
			shutdownListeners.remove(listener);
		}
	}

	public AmqpConfiguration getAmqpConfiguration() {
		return amqpConfiguration;
	}

	public void setAmqpConfiguration(AmqpConfiguration amqpConfiguration) {
		this.amqpConfiguration = amqpConfiguration;
	}

}
