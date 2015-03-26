package com.ctrip.hermes.build;

import java.util.ArrayList;
import java.util.List;

import org.unidal.initialization.Module;
import org.unidal.lookup.configuration.AbstractResourceConfigurator;
import org.unidal.lookup.configuration.Component;

import com.ctrip.hermes.HermesProducerModule;
import com.ctrip.hermes.codec.Codec;
import com.ctrip.hermes.codec.CodecType;
import com.ctrip.hermes.codec.JsonCodec;
import com.ctrip.hermes.endpoint.DefaultEndpointChannelManager;
import com.ctrip.hermes.endpoint.DefaultEndpointManager;
import com.ctrip.hermes.endpoint.EndpointChannelManager;
import com.ctrip.hermes.endpoint.EndpointManager;
import com.ctrip.hermes.meta.MetaManager;
import com.ctrip.hermes.meta.MetaService;
import com.ctrip.hermes.meta.entity.Endpoint;
import com.ctrip.hermes.meta.internal.DefaultMetaManager;
import com.ctrip.hermes.meta.internal.DefaultMetaService;
import com.ctrip.hermes.meta.internal.LocalMetaLoader;
import com.ctrip.hermes.meta.internal.MetaLoader;
import com.ctrip.hermes.meta.internal.RemoteMetaLoader;
import com.ctrip.hermes.partition.HashPartitioningAlgo;
import com.ctrip.hermes.partition.PartitioningAlgo;
import com.ctrip.hermes.pipeline.Pipeline;
import com.ctrip.hermes.pipeline.PipelineSink;
import com.ctrip.hermes.pipeline.ValveRegistry;
import com.ctrip.hermes.pipeline.spi.Valve;
import com.ctrip.hermes.pipeline.spi.internel.TracingMessageValve;
import com.ctrip.hermes.producer.DefaultProducer;
import com.ctrip.hermes.producer.api.Producer;
import com.ctrip.hermes.producer.pipeline.DefaultMessageSink;
import com.ctrip.hermes.producer.pipeline.DefaultProducerSinkManager;
import com.ctrip.hermes.producer.pipeline.ProducerPipeline;
import com.ctrip.hermes.producer.pipeline.ProducerSinkManager;
import com.ctrip.hermes.producer.pipeline.ProducerValveRegistry;
import com.ctrip.hermes.producer.sender.BatchableMessageSender;
import com.ctrip.hermes.producer.sender.MessageSender;
import com.ctrip.hermes.producer.sender.SimpleMessageSender;
import com.ctrip.hermes.remoting.command.CommandProcessorManager;
import com.ctrip.hermes.remoting.command.CommandRegistry;
import com.ctrip.hermes.remoting.command.DefaultCommandRegistry;

public class ComponentsConfigurator extends AbstractResourceConfigurator {

	private final static String PRODUCER = "producer";

	@Override
	public List<Component> defineComponents() {
		List<Component> all = new ArrayList<Component>();

		all.add(C(Module.class, HermesProducerModule.ID, HermesProducerModule.class));

		// producer
		all.add(C(Producer.class, DefaultProducer.class) //
		      .req(Pipeline.class, PRODUCER)//
		);

		// pipeline
		all.add(C(Pipeline.class, PRODUCER, ProducerPipeline.class) //
		      .req(ValveRegistry.class, PRODUCER) //
		      .req(ProducerSinkManager.class)//
		);

		// valves
		all.add(C(ValveRegistry.class, PRODUCER, ProducerValveRegistry.class));
		all.add(C(Valve.class, TracingMessageValve.ID, TracingMessageValve.class));

		// sinks
		all.add(C(ProducerSinkManager.class, DefaultProducerSinkManager.class) //
		      .req(MetaService.class)//
		);

		all.add(C(PipelineSink.class, Endpoint.BROKER, DefaultMessageSink.class) //
		      .req(MessageSender.class, Endpoint.BROKER)//
		);
		all.add(C(PipelineSink.class, Endpoint.LOCAL, DefaultMessageSink.class) //
		      .req(MessageSender.class, Endpoint.LOCAL)//
		);
		all.add(C(PipelineSink.class, Endpoint.TRANSACTION, DefaultMessageSink.class) //
		      .req(MessageSender.class, Endpoint.TRANSACTION)//
		);
		// TODO kafka
		// all.add(C(PipelineSink.class, Endpoint.KAFKA, KafkaMessageSink.class) //
		// .req(MetaService.class, StoredMessageCodec.class, MessageCodec.class)); //

		// message sender
		all.add(C(MessageSender.class, Endpoint.BROKER, BatchableMessageSender.class)//
		      .req(EndpointManager.class)//
		      .req(EndpointChannelManager.class)//
		      .req(PartitioningAlgo.class)//
		      .req(MetaService.class)//
		);
		all.add(C(MessageSender.class, Endpoint.LOCAL, SimpleMessageSender.class)//
		      .req(EndpointManager.class)//
		      .req(EndpointChannelManager.class)//
		      .req(PartitioningAlgo.class)//
		      .req(MetaService.class)//
		);
		all.add(C(MessageSender.class, Endpoint.TRANSACTION, BatchableMessageSender.class)//
		      .req(EndpointManager.class)//
		      .req(EndpointChannelManager.class)//
		      .req(PartitioningAlgo.class)//
		      .req(MetaService.class)//
		);

		// partition algo
		all.add(C(PartitioningAlgo.class, HashPartitioningAlgo.class));

		// meta
		all.add(C(MetaLoader.class, LocalMetaLoader.ID, LocalMetaLoader.class));
		all.add(C(MetaLoader.class, RemoteMetaLoader.ID, RemoteMetaLoader.class));
		all.add(C(MetaManager.class, DefaultMetaManager.class));
		all.add(C(MetaService.class, DefaultMetaService.class) //
		      .req(MetaManager.class)//
		);

		// endpoint manager
		all.add(C(EndpointManager.class, DefaultEndpointManager.class) //
		      .req(MetaService.class)//
		);

		// endpoint channel
		all.add(C(EndpointChannelManager.class, DefaultEndpointChannelManager.class) //
		      .req(CommandProcessorManager.class)//
		);

		// command processor
		all.add(C(CommandProcessorManager.class, CommandProcessorManager.class) //
		      .req(CommandRegistry.class)//
		);
		all.add(C(CommandRegistry.class, DefaultCommandRegistry.class));

		// codec
		all.add(C(Codec.class, CodecType.JSON.toString(), JsonCodec.class));

		return all;
	}

	public static void main(String[] args) {
		generatePlexusComponentsXmlFile(new ComponentsConfigurator());
	}
}
