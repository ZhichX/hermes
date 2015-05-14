package com.ctrip.hermes.consumer.build;

import java.util.ArrayList;
import java.util.List;

import org.unidal.lookup.configuration.AbstractResourceConfigurator;
import org.unidal.lookup.configuration.Component;

import com.ctrip.hermes.consumer.ConsumerType;
import com.ctrip.hermes.consumer.DefaultConsumer;
import com.ctrip.hermes.consumer.engine.DefaultEngine;
import com.ctrip.hermes.consumer.engine.bootstrap.BrokerConsumerBootstrap;
import com.ctrip.hermes.consumer.engine.bootstrap.DefaultConsumerBootstrapManager;
import com.ctrip.hermes.consumer.engine.bootstrap.DefaultConsumerBootstrapRegistry;
import com.ctrip.hermes.consumer.engine.bootstrap.strategy.BrokerConsumptionStrategy;
import com.ctrip.hermes.consumer.engine.bootstrap.strategy.BrokerLongPollingConsumptionStrategy;
import com.ctrip.hermes.consumer.engine.bootstrap.strategy.DefaultBrokerConsumptionRegistry;
import com.ctrip.hermes.consumer.engine.command.processor.ConsumeMessageCommandProcessor;
import com.ctrip.hermes.consumer.engine.consumer.pipeline.internal.ConsumerTracingValve;
import com.ctrip.hermes.consumer.engine.lease.ConsumerLeaseManager;
import com.ctrip.hermes.consumer.engine.notifier.ConsumerNotifier;
import com.ctrip.hermes.consumer.engine.notifier.DefaultConsumerNotifier;
import com.ctrip.hermes.consumer.engine.pipeline.ConsumerPipeline;
import com.ctrip.hermes.consumer.engine.pipeline.ConsumerValveRegistry;
import com.ctrip.hermes.consumer.engine.pipeline.DefaultConsumerPipelineSink;
import com.ctrip.hermes.core.lease.LeaseManager;
import com.ctrip.hermes.core.message.codec.MessageCodec;
import com.ctrip.hermes.core.transport.command.CommandType;
import com.ctrip.hermes.core.transport.command.processor.CommandProcessor;
import com.ctrip.hermes.core.transport.endpoint.ClientEndpointChannelManager;
import com.ctrip.hermes.core.transport.endpoint.EndpointManager;

public class ComponentsConfigurator extends AbstractResourceConfigurator {

	@Override
	public List<Component> defineComponents() {
		List<Component> all = new ArrayList<Component>();

		// consumer
		all.add(A(DefaultConsumer.class));

		// engine
		all.add(A(DefaultEngine.class));

		all.add(C(CommandProcessor.class, CommandType.MESSAGE_CONSUME.toString(), ConsumeMessageCommandProcessor.class) //
		      .req(ConsumerNotifier.class)//
		      .req(MessageCodec.class));

		// bootstrap
		all.add(A(DefaultConsumerBootstrapManager.class));
		all.add(A(DefaultConsumerBootstrapRegistry.class));
		all.add(A(BrokerConsumerBootstrap.class));

		// consumption strategy
		all.add(A(DefaultBrokerConsumptionRegistry.class));
		all.add(C(BrokerConsumptionStrategy.class, ConsumerType.LONG_POLLING.toString(),
		      BrokerLongPollingConsumptionStrategy.class)//
		      .req(ConsumerNotifier.class)//
		      .req(EndpointManager.class)//
		      .req(ClientEndpointChannelManager.class)//
		      .req(LeaseManager.class)//
		      .req(MessageCodec.class));

		all.add(A(DefaultConsumerPipelineSink.class));

		// notifier
		all.add(A(DefaultConsumerNotifier.class));
		all.add(A(ConsumerValveRegistry.class));

		all.add(A(ConsumerTracingValve.class));

		all.add(A(ConsumerPipeline.class));

		all.add(A(ConsumerLeaseManager.class));

		return all;
	}

	public static void main(String[] args) {
		generatePlexusComponentsXmlFile(new ComponentsConfigurator());
	}
}
