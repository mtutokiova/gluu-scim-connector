package org.mule.modules.gluuscim;

import org.mule.api.MuleEvent;
import org.mule.api.annotations.Config;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.lifecycle.Start;
import org.mule.modules.gluuscim.client.GluuSCIMClient;
import org.mule.modules.gluuscim.config.GluuSCIMConnectorConfig;
import org.mule.modules.gluuscim.exception.GluuSCIMConnectorException;
import org.mule.modules.gluuscim.exception.GluuSCIMServerErrorException;

import com.fasterxml.jackson.databind.JsonNode;

@Connector(name="gluu-scim", friendlyName="GluuSCIM")
public class GluuSCIMConnector {

    @Config
    GluuSCIMConnectorConfig config;
    
    private GluuSCIMClient client;
    
    @Start
    public void init() {
        setClient(new GluuSCIMClient(config));
    }


    /**
     * Searches user by attribute
     *
     * @param attribute - User attribute to search for
     * @param value - Search value
     * @return Gluu user 
     * @throws GluuSCIMConnectorException 
     * @throws GluuSCIMServerErrorException 
     */
    @Processor
    public String getUser(MuleEvent event, String attribute, String value) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
        return client.getUser(event, attribute, value);
    }
    
    /**
     * Creates user in gluu
     *
     * @param firstName - First Name
     * @param lastName - Last Name
     * @param displayName - Display Name (e.g. First Name + Last Name)
     * @param email - Email
     * @param password - Password
     * @param entitlements - Products which users is subscribed to in json format e.g. {"printPlusWeb":["{\"Print + Web\":{\"product\":\"Print + Web\",\"entitlementStartDate\":\"1469051438\",\"entitlementEndDate\":\"1500508800\"}}"]}
     * @return Gluu User
     * @throws GluuSCIMConnectorException 
     * @throws GluuSCIMServerErrorException 
     */
    @Processor
    public String createUser(MuleEvent event, String firstName, String lastName, String displayName, String email, String password, JsonNode entitlements) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
        return client.createUser(event, firstName, lastName, displayName, email, password, entitlements);
    }
    
    /**
     * Updates user in gluu
     *
     * @param firstName - First Name
     * @param lastName - Last Name
     * @param displayName - Display Name (e.g. First Name + Last Name)
     * @param email - Email
     * @param password - Password
     * @param entitlements - Products which users is subscribed to in json format e.g. {"printPlusWeb":["{\"Print + Web\":{\"product\":\"Print + Web\",\"entitlementStartDate\":\"1469051438\",\"entitlementEndDate\":\"1500508800\"}}"]}
     * @return Gluu User
     * @throws GluuSCIMConnectorException 
     * @throws GluuSCIMServerErrorException 
     */
    @Processor
    public String updateUser(MuleEvent event, String gluuId, String firstName, String lastName, String displayName, String email, String password, JsonNode entitlements) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
        return client.updateUser(event, gluuId, firstName, lastName, displayName, email, password, entitlements);
    }

    public GluuSCIMConnectorConfig getConfig() {
        return config;
    }

    public void setConfig(GluuSCIMConnectorConfig config) {
        this.config = config;
    }
    
    public GluuSCIMClient getClient() {
		return client;
	}

	public void setClient(GluuSCIMClient client) {
		this.client = client;
	}

}