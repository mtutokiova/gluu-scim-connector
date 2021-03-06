package org.mule.modules.gluuscim;

import org.mule.api.MuleEvent;
import org.mule.api.annotations.Config;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.lifecycle.Start;
import org.mule.modules.gluuscim.client.GluuSCIMClient;
import org.mule.modules.gluuscim.config.GluuSCIMConnectorConfig;
import org.mule.modules.gluuscim.entities.GluuSCIMUser;
import org.mule.modules.gluuscim.exception.GluuSCIMConnectorException;
import org.mule.modules.gluuscim.exception.GluuSCIMServerErrorException;

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
    public GluuSCIMUser getUser(MuleEvent event, String attribute, String value) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
        return client.getUser(event, attribute, value);
    }
    
    /**
     * Creates user in gluu
     *
     * @return Gluu User
     * @throws GluuSCIMConnectorException 
     * @throws GluuSCIMServerErrorException 
     */
    @Processor
    public GluuSCIMUser createUser(MuleEvent event, GluuSCIMUser user) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
        return client.createUser(event, user);
    }
    
    /**
     * Updates user in gluu
     *
     * @return Gluu User
     * @throws GluuSCIMConnectorException 
     * @throws GluuSCIMServerErrorException 
     */
    @Processor
    public GluuSCIMUser updateUser(MuleEvent event, GluuSCIMUser user) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
        return client.updateUser(event, user);
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