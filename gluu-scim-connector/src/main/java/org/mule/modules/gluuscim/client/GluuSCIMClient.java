package org.mule.modules.gluuscim.client;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.mule.api.MuleEvent;
import org.mule.api.store.ObjectStoreException;
import org.mule.modules.gluuscim.config.GluuSCIMConnectorConfig;
import org.mule.modules.gluuscim.entities.GluuSCIMEntitlement;
import org.mule.modules.gluuscim.entities.GluuSCIMGetAatTokenJsonResponse;
import org.mule.modules.gluuscim.entities.GluuSCIMGetAuthorizedTokenJsonRequest;
import org.mule.modules.gluuscim.entities.GluuSCIMGetAuthorizedTokenJsonResponse;
import org.mule.modules.gluuscim.entities.GluuSCIMGetTicketJsonResponse;
import org.mule.modules.gluuscim.entities.GluuSCIMGetTokenJsonResponse;
import org.mule.modules.gluuscim.entities.GluuSCIMGetUserJsonRequest;
import org.mule.modules.gluuscim.entities.GluuSCIMUser;
import org.mule.modules.gluuscim.entities.GluuSCIMUserJsonRequest;
import org.mule.modules.gluuscim.entities.GluuSCIMUserNameJsonObject;
import org.mule.modules.gluuscim.exception.GluuSCIMConnectorException;
import org.mule.modules.gluuscim.exception.GluuSCIMServerErrorException;
import org.mule.util.store.PartitionedPersistentObjectStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
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

	private static final String GET_AAT_TOKEN = "oxauth/seam/resource/restv1/oxauth/token";
	private static final String GET_SCIM_TOKEN = "oxauth/seam/resource/restv1/requester/rpt";
	private static final String AUTHORIZE_SCIM_TOKEN = "oxauth/seam/resource/restv1/requester/perm";
	private static final String SEARCH_USER = "identity/seam/resource/restv1/scim/v2/Users/Search";
	private static final String USER_ENDPOINT = "identity/seam/resource/restv1/scim/v2/Users";
	
	private static final String AAT_TOKEN = "aatToken";
	private static final String AAT_REFRESH_TOKEN = "aatRefreshToken";
	private static final String AUTHORIZATION = "Authorization";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String BEARER = "Bearer ";
	private static final String BASIC = "Basic ";
	
	private static final String USER_EXTENSION_SCHEMA = "urn:ietf:params:scim:schemas:extension:gluu:2.0:User";
	private static final String USER_CORE_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:User";
	private static final String PRODUCT = "product";
	private static final String ENTITLEMENT_START_DATE = "entitlementStartDate";
	private static final String ENTITLEMENT_END_DATE = "entitlementEndDate";
	
	private static final String DEFAULT_USER_OBJECT_STORE = "_defaultUserObjectStore";

	private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();
	private transient final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	
	// set initial token expiration time to past date
	private static long AAT_TOKEN_EXPIRATION_TIME = System.currentTimeMillis()/1000 - 5000;

	private final Client client;
	private final WebResource apiResource;
	private final GluuSCIMConnectorConfig connectorConfig;

	private PartitionedPersistentObjectStore<Serializable> objectStore;
//	private TestObjectStore objectStore;
	
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
	public GluuSCIMUser getUser(MuleEvent event /*TestObjectStore objectStore*/, String attribute, String value) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
		this.objectStore = event.getMuleContext().getRegistry().lookupObject(DEFAULT_USER_OBJECT_STORE);
//		this.objectStore = objectStore;
		
		GluuSCIMGetUserJsonRequest request = new GluuSCIMGetUserJsonRequest();
		request.setAttribute(attribute);
		request.setValue(value);
		
		String jsonRequest = null;
		try {
			jsonRequest = JSON_OBJECT_MAPPER.writeValueAsString(request);
		} catch (JsonProcessingException e1) {
			throw new GluuSCIMConnectorException(e1.getMessage());
		}
		
		String authorizedScimToken = obtainToken(RequestMethod.POST, SEARCH_USER, jsonRequest);
		
		System.out.println(String.format("Sending request to %s with authorized SCIM token %s in header", apiResource + "/" + SEARCH_USER, authorizedScimToken));
		LOGGER.info(String.format("Sending request to %s with authorized SCIM token %s in header", apiResource + "/" + SEARCH_USER, authorizedScimToken));
		
		String validatedResponse = getValidatedResponse(apiResource
				.path(SEARCH_USER)
				.accept(MediaType.APPLICATION_JSON)
				.header(CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.header(AUTHORIZATION, authorizedScimToken)
				.type(MediaType.APPLICATION_JSON)
				.post(ClientResponse.class, jsonRequest));
		
		return mapResponseJsonToUserObject(validatedResponse);
		
	}

	/** Returns response from the create user call */
	public GluuSCIMUser createUser(MuleEvent event /*TestObjectStore objectStore*/, GluuSCIMUser user) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
		this.objectStore = event.getMuleContext().getRegistry().lookupObject(DEFAULT_USER_OBJECT_STORE);
//		this.objectStore = objectStore;
		
		String jsonRequest = getUserJsonRequest(user);
		
		
		String authorizedScimToken = obtainToken(RequestMethod.POST, USER_ENDPOINT, jsonRequest);
		
		System.out.println(String.format("Sending request to %s with authorized SCIM token %s in header", apiResource + "/" + USER_ENDPOINT, authorizedScimToken));
		LOGGER.info(String.format("Sending request to %s with authorized SCIM token %s in header", apiResource + "/" + USER_ENDPOINT, authorizedScimToken));
		
		String validatedResponse = getValidatedResponse(apiResource
				.path(USER_ENDPOINT)
				.accept(MediaType.APPLICATION_JSON)
				.header(CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.header(AUTHORIZATION, authorizedScimToken)
				.post(ClientResponse.class, jsonRequest));
		
		return mapResponseJsonToUserObject(validatedResponse);
	}

	/** Returns response from the update user call */
	public GluuSCIMUser updateUser(MuleEvent event /*TestObjectStore objectStore*/, GluuSCIMUser user) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
		this.objectStore = event.getMuleContext().getRegistry().lookupObject(DEFAULT_USER_OBJECT_STORE);
//		this.objectStore = objectStore;
		
		String url = USER_ENDPOINT + "/" + user.getGluuId();

		String jsonRequest = getUserJsonRequest(user);
		String authorizedScimToken = obtainToken(RequestMethod.PUT, url, jsonRequest);
		
		System.out.println(String.format("Sending request to %s with authorized SCIM token %s in header", apiResource + "/" + url, authorizedScimToken));
		LOGGER.info(String.format("Sending request to %s with authorized SCIM token %s in header", apiResource + "/" + url, authorizedScimToken));
		
		String validatedResponse = getValidatedResponse(apiResource
				.path(url)
				.accept(MediaType.APPLICATION_JSON)
				.header(CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.header(AUTHORIZATION, authorizedScimToken)
				.put(ClientResponse.class, jsonRequest));
		
		return mapResponseJsonToUserObject(validatedResponse);
	}


	/////////////
	/// UTILS ///
	/////////////
	
	/** Do all the steps required to obtain a valid scim access token including refresh token if needed */
	private String obtainToken(RequestMethod requestMethod, String url, String jsonRequest) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
		
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
		String scimTicket = getScimTicket(requestMethod, url, BEARER + scimToken, jsonRequest);
		
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
				.header(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_TYPE)
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
	private String getScimTicket(RequestMethod requestMethod, String url, String scimToken, String jsonRequest) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
		System.out.println(String.format("Sending request to %s with SCIM token %s in header and request %s", apiResource + "/" + SEARCH_USER, scimToken, jsonRequest));
		LOGGER.info(String.format("Sending request to %s with SCIM token %s in header and request %s", apiResource + "/" + SEARCH_USER, scimToken, jsonRequest));
		
		Builder requestBuilder = apiResource
				.path(url)
				.accept(MediaType.APPLICATION_JSON)
				.header(CONTENT_TYPE, MediaType.APPLICATION_JSON)
				.header(AUTHORIZATION, scimToken)
				.type(MediaType.APPLICATION_JSON);
		
		ClientResponse response = null;
		
		switch (requestMethod) {
		case POST:
			response = requestBuilder.post(ClientResponse.class, jsonRequest);
			break;
		case PUT:
			response = requestBuilder.put(ClientResponse.class, jsonRequest);
		default:
			break;
		}
		
		String responseString = getValidatedResponse(response);
		
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
					.header(CONTENT_TYPE, MediaType.APPLICATION_JSON)
					.post(ClientResponse.class, jsonRequest));
			
			return JSON_OBJECT_MAPPER.readValue(responseString, GluuSCIMGetAuthorizedTokenJsonResponse.class).getAuthorizedToken();

		} catch (IOException e) {
			throw new GluuSCIMConnectorException(e.getMessage());
		}
	}
	
	private String getUserJsonRequest(GluuSCIMUser user) throws GluuSCIMConnectorException {
		GluuSCIMUserNameJsonObject name = new GluuSCIMUserNameJsonObject();
		name.setFamilyName(user.getLastName());
		name.setGivenName(user.getFirstName());
		
		GluuSCIMUserJsonRequest request = new GluuSCIMUserJsonRequest();
		request.setSchemas(getSchemasArray(user));
		request.setUserName(user.getEmail());
		request.setName(name);
		request.setDisplayName(user.getDisplayName());
		request.setPassword(user.getPassword());
		request.setUserExtension(getUserEntitlementsJson(user));

		String jsonRequest = null;
		try {
			jsonRequest = JSON_OBJECT_MAPPER.writeValueAsString(request);
		} catch (JsonProcessingException e1) {
			throw new GluuSCIMConnectorException(e1.getMessage());
		}
		return jsonRequest;
	}
	
	private String[] getSchemasArray(GluuSCIMUser user) {
		String[] schemas = null;
		if (user.hasEntitlements()) {
			schemas = new String[]{USER_CORE_SCHEMA, USER_EXTENSION_SCHEMA};
		} else {
			schemas = new String[]{USER_CORE_SCHEMA};
		}
		return schemas;
	}

	private ObjectNode getUserEntitlementsJson(GluuSCIMUser user) {
		ObjectNode entitlementsJson = JSON_OBJECT_MAPPER.createObjectNode();
		
		if(user.hasEntitlements()){
			for (GluuSCIMEntitlement entitlement : user.getEntitlements()) {
				
				ObjectNode productSubNode = JSON_OBJECT_MAPPER.createObjectNode();
				productSubNode.put(PRODUCT, entitlement.getProductName());
				productSubNode.put(ENTITLEMENT_START_DATE, entitlement.getStartDate());
				productSubNode.put(ENTITLEMENT_END_DATE, entitlement.getEndDate());
				
				ObjectNode productNode = JSON_OBJECT_MAPPER.createObjectNode();
				productNode.set(entitlement.getProductName(), productSubNode);
				
				ArrayNode productMainNode = JSON_OBJECT_MAPPER.createArrayNode();
				productMainNode.add(productNode.toString());
				
				entitlementsJson.set(entitlement.getProductCode(), productMainNode);
				
			}
		}
		return entitlementsJson;
	}
	
	private GluuSCIMUser mapResponseJsonToUserObject(String validatedResponse) throws GluuSCIMConnectorException {
		GluuSCIMUser user = null;
		
		try {
			JsonNode jsonResponse = JSON_OBJECT_MAPPER.readTree(validatedResponse);
			user = new GluuSCIMUser();
			
			user.setDisplayName(jsonResponse.get("displayName").asText());		
			user.setGluuId(jsonResponse.get("id").asText());
			user.setEmail(jsonResponse.get("userName").asText());
			user.setPassword(jsonResponse.get("password").asText());
			
			
			if(jsonResponse.get(USER_EXTENSION_SCHEMA) != null){
				List<GluuSCIMEntitlement> entitlements = new ArrayList<GluuSCIMEntitlement>();
				
				String entitlementsString = jsonResponse.get(USER_EXTENSION_SCHEMA).toString();
				entitlementsString = entitlementsString.replace("\"{", "{");
				entitlementsString = entitlementsString.replace("}\"", "}");
				entitlementsString = entitlementsString.replace("\\\"", "\"");
				
				Iterator<Entry<String, JsonNode>> entitlementsFieldsIterator = JSON_OBJECT_MAPPER.readTree(entitlementsString).fields();
				for (Iterator<Entry<String, JsonNode>> fields = entitlementsFieldsIterator; fields.hasNext();) {
					Entry<String, JsonNode> field = fields.next();
					JsonNode fieldValue = field.getValue().get(0).fields().next().getValue();
					
					GluuSCIMEntitlement entitlement = new GluuSCIMEntitlement();
					entitlement.setProductCode(field.getKey());
					entitlement.setProductName(fieldValue.get(PRODUCT).asText());
					entitlement.setStartDate(fieldValue.get(ENTITLEMENT_START_DATE).asText());
					entitlement.setEndDate(fieldValue.get(ENTITLEMENT_END_DATE).asText());
					
					entitlements.add(entitlement);
				}
				user.setEntitlements(entitlements);
			}

			JsonNode jsonNameNode = jsonResponse.get("name");
			user.setFirstName(jsonNameNode.get("givenName").asText());
			user.setLastName(jsonNameNode.get("familyName").asText());
		
		} catch (IOException e) {
			throw new GluuSCIMConnectorException(e.getMessage());
		}
		
		return user;
	}
	
	/** Returns the response string in case of valid response or throws an exception in case of invalid response */
	private String getValidatedResponse(ClientResponse clientResponse) throws GluuSCIMConnectorException, GluuSCIMServerErrorException {
		String responseEntity = clientResponse.getEntity(String.class);
		int responseStatus = clientResponse.getStatus();

		System.out.println(String.format("Getting response with status %s: %s", responseStatus, responseEntity));
		LOGGER.info(String.format("Getting response with status %s: %s", responseStatus, responseEntity));
		
		if(responseStatus == 500 ){
			throw new GluuSCIMServerErrorException();
		} else if(responseStatus != 200 && responseStatus != 201 && responseStatus != 403){
			throw new GluuSCIMConnectorException(responseEntity);
		}
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
		connectorConfig.setAatRefreshToken("2de26069-06d6-4b38-b7fd-74b5de34b6d0");
		connectorConfig.setUsername("@!E0E2.8150.B9D2.14A0!0001!6A42.EB0A!0008!C1BA.E22F");
		connectorConfig.setPassword("5ab8e319-b4d4-4808-b06d-583bb109ca90");
		connectorConfig.setRedirectUri("https://dev-economistapi.cloudhub.io");
		
		GluuSCIMClient client = new GluuSCIMClient(connectorConfig);
//		System.out.println(client.getUser(client.new TestObjectStore(), "uid", "guest-ajamnssi@example.com"));
		
		ObjectNode printPlusWebSubNode = JSON_OBJECT_MAPPER.createObjectNode();
		printPlusWebSubNode.put(PRODUCT, "Print + Web");
		printPlusWebSubNode.put(ENTITLEMENT_START_DATE, "1469051438");
		printPlusWebSubNode.put(ENTITLEMENT_END_DATE, "1500508800");
		
		ObjectNode printPlusWebNode = JSON_OBJECT_MAPPER.createObjectNode();
		printPlusWebNode.put("Print + Web", printPlusWebSubNode);
		
		ArrayNode printPlusWebMainNode = JSON_OBJECT_MAPPER.createArrayNode();
		printPlusWebMainNode.add(printPlusWebNode.toString());
		
		ObjectNode entitlements = JSON_OBJECT_MAPPER.createObjectNode();
		entitlements.put("printPlusWeb", printPlusWebMainNode);
		
//		System.out.println(client.updateUser(client.new TestObjectStore(), "@!E0E2.8150.B9D2.14A0!0001!6A42.EB0A!0000!C9A3.6E92", "Tina", "Turner", "fancydisplayName", "email44@test.com_edited", "password55_edited", entitlements));
	
	
		String jsonResponse = "{\"printPlusWeb\":[{\"Print + Web\":{\"product\":\"Print + Web\",\"entitlementStartDate\":\"1469051438\",\"entitlementEndDate\":\"1500508800\"}}], \"printPlusDigital\":[{\"Print + Digital\":{\"product\":\"Print + Digital\",\"entitlementStartDate\":\"1469051438\",\"entitlementEndDate\":\"1500508800\"}}]}";

		
		try {
			Iterator<Entry<String, JsonNode>> fieldsIterator = JSON_OBJECT_MAPPER.readTree(jsonResponse).fields();
			for (Iterator<Entry<String, JsonNode>> fields = fieldsIterator; fields.hasNext();) {
				Entry<String, JsonNode> field = fields.next();
				JsonNode fieldValue = field.getValue().get(0).fields().next().getValue();
				
				GluuSCIMEntitlement entitlement = new GluuSCIMEntitlement();
				entitlement.setProductCode(field.getKey());
				entitlement.setProductName(fieldValue.get(PRODUCT).asText());
				entitlement.setStartDate(fieldValue.get(ENTITLEMENT_START_DATE).asText());
				entitlement.setEndDate(fieldValue.get(ENTITLEMENT_END_DATE).asText());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
