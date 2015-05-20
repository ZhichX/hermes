package com.ctrip.hermes.consumer.engine;

import java.util.List;

import org.unidal.lookup.annotation.Inject;
import org.unidal.lookup.annotation.Named;

import com.ctrip.hermes.consumer.engine.bootstrap.ConsumerBootstrap;
import com.ctrip.hermes.consumer.engine.bootstrap.ConsumerBootstrapManager;
import com.ctrip.hermes.core.meta.MetaService;
import com.ctrip.hermes.meta.entity.Topic;

@Named(type = Engine.class)
public class DefaultEngine extends Engine {

	@Inject
	private ConsumerBootstrapManager m_consumerManager;

	@Inject
	private MetaService m_metaService;

	@Override
	public SubscribeHandle start(List<Subscriber> subscribers) {
		CompositeSubscribeHandle handle = new CompositeSubscribeHandle();

		for (Subscriber s : subscribers) {
			List<Topic> topics = m_metaService.findTopicsByPattern(s.getTopicPattern());

			for (Topic topic : topics) {
				String endpointType = m_metaService.getEndpointType(topic.getName());
				ConsumerContext consumerContext = new ConsumerContext(topic, s.getGroupId(), s.getConsumer(),
				      s.getMessageClass(), s.getConsumerType());
				ConsumerBootstrap consumerBootstrap = m_consumerManager.findConsumerBootStrap(endpointType);
				handle.addSubscribeHandle(consumerBootstrap.start(consumerContext));
			}
		}

		return handle;
	}

}
