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
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hieupv.azuresdkwithjavasample.utils.RetrieveConfig;
import com.jayway.jsonpath.JsonPath;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.websocket.DeploymentException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
            log.info("Starting a deployment for an Azure Resource: " + deploymentName);

            try {
                azureResourceManager.deployments().define(deploymentName)
                        .withExistingResourceGroup(rgName)
                        .withTemplate(templateJson)
                        .withParameters(parameterJson)
                        .withMode(DeploymentMode.INCREMENTAL)
                        .create();
            } catch (ManagementException e){
                log.error("====>>> Exception create resource: " + e.getMessage(), e);
                log.error("====>>> Failed to create resource: " + parentPath);
            }

            log.info("Done a deployment for an Azure Resource: " + deploymentName);
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
            JsonNode tmp = null;
            File directory = new File(parentPath);

            if (directory.getName().matches("nsg(.*)")) {
                String parentPathDir = directory.getParent();
                File sourceDirectory = new File(directory.getPath());
                File destinationDirectory = new File(parentPathDir + "/sgr-" + directory.getName());
                try {
                    // Copy directory
                    FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

                    // Modify file contents
                    // Remove from original file contents
                    final ObjectMapper mapperDir = new ObjectMapper();
                    final JsonNode param = mapperDir.readTree(new File(sourceDirectory.getPath() + "/template.json"));
                    JsonNode paramNode = param.get("resources");

                    for (JsonNode paramNodeIndex : paramNode) {
                        Map<String, Object> mapKeyOfParameters = mapperDir.readValue(paramNodeIndex.toString(), new TypeReference<Map>() {
                        });
                        mapKeyOfParameters.forEach((k, v) -> {
                            if (k.equals("type") && paramNodeIndex.at("/type").toString().equals("\"Microsoft.Network/networkSecurityGroups/securityRules\"")) {
//                                        ((ObjectNode) paramNode).remove(k);
                                ((ObjectNode) paramNodeIndex).removeAll();

                            }
                            if (k.equals("type") && paramNodeIndex.at("/type").toString().equals("\"Microsoft.Network/networkSecurityGroups\"")) {
                                ((ObjectNode) paramNodeIndex).remove("dependsOn");

                                ((ObjectNode) paramNodeIndex).remove("properties");
                            }
                        });
                    }

                    String first = param.toString().replaceAll(",\\{\\}","");
                    String second = first.replaceAll("\\[\\{\\},","[");

                    // Remove from clone file contents
                    final ObjectMapper mapperClone = new ObjectMapper();
                    final JsonNode paramClone = mapperClone.readTree(new File(destinationDirectory.getPath() + "/template.json"));
                    JsonNode paramNodeClone = paramClone.get("resources");

                    for (JsonNode paramNodeIndex : paramNodeClone) {
                        Map<String, Object> mapKeyOfParameters = mapperClone.readValue(paramNodeIndex.toString(), new TypeReference<Map>() {
                        });
                        mapKeyOfParameters.forEach((k, v) -> {
                            if (k.equals("type") && paramNodeIndex.at("/type").toString().equals("\"Microsoft.Network/networkSecurityGroups\"")) {
                                ((ObjectNode) paramNodeIndex).removeAll();

                            }
                            if (k.equals("type") && paramNodeIndex.at("/type").toString().equals("\"Microsoft.Network/networkSecurityGroups/securityRules\"")) {
                                ((ObjectNode) paramNodeIndex).remove("dependsOn");
                            }
                        });

                    }
                    String firstClone = paramClone.toString().replaceAll(",\\{\\}","");
                    String secondClone = firstClone.replaceAll("\\[\\{\\},","[");
                    FileUtils.writeStringToFile(new File(destinationDirectory.getPath() + "/template.json"), secondClone, Charset.forName("UTF-8"));

                    return second;
                } catch (IOException e) {
                    log.error("Error when modify directory " + sourceDirectory + " and " + destinationDirectory);
                    throw new RuntimeException(e);
                }
            } else if (directory.getName().matches("vnet(.*)")) {
                String parentPathDir = directory.getParent();
                File sourceDirectory = new File(directory.getPath());
                File destinationDirectory = new File(parentPathDir + "/snet-" + directory.getName());
                try {
                    // Copy directory
                    FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

                    // Modify file contents
                    // Remove from original file contents
                    final ObjectMapper mapperDir = new ObjectMapper();
                    final JsonNode param = mapperDir.readTree(new File(sourceDirectory.getPath() + "/template.json"));
                    JsonNode paramNode = param.get("resources");

                    for (JsonNode paramNodeIndex : paramNode) {
                        Map<String, Object> mapKeyOfParameters = mapperDir.readValue(paramNodeIndex.toString(), new TypeReference<Map>() {
                        });
                        mapKeyOfParameters.forEach((k, v) -> {
                            if (k.equals("type") && paramNodeIndex.at("/type").toString().equals("\"Microsoft.Network/virtualNetworks/subnets\"")) {
                                ((ObjectNode) paramNodeIndex).removeAll();
                            }
                            if (k.equals("type") && paramNodeIndex.at("/type").toString().equals("\"Microsoft.Network/virtualNetworks\"")) {
                                ((ObjectNode) paramNodeIndex).remove("dependsOn");

                                ((ObjectNode) paramNodeIndex).remove("properties/subnets");
                            }
                        });
                    }

                    String first = param.toString().replaceAll(",\\{\\}", "");
                    String second = first.replaceAll("\\[\\{\\},", "[");

                    // Remove from clone file contents
                    final ObjectMapper mapperClone = new ObjectMapper();
                    final JsonNode paramClone = mapperClone.readTree(new File(destinationDirectory.getPath() + "/template.json"));
                    JsonNode paramNodeClone = paramClone.get("resources");

                    for (JsonNode paramNodeIndex : paramNodeClone) {
                        Map<String, Object> mapKeyOfParameters = mapperClone.readValue(paramNodeIndex.toString(), new TypeReference<Map>() {
                        });
                        mapKeyOfParameters.forEach((k, v) -> {
                            if (k.equals("type") && paramNodeIndex.at("/type").toString().equals("\"Microsoft.Network/virtualNetworks\"")) {
//                                        ((ObjectNode) paramNode).remove(k);
                                ((ObjectNode) paramNodeIndex).removeAll();

                            }
                            if (k.equals("type") && paramNodeIndex.at("/type").toString().equals("\"Microsoft.Network/virtualNetworks/subnets\"")) {
                                ((ObjectNode) paramNodeIndex).remove("dependsOn");
                            }
                        });

                    }
                    String firstClone = paramClone.toString().replaceAll(",\\{\\}", "");
                    String secondClone = firstClone.replaceAll("\\[\\{\\},", "[");
                    FileUtils.writeStringToFile(new File(destinationDirectory.getPath() + "/template.json"), secondClone, Charset.forName("UTF-8"));

                    return second;
                } catch (IOException e) {
                    log.error("Error when modify directory " + sourceDirectory + " and " + destinationDirectory);
                    throw new RuntimeException(e);
                }
            } else if(directory.getName().matches("diskos(.*)")) {
                try {
                    final ObjectMapper mapperDiskOS = new ObjectMapper();
                    final JsonNode resources = mapperDiskOS.readTree(new File(parentPath + "/template.json"));

                    if (resources.at("/resources").get(0).get("location").toString().equals("\"koreacentral\"") && !resources.at("/resources").get(0).at("/properties").get("hyperVGeneration").isNull()) {
                        ((ObjectNode) resources.at("/resources").get(0).at("/properties")).remove("supportsHibernation");
                    }
                    return resources.toString();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return "File not found";
                }
            } else if(directory.getName().matches("vm(.*)")){
                try {
                    final ObjectMapper mapperDiskOS = new ObjectMapper();
                    final JsonNode resources = mapperDiskOS.readTree(new File(parentPath + "/template.json"));

                    if (!resources.at("/resources").get(0).at("/properties/storageProfile").get("osDisk").isNull()) {
                        ((ObjectNode) resources.at("/resources").get(0).at("/properties/storageProfile")).remove("osDisk");
                    }
                    if (resources.at("/resources").get(0).get("location").toString().equals("\"koreacentral\"") && !resources.at("/resources").get(0).at("/properties/osProfile").get("requireGuestProvisionSignal").isNull()){
                        ((ObjectNode) resources.at("/resources").get(0).at("/properties/osProfile")).remove("requireGuestProvisionSignal");
                    }
                    return resources.toString();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return "File not found";
                }
            } else {
                tmp = mapper.readTree(new File(parentPath + "/template.json"));
            }
            return tmp.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "File not found";
        }
    }

    private String getParameter(String parentPath) throws IllegalAccessException, JsonProcessingException, IOException {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode param = mapper.readTree(new File(parentPath+"/parameters.json"));
            JsonNode paramNode = param.get("parameters");

            Map<String, Object> mapKeyOfParameters = mapper.readValue(paramNode.toString(), new TypeReference<Map>() {});
            mapKeyOfParameters.forEach((k, v) -> {
                if(paramNode.at("/"+k+"/value").toString().equals("null")){
                    ((ObjectNode)paramNode).remove(k);
                }
            });
            return paramNode.toString();
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


