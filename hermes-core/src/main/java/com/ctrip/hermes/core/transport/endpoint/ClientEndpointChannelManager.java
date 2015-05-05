package com.ctrip.hermes.core.transport.endpoint;

import com.ctrip.hermes.meta.entity.Endpoint;

/**
 * @author Leo Liang(jhliang@ctrip.com)
 *
 */
public interface ClientEndpointChannelManager {

	ClientEndpointChannel getChannel(Endpoint endpoint, EndpointChannelEventListener... listeners);

	void closeChannel(Endpoint endpoint);
}
