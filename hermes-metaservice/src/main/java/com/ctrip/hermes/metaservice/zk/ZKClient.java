package com.ctrip.hermes.metaservice.zk;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.CuratorFrameworkFactory.Builder;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.unidal.lookup.annotation.Inject;
import org.unidal.lookup.annotation.Named;

import com.ctrip.hermes.core.utils.HermesThreadFactory;

/**
 * @author Leo Liang(jhliang@ctrip.com)
 *
 */
@Named(type = ZKClient.class)
public class ZKClient implements Initializable {

	private CuratorFramework m_client;

	@Inject
	private ZKConfig m_config;

	@Override
	public void initialize() throws InitializationException {
		Builder builder = CuratorFrameworkFactory.builder();

		builder.connectionTimeoutMs(m_config.getZkConnectionTimeoutMillis());
		builder.connectString(m_config.getZkConnectionString());
		builder.maxCloseWaitMs(m_config.getZkCloseWaitMillis());
		builder.namespace(m_config.getZkNamespace());
		builder.retryPolicy(new ExponentialBackoffRetry(m_config.getZkRetryBaseSleepTimeMillis(), m_config
		      .getZkRetryMaxRetries()));
		builder.sessionTimeoutMs(m_config.getZkSessionTimeoutMillis());
		builder.threadFactory(HermesThreadFactory.create("MetaService-Zk", true));

		m_client = builder.build();
		m_client.start();
		try {
			m_client.blockUntilConnected();
		} catch (InterruptedException e) {
			throw new InitializationException(e.getMessage(), e);
		}
	}

	public CuratorFramework getClient() {
		return m_client;
	}
}
