package com.ctrip.hermes.broker.queue;

import java.util.List;
import java.util.Map;

import org.unidal.tuple.Pair;

import com.ctrip.hermes.broker.queue.storage.MessageQueueStorage;
import com.ctrip.hermes.core.transport.command.SendMessageCommand.MessageBatchWithRawData;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * @author Leo Liang(jhliang@ctrip.com)
 *
 */
public abstract class AbstractMessageQueue implements MessageQueue {

	protected String m_topic;

	protected int m_partition;

	protected MessageQueueDumper m_dumper;

	protected MessageQueueStorage m_storage;

	public AbstractMessageQueue(String topic, int partition, MessageQueueStorage storage) {
		m_topic = topic;
		m_partition = partition;
		m_storage = storage;
		m_dumper = getMessageQueuePartitionDumper();
	}

	@Override
	public ListenableFuture<Map<Integer, Boolean>> appendMessageAsync(boolean isPriority, MessageBatchWithRawData batch) {
		m_dumper.startIfNecessary();

		SettableFuture<Map<Integer, Boolean>> future = SettableFuture.create();

		m_dumper.submit(future, batch, isPriority);

		return future;
	}

	@Override
	public MessageQueueCursor createCursor(String groupId) {
		MessageQueueCursor cursor = doCreateCursor(groupId);
		cursor.init();
		return cursor;
	}

	@Override
	public void nack(boolean resend, boolean isPriority, String groupId, List<Pair<Long, Integer>> msgSeqs) {
		doNack(resend, isPriority, groupId, msgSeqs);
	}

	@Override
	public void ack(boolean resend, boolean isPriority, String groupId, long msgSeq) {
		doAck(resend, isPriority, groupId, msgSeq);
	}

	protected abstract MessageQueueDumper getMessageQueuePartitionDumper();

	protected abstract MessageQueueCursor doCreateCursor(String groupId);

	protected abstract void doNack(boolean resend, boolean isPriority, String groupId, List<Pair<Long, Integer>> msgSeqs);

	protected abstract void doAck(boolean resend, boolean isPriority, String groupId, long msgSeq);
}
