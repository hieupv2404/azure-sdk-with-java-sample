package com.hieupv.azuresdkwithjavasample;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.Region;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.AzureAuthorityHosts;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.Deployment;
import com.azure.resourcemanager.resources.models.DeploymentMode;
import com.azure.resourcemanager.resources.models.DeploymentOperation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hieupv.azuresdkwithjavasample.utils.Utils;
import lombok.extern.log4j.Log4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@SpringBootApplication
@Configuration
@EnableScheduling
public class AzureSdkWithJavaSampleApplication {

    //=================================================================
    // Authenticate

    static final AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);
    static final TokenCredential credential = new DefaultAzureCredentialBuilder()
            .authorityHost(profile.getEnvironment().getActiveDirectoryEndpoint())
            .build();

    static final AzureResourceManager azureResourceManager = AzureResourceManager
            .configure()
            .withLogLevel(HttpLogDetailLevel.BASIC)
            .authenticate(credential, profile)
            .withDefaultSubscription();

    static final String rgName = "cmc-krc-deploy-rg";
    static final String deploymentName = "cmc-krc-deploy-dp";


    public static void main(String[] args) {
        SpringApplication.run(AzureSdkWithJavaSampleApplication.class, args);
    }

    /**
     * Main function which runs the actual sample.
     *
     * @param azureResourceManager instance of the azure client
     * @return true if sample runs successfully
     */

    public static boolean runSample(AzureResourceManager azureResourceManager) throws IOException, IllegalAccessException {

        try {
            String templateJson = AzureSdkWithJavaSampleApplication.getTemplate();

            String parameterJson = AzureSdkWithJavaSampleApplication.getParameter();


            //=============================================================
            // Create resource group.

            System.out.println("Creating a resource group with name: " + rgName);

            azureResourceManager.resourceGroups().define(rgName)
                    .withRegion(Region.KOREA_CENTRAL)
                    .create();

            System.out.println("Created a resource group with name: " + rgName);


            //=============================================================
            // Create a deployment for an Azure App Service via an ARM
            // template.

            System.out.println("====> Template: " + templateJson);
            System.out.println("====> Parameter: " + parameterJson);
            //
            System.out.println("Starting a deployment for an Azure App Service: " + deploymentName);

            azureResourceManager.deployments().define(deploymentName)
                    .withExistingResourceGroup(rgName)
                    .withTemplate(templateJson)
                    .withParameters(parameterJson)
                    .withMode(DeploymentMode.INCREMENTAL)
                    .create();

            System.out.println("Started a deployment for an Azure VM: " + deploymentName);
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
                        System.out.println(operationTxt);
                    }
                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }

        }
    }

    private static String getTemplate() throws IllegalAccessException, JsonProcessingException, IOException {
//        try (InputStream embeddedTemplate = AzureSdkWithJavaSampleApplication.class.getResourceAsStream("/templateValue.json")) {


//        try (InputStream template = AzureSdkWithJavaSampleApplication.class.getResourceAsStream("D:/Document/dmoa/cpl/cmc-deploy/cmc-krc-deploy-vm/template.json")){
//
//            final ObjectMapper mapper = new ObjectMapper();
//            final JsonNode tmp = mapper.readTree(template);
//
////            AzureSdkWithJavaSampleApplication.validateAndAddFieldValue("string", "F1", "skuName", null, tmp);
//
//            return tmp.toString();
//        }

        try {
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode tmp = mapper.readTree(new File("D:/Document/dmoa/cpl/cmc-deploy/cmc-krc-deploy-vm/template.json"));

//            AzureSdkWithJavaSampleApplication.validateAndAddFieldValue("string", "F1", "skuName", null, tmp);

            return tmp.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "File not found";
        }
    }

    private static String getParameter() throws IllegalAccessException, JsonProcessingException, IOException {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode param = mapper.readTree(new File("D:/Document/dmoa/cpl/cmc-deploy/cmc-krc-deploy-vm/parameters.json"));
            return param.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "File not found";
        }
    }

    private static void validateAndAddFieldValue(String type, String fieldValue, String fieldName, String errorMessage,
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

//    @Scheduled(cron = "* 6 * * * *")
    private static void runCreateResources(){
        try {
            runSample(azureResourceManager);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

//    @Scheduled(cron = "* 3 * * * *")
    private static void deleteResourceGroup(){
        try {
            System.out.println("Deleting Resource Group: " + rgName);
            azureResourceManager.resourceGroups().beginDeleteByName(rgName);
            System.out.println("Deleted Resource Group: " + rgName);
        } catch (NullPointerException npe) {
            System.out.println("Did not create any resources in Azure. No clean up is necessary");
        } catch (Exception g) {
            g.printStackTrace();
        }
    }

}


