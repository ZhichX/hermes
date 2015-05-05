package com.ctrip.hermes.producer.sender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.unidal.lookup.annotation.Named;

import com.ctrip.hermes.core.message.ProducerMessage;
import com.ctrip.hermes.core.result.SendResult;
import com.ctrip.hermes.core.transport.command.SendMessageCommand;
import com.ctrip.hermes.core.transport.endpoint.EndpointChannel;
import com.ctrip.hermes.core.transport.endpoint.ClientEndpointChannelManager;
import com.ctrip.hermes.meta.entity.Endpoint;
import com.google.common.util.concurrent.SettableFuture;

/**
 * @author Leo Liang(jhliang@ctrip.com)
 *
 */
@Named(type = MessageSender.class, value = Endpoint.BROKER)
public class BatchableMessageSender extends AbstractMessageSender implements MessageSender {

	private ConcurrentMap<Endpoint, EndpointWritingWorkerThread> m_workers = new ConcurrentHashMap<>();

	@Override
	public Future<SendResult> doSend(ProducerMessage<?> msg) {

		Endpoint endpoint = m_endpointManager.getEndpoint(msg.getTopic(), msg.getPartitionNo());

		createWorkerIfNecessary(endpoint);

		return m_workers.get(endpoint).submit(msg);
	}

	private void createWorkerIfNecessary(Endpoint endpoint) {
		if (!m_workers.containsKey(endpoint)) {
			synchronized (m_workers) {
				if (!m_workers.containsKey(endpoint)) {
					EndpointWritingWorkerThread worker = new EndpointWritingWorkerThread(endpoint, m_clientEndpointChannelManager);

					worker.setDaemon(true);
					worker.setName("ProducerChannelWorkerThread-Channel-" + endpoint.getId());
					worker.start();

					m_workers.put(endpoint, worker);
				}
			}
		}
	}

	/**
	 * 
	 * @author Leo Liang(jhliang@ctrip.com)
	 *
	 */
	private static class EndpointWritingWorkerThread extends Thread {

		private BlockingQueue<ProducerChannelWorkerContext> m_queue = new LinkedBlockingQueue<>();

		private ClientEndpointChannelManager m_clientEndpointChannelManager;

		private Endpoint m_endpoint;

		private static final int BATCH_SIZE = 3000;

		private static final int INTERVAL_MILLISECONDS = 50;

		public EndpointWritingWorkerThread(Endpoint endpoint, ClientEndpointChannelManager endpointChannelManager) {
			m_clientEndpointChannelManager = endpointChannelManager;
			m_endpoint = endpoint;
		}

		public Future<SendResult> submit(ProducerMessage<?> msg) {
			SettableFuture<SendResult> future = SettableFuture.create();

			m_queue.offer(new ProducerChannelWorkerContext(msg, future));

			return future;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			while (!Thread.interrupted()) {
				try {
					List<ProducerChannelWorkerContext> contexts = new ArrayList<>();
					m_queue.drainTo(contexts, BATCH_SIZE);

					if (!contexts.isEmpty()) {
						SendMessageCommand command = new SendMessageCommand();
						for (ProducerChannelWorkerContext context : contexts) {
							command.addMessage(context.m_msg, context.m_future);
						}

						EndpointChannel channel = m_clientEndpointChannelManager.getChannel(m_endpoint);

						channel.writeCommand(command);

					}

					TimeUnit.MILLISECONDS.sleep(INTERVAL_MILLISECONDS);

				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (Exception e) {

				}
			}
		}

		private static class ProducerChannelWorkerContext {
			private ProducerMessage<?> m_msg;

			private SettableFuture<SendResult> m_future;

			/**
			 * @param msg
			 * @param future
			 */
			public ProducerChannelWorkerContext(ProducerMessage<?> msg, SettableFuture<SendResult> future) {
				m_msg = msg;
				m_future = future;
			}

		}

	}

}
