package org.mule.modules.gluuscim.client;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.codec.binary.Base64;
import org.mule.api.store.ObjectStoreException;
import org.mule.modules.gluuscim.config.GluuSCIMConnectorConfig;
import org.mule.modules.gluuscim.entities.GluuSCIMGetAatTokenJsonResponse;
import org.mule.modules.gluuscim.entities.GluuSCIMGetAuthorizedTokenJsonRequest;
import org.mule.modules.gluuscim.entities.GluuSCIMGetAuthorizedTokenJsonResponse;
import org.mule.modules.gluuscim.entities.GluuSCIMGetTicketJsonResponse;
import org.mule.modules.gluuscim.entities.GluuSCIMGetTokenJsonResponse;
import org.mule.modules.gluuscim.exception.GluuSCIMConnectorException;
import org.mule.modules.gluuscim.exception.GluuSCIMServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;
import org.mule.util.store.PartitionedInMemoryObjectStore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class GluuSCIMClientTokenHelper {
	
	private static final String GET_AAT_TOKEN = "oxauth/seam/resource/restv1/oxauth/token";
	private static final String GET_SCIM_TOKEN = "oxauth/seam/resource/restv1/requester/rpt";
	private static final String AUTHORIZE_SCIM_TOKEN = "oxauth/seam/resource/restv1/requester/perm";
	private static final String SEARCH_USER = "identity/seam/resource/restv1/scim/v2/Users/Search";
	
	private static final String AAT_TOKEN = "aatToken";
	private static final String AAT_REFRESH_TOKEN = "aatRefreshToken";
	private static final String BEARER = "Bearer ";
	private static final String BASIC = "Basic ";
	private static final String AUTHORIZATION = "Authorization";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();

	// set initial token expiration time to past date
	private static long AAT_TOKEN_EXPIRATION_TIME = System.currentTimeMillis()/1000 - 5000;
	
	private final GluuSCIMConnectorConfig connectorConfig;
	private final String host;
	private final WebResource apiResource;

	private PartitionedInMemoryObjectStore<Serializable> objectStore;
//	private TestObjectStore objectStore;

	private transient final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	
	public GluuSCIMClientTokenHelper(final GluuSCIMConnectorConfig connectorConfig, final String host, final WebResource apiResource) {
		this.connectorConfig = connectorConfig;
		this.host = host;
		this.apiResource = apiResource;
	}

	/** Do all the steps required to obtain a valid scim access token including refresh token if needed */
	public String obtainToken(PartitionedInMemoryObjectStore<Serializable>/*TestObjectStore*/ objectStore, RequestMethod requestMethod, String url, String jsonRequest) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
		this.objectStore = objectStore;
		
		// do refresh token a minute before token expires
		if (AAT_TOKEN_EXPIRATION_TIME < System.currentTimeMillis()/1000 + 60) {
			refreshToken();
		}
		
		String aatToken = getAatToken();
		String scimToken = getScimToken(BEARER + aatToken);
		String scimTicket = getScimTicket(requestMethod, url, BEARER + scimToken, jsonRequest);
		
		return BEARER + getAuthorizedScimToken(BEARER + aatToken, scimToken, scimTicket, this.host);
	}
	
	/////////////
	/// UTILS ///
	/////////////
	
	private String getAatToken() throws GluuSCIMConnectorException {
		String aatRefreshToken = null;
		try {
			if(objectStore.contains(AAT_TOKEN)){
				aatRefreshToken = (String)objectStore.retrieve(AAT_TOKEN);
			}
			else throw new GluuSCIMConnectorException("Exception in getting AAT Token");
		} catch (ObjectStoreException e) {
			throw new GluuSCIMConnectorException(e.getMessage());
		}
		return aatRefreshToken;
	}

	/** Do the "oxauth/seam/resource/restv1/oxauth/token" call */
	private void refreshToken() throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
		String authString = connectorConfig.getUsername() + ":" + connectorConfig.getPassword();
        String authStringEncrypted = BASIC + new String(Base64.encodeBase64(authString.getBytes()));
        
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		formData.add("grant_type", "authorization_code");
		formData.add("code", getAatRefreshToken());
		formData.add("redirect_uri", getRedirectUri());
		
		logInfo(apiResource + "/" + GET_AAT_TOKEN, authStringEncrypted, formData);
		
		String responseString = GluuSCIMClientMappingHelper.getValidatedResponse(apiResource
				.path(GET_AAT_TOKEN)
				.header(AUTHORIZATION, authStringEncrypted)
				.header(CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_TYPE)
				.post(ClientResponse.class, formData));
		
		try {
			GluuSCIMGetAatTokenJsonResponse jsonResponse = JSON_OBJECT_MAPPER.readValue(responseString, GluuSCIMGetAatTokenJsonResponse.class);
			AAT_TOKEN_EXPIRATION_TIME = System.currentTimeMillis()/1000 + jsonResponse.getExpirationTimeInSeconds();
			if(objectStore.contains(AAT_TOKEN)){
				objectStore.remove(AAT_TOKEN);
			}
			objectStore.store(AAT_TOKEN, jsonResponse.getAatToken());
			if(objectStore.contains(AAT_REFRESH_TOKEN)){
				objectStore.remove(AAT_REFRESH_TOKEN);
			}
			objectStore.store(AAT_REFRESH_TOKEN, jsonResponse.getAatRefreshToken());
		} catch (IOException | ObjectStoreException e) {
			throw new GluuSCIMConnectorException(e.getMessage());
		}
	}

	private String getAatRefreshToken() throws GluuSCIMConnectorException {
		String aatRefreshToken = null;
		try {
			aatRefreshToken = objectStore.contains(AAT_REFRESH_TOKEN) ? (String)objectStore.retrieve(AAT_REFRESH_TOKEN) : connectorConfig.getAatRefreshToken();
		} catch (ObjectStoreException e) {
			throw new GluuSCIMConnectorException(e.getMessage());
		}
		return aatRefreshToken;
	}
	
	/** Returns response from the "oxauth/seam/resource/restv1/requester/rpt" call */
	private String getScimToken(String aatToken) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {

		System.out.println(String.format("Sending request to %s with UMA AAT token %s in header", apiResource + "/" + GET_SCIM_TOKEN, aatToken));
		LOGGER.info(String.format("Sending request to %s with UMA AAT token %s in header", apiResource + "/" + GET_SCIM_TOKEN, aatToken));
		
		String responseString = GluuSCIMClientMappingHelper.getValidatedResponse(apiResource
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
		
		String responseString = GluuSCIMClientMappingHelper.getValidatedResponse(response);
		
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
			
			String responseString = GluuSCIMClientMappingHelper.getValidatedResponse(apiResource
					.path(AUTHORIZE_SCIM_TOKEN)
					.header(AUTHORIZATION, aatToken)
					.header(CONTENT_TYPE, MediaType.APPLICATION_JSON)
					.post(ClientResponse.class, jsonRequest));
			
			return JSON_OBJECT_MAPPER.readValue(responseString, GluuSCIMGetAuthorizedTokenJsonResponse.class).getAuthorizedToken();

		} catch (IOException e) {
			throw new GluuSCIMConnectorException(e.getMessage());
		}
	}
	
	/** Logs info about request sent to Gluu server */
	private void logInfo(String endpointUrl, String authStringEncrypted, MultivaluedMap<String, String> formData) {
		String properties = "";
		for (Entry<String, List<String>> property : formData.entrySet()) {
			properties += property.getKey() + ": " + property.getValue() + ",";	
		}
		LOGGER.info(String.format("Sending request to %s with Authorization %s in header and properties: ", apiResource + "/" + GET_AAT_TOKEN, authStringEncrypted, properties));
	}

	/** Returns the URL to mulesoft runtime server */
	private String getRedirectUri() {
		return "https://" + connectorConfig.getRedirectUri();
	}
}
