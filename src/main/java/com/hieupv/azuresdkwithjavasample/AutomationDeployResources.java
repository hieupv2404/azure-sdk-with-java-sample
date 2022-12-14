package com.hieupv.azuresdkwithjavasample;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.Region;
import com.azure.core.management.exception.ManagementException;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.Deployment;
import com.azure.resourcemanager.resources.models.DeploymentMode;
import com.azure.resourcemanager.resources.models.DeploymentOperation;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hieupv.azuresdkwithjavasample.utils.RetrieveConfig;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.websocket.DeploymentException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

@Log4j2
@Builder
public class AutomationDeployResources {

    private String projectName;

    private AzureResourceManager azureResourceManager;
    //=================================================================
    // Authenticate

    private String rgName;
    private String deploymentName;

    private String parentPath;

    /**
     * Main function which runs the actual sample.
     *
     * @param azureResourceManager instance of the azure client
     * @return true if sample runs successfully
     */

    private boolean runSample(AzureResourceManager azureResourceManager) throws IOException, IllegalAccessException {

//        Deployment deployment1 = azureResourceManager.deployments()
//                .getByResourceGroup(rgName, deploymentName);
//        PagedIterable<DeploymentOperation> operations1 = deployment1.deploymentOperations()
//                .list();
//        log.info(operations1.toString());
        try {
            String templateJson = getTemplate(parentPath);

            String parameterJson = getParameter(parentPath);


            //=============================================================
            // Create resource group.
            log.info("Creating a resource group with name: " + rgName);
            boolean isResourceGroupExist = false;
            for (ResourceGroup rGroup : azureResourceManager.resourceGroups().list()) {
                if(rGroup.name().equals(rgName)){
                    isResourceGroupExist = true;
                }

            }
            if(!isResourceGroupExist) {

                azureResourceManager.resourceGroups().define(rgName)
                        .withRegion(Region.create(RetrieveConfig.config.get(projectName + "-rgRegionName"), RetrieveConfig.config.get(projectName + "-rgRegionLabel")))
                        .create();

                log.info("Created a resource group with name: " + rgName);
            } else{
                log.info(rgName + " is exists!");

            }

            //=============================================================
            // Create a deployment for an Azure App Service via an ARM
            // template.

            log.info("====> Template: " + templateJson);
            log.info("====> Parameter: " + parameterJson);
            //
            log.info("Starting a deployment for an Azure App Service: " + deploymentName);

            try {
                azureResourceManager.deployments().define(deploymentName)
                        .withExistingResourceGroup(rgName)
                        .withTemplate(templateJson)
                        .withParameters(parameterJson)
                        .withMode(DeploymentMode.INCREMENTAL)
                        .create();
            } catch (ManagementException e){
                log.error(e.getMessage(), e);
                log.error(e.getCause());
                log.error(e.getStackTrace());
            }

            log.info("Started a deployment for an Azure VM: " + deploymentName);
            return true;
        } finally {
            try {
                Deployment deployment = azureResourceManager.deployments()
                        .getByResourceGroup(rgName, deploymentName);
                PagedIterable<DeploymentOperation> operations = deployment.deploymentOperations()
                        .list();

                for (DeploymentOperation operation : operations) {
                    if (operation.targetResource() != null) {
                        String operationTxt = String.format("id:%s name:%s type: %s provisioning-state:%s code: %s msg: %s",
                                operation.targetResource().id(),
                                operation.targetResource().resourceName(),
                                operation.targetResource().resourceType(),
                                operation.provisioningState(),
                                operation.statusCode(),
                                operation.statusMessage());
                        log.info(operationTxt);
                    }
                }
            } catch (Exception ex) {
                log.info(ex.getMessage());
            }

        }
    }

    private String getTemplate(String parentPath) throws IllegalAccessException, JsonProcessingException, IOException {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            log.info("===> Parent Path: " + parentPath);
            final JsonNode tmp = mapper.readTree(new File(parentPath+"/template.json"));
            return tmp.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "File not found";
        }
    }

    private String getParameter(String parentPath) throws IllegalAccessException, JsonProcessingException, IOException {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            log.info("===> Parent Path: " + parentPath);
            final JsonNode param = mapper.readTree(new File(parentPath+"/parameters.json"));
            return param.get("parameters").toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "File not found";
        }
    }

    private void validateAndAddFieldValue(String type, String fieldValue, String fieldName, String errorMessage,
                                                 JsonNode tmp) throws IllegalAccessException {
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode parameter = mapper.createObjectNode();
        parameter.put("type", type);
        if ("int".equals(type)) {
            parameter.put("defaultValue", Integer.parseInt(fieldValue));
        } else {
            parameter.put("defaultValue", fieldValue);
        }
        ObjectNode.class.cast(tmp.get("parameters")).replace(fieldName, parameter);
    }

    public boolean runCreateResources() {
        try {
            return runSample(azureResourceManager);
        } catch (Exception e) {
            log.info(e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteResourceGroup() {
        try {
            log.info("Deleting Resource Group: " + rgName);
            azureResourceManager.resourceGroups().beginDeleteByName(rgName);
            log.info("Deleted Resource Group: " + rgName);
            return true;
        } catch (NullPointerException npe) {
            log.info("Did not create any resources in Azure. No clean up is necessary");
            return false;
        } catch (Exception g) {
            g.printStackTrace();
            return false;
        }
    }

}


