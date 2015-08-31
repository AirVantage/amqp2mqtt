AMQP to MQTT adapter
==========================

Description
-------
This application connects to an AMQP file and is listening to the messages pushed in the queue. For each received message, its content is pushed into a different MQTT topic following the nature of the message (monitoring data or operation state change).

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