package com.iapsolutions.mqtt;

import java.util.concurrent.BlockingQueue;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttPublisher implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MqttPublisher.class);
	protected BlockingQueue<String> queue = null;

	public MqttPublisher(BlockingQueue<String> queue) {
		this.queue = queue;
	}
	
	public void run() {
		LOGGER.debug("entering");
		MqttClient mqttClient = null;
		String propMqttBrokerUrl;
		
		// Read the properties
		String propKey = "mqtt.broker.url";
		propMqttBrokerUrl = PropertyHandler.getInstance().getValue(propKey);
		LOGGER.trace("property: " + propKey + "=" + propMqttBrokerUrl);
		
		try {
			LOGGER.trace("connecting to MQTT broker");
			mqttClient = new MqttClient(propMqttBrokerUrl, "client1");
			mqttClient.connect();
			LOGGER.trace("connected");

			// Read incoming data from the ZMQ broker
			while (!Thread.currentThread().isInterrupted()) {
				try {				
					// Read the next message from the queue
					String queueMessage = queue.take();
					LOGGER.trace("queueMessage: " + queueMessage);
					String[] parts = queueMessage.split("#");
					if (parts.length == 2) {
						String topic = parts[0];
						String data = parts[1];											
						// Publish the message in the MQTT broker
						MqttMessage message = new MqttMessage();
						message.setPayload(data.getBytes());
						mqttClient.publish(topic, message);						
					}
					else {
						LOGGER.error("The message must only contain 2 parts (topic#data)");
					}

				} catch (InterruptedException e) {
					LOGGER.error("InterruptedException " + e.getMessage());
					e.printStackTrace();
				} catch (MqttException e) {
					LOGGER.error("MqttException " + e.getMessage());
					e.printStackTrace();
				}

			}
		} catch (MqttException e) {
			LOGGER.error("MqttException " + e.getMessage());
			e.printStackTrace();
		}
		finally {
			try {
				if (mqttClient != null) { 
					mqttClient.disconnect();
				}
			} catch (MqttException e) {
				LOGGER.error("MqttException " + e.getMessage());
				e.printStackTrace();
			}
		}
		
		LOGGER.debug("leaving");
	}
	
}
