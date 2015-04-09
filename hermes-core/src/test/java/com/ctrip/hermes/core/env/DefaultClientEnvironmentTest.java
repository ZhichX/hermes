package com.ctrip.hermes.core.env;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import org.junit.Test;
import org.unidal.lookup.ComponentTestCase;

public class DefaultClientEnvironmentTest extends ComponentTestCase {

	@Test
	public void test() throws Exception {
		ClientEnvironment env = lookup(ClientEnvironment.class);
		Properties config = env.getProducerConfig("test_topic");
		assertNotNull(config);
		assertEquals(config.getProperty("hermes"), "mq");

		config = env.getProducerConfig("not_exist");
		assertNotNull(config);
		assertEquals(0, config.size());
	}

}
