package org.mule.modules.gluuscim.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.mule.api.MuleEvent;
import org.mule.api.store.ObjectStoreException;
import org.mule.modules.gluuscim.config.GluuSCIMConnectorConfig;
import org.mule.modules.gluuscim.entities.GluuSCIMEntitlement;
import org.mule.modules.gluuscim.entities.GluuSCIMGetUserJsonRequest;
import org.mule.modules.gluuscim.entities.GluuSCIMUser;
import org.mule.modules.gluuscim.exception.GluuSCIMConnectorException;
import org.mule.modules.gluuscim.exception.GluuSCIMServerErrorException;
import org.mule.util.store.PartitionedInMemoryObjectStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

/**
 * Client implementation for calling Gluu SCIM endpoints.
 * 
 * @author martinatutokiova
 */

public class GluuSCIMClient {

	private static final String SEARCH_USER = "identity/seam/resource/restv1/scim/v2/Users/Search";
	private static final String USER_ENDPOINT = "identity/seam/resource/restv1/scim/v2/Users";
	
	private static final String AUTHORIZATION = "Authorization";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String HTTPS = "https://";
	
	private static final String DEFAULT_USER_OBJECT_STORE = "_defaultTransientUserObjectStore";

	private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();
	private transient final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	
	private final Client client;
	private final WebResource apiResource;
	private final GluuSCIMClientTokenHelper tokenHelper;

	/**
	 * Constructor - Setup Jersey Client and WebResource objects
	 * @param connector
	 */
	public GluuSCIMClient (GluuSCIMConnectorConfig connectorConfig) {
		ClientConfig clientConfig = new DefaultClientConfig();
		clientConfig.getClasses().add(JacksonJaxbJsonProvider.class);
		clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
		this.client = Client.create(clientConfig);

		String host = HTTPS + connectorConfig.getHost();
		this.apiResource = this.client.resource(host);
        this.tokenHelper = new GluuSCIMClientTokenHelper(connectorConfig, host, apiResource);
	}
	
	/** Returns response from the get user call */
	public GluuSCIMUser getUser(MuleEvent event/*TestObjectStore objectStore*/, String attribute, String value) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
		try{
			LOGGER.info(String.format("Processing getUser search request for attribute %s and value %s", attribute, value));
			
			PartitionedInMemoryObjectStore<Serializable> objectStore = event.getMuleContext().getRegistry().lookupObject(DEFAULT_USER_OBJECT_STORE);
			
			GluuSCIMGetUserJsonRequest request = new GluuSCIMGetUserJsonRequest();
			request.setAttribute(attribute);
			request.setValue(value);
			
			String jsonRequest = null;
			try {
				jsonRequest = JSON_OBJECT_MAPPER.writeValueAsString(request);
			} catch (JsonProcessingException e1) {
				throw new GluuSCIMConnectorException(e1.getMessage());
			}
			
			String authorizedScimToken = tokenHelper.obtainToken(objectStore, RequestMethod.POST, SEARCH_USER, jsonRequest);
			
			LOGGER.info(String.format("Sending request to %s with authorized SCIM token %s in header", apiResource + "/" + SEARCH_USER, authorizedScimToken));
			
			String validatedResponse = GluuSCIMClientMappingHelper.getValidatedResponse(apiResource
					.path(SEARCH_USER)
					.accept(MediaType.APPLICATION_JSON)
					.header(CONTENT_TYPE, MediaType.APPLICATION_JSON)
					.header(AUTHORIZATION, authorizedScimToken)
					.type(MediaType.APPLICATION_JSON)
					.post(ClientResponse.class, jsonRequest));
			
			return GluuSCIMClientMappingHelper.mapToUserObject(validatedResponse);
		} catch (RuntimeException e){
			throw new GluuSCIMConnectorException(e.getMessage());
		}
		
	}

	/** Returns response from the create user call */
	public GluuSCIMUser createUser(MuleEvent event/*TestObjectStore objectStore*/, GluuSCIMUser user) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
		try{
			PartitionedInMemoryObjectStore<Serializable> objectStore = event.getMuleContext().getRegistry().lookupObject(DEFAULT_USER_OBJECT_STORE);
			
			String jsonRequest = GluuSCIMClientMappingHelper.mapToJsonRequest(user);
			String authorizedScimToken = tokenHelper.obtainToken(objectStore, RequestMethod.POST, USER_ENDPOINT, jsonRequest);
			
			LOGGER.info(String.format("Sending request to %s with authorized SCIM token %s in header %s", apiResource + "/" + USER_ENDPOINT, authorizedScimToken, jsonRequest));
			
			String validatedResponse = GluuSCIMClientMappingHelper.getValidatedResponse(apiResource
					.path(USER_ENDPOINT)
					.accept(MediaType.APPLICATION_JSON)
					.header(CONTENT_TYPE, MediaType.APPLICATION_JSON)
					.header(AUTHORIZATION, authorizedScimToken)
					.post(ClientResponse.class, jsonRequest));
			
			return GluuSCIMClientMappingHelper.mapToUserObject(validatedResponse);
		} catch (RuntimeException e){
			throw new GluuSCIMConnectorException(e.getMessage());
		}
	}

	/** Returns response from the update user call */
	public GluuSCIMUser updateUser(MuleEvent event/*TestObjectStore objectStore*/, GluuSCIMUser user) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
		try{
			
			PartitionedInMemoryObjectStore<Serializable> objectStore = event.getMuleContext().getRegistry().lookupObject(DEFAULT_USER_OBJECT_STORE);
			
			String url = USER_ENDPOINT + "/" + user.getGluuId();
			
			String jsonRequest = GluuSCIMClientMappingHelper.mapToJsonRequest(user);
			String authorizedScimToken = tokenHelper.obtainToken(objectStore, RequestMethod.PUT, url, jsonRequest);
			
			LOGGER.info(String.format("Sending request to %s with authorized SCIM token %s in header", apiResource + "/" + url, authorizedScimToken));
			
			String validatedResponse = GluuSCIMClientMappingHelper.getValidatedResponse(apiResource
					.path(url)
					.accept(MediaType.APPLICATION_JSON)
					.header(CONTENT_TYPE, MediaType.APPLICATION_JSON)
					.header(AUTHORIZATION, authorizedScimToken)
					.put(ClientResponse.class, jsonRequest));
			
			return GluuSCIMClientMappingHelper.mapToUserObject(validatedResponse);
		} catch (RuntimeException e){
			throw new GluuSCIMConnectorException(e.getMessage());
		}
	}
	
	//////////////
	/// RUNNER ///
	//////////////
	
	public static void main(String[] args) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
		GluuSCIMConnectorConfig connectorConfig = new GluuSCIMConnectorConfig();
		connectorConfig.setHost("idp.d.aws.economist.com");
		connectorConfig.setAatRefreshToken("f0799352-6200-4e33-8c8d-c6aa7bff00cd");
		connectorConfig.setUsername("@!E0E2.8150.B9D2.14A0!0001!6A42.EB0A!0008!C1BA.E22F");
		connectorConfig.setPassword("5ab8e319-b4d4-4808-b06d-583bb109ca90");
		connectorConfig.setRedirectUri("https://dev-economistapi.cloudhub.io");
		
//		GluuSCIMClient client = new GluuSCIMClient(connectorConfig);
//		client.getUser(client.new TestObjectStore(), "uid", "martina.tutokiova+07101@gmail.com");
		
		GluuSCIMUser user = new GluuSCIMUser();
		user.setDisplayName("Martina Tutokiova");
		user.setEmail("martina.tutokiova+07101@gmail.com");
		user.setFirstName("Martina_updated");
		user.setLastName("Tutokiova_updated");
		user.setLegacyPassword("legacyPassword_update");
		user.setPassword("password_update");
		user.setGluuId("@!E0E2.8150.B9D2.14A0!0001!6A42.EB0A!0000!04F9.FD0A");
		
		List<GluuSCIMEntitlement> entitlements = new ArrayList<GluuSCIMEntitlement>();
		GluuSCIMEntitlement entitlement = new GluuSCIMEntitlement();
		entitlement.setEndDate("1400000001");
		entitlement.setProductCode("Digital");
		entitlement.setProductName("Digital");
		entitlement.setStartDate("15000000001");
		entitlements.add(entitlement);
		user.setEntitlements(entitlements);
//		client.updateUser(client.new TestObjectStore(), user);
//		client.createUser(client.new TestObjectStore(), user);
	}
	
	public class TestObjectStore {
		
		private Map<String, String> map = new HashMap<String, String>();
		
		public boolean contains(String parameter){
			return map.containsKey(parameter);
		}
		
		public void store(String key, String value){
			map.put(key, value);
		}
		
		public void remove(String key) throws ObjectStoreException{
			map.remove(key);
		}
		
		public String retrieve(String key) throws ObjectStoreException{
			return map.get(key);
		}
	}

}
