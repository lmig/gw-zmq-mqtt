package com.iapsolutions.mqtt;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import com.iapsolutions.factjson.parser.FactToJson;

public class ZmqSubscriber implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(ZmqSubscriber.class);
	protected BlockingQueue<String> queue = null;
	
	public ZmqSubscriber(BlockingQueue<String> queue) {
		this.queue = queue;
	}
	
	public void run() {
		LOGGER.debug("entering");
		ZMQ.Context zmqContext;
		ZMQ.Socket zmqSocket;
		String propZmqSubscriberUrl;
		List<String> propZmqFilters = new LinkedList<String>();
		List<String> propMqttPublisherTopics = new LinkedList<String>();
		Integer i;
		
		// Read the properties
		String propKey = "zmq.southbound.subscriber.url";
		propZmqSubscriberUrl = PropertyHandler.getInstance().getValue(propKey);
		LOGGER.trace("property: " + propKey + "=" + propZmqSubscriberUrl);
		i = 1;
		while (true) {
			LOGGER.trace("i=" + i);
			String propKey1 = "zmq.southbound.subscriber.filter." + Integer.toString(i);
			String value1 = PropertyHandler.getInstance().getValue(propKey1);
			String propKey2 = "mqtt.publisher.topic." + Integer.toString(i);
			String value2 = PropertyHandler.getInstance().getValue(propKey2);

			if (value1 == null || value2 == null) {
				break;
			}
			
			LOGGER.trace("property: " + propKey1 + "=" + value1);
			LOGGER.trace("property: " + propKey2 + "=" + value2);

			propZmqFilters.add(value1);
			propMqttPublisherTopics.add(value2);
			i++;
		}
		
		// Connect to the ZMQ broker
		LOGGER.trace("connecting to ZMQ broker");
		zmqContext = ZMQ.context(1);
		zmqSocket = zmqContext.socket(ZMQ.SUB);
		zmqSocket.connect(propZmqSubscriberUrl);
		LOGGER.trace("connected");

		// Setup the ZMQ subscriber filters
		for (String filter : propZmqFilters) {
			zmqSocket.subscribe(filter.getBytes());
		}
		
		// Read incoming data from the ZMQ broker
		while (!Thread.currentThread().isInterrupted()) {
			try {
				ZMsg zmqMessage = ZMsg.recvMsg(zmqSocket);
				// New data received
				String currentFilter = zmqMessage.popString();
				String receivedData = new String(zmqMessage.pop().getData(), "UTF-8");
				LOGGER.debug("currentFilter=" + currentFilter);
				LOGGER.debug("receivedData=" + receivedData);
				
				// Send the data + the topic to the MQTT thread via the queue
				boolean found = false;
				for (i = 0; i < propZmqFilters.size(); i++) {
					if (propZmqFilters.get(i).compareTo(currentFilter) == 0) {
						found = true;
						break;
					}
				}
				
				if (found) {
					FactToJson parser = new FactToJson();
					String jsonString = parser.parse(receivedData);
					LOGGER.debug("JSON: " + jsonString);					
					String message = propMqttPublisherTopics.get(i) + "#" + jsonString;
					queue.put(message);
				}
				else {
					LOGGER.error("no match found for filter " + currentFilter);
				}		
			} catch (NullPointerException e) {
				LOGGER.error("NullPointerException" + e.getMessage());
				e.printStackTrace();
			} catch (SecurityException e) {
				LOGGER.error("SecurityException " + e.getMessage());
				e.printStackTrace();
			} catch (Exception e) {
				LOGGER.error("Exception " + e.getMessage());
				e.printStackTrace();
			}
		}
		
		LOGGER.debug("leaving");
	}	
}
