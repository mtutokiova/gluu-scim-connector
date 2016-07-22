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
     * Custom processor
     *
     * @param friend Name to be used to generate a greeting message.
     * @return A greeting message
     * @throws GluuSCIMConnectorException 
     * @throws GluuSCIMServerErrorException 
     */
    @Processor
    public String getUser(MuleEvent event) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
        /*
         * MESSAGE PROCESSOR CODE GOES HERE
         */
        return client.getUser(event);
    }
    
    /**
     * Custom processor
     *
     * @param friend Name to be used to generate a greeting message.
     * @return A greeting message
     * @throws GluuSCIMConnectorException 
     * @throws GluuSCIMServerErrorException 
     */
    @Processor
    public String createUser(MuleEvent event) throws GluuSCIMServerErrorException, GluuSCIMConnectorException {
        /*
         * MESSAGE PROCESSOR CODE GOES HERE
         */
        return client.createUser(event);
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