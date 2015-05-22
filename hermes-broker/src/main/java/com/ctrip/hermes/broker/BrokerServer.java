package com.ctrip.hermes.broker;

import com.ctrip.hermes.broker.bootstrap.BrokerBootstrap;
import com.ctrip.hermes.core.utils.PlexusComponentLocator;

public class BrokerServer {
	public static void main(String[] args) throws Exception {
		PlexusComponentLocator.lookup(BrokerBootstrap.class).start();
	}
}
