package com.iapsolutions.mqtt;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(PropertyHandler.class);
	private static PropertyHandler instance = null;
	private Properties prop = null;

	private PropertyHandler() {
		LOGGER.trace("entering");
		
		prop = new Properties();
		try {
            // Load the properties file
    		ClassLoader classLoader = ClassLoader.getSystemClassLoader();    
    		prop.load(classLoader.getResourceAsStream("config.properties"));
    	} catch (IOException ex) {
    		LOGGER.error("IOException" + ex.getMessage());
        }
		
		LOGGER.trace("leaving");
	}

	public static synchronized PropertyHandler getInstance(){
		if (instance == null) {
			instance = new PropertyHandler();
		}
		
		return(instance);
	}

	public String getValue(String propKey){
		return (this.prop.getProperty(propKey));
	}
}
