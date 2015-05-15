package com.ctrip.hermes.consumer.engine.bootstrap;

import java.util.Arrays;

import org.unidal.lookup.annotation.Inject;
import org.unidal.lookup.annotation.Named;

import com.ctrip.hermes.meta.entity.Endpoint;

/**
 * @author Leo Liang(jhliang@ctrip.com)
 *
 */
@Named(type = ConsumerBootstrapManager.class)
public class DefaultConsumerBootstrapManager implements ConsumerBootstrapManager {

	@Inject
	private ConsumerBootstrapRegistry m_registry;

	public ConsumerBootstrap findConsumerBootStrap(String endpointType) {

		if (Arrays.asList(Endpoint.BROKER, Endpoint.KAFKA).contains(endpointType)) {
			return m_registry.findConsumerBootstrap(endpointType);
		} else {
			throw new IllegalArgumentException(String.format("unknow endpoint type: %s", endpointType));
		}

	}

}
