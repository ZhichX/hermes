package com.ctrip.hermes.meta.service;

import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.unidal.lookup.annotation.Inject;
import org.unidal.lookup.annotation.Named;

import com.ctrip.hermes.meta.core.AbstractMetaService;
import com.ctrip.hermes.meta.core.MetaManager;
import com.ctrip.hermes.meta.core.MetaService;

@Named(type = MetaService.class, value = ServerMetaService.ID)
public class ServerMetaService extends AbstractMetaService {

	public static final String ID = "server-meta";

	@Inject(ServerMetaManager.ID)
	private MetaManager m_manager;

	@Override
	public void initialize() throws InitializationException {
		refreshMeta(m_manager.getMeta());
	}

}
