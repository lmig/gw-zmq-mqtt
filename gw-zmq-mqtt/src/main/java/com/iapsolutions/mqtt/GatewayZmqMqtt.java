package com.iapsolutions.mqtt;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GatewayZmqMqtt {
	private static final Logger LOGGER = LoggerFactory.getLogger(GatewayZmqMqtt.class);

	public static void main(String[] args) {
	    LOGGER.trace("entering");
	    
	    BlockingQueue<String> queue = new ArrayBlockingQueue<String>(100);
	    MqttPublisher mqttPublisher = new MqttPublisher(queue);
	    ZmqSubscriber zmqSubscriber = new ZmqSubscriber(queue);
	    
	    // Startup the ZMQ subscriber and MQTT publisher threads 
		new Thread(mqttPublisher, "MqttPublisher").start();
		new Thread(zmqSubscriber, "ZmqSubscriber").start();
		
		while (true) {
			try {
				TimeUnit.SECONDS.sleep(10);
			}
			catch (InterruptedException e) {
			}
		}	    
	}
}
