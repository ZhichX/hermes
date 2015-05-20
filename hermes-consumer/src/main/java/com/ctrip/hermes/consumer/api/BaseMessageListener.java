package com.ctrip.hermes.consumer.api;

import java.util.List;

import com.ctrip.hermes.core.constants.CatConstants;
import com.ctrip.hermes.core.message.ConsumerMessage;
import com.ctrip.hermes.core.message.ConsumerMessage.MessageStatus;
import com.ctrip.hermes.core.message.PropertiesHolder;
import com.ctrip.hermes.core.message.PropertiesHolderAware;
import com.dianping.cat.Cat;
import com.dianping.cat.configuration.NetworkInterfaceManager;
import com.dianping.cat.message.Event;
import com.dianping.cat.message.Transaction;
import com.dianping.cat.message.spi.MessageTree;

public abstract class BaseMessageListener<T> implements MessageListener<T> {

	private String m_groupId;

	public BaseMessageListener(String groupId) {
		m_groupId = groupId;
	}

	@Override
	public void onMessage(List<ConsumerMessage<T>> msgs) {
		if (msgs != null && !msgs.isEmpty()) {
			String topic = msgs.get(0).getTopic();

			for (ConsumerMessage<T> msg : msgs) {
				Transaction t = Cat.newTransaction("Message.Consumed", topic);
				MessageTree tree = Cat.getManager().getThreadLocalMessageTree();

				if (msg instanceof PropertiesHolderAware) {
					PropertiesHolder holder = ((PropertiesHolderAware) msg).getPropertiesHolder();
					String rootMsgId = holder.getDurableSysProperty(CatConstants.ROOT_MESSAGE_ID);
					String parentMsgId = holder.getDurableSysProperty(CatConstants.CURRENT_MESSAGE_ID);

					tree.setRootMessageId(rootMsgId);
					tree.setParentMessageId(parentMsgId);
				}

				try {
					t.addData("topic", topic);
					t.addData("key", msg.getKey());
					t.addData("groupId", m_groupId);
					// TODO
					t.addData("appId", "demo-app");

					onMessage(msg);
					// by design, if nacked, no effect
					msg.ack();

					String ip = NetworkInterfaceManager.INSTANCE.getLocalHostAddress();
					Cat.logEvent("Consumer:" + ip, msg.getTopic() + ":" + m_groupId, Event.SUCCESS, "key=" + msg.getKey());
					Cat.logEvent("Message:" + topic, "Consumed:" + ip, Event.SUCCESS, "key=" + msg.getKey());
					Cat.logMetricForCount(msg.getTopic());
					t.setStatus(MessageStatus.SUCCESS.equals(msg.getStatus()) ? Transaction.SUCCESS : "FAILED-WILL-RETRY");
				} catch (RuntimeException | Error e) {
					Cat.logError(e);
					t.setStatus(e);
				} finally {
					t.complete();
				}
			}

		}
	}

	protected abstract void onMessage(ConsumerMessage<T> msg);

}
