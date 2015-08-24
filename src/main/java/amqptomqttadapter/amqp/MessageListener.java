package amqptomqttadapter.amqp;

public interface MessageListener {

    /**
     * Starts the listening process.
     */
    public void start();

    /**
     * Frees any active resources that the listener has.
     */
    public void cleanupResources();

    /**
     * Sets the AMQP connection manager for the listener.
     * 
     * @param amqpConnectionManager The connection manager to set.
     */
    public void setAmqpConnectionManager(AmqpConnectionManager amqpConnectionManager);

    /**
     * Sets the AMQP host name to listen to.
     * 
     * @param hostName Host name to listen to.
     */
    public void setHostName(String hostName);

}
