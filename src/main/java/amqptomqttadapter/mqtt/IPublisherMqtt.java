package amqptomqttadapter.mqtt;

public interface IPublisherMqtt {

    public void produce(String event, String topicName);

}
