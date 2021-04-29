package com.ibm;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class Configs {
    public Configs(String runMode) throws IOException {
        if (runMode.equals("local")){
            String configFile = "/application.properties";
            Properties properties = new Properties();
            InputStream reader = getClass().getResourceAsStream(configFile);
            properties.load(reader);
            this.namespace = properties.getProperty("namespace");
            this.deployments = properties.getProperty("deployment").split(",");
            this.udpServerPort = Integer.parseInt(properties.getProperty("udpServerPort"));
        }else{
            this.namespace = System.getenv("namespace");
            this.deployments = System.getenv("deployment").split(",");
            this.udpServerPort = Integer.parseInt(System.getenv("udpServerPort"));
        }

    }

    public String getNamespace() {
        return namespace;
    }
    public String[] getDeployments() {
        return deployments;
    }
    public int getUdpServerPort() {
        return udpServerPort;
    }

    private String namespace;
    private String[] deployments;
    private int udpServerPort;
}
