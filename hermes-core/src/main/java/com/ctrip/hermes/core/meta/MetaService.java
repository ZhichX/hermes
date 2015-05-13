package com.ctrip.hermes.core.meta;

import java.util.List;

import com.ctrip.hermes.core.bo.Tpg;
import com.ctrip.hermes.core.lease.Lease;
import com.ctrip.hermes.core.lease.LeaseAcquireResponse;
import com.ctrip.hermes.meta.entity.Codec;
import com.ctrip.hermes.meta.entity.Datasource;
import com.ctrip.hermes.meta.entity.Endpoint;
import com.ctrip.hermes.meta.entity.Meta;
import com.ctrip.hermes.meta.entity.Partition;
import com.ctrip.hermes.meta.entity.Storage;
import com.ctrip.hermes.meta.entity.Topic;

public interface MetaService {

	String getEndpointType(String topic);

	/**
	 * @param topic
	 * @return
	 */
	List<Partition> getPartitions(String topic);

	/**
	 * @param endpointId
	 * @return
	 */
	Endpoint findEndpoint(String endpointId);

	/**
	 * 
	 * @param topic
	 */
	Storage findStorage(String topic);

	/**
	 * @param topic
	 * @return
	 */

	Partition findPartition(String topic, int shard);

	/**
	 * @param topicPattern
	 * @return
	 */
	List<Topic> findTopicsByPattern(String topicPattern);

	/**
	 * 
	 * @param topic
	 * @return
	 */
	Topic findTopic(String topic);

	/**
	 * 
	 * @param topic
	 * @return
	 */
	Codec getCodecByTopic(String topic);

	/**
	 * 
	 * @param topic
	 * @param groupId
	 * @return
	 */
	List<Partition> getPartitions(String topic, String groupId);

	/**
	 * @param groupId
	 * @return
	 */
	int getGroupIdInt(String groupId);

	List<Datasource> listMysqlDataSources();

	Topic findTopic(long topicId);

	int getAckTimeoutSeconds(String topic);

	Codec getCodecByType(String codecType);

	public void refreshMeta(Meta meta);

	LeaseAcquireResponse tryAcquireConsumerLease(Tpg tpg, String sessionId);

	LeaseAcquireResponse tryRenewConsumerLease(Tpg tpg, Lease lease, String sessionId);

}
