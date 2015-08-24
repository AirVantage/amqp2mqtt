AMQP to MQTT adapter
==========================

Description
-------

This application was built initially for PSA customer. They wanted to receive the monitoring data + the operation state changes through the notification mechanism. However, they were not able to connect to our RabbitMQ AMQP solution in an easy way. So they asked us to push such data into a different MQTT topic following if it is monitoring data or operation state changes.

This application connects to an AMQP file and is listening to the messages pushed in the queue. For each received message, following its nature (monitoring data or operation state change), the content is pushed into a different MQTT topic (topic names can be configured).

Configuration
-------------
The configuration file `config.properties` contains :
* The parameters to connect to the AMQP file.
* The parameters to connect to the MQTT broker and the topic names.

Running
--------------------
The application has been built into a jar containing all the dependencies. 
The configuration file `config.properties` has to be adapted and to be placed in the same directory as the jar.
To run the jar :
> cd AMQPToMQTTAdapter && java -jar amqpToMqttAdapter-1.0-SNAPSHOT-jar-with-dependencies.jar

A log file `amqpToMqttAdapter.log` will be created.