package com.ctrip.hermes.portal.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.codehaus.plexus.util.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unidal.dal.jdbc.DalException;
import org.unidal.dal.jdbc.DalNotFoundException;

import com.alibaba.fastjson.JSON;
import com.ctrip.hermes.core.utils.PlexusComponentLocator;
import com.ctrip.hermes.meta.entity.Topic;
import com.ctrip.hermes.metaservice.model.Schema;
import com.ctrip.hermes.portal.pojo.SchemaView;
import com.ctrip.hermes.portal.server.RestException;
import com.ctrip.hermes.portal.service.SchemaService;
import com.ctrip.hermes.portal.service.TopicService;
import com.google.common.io.ByteStreams;

@Path("/schemas/")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class SchemaResource {

	private static final Logger logger = LoggerFactory.getLogger(SchemaResource.class);

	private SchemaService schemaService = PlexusComponentLocator.lookup(SchemaService.class);

	private TopicService topicService = PlexusComponentLocator.lookup(TopicService.class);

	/**
	 * 
	 * @param fileInputStream
	 * @param fileHeader
	 * @param jarInputStream
	 * @param jarHeader
	 * @param content
	 * @param topicId
	 * @return
	 */
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response createSchema(@FormDataParam("file") InputStream fileInputStream,
	      @FormDataParam("file") FormDataContentDisposition fileHeader, @FormDataParam("schema") String content,
	      @FormDataParam("topicId") long topicId) {
		logger.debug("create schema, topicId {}, content {}, fileHeader {}", topicId, content, fileHeader);
		if (StringUtils.isEmpty(content)) {
			throw new RestException("HTTP POST body is empty", Status.BAD_REQUEST);
		}
		SchemaView schemaView = null;
		try {
			schemaView = JSON.parseObject(content, SchemaView.class);
		} catch (Exception e) {
			logger.warn("parse schema failed, content {}", content);
			throw new RestException(e, Status.BAD_REQUEST);
		}

		Topic topic = topicService.getTopic(topicId);
		if (topic == null) {
			throw new RestException("Topic not found: " + topicId, Status.NOT_FOUND);
		}

		byte[] fileContent = null;
		if (fileInputStream != null) {
			try {
				fileContent = ByteStreams.toByteArray(fileInputStream);
			} catch (IOException e) {
				logger.warn("Read file input stream failed", e);
				throw new RestException(e, Status.BAD_REQUEST);
			}
		} else {
			if ("avro".equalsIgnoreCase(schemaView.getType())) {
				throw new RestException("avro schema file needed.", Status.BAD_REQUEST);
			}
		}

		try {
			if ("avro".equalsIgnoreCase(schemaView.getType())) {
				schemaService.checkAvroSchema(topic.getName() + "-value", fileContent);
			}
			schemaView = schemaService.createSchema(schemaView, topic);
			schemaView = schemaService.updateSchemaFile(schemaView, fileContent, fileHeader);
		} catch (Exception e) {
			logger.warn("Create schema failed", e);
			if (schemaView.getId() != null) {
				try {
					schemaService.deleteSchema(schemaView.getId());
				} catch (DalException e1) {
					throw new RestException(e1, Status.INTERNAL_SERVER_ERROR);
				}
			}
			throw new RestException(e, Status.INTERNAL_SERVER_ERROR);
		}
		return Response.status(Status.CREATED).entity(schemaView).build();
	}

	/**
	 * 
	 * @param schemaId
	 * @return
	 */
	@DELETE
	@Path("{id}")
	public Response deleteSchema(@PathParam("id") long schemaId) {
		logger.debug("delete schema {}", schemaId);
		try {
			schemaService.deleteSchema(schemaId);
		} catch (DalNotFoundException e) {
			throw new RestException("Schema not found: " + schemaId, Status.NOT_FOUND);
		} catch (DalException e) {
			logger.warn("Delete schema failed", e);
			throw new RestException(e, Status.INTERNAL_SERVER_ERROR);
		}
		return Response.status(Status.OK).build();
	}

	/**
	 * 
	 * @param schemaId
	 * @return
	 */
	@GET
	@Path("{id}/schema")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadSchema(@PathParam("id") long schemaId) {
		logger.debug("download schema {}", schemaId);
		Schema schema = null;
		try {
			schema = schemaService.getSchemaMeta(schemaId);
		} catch (DalNotFoundException e) {
			throw new RestException("Schema not found: " + schemaId, Status.NOT_FOUND);
		} catch (Exception e) {
			logger.warn("Download schema failed", e);
			throw new RestException(e, Status.INTERNAL_SERVER_ERROR);
		}

		String fileProperties = schema.getSchemaProperties();
		if (StringUtils.isEmpty(fileProperties)) {
			throw new RestException("Schema file not found: " + schemaId, Status.NOT_FOUND);
		}

		return Response.status(Status.OK).header("content-disposition", fileProperties).entity(schema.getSchemaContent())
		      .build();
	}

	/**
	 * 
	 * @param schemaId
	 * @return
	 */
	@GET
	@Path("{id}/jar")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadJar(@PathParam("id") long schemaId) {
		logger.debug("download jar {}", schemaId);
		Schema schema = null;
		try {
			schema = schemaService.getSchemaMeta(schemaId);
		} catch (DalNotFoundException e) {
			throw new RestException("Schema not found: " + schemaId, Status.NOT_FOUND);
		} catch (Exception e) {
			logger.warn("Download jar failed", e);
			throw new RestException(e, Status.INTERNAL_SERVER_ERROR);
		}

		String fileProperties = schema.getJarProperties();
		if (StringUtils.isEmpty(fileProperties)) {
			throw new RestException("Schema file not found: " + schemaId, Status.NOT_FOUND);
		}

		return Response.status(Status.OK).header("content-disposition", fileProperties).entity(schema.getJarContent())
		      .build();
	}

	/**
	 * 
	 * @param name
	 * @return
	 */
	@GET
	@Path("{id}")
	public SchemaView getSchema(@PathParam("id") long schemaId) {
		logger.debug("get schema {}", schemaId);
		SchemaView schema = null;
		try {
			schema = schemaService.getSchemaView(schemaId);
		} catch (DalNotFoundException e) {
			throw new RestException("Schema not found: " + schemaId, Status.NOT_FOUND);
		} catch (Exception e) {
			logger.warn("get schema failed", e);
			throw new RestException(e, Status.INTERNAL_SERVER_ERROR);
		}
		return schema;
	}

	/**
	 * 
	 * @param schemaName
	 * @return
	 */
	@GET
	public List<SchemaView> findSchemas() {
		List<SchemaView> returnResult = new ArrayList<SchemaView>();
		try {
			List<Schema> schemaMetas = schemaService.listLatestSchemaMeta();
			for (Schema schema : schemaMetas) {
				SchemaView schemaView = new SchemaView(schema);
				returnResult.add(schemaView);
			}
		} catch (Exception e) {
			logger.warn("list latest schema failed", e);
			throw new RestException(e, Status.INTERNAL_SERVER_ERROR);
		}
		return returnResult;
	}

	@POST
	@Path("{id}/compatibility")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response compatibility(@PathParam("id") Long schemaId, @FormDataParam("file") InputStream fileInputStream,
	      @FormDataParam("file") FormDataContentDisposition fileHeader) {
		logger.debug("test compatilibity schemaId {} fileHeader {}", schemaId, fileHeader);
		Schema schema = null;
		try {
			schema = schemaService.getSchemaMeta(schemaId);
		} catch (DalNotFoundException e) {
			throw new RestException("Schema not found: " + schemaId, Status.NOT_FOUND);
		} catch (Exception e) {
			logger.warn("get schema failed, schemaId {}", schemaId);
			throw new RestException(e, Status.INTERNAL_SERVER_ERROR);
		}

		boolean result = false;
		try {
			byte[] fileContent = ByteStreams.toByteArray(fileInputStream);
			result = schemaService.verifyCompatible(schema, fileContent);
		} catch (Exception e) {
			logger.warn("Read input stream failed", e);
			throw new RestException(e, Status.INTERNAL_SERVER_ERROR);
		}
		Map<String, Object> entity = new HashMap<>();
		entity.put("is_compatible", result);
		return Response.status(Status.OK).entity(entity).build();
	}

	@GET
	@Path("{id}/compatibility")
	public Response getCompatibility(@PathParam("id") Long schemaId) {
		logger.debug("get compatibility {}", schemaId);
		Schema schema = null;
		try {
			schema = schemaService.getSchemaMeta(schemaId);
		} catch (DalNotFoundException e) {
			throw new RestException("Schema not found: " + schemaId, Status.NOT_FOUND);
		} catch (Exception e) {
			logger.warn("get schema failed, schemaId {}", schemaId);
			throw new RestException(e, Status.INTERNAL_SERVER_ERROR);
		}

		String result = null;
		try {
			result = schemaService.getCompatible(schema);
		} catch (Exception e) {
			logger.warn("get compatible failed", e);
			throw new RestException(e, Status.INTERNAL_SERVER_ERROR);
		}
		return Response.status(Status.OK).entity(result).build();
	}
}
