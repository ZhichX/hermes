package com.ctrip.hermes.meta.server;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;

import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.unidal.lookup.annotation.Inject;
import org.unidal.lookup.annotation.Named;

import com.ctrip.hermes.core.env.ClientEnvironment;
import com.ctrip.hermes.core.utils.PlexusComponentLocator;
import com.ctrip.hermes.meta.resource.TopicResource;

@Named
public class MetaRestServer implements LogEnabled {
	public static final String META_HOST = "meta-host";

	public static final String META_PORT = "meta-port";

	private Logger m_logger;

	private HttpServer m_server;
	
	@Inject
	private ClientEnvironment m_env;

	private ResourceConfig configResource() {
		ResourceConfig rc = new ResourceConfig();
		rc.register(CharsetResponseFilter.class);
		rc.register(CORSResponseFilter.class);
		rc.register(ObjectMapperProvider.class);
		rc.register(MultiPartFeature.class);
		rc.packages(TopicResource.class.getPackage().getName());
		return rc;
	}

	@Override
	public void enableLogging(Logger logger) {
		m_logger = logger;
	}

	private URI getBaseURI() throws IOException {
		Properties m_properties = m_env.getGlobalConfig();
		int port = Integer.valueOf(m_properties.getProperty(META_PORT));
		String host = m_properties.getProperty(META_HOST);
		URI result = UriBuilder.fromUri("http://" + host).port(port).build();
		return result;
	}

	private void showPaths(ResourceConfig rc, URI baseURI) {
		Set<Class<?>> classes = rc.getClasses();
		for (Class<?> cls : classes) {
			Path classPath = cls.getAnnotation(Path.class);
			if (classPath != null) {
				m_logger.debug("REST Root API: " + baseURI + classPath.value());
			}
			Method[] methods = cls.getDeclaredMethods();
			for (Method method : methods) {
				String op = null;
				Annotation[] annotations = method.getDeclaredAnnotations();
				for (Annotation a : annotations) {
					if (a.annotationType().equals(GET.class)) {
						op = GET.class.getSimpleName().toUpperCase();
					} else if (a.annotationType().equals(POST.class)) {
						op = POST.class.getSimpleName().toUpperCase();
					} else if (a.annotationType().equals(DELETE.class)) {
						op = DELETE.class.getSimpleName().toUpperCase();
					} else if (a.annotationType().equals(PUT.class)) {
						op = PUT.class.getSimpleName().toUpperCase();
					} else if (a.annotationType().equals(HEAD.class)) {
						op = HEAD.class.getSimpleName().toUpperCase();
					}
					if (op != null) {
						break;
					}
				}
				Path methodPath = method.getAnnotation(Path.class);
				if (methodPath != null && op != null) {
					m_logger.info("REST API: " + op + " " + baseURI + classPath.value() + methodPath.value());
				}
			}
		}
	}

	public void start() throws IOException {
		ResourceConfig rc = configResource();
		URI baseURI = getBaseURI();
		if (!baseURI.getHost().equals("localhost") && !baseURI.getHost().equals("0.0.0.0")
		      && !baseURI.getHost().equals("127.0.0.1")) {
			m_logger.info("invalid host: " + baseURI.getHost());
			return;
		}
		m_server = GrizzlyHttpServerFactory.createHttpServer(baseURI, rc);
		try {
			m_server.start();
			showPaths(rc, baseURI);
		} catch (IOException e) {
			m_logger.error(e.getMessage());
		}
		m_logger.info("Base URI: " + baseURI);
	}

	public void stop() {
		if (m_server != null) {
			m_server.shutdownNow();
		}
	}

	public static void main(String[] args) throws InterruptedException, IOException {
		MetaRestServer server = PlexusComponentLocator.lookup(MetaRestServer.class);
		server.start();

		Thread.currentThread().join();
	}
}
