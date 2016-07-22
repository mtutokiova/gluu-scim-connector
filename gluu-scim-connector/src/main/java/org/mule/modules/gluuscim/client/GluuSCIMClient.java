package org.mule.modules.gluuscim.client;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.mule.api.MuleEvent;
import org.mule.api.store.ObjectStoreException;
import org.mule.modules.gluuscim.config.GluuSCIMConnectorConfig;
import org.mule.modules.gluuscim.entities.GluuSCIMGetAatTokenJsonResponse;
import org.mule.modules.gluuscim.entities.GluuSCIMGetAuthorizedTokenJsonRequest;
import org.mule.modules.gluuscim.entities.GluuSCIMGetAuthorizedTokenJsonResponse;
import org.mule.modules.gluuscim.entities.GluuSCIMGetTicketJsonResponse;
import org.mule.modules.gluuscim.entities.GluuSCIMGetTokenJsonResponse;
import org.mule.modules.gluuscim.entities.GluuSCIMPutUserJsonRequest;
import org.mule.modules.gluuscim.entities.GluuSCIMUserExtension;
import org.mule.modules.gluuscim.entities.GluuSCIMUserObjectName;
import org.mule.modules.gluuscim.exception.GluuSCIMConnectorException;
import org.mule.modules.gluuscim.exception.GluuSCIMServerErrorException;
import org.mule.util.store.PartitionedPersistentObjectStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.MediaType;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Client implementation for calling Gluu SCIM endpoints.
 * 
 * @author martinatutokiova
 */

public class GluuSCIMClient {

	private static final String APPLICATION_JSON = "application/json";
	private static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String GET_AAT_TOKEN = "oxauth/seam/resource/restv1/oxauth/token";
	private static final String GET_SCIM_TOKEN = "oxauth/seam/resource/restv1/requester/rpt";
	private static final String AUTHORIZE_SCIM_TOKEN = "oxauth/seam/resource/restv1/requester/perm";
	private static final String GET_USER = "identity/seam/resource/restv1/scim/v2/Users/@!E0E2.8150.B9D2.14A0!0001!6A42.EB0A!0000!236E.FBB7";
	
	private static final String AAT_TOKEN = "aatToken";
	private static final String AAT_REFRESH_TOKEN = "aatRefreshToken";
	private static final String AUTHORIZATION = "Authorization";
	private static final String BEARER = "Bearer ";
	private static final String BASIC = "Basic ";
	private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();
	

//	private PartitionedPersistentObjectStore<Serializable> objectStore;
	private TestObjectStore objectStore;
	
	// set initial token expiration time to past date
	private static long AAT_TOKEN_EXPIRATION_TIME = System.currentTimeMillis()/1000 - 5000;
	
	private transient final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	private final Client client;
	private final WebResource apiResource;
	private final GluuSCIMConnectorConfig connectorConfig;
	
	/**
	 * Constructor - Setup Jersey Client and WebResource objects
	 * @param connector
	 */
	public GluuSCIMClient (GluuSCIMConnectorConfig connectorConfig) {
		this.connectorConfig = connectorConfig;
		
		ClientConfig clientConfig = new DefaultClientConfig();
		clientConfig.getClasses().add(JacksonJaxbJsonProvider.class);
		clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        this.client = Client.create(clientConfig);
        this.apiResource = this.client.resource(getApiUrl());
	}
	
	/** Returns response from the get user call */
	public String getUser(/*MuleEvent event*/ TestObjectStore objectStore) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
		String authorizedScimToken = obtainToken(objectStore, null);
		
		System.out.println(String.format("Sending request to %s with authorized SCIM token %s in header", apiResource + "/" + GET_USER, authorizedScimToken));
		LOGGER.info(String.format("Sending request to %s with authorized SCIM token %s in header", apiResource + "/" + GET_USER, authorizedScimToken));
		
		String responseString = getValidatedResponse(apiResource
				.path(GET_USER)
				.accept("application/scim+json")
				.header("Content-Type", "application/scim+json")
				.header(AUTHORIZATION, authorizedScimToken)
				.get(ClientResponse.class));
		
		return responseString;
		
//		try {
//			return jsonObjectMapper.readValue(responseString, GluuSCIMGetTicketJsonResponse.class).getScimTicket();
//		} catch (IOException e) {
//			throw new GluuSCIMConnectorException(e.getMessage());
//		}
		
	}
	
	/** Returns response from the create user call */
	public String createUser(/*MuleEvent event*/ TestObjectStore objectStore) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
		
		GluuSCIMUserObjectName name = new GluuSCIMUserObjectName();
		name.setFamilyName("myTestingFamilyName");
		name.setGivenName("myTestingGivenName");
		
		GluuSCIMUserExtension userExtension = new GluuSCIMUserExtension();
		userExtension.setPrintPlusDigital(new String[]{"abc", "def"});
		
		GluuSCIMPutUserJsonRequest request = new GluuSCIMPutUserJsonRequest();
		request.setSchemas(new String[]{"urn:ietf:params:scim:schemas:core:2.0:User",
				 "urn:ietf:params:scim:schemas:extension:gluu:2.0:User"});
		request.setName(name);
//		user.setUserName("myTestingName@example.com");
		request.setUserExtension(userExtension);

		String jsonRequest = null;
		try {
			jsonRequest = JSON_OBJECT_MAPPER.writeValueAsString(request);
		} catch (JsonProcessingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		String authorizedScimToken = obtainToken(objectStore, jsonRequest);
		
		
		System.out.println(String.format("Sending request to %s with authorized SCIM token %s in header", apiResource + "/" + GET_USER, authorizedScimToken));
		LOGGER.info(String.format("Sending request to %s with authorized SCIM token %s in header", apiResource + "/" + GET_USER, authorizedScimToken));
		
		String responseString = getValidatedResponse(apiResource
				.path(GET_USER)
//				.accept("application/json")
				.header(CONTENT_TYPE, APPLICATION_JSON)
				.header(AUTHORIZATION, authorizedScimToken)
				.put(ClientResponse.class, jsonRequest));
		
		return responseString;
		
//		try {
//			return jsonObjectMapper.readValue(responseString, GluuSCIMGetTicketJsonResponse.class).getScimTicket();
//		} catch (IOException e) {
//			throw new GluuSCIMConnectorException(e.getMessage());
//		}
		
	}

	/////////////
	/// UTILS ///
	/////////////
	
	/** Do all the steps required to obtain a valid scim access token including refresh token if needed */
	private String obtainToken(TestObjectStore objectStore, String jsonRequest) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
//		this.objectStore = event.getMuleContext().getRegistry().lookupObject("_defaultUserObjectStore");
		this.objectStore = objectStore;
		
		// do refresh token a minute before token expires
		if (AAT_TOKEN_EXPIRATION_TIME < System.currentTimeMillis()/1000 + 60) {
			refreshToken();
		}
		
		String aatToken = null;
		try {
			aatToken = (String)objectStore.retrieve(AAT_TOKEN);
		} catch (ObjectStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String scimToken = getScimToken(BEARER + aatToken);
		String scimTicket = getScimTicket(BEARER + scimToken, jsonRequest);
		
		return BEARER + getAuthorizedScimToken(BEARER + aatToken, scimToken, scimTicket, getApiUrl());
	}

	/** Do the "oxauth/seam/resource/restv1/oxauth/token" call */
	private void refreshToken() throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
		String aatRefreshToken = null;

		try {
			aatRefreshToken = objectStore.contains(AAT_REFRESH_TOKEN) ? (String)objectStore.retrieve(AAT_REFRESH_TOKEN) : connectorConfig.getAatRefreshToken();

		} catch (ObjectStoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		String authString = connectorConfig.getUsername() + ":" + connectorConfig.getPassword();
        String authStringEncrypted = BASIC + new String(Base64.encodeBase64(authString.getBytes()));
        
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("grant_type", "authorization_code");
		formData.add("code", aatRefreshToken);
		formData.add("redirect_uri", getRedirectUri());
		
		logInfo(apiResource + "/" + GET_AAT_TOKEN, authStringEncrypted, formData);
		
		String responseString = getValidatedResponse(apiResource
				.path(GET_AAT_TOKEN)
				.header(AUTHORIZATION, authStringEncrypted)
				.header(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
				.post(ClientResponse.class, formData));
		
		try {
			GluuSCIMGetAatTokenJsonResponse jsonResponse = JSON_OBJECT_MAPPER.readValue(responseString, GluuSCIMGetAatTokenJsonResponse.class);
			AAT_TOKEN_EXPIRATION_TIME = System.currentTimeMillis()/1000 + jsonResponse.getExpirationTimeInSeconds();
			objectStore.remove(AAT_TOKEN);
			objectStore.store(AAT_TOKEN, jsonResponse.getAatToken());
			objectStore.remove(AAT_REFRESH_TOKEN);
			objectStore.store(AAT_REFRESH_TOKEN, jsonResponse.getAatRefreshToken());
			
		} catch (IOException e) {
			throw new GluuSCIMConnectorException(e.getMessage());
		} catch (ObjectStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/** Returns response from the "oxauth/seam/resource/restv1/requester/rpt" call */
	private String getScimToken(String aatToken) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {

		System.out.println(String.format("Sending request to %s with UMA AAT token %s in header", apiResource + "/" + GET_SCIM_TOKEN, aatToken));
		LOGGER.info(String.format("Sending request to %s with UMA AAT token %s in header", apiResource + "/" + GET_SCIM_TOKEN, aatToken));
		
		String responseString = getValidatedResponse(apiResource
				.path(GET_SCIM_TOKEN)
				.header(AUTHORIZATION, aatToken)
				.post(ClientResponse.class));
		
		try {
			return JSON_OBJECT_MAPPER.readValue(responseString, GluuSCIMGetTokenJsonResponse.class).getScimToken();
		} catch (IOException e) {
			throw new GluuSCIMConnectorException(e.getMessage());
		}
	}
	
	/** Returns response from the "identity/seam/resource/restv1/scim/v2/Users/" call */
	private String getScimTicket(String scimToken, String jsonRequest) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
		
		System.out.println(String.format("Sending request to %s with SCIM token %s in header and request %s", apiResource + "/" + GET_USER, scimToken, jsonRequest));
		LOGGER.info(String.format("Sending request to %s with SCIM token %s in header and request %s", apiResource + "/" + GET_USER, scimToken, jsonRequest));
		
		String responseString = getValidatedResponse(apiResource
				.path(GET_USER)
//				.accept("application/json")
				.header(CONTENT_TYPE, APPLICATION_JSON)
				.header(AUTHORIZATION, scimToken)
				.put(ClientResponse.class, jsonRequest));
		
		try {
			return JSON_OBJECT_MAPPER.readValue(responseString, GluuSCIMGetTicketJsonResponse.class).getScimTicket();
		} catch (IOException e) {
			throw new GluuSCIMConnectorException(e.getMessage());
		}
	}
	
	/** Returns response from the "oxauth/seam/resource/restv1/requester/perm" call */
	private String getAuthorizedScimToken(String aatToken, String scimToken, String scimTicket, String host) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {

		GluuSCIMGetAuthorizedTokenJsonRequest request = new GluuSCIMGetAuthorizedTokenJsonRequest();
		request.setScimToken(scimToken);
		request.setScimTicket(scimTicket);
		request.setHost(host);
		
		try {
			String jsonRequest = JSON_OBJECT_MAPPER.writeValueAsString(request);

			System.out.println(String.format("Sending request to %s with UMA AAT token %s in header and body: %s", apiResource + "/" + AUTHORIZE_SCIM_TOKEN, aatToken, jsonRequest));
			LOGGER.info(String.format("Sending request to %s with UMA AAT token %s in header and body: %s", apiResource + "/" + AUTHORIZE_SCIM_TOKEN, aatToken, jsonRequest));
			
			String responseString = getValidatedResponse(apiResource
					.path(AUTHORIZE_SCIM_TOKEN)
					.header(AUTHORIZATION, aatToken)
					.header(CONTENT_TYPE, APPLICATION_JSON)
					.post(ClientResponse.class, jsonRequest));
			
			return JSON_OBJECT_MAPPER.readValue(responseString, GluuSCIMGetAuthorizedTokenJsonResponse.class).getAuthorizedToken();

		} catch (IOException e) {
			throw new GluuSCIMConnectorException(e.getMessage());
		}
	}
	
	/** Returns the response string in case of valid response or throws an exception in case of invalid response */
	private String getValidatedResponse(ClientResponse clientResponse) throws GluuSCIMConnectorException, GluuSCIMServerErrorException {
		String responseEntity = clientResponse.getEntity(String.class);
		int responseStatus = clientResponse.getStatus();

		System.out.println(String.format("Getting response with status %s: %s", responseStatus, responseEntity));
		LOGGER.info(String.format("Getting response with status %s: %s", responseStatus, responseEntity));
		
		if(responseStatus == 500 ){
			throw new GluuSCIMServerErrorException();
		}
//		} else if(responseStatus != 200 && responseStatus != 201){
//			throw new GluuSCIMConnectorException(responseEntity);
//		}
		return responseEntity;
	}
	
	/** Logs info about request sent to Gluu server */
	private void logInfo(String endpointUrl, String authStringEncrypted, MultivaluedMap<String, String> formData) {
		String properties = "";
		for (Entry<String, List<String>> property : formData.entrySet()) {
			properties += property.getKey() + ": " + property.getValue() + ",";	
		}
		LOGGER.info(String.format("Sending request to %s with Authorization %s in header and properties: ", apiResource + "/" + GET_AAT_TOKEN, authStringEncrypted, properties));
	}
	
	/** Returns the URL to gluu server */
	private String getApiUrl() {
		return "https://" + connectorConfig.getHost();
	}
	
	/** Returns the URL to mulesoft runtime server */
	private String getRedirectUri() {
		return "https://" + connectorConfig.getRedirectUri();
	}
	
	//////////////
	/// RUNNER ///
	//////////////
	
	public static void main(String[] args) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
		GluuSCIMConnectorConfig connectorConfig = new GluuSCIMConnectorConfig();
		connectorConfig.setHost("idp.d.aws.economist.com");
		connectorConfig.setAatRefreshToken("e6dc3ff2-66a7-49ea-9806-6d4151d18d9a");
		connectorConfig.setUsername("@!E0E2.8150.B9D2.14A0!0001!6A42.EB0A!0008!F439.0592");
		connectorConfig.setPassword("1cdb3c2e-15d0-47ab-a8c5-a3bce017026e");
		connectorConfig.setRedirectUri("https://dev-economistapi.cloudhub.io");
		
		GluuSCIMClient client = new GluuSCIMClient(connectorConfig);
		System.out.println(client.createUser(client.new TestObjectStore()));
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
