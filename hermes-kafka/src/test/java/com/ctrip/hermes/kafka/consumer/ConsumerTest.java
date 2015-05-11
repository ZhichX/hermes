package com.ctrip.hermes.kafka.consumer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.ctrip.hermes.consumer.api.BaseMessageListener;
import com.ctrip.hermes.consumer.api.MessageListener;
import com.ctrip.hermes.consumer.engine.Engine;
import com.ctrip.hermes.consumer.engine.Subscriber;
import com.ctrip.hermes.core.message.ConsumerMessage;
import com.ctrip.hermes.producer.api.Producer;
import com.ctrip.hermes.producer.api.Producer.MessageHolder;

public class ConsumerTest {

	@Test
	public void testOneConsumerOneGroup() throws IOException {
		String topic = "kafka.SimpleTopic";
		String group = "group1";

		Producer producer = Producer.getInstance();

		Engine engine = Engine.getInstance();

		Subscriber s = new Subscriber(topic, group, new BaseMessageListener<VisitEvent>(group) {

			@Override
			protected void onMessage(ConsumerMessage<VisitEvent> msg) {
				VisitEvent event = msg.getBody();
				System.out.println("Receive: " + event);
			}
		});

		System.out.println("Starting consumer...");
		engine.start(Arrays.asList(s));

		try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
			while (true) {
				String line = in.readLine();
				if ("q".equals(line)) {
					break;
				}

				VisitEvent event = ProducerTest.generateEvent();
				MessageHolder holder = producer.message(topic, null, event);
				holder.send();
				System.out.println("Sent: " + event);
			}
		}

	}

	@Test
	public void testTwoConsumerOneGroup() throws IOException {
		String topic = "kafka.SimpleTopic";
		String group = "group1";

		Producer producer = Producer.getInstance();

		Engine engine = Engine.getInstance();

		Subscriber s1 = new Subscriber(topic, group, new MessageListener<VisitEvent>() {

			@Override
			public void onMessage(List<ConsumerMessage<VisitEvent>> msgs) {
				for (ConsumerMessage<VisitEvent> msg : msgs) {
					VisitEvent event = msg.getBody();
					System.out.println("Consumer1 Receive: " + event);
				}
			}
		});

		System.out.println("Starting consumer1...");
		engine.start(Arrays.asList(s1));

		Subscriber s2 = new Subscriber(topic, group, new MessageListener<VisitEvent>() {

			@Override
			public void onMessage(List<ConsumerMessage<VisitEvent>> msgs) {
				for (ConsumerMessage<VisitEvent> msg : msgs) {
					VisitEvent event = msg.getBody();
					System.out.println("Consumer2 Receive: " + event);
				}
			}
		});

		System.out.println("Starting consumer2...");
		engine.start(Arrays.asList(s2));

		try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
			while (true) {
				String line = in.readLine();
				if ("q".equals(line)) {
					break;
				}

				VisitEvent event = ProducerTest.generateEvent();
				MessageHolder holder = producer.message(topic, null, event);
				holder.send();
				System.out.println("Sent: " + event);
			}
		}
	}

	@Test
	public void testTwoConsumerTwoGroup() throws IOException {
		String topic = "kafka.SimpleTopic";
		String group1 = "group1";
		String group2 = "group2";

		Producer producer = Producer.getInstance();

		Engine engine = Engine.getInstance();

		Subscriber s1 = new Subscriber(topic, group1, new MessageListener<VisitEvent>() {

			@Override
			public void onMessage(List<ConsumerMessage<VisitEvent>> msgs) {
				for (ConsumerMessage<VisitEvent> msg : msgs) {
					VisitEvent event = msg.getBody();
					System.out.println("Consumer1 Receive: " + event);
				}
			}
		});

		System.out.println("Starting consumer1...");
		engine.start(Arrays.asList(s1));

		Subscriber s2 = new Subscriber(topic, group2, new MessageListener<VisitEvent>() {

			@Override
			public void onMessage(List<ConsumerMessage<VisitEvent>> msgs) {
				for (ConsumerMessage<VisitEvent> msg : msgs) {
					VisitEvent event = msg.getBody();
					System.out.println("Consumer2 Receive: " + event);
				}
			}
		});

		System.out.println("Starting consumer2...");
		engine.start(Arrays.asList(s2));

		try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
			while (true) {
				String line = in.readLine();
				if ("q".equals(line)) {
					break;
				}

				VisitEvent event = ProducerTest.generateEvent();
				MessageHolder holder = producer.message(topic, null, event);
				holder.send();
				System.out.println("Sent: " + event);
			}
		}
	}
}
