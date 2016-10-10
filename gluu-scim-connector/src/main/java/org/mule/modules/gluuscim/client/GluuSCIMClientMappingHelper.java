package org.mule.modules.gluuscim.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.mule.modules.gluuscim.entities.GluuSCIMEntitlement;
import org.mule.modules.gluuscim.entities.GluuSCIMUser;
import org.mule.modules.gluuscim.entities.GluuSCIMUserJsonRequest;
import org.mule.modules.gluuscim.entities.GluuSCIMUserNameJsonObject;
import org.mule.modules.gluuscim.exception.GluuSCIMConnectorException;
import org.mule.modules.gluuscim.exception.GluuSCIMServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.jersey.api.client.ClientResponse;

public class GluuSCIMClientMappingHelper {

	private static final String USER_EXTENSION_SCHEMA = "urn:ietf:params:scim:schemas:extension:gluu:2.0:User";
	private static final String USER_CORE_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:User";
	private static final String PRODUCT = "product";
	private static final String ENTITLEMENT_START_DATE = "entitlementStartDate";
	private static final String ENTITLEMENT_END_DATE = "entitlementEndDate";
	private static final String NAME = "name";
	private static final String FAMILY_NAME = "familyName";
	private static final String GIVEN_NAME = "givenName";
	private static final String DISPLAY_NAME = "displayName";
	private static final String USER_NAME = "userName";
	private static final String PASSWORD = "password";
	private static final String LEGACY_PASSWORD = "legacyPassword";
	private static final String ID = "id";
	
	private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();
	private transient final static Logger LOGGER = LoggerFactory.getLogger(GluuSCIMClientTokenHelper.class);

	static String mapToJsonRequest(GluuSCIMUser user) throws GluuSCIMConnectorException {
		GluuSCIMUserNameJsonObject name = new GluuSCIMUserNameJsonObject();
		name.setFamilyName(user.getLastName());
		name.setGivenName(user.getFirstName());
		
		GluuSCIMUserJsonRequest request = new GluuSCIMUserJsonRequest();
		request.setSchemas(getSchemasArray(user));
		request.setUserName(user.getEmail());
		request.setName(name);
		request.setDisplayName(user.getDisplayName());
		request.setPassword(user.getPassword());
		request.setUserExtension(getCustomAttributesJson(user));

		String jsonRequest = null;
		try {
			jsonRequest = JSON_OBJECT_MAPPER.writeValueAsString(request);
		} catch (JsonProcessingException e1) {
			throw new GluuSCIMConnectorException(e1.getMessage());
		}
		return jsonRequest;
	}
	
	static GluuSCIMUser mapToUserObject(String validatedResponse) throws GluuSCIMConnectorException {
		GluuSCIMUser user = null;
		
		try {
			JsonNode jsonResponse = JSON_OBJECT_MAPPER.readTree(validatedResponse);
			user = new GluuSCIMUser();
			
			user.setDisplayName(jsonResponse.get(DISPLAY_NAME).asText());		
			user.setGluuId(jsonResponse.get(ID).asText());
			user.setEmail(jsonResponse.get(USER_NAME).asText());
			user.setPassword(jsonResponse.get(PASSWORD).asText());
			
			if(jsonResponse.get(USER_EXTENSION_SCHEMA) != null){
				List<GluuSCIMEntitlement> entitlements = new ArrayList<GluuSCIMEntitlement>();
				
				String entitlementsString = jsonResponse.get(USER_EXTENSION_SCHEMA).toString();
				entitlementsString = entitlementsString.replace("\"{", "{");
				entitlementsString = entitlementsString.replace("}\"", "}");
				entitlementsString = entitlementsString.replace("\\\"", "\"");
				
				Iterator<Entry<String, JsonNode>> entitlementsFieldsIterator = JSON_OBJECT_MAPPER.readTree(entitlementsString).fields();
				for (Iterator<Entry<String, JsonNode>> fields = entitlementsFieldsIterator; fields.hasNext();) {
					Entry<String, JsonNode> field = fields.next();
					
					if(field.getValue().isValueNode()){
						String fieldValueText = field.getValue().asText();
					
						switch (field.getKey()) {
						case LEGACY_PASSWORD:
							user.setLegacyPassword(fieldValueText);
							break;
						default:
							break;
						}
					} else {
						JsonNode fieldValue = field.getValue().get(0).fields().next().getValue();
						GluuSCIMEntitlement entitlement = new GluuSCIMEntitlement();
						entitlement.setProductCode(field.getKey());
						entitlement.setProductName(fieldValue.get(PRODUCT).asText());
						entitlement.setStartDate(fieldValue.get(ENTITLEMENT_START_DATE).asText());
						entitlement.setEndDate(fieldValue.get(ENTITLEMENT_END_DATE).asText());
						
						entitlements.add(entitlement);
					}
				}
				user.setEntitlements(entitlements);
			}

			JsonNode jsonNameNode = jsonResponse.get(NAME);
			user.setFirstName(jsonNameNode.get(GIVEN_NAME).asText());
			user.setLastName(jsonNameNode.get(FAMILY_NAME).asText());
		
		} catch (IOException e) {
			throw new GluuSCIMConnectorException(e.getMessage());
		}
		
		return user;
	}
	
	/** Returns the response string in case of valid response or throws an exception in case of invalid response */
	static String getValidatedResponse(ClientResponse clientResponse) throws GluuSCIMConnectorException, GluuSCIMServerErrorException {
		String responseEntity = clientResponse.getEntity(String.class);
		int responseStatus = clientResponse.getStatus();

		LOGGER.info(String.format("Getting response with status %s: %s", responseStatus, responseEntity));
		System.out.println(String.format("Getting response with status %s: %s", responseStatus, responseEntity));
		
		if(responseStatus == 500 ){
			throw new GluuSCIMServerErrorException();
		} else if(responseStatus != 200 && responseStatus != 201 && responseStatus != 403){
			throw new GluuSCIMConnectorException(responseEntity);
		}
		return responseEntity;
	}
	
	/////////////
	/// UTILS ///
	/////////////
	
	private static String[] getSchemasArray(GluuSCIMUser user) {
		String[] schemas = null;
		if (user.hasEntitlements() || (user.getLegacyPassword() != null && !user.getLegacyPassword().isEmpty())) {
			schemas = new String[]{USER_CORE_SCHEMA, USER_EXTENSION_SCHEMA};
		} else {
			schemas = new String[]{USER_CORE_SCHEMA};
		}
		return schemas;
	}

	private static ObjectNode getCustomAttributesJson(GluuSCIMUser user) {
		ObjectNode customAttributesJson = JSON_OBJECT_MAPPER.createObjectNode();
		
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
				
				customAttributesJson.set(entitlement.getProductCode(), productMainNode);
			}
		}
		customAttributesJson.put(LEGACY_PASSWORD, user.getLegacyPassword());
		return customAttributesJson;
	}
	
}
