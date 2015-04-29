package com.ctrip.hermes.core.transport.endpoint;

import com.ctrip.hermes.core.transport.command.Command;

/**
 * @author Leo Liang(jhliang@ctrip.com)
 *
 */
public interface EndpointChannel {

	void writeCommand(Command command);

	void start();

	void addListener(EndpointChannelEventListener... listeners);

	boolean isClosed();

	void close();

	String getHost();

}
