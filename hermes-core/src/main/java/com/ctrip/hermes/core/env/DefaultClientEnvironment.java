package com.ctrip.hermes.core.env;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.unidal.lookup.annotation.Named;

@Named(type = ClientEnvironment.class)
public class DefaultClientEnvironment implements ClientEnvironment, Initializable, LogEnabled {
	private final static String PRODUCER_DEFAULT_FILE = "/hermes-producer.properties";

	private final static String PRODUCER_PATTERN = "/hermes-producer-%s.properties";

	private final static String CONSUMER_DEFAULT_FILE = "/hermes-consumer.properties";

	private final static String CONSUMER_PATTERN = "/hermes-consumer-%s.properties";

	private final static String GLOBAL_DEFAULT_FILE = "/hermes.properties";

	private ConcurrentMap<String, Properties> m_producerCache = new ConcurrentHashMap<>();

	private ConcurrentMap<String, Properties> m_consumerCache = new ConcurrentHashMap<>();

	private Properties m_producerDefault;

	private Properties m_consumerDefault;

	private Properties m_globalDefault;

	private Logger logger;

	@Override
	public Properties getProducerConfig(String topic) throws IOException {
		Properties properties = m_producerCache.get(topic);
		if (properties == null) {
			properties = readConfigFile(String.format(PRODUCER_PATTERN, topic), m_producerDefault);
			m_producerCache.putIfAbsent(topic, properties);
		}

		return properties;
	}

	@Override
	public Properties getConsumerConfig(String topic) throws IOException {
		Properties properties = m_consumerCache.get(topic);
		if (properties == null) {
			properties = readConfigFile(String.format(CONSUMER_PATTERN, topic), m_consumerDefault);
			m_consumerCache.putIfAbsent(topic, properties);
		}

		return properties;
	}

	@Override
	public Properties getGlobalConfig() throws IOException {
		return m_globalDefault;
	}

	private Properties readConfigFile(String configPath) throws IOException {
		return readConfigFile(configPath, null);
	}

	private Properties readConfigFile(String configPath, Properties defaults) throws IOException {
		InputStream in = this.getClass().getResourceAsStream(configPath);
		logger.info("Reading config from resource: " + configPath);
		if (in == null) {
			// load outside resource under current user path
			Path path = new File(System.getProperty("user.dir") + configPath).toPath();
			if (Files.isReadable(path)) {
				in = new FileInputStream(path.toFile());
				logger.info("Reading config from file: " + path);
			}
		}
		Properties props = new Properties();
		if (defaults != null) {
			props.putAll(defaults);
		}

		if (in != null) {
			props.load(in);
		}

		return props;
	}

	@Override
	public void initialize() throws InitializationException {
		try {
			m_producerDefault = readConfigFile(PRODUCER_DEFAULT_FILE);
			m_consumerDefault = readConfigFile(CONSUMER_DEFAULT_FILE);
			m_globalDefault = readConfigFile(GLOBAL_DEFAULT_FILE);
		} catch (IOException e) {
			throw new InitializationException("Error read producer default config file", e);
		}
	}

	@Override
	public void enableLogging(Logger logger) {
		this.logger = logger;
	}

}
