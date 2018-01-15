package com.sap.demo.bookstore.service;

import java.util.Arrays;
import java.util.List;
import java.sql.Connection;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import com.sap.cloud.sdk.hana.connectivity.cds.CDSException;
import com.sap.cloud.sdk.hana.connectivity.cds.CDSQuery;
import com.sap.cloud.sdk.hana.connectivity.cds.CDSSelectQueryBuilder;
import com.sap.cloud.sdk.hana.connectivity.cds.CDSSelectQueryResult;
import com.sap.cloud.sdk.hana.connectivity.cds.ConditionBuilder;
import com.sap.cloud.sdk.hana.connectivity.handler.CDSDataSourceHandler;
import com.sap.cloud.sdk.hana.connectivity.handler.DataSourceHandlerFactory;
import com.sap.cloud.sdk.service.prov.api.EntityData;
import com.sap.cloud.sdk.service.prov.api.operations.Query;
import com.sap.cloud.sdk.service.prov.api.operations.Read;
import com.sap.cloud.sdk.service.prov.api.request.QueryRequest;
import com.sap.cloud.sdk.service.prov.api.request.ReadRequest;
import com.sap.cloud.sdk.service.prov.api.response.ErrorResponse;
import com.sap.cloud.sdk.service.prov.api.response.QueryResponse;
import com.sap.cloud.sdk.service.prov.api.response.ReadResponse;

public class StoreService {

	private static Logger logger = LoggerFactory.getLogger(StoreService.class);

	@Query(entity = "Author", serviceName = "store")
	public QueryResponse findAuthors(QueryRequest queryRequest) {
		try {
			QueryResponse queryResponse = QueryResponse.setSuccess().setEntityData(getEntitySet(queryRequest))
					.response();
			return queryResponse;
		} catch (Exception e) {
			return null;
		}
	}

	@Read(entity = "Author", serviceName = "store")
	public ReadResponse getAuthor(ReadRequest readRequest) {
		try {
			ReadResponse readResponse = ReadResponse.setSuccess().setData(readEntity(readRequest)).response();
			return readResponse;
		} catch (Exception e) {
			return null;
		}
	}

	@Query(entity = "Book", serviceName = "store")
	public QueryResponse findBooks(QueryRequest queryRequest) {
		try {
			QueryResponse queryResponse = QueryResponse.setSuccess().setEntityData(getEntitySet(queryRequest))
					.response();
			return queryResponse;
		} catch (Exception e) {
			return null;
		}
	}

	@Read(entity = "Book", serviceName = "store")
	public ReadResponse getBook(ReadRequest readRequest) {
		try {
			ReadResponse readResponse = ReadResponse.setSuccess().setData(readEntity(readRequest)).response();
			return readResponse;
		} catch (Exception e) {
			return null;
		}
	}

	private EntityData readAuthor(Map<String, Object> authorId, QueryRequest queryRequest) {

		CDSDataSourceHandler dsHandler = DataSourceHandlerFactory.getInstance().getCDSHandler(getConnection(),
				"store");
		List<String> properties = Arrays.asList("authorId");
		EntityData entityData = null;

		try {
			entityData = dsHandler.executeRead("Author", authorId, properties);

		} catch (CDSException e) {
			e.printStackTrace();
		}

		return entityData;

	}

	/**
	 * 
	 * */
	private List<EntityData> getBooksForAuthor(Map<String, Object> authorId) {
		String fullQualifiedName = "store.Book";
		CDSDataSourceHandler dsHandler = DataSourceHandlerFactory.getInstance().getCDSHandler(getConnection(), "store");

		try {

			CDSQuery cdsQuery = new CDSSelectQueryBuilder(fullQualifiedName).where(
					new ConditionBuilder().columnName("authorId").EQ(authorId.get("authorId").toString()).build())
					.build();

			CDSSelectQueryResult cdsSelectQueryResult = dsHandler.executeQuery(cdsQuery);
			return cdsSelectQueryResult.getResult();
		} catch (CDSException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Query(entity = "Book", serviceName = "store", sourceEntity = "Author")
	public QueryResponse getAuthorBooks(QueryRequest queryRequest) {
		QueryResponse queryResponse = null;
		EntityData authorEntity;

		try {

			String sourceEntityName = queryRequest.getSourceEntityName();

			if (sourceEntityName.equals("Author")) {
				
				authorEntity = readAuthor(queryRequest.getSourceKeys(), queryRequest);

				if (authorEntity == null) {
					ErrorResponse errorResponse = ErrorResponse.getBuilder().setMessage("Author not found")
							.setStatusCode(400).response();

					queryResponse = QueryResponse.setError(errorResponse);
				} else {
					queryResponse = QueryResponse.setSuccess()
							.setEntityData(getBooksForAuthor(queryRequest.getSourceKeys())).response();
				}

			} else {

			}

		} catch (Exception e) {
			logger.error("==> Exception fetching author books: " + e.getMessage());
			queryResponse = QueryResponse
					.setError(ErrorResponse.getBuilder().setMessage(e.getMessage()).setCause(e).response());
		}

		return queryResponse;
	}

	private List<EntityData> getEntitySet(QueryRequest queryRequest) {
		String fullQualifiedName = queryRequest.getEntityMetadata().getNamespace() + "."
				+ queryRequest.getEntityMetadata().getName();

		CDSDataSourceHandler dsHandler = DataSourceHandlerFactory.getInstance().getCDSHandler(getConnection(),
				queryRequest.getEntityMetadata().getNamespace());
		try {
			CDSQuery cdsQuery = new CDSSelectQueryBuilder(fullQualifiedName).build();
			CDSSelectQueryResult cdsSelectQueryResult = dsHandler.executeQuery(cdsQuery);
			return cdsSelectQueryResult.getResult();
		} catch (Exception e) {
			logger.error("==> Exception while fetching query data from CDS: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	private EntityData readEntity(ReadRequest readRequest) throws Exception {
		CDSDataSourceHandler dsHandler = DataSourceHandlerFactory.getInstance().getCDSHandler(getConnection(),
				readRequest.getEntityMetadata().getNamespace());
		EntityData ed = dsHandler.executeRead(readRequest.getEntityMetadata().getName(), readRequest.getKeys(),
				readRequest.getEntityMetadata().getElementNames());
		return ed;
	}

	private static Connection getConnection() {
		Connection conn = null;
		Context ctx;
		try {
			ctx = new InitialContext();
			conn = ((DataSource) ctx.lookup("java:comp/env/jdbc/java-hdi-container")).getConnection();
			System.out.println("conn = " + conn);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return conn;
	}

}
