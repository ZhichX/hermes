package com.ctrip.hermes.producer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unidal.lookup.annotation.Inject;
import org.unidal.lookup.annotation.Named;

import com.ctrip.hermes.core.exception.MessageSendException;
import com.ctrip.hermes.core.message.ProducerMessage;
import com.ctrip.hermes.core.meta.MetaService;
import com.ctrip.hermes.core.pipeline.Pipeline;
import com.ctrip.hermes.core.result.CompletionCallback;
import com.ctrip.hermes.core.result.SendResult;
import com.ctrip.hermes.core.service.SystemClockService;
import com.ctrip.hermes.meta.entity.Topic;
import com.ctrip.hermes.producer.api.Producer;
import com.ctrip.hermes.producer.build.BuildConstants;

@Named(type = Producer.class)
public class DefaultProducer extends Producer {

	private static final Logger log = LoggerFactory.getLogger(DefaultProducer.class);

	@Inject(BuildConstants.PRODUCER)
	private Pipeline<Future<SendResult>> m_pipeline;

	@Inject
	private SystemClockService m_systemClockService;

	@Inject
	private MetaService m_metaService;

	@Override
	public DefaultMessageHolder message(String topic, String partitionKey, Object body) {
		return new DefaultMessageHolder(topic, partitionKey, body);
	}

	class DefaultMessageHolder implements MessageHolder {
		private ProducerMessage<Object> m_msg;

		public DefaultMessageHolder(String topic, String partitionKey, Object body) {
			m_msg = new ProducerMessage<Object>(topic, body);
			m_msg.setPartitionKey(partitionKey);
		}

		@Override
		public Future<SendResult> send() {
			Topic topic = m_metaService.findTopicByName(m_msg.getTopic());

			if (topic == null) {
				log.error("Topic {} not found.", m_msg.getTopic());
				throw new IllegalArgumentException(String.format("Topic %s not found.", m_msg.getTopic()));
			}

			m_msg.setBornTime(m_systemClockService.now());
			return m_pipeline.put(m_msg);
		}

		@Override
		public MessageHolder withRefKey(String key) {
			if (key != null && key.length() > 90) {
				throw new IllegalArgumentException(String.format(
				      "RefKey's length must not larger than 90 characters(refKey=%s)", key));
			}

			m_msg.setKey(key);
			return this;
		}

		@Override
		public MessageHolder withPriority() {
			m_msg.setPriority(true);
			return this;
		}

		@Override
		public MessageHolder addProperty(String key, String value) {
			m_msg.addDurableAppProperty(key, value);
			return this;
		}

		@Override
		public MessageHolder setCallback(CompletionCallback<SendResult> callback) {
			m_msg.setCallback(callback);
			return this;
		}

		@Override
		public SendResult sendSync() throws MessageSendException {
			try {
				return send().get();
			} catch (ExecutionException | InterruptedException e) {
				throw new MessageSendException(e);
			}
		}
	}
}
