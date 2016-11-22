package org.mule.modules.gluuscim.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
	private static final String ENTITLEMENTS = "Entitlements";
	private static final String PRODUCT = "Product";
	private static final String ENTITLEMENT_START_DATE = "Start";
	private static final String ENTITLEMENT_END_DATE = "End";
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
				
				String customAttributesString = jsonResponse.get(USER_EXTENSION_SCHEMA).toString();
				customAttributesString = customAttributesString.replace("\\\"{", "{");
				customAttributesString = customAttributesString.replace("\"{", "{");
				customAttributesString = customAttributesString.replace("}\\\"", "}");
				customAttributesString = customAttributesString.replace("}\"", "}");
				customAttributesString = customAttributesString.replace("\\\\\"", "\"");
				customAttributesString = customAttributesString.replace("\\\"", "\"");
				
				Iterator<Entry<String, JsonNode>> customFieldsIterator = JSON_OBJECT_MAPPER.readTree(customAttributesString).fields();
				for (Iterator<Entry<String, JsonNode>> fields = customFieldsIterator; fields.hasNext();) {
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
						JsonNode fieldValue = field.getValue().get(0);
						String productType = fieldValue.get(PRODUCT).asText();

						JsonNode entitlementsValue = JSON_OBJECT_MAPPER.readTree(fieldValue.get(ENTITLEMENTS).asText());
						
						for (Iterator<JsonNode> entitlementsIterator = entitlementsValue.elements(); entitlementsIterator.hasNext();) {
							JsonNode entitlementJson = entitlementsIterator.next();
						
							GluuSCIMEntitlement entitlement = new GluuSCIMEntitlement();
							entitlement.setProductType(productType);
							entitlement.setStartDate(entitlementJson.get(ENTITLEMENT_START_DATE).asText());
							entitlement.setEndDate(entitlementJson.get(ENTITLEMENT_END_DATE).asText());
							entitlement.setCanonicalId(entitlementJson.get(ID).asText());
							
							entitlements.add(entitlement);
						}
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
			Map<String, List<GluuSCIMEntitlement>> entitlementsMap = new HashMap<String, List<GluuSCIMEntitlement>>();
			
			// currently have only Digital and Web products, 
			// however to support new product types in future, sorting them by product type first  
			for (GluuSCIMEntitlement entitlement: user.getEntitlements()){
				if(entitlementsMap.containsKey(entitlement.getProductType())){
					entitlementsMap.get(entitlement.getProductType()).add(entitlement);
				} else {
					List<GluuSCIMEntitlement> entitlementsList = new ArrayList<GluuSCIMEntitlement>();
					entitlementsList.add(entitlement);
					entitlementsMap.put(entitlement.getProductType(), entitlementsList);
				}
			}
			
			for (String productType: entitlementsMap.keySet()) {

				ArrayNode entitlementsMainNode = JSON_OBJECT_MAPPER.createArrayNode();
				for (GluuSCIMEntitlement entitlement : entitlementsMap.get(productType)) {
					
					ObjectNode entitlementSubNode = JSON_OBJECT_MAPPER.createObjectNode();
					entitlementSubNode.put(ID, entitlement.getCanonicalId());
					entitlementSubNode.put(ENTITLEMENT_START_DATE, entitlement.getStartDate());
					entitlementSubNode.put(ENTITLEMENT_END_DATE, entitlement.getEndDate());
					
					entitlementsMainNode.add(entitlementSubNode.toString());
				}
				ObjectNode productNode = JSON_OBJECT_MAPPER.createObjectNode();
				productNode.put(PRODUCT, productType);
				productNode.set(ENTITLEMENTS, entitlementsMainNode);
				ObjectNode productSubNode = JSON_OBJECT_MAPPER.createObjectNode();
				productSubNode.set(productType, productNode);
				ArrayNode productMainNode = JSON_OBJECT_MAPPER.createArrayNode();
				productMainNode.add(productNode.toString());
				customAttributesJson.set(productType, productMainNode);
			}
			
		}
		customAttributesJson.put(LEGACY_PASSWORD, user.getLegacyPassword());
		return customAttributesJson;
	}
	
}
