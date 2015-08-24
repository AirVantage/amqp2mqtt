package amqptomqttadapter.amqp;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Properties;


import org.apache.log4j.Logger;
import org.jvnet.jaxb2_commons.lang.StringUtils;

import amqptomqttadapter.avopclient.utils.PropertyLoader;


public class AmqpConfiguration {

    private static final Logger LOG = Logger.getLogger(AmqpConfiguration.class);
    /**
     * Host name separator (known by convention).
     */
    private static final char HOST_NAME_SEPARATOR = ',';

    private String[] hostNames;

    private final static String contentEncoding = "UTF-8";

    private String requestRoutingKey;

    private String userId;

    private String userName;

    private String password;

    private String virtualHost;

    /**
     * Delay in seconds between reconnection attempts for AMQP connection losses.
     */
    private int reconnectionDelay = 5;

    private String receiveQueueName;

    private int portNumber;

    private boolean useSsl;

    private boolean enabled = true;

    private static AmqpConfiguration instance = new AmqpConfiguration();

    private AmqpConfiguration() {
        super();
        loadProperties();
    }

    public static AmqpConfiguration getInstance() {

        return instance;
    }

    public String getHostNameString() {
        return StringUtils.join(Arrays.asList(hostNames).iterator(), new String(new char[] { HOST_NAME_SEPARATOR }));
    }

    public void setHostNameString(String hostNames) {
        this.hostNames = StringUtils.split(hostNames, HOST_NAME_SEPARATOR);
    }

    public String[] getHostNames() {
        return hostNames;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public String getRequestRoutingKey() {
        return requestRoutingKey;
    }

    public void setRequestRoutingKey(String routingKey) {
        this.requestRoutingKey = routingKey;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    public int getReconnectionDelay() {
        return reconnectionDelay;
    }

    public void setReconnectionDelay(int reconnectionDelay) {
        this.reconnectionDelay = reconnectionDelay;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    public String getReceiveQueueName() {
        return receiveQueueName;
    }

    public void setReceiveQueueName(String receiveQueueName) {
        this.receiveQueueName = receiveQueueName;
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    public void setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
                prop = PropertyLoader.load("src/main/resources/conf.properties");
                error = false;
            } catch (Exception e) {
                LOG.debug("Could not find conf.properties in the development directory. " + e.getMessage());
            }

        } catch (Exception e) {
            LOG.error("Could not find any conf.properties. " + e.getMessage());
        }
        if (!error) {
            String[] lesHostNames;
            if (this.hostNames != null) {
                lesHostNames = new String[this.hostNames.length + 1];
            } else {
                lesHostNames = new String[1];
            }
            lesHostNames[lesHostNames.length - 1] = prop.getProperty("amqp.hostname", "");
            this.hostNames = lesHostNames;
            this.userName = prop.getProperty("amqp.username", "");
            this.password = prop.getProperty("amqp.password", "");
            this.virtualHost = prop.getProperty("amqp.virtualhost", "");
            this.receiveQueueName = prop.getProperty("amqp.queuename", "");
            this.portNumber = Integer.valueOf(prop.getProperty("amqp.portnumber", String.valueOf(0)));
            this.useSsl = Boolean.valueOf(prop.getProperty("amqp.usessl", String.valueOf("true")));

            res = true;
            LOG.debug("Configuration file for AmqpConfiguration has been successfully loaded.");
        }
        return res;
    }
}
