package com.hieupv.azuresdkwithjavasample;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.hieupv.azuresdkwithjavasample.utils.RetrieveConfig;
import it.sauronsoftware.cron4j.Scheduler;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@SpringBootApplication
@Configuration
@EnableScheduling
@Log4j2
public class AzureSdkWithJavaSampleApplication {
    static List<AutomationDeployResources> automationDeployResourcesList = new ArrayList<>();
    static List<AutomationDeleteResources> automationDeleteResourcesList = new ArrayList<>();

    static List<String> subDirectoriesAfterSortReversed = new ArrayList<>();
    static List<Scheduler> schedulerList = new ArrayList<>();
    static String tenantId = null;
    static String subscriptionId = null;

    public static void main(String[] args) throws IOException {
        SpringApplication.run(AzureSdkWithJavaSampleApplication.class, args);

        // Load env variable
        RetrieveConfig.ROOT_PATH_DEPLOY = System.getenv("ROOT_PATH_DEPLOY");
        tenantId = System.getenv("TENANT_ID");
        subscriptionId = System.getenv("SUBSCRIPTION_ID");
        // End load env variable

        AzureProfile profile = new AzureProfile(tenantId, subscriptionId, AzureEnvironment.AZURE);
        TokenCredential credential = new DefaultAzureCredentialBuilder()
                .authorityHost(profile.getEnvironment().getActiveDirectoryEndpoint())
                .build();

        AzureResourceManager azureResourceManager = AzureResourceManager
                .configure()
                .withLogLevel(HttpLogDetailLevel.BASIC)
                .authenticate(credential, profile)
                .withDefaultSubscription();

        RetrieveConfig.retrieveConfig();
        RetrieveConfig.setPriority();
        File[] directories = RetrieveConfig.getAllProject(RetrieveConfig.ROOT_PATH_DEPLOY);

        for (int i = 0; i < directories.length; i++) {
            String projectName = directories[i].getName();
            File[] subDirectories = RetrieveConfig.getAllProject(directories[i].getPath());
            // Create with schedule
            Scheduler create = new Scheduler();
            create.schedule(RetrieveConfig.config.get(projectName + "-schedulerToCreate"), () -> {
                log.info("====>>> 1st Run job to deploy for " + projectName);
                subDirectoriesAfterSort(subDirectories).forEach(directory -> {
                    log.info("====================================");
                    log.info("====>>> Start deploy resource path " + directory);
                    if (directory.matches("(.*)diskos(.*)")) {
                        log.info("Ignoring Deploy Disk OS");
                        return;
                    }
                    AutomationDeployResources automation = AutomationDeployResources.builder()
                            .parentPath(directory)
                            .projectName(projectName)
                            .azureResourceManager(azureResourceManager)
                            .rgName(RetrieveConfig.config.get(projectName + "-rgName"))
                            .deploymentName(RetrieveConfig.config.get(projectName + "-deploymentName"))
                            .build();
                    automationDeployResourcesList.add(automation);
                    automation.runCreateResources();
                    log.info("====>>> End deploy resource path " + directory);

                    File autoGenFile = new File(directory);
                    String parentPath = null;
                    if (autoGenFile.getName().matches("nsg(.*)")) {
                        parentPath = autoGenFile.getParent();
                        String directoryGenned = parentPath + "/sgr-" + autoGenFile.getName();
                        log.info("====================================");
                        log.info("====>>> Start deploy resource path " + directoryGenned);
                        AutomationDeployResources automationWithGenFolder = AutomationDeployResources.builder()
                                .parentPath(directoryGenned)
                                .projectName(projectName)
                                .azureResourceManager(azureResourceManager)
                                .rgName(RetrieveConfig.config.get(projectName + "-rgName"))
                                .deploymentName(RetrieveConfig.config.get(projectName + "-deploymentName"))
                                .build();
                        automationDeployResourcesList.add(automation);
                        automationWithGenFolder.runCreateResources();
                    } else if (autoGenFile.getName().matches("vnet(.*)")) {
                        parentPath = autoGenFile.getParent();
                        String directoryGenned = parentPath + "/snet-" + autoGenFile.getName();
                        log.info("====================================");
                        log.info("====>>> Start deploy resource path " + directoryGenned);
                        AutomationDeployResources automationWithGenFolder = AutomationDeployResources.builder()
                                .parentPath(directoryGenned)
                                .projectName(projectName)
                                .azureResourceManager(azureResourceManager)
                                .rgName(RetrieveConfig.config.get(projectName + "-rgName"))
                                .deploymentName(RetrieveConfig.config.get(projectName + "-deploymentName"))
                                .build();
                        automationDeployResourcesList.add(automation);
                        automationWithGenFolder.runCreateResources();
                    }
                });

            });
            create.start();
            log.info("====>>> Run scheduling create resources for " + projectName + " ....");
            schedulerList.add(create);

            // Schedule the delete resources
            Scheduler delete = new Scheduler();
            delete.schedule(RetrieveConfig.config.get(projectName + "-schedulerToDelete"), () -> {
                subDirectoriesAfterSortReversed = new ArrayList<>(subDirectoriesAfterSort(subDirectories));
                Collections.reverse(subDirectoriesAfterSortReversed);
                log.info("====================================");
                log.info("====>>> Start delete resource");
                AutomationDeleteResources automationDeleteResources = AutomationDeleteResources.builder()
                        .configDeleteFilePath(RetrieveConfig.ROOT_PATH_DEPLOY + "/" + projectName + "/script/")
                        .rgName(RetrieveConfig.config.get(projectName + "-rgName"))
                        .build();
                automationDeleteResourcesList.add(automationDeleteResources);
                // Create file script auto delete resource
                try {
                    automationDeleteResources.createScriptDeleteResource(subDirectoriesAfterSortReversed);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                // Detele Resource with script
                File[] folderScript = RetrieveConfig.getAllProject(RetrieveConfig.ROOT_PATH_DEPLOY + "/" + projectName + "/script/");

                for (File file : folderScript) {
                    automationDeleteResources.runScriptDeleteResource(file.getPath());
                }
                log.info("====>>> End delete resource path");
            });
            // Starts the scheduler to delete resources.
            delete.start();
            log.info("====>>> Run scheduling delete resources for " + projectName + " ....");
            schedulerList.add(delete);
        }

    }

    public static List<String> subDirectoriesAfterSort(File[] subDirectories) {
        List<String> priorityList = RetrieveConfig.priorityList;
        String[] subDirectoriesAfterSort = new String[priorityList.size()];
        for (int i = 0; i < subDirectories.length; i++) {
            for (int j = 0; j < priorityList.size(); j++) {
                if (subDirectories[i].getName().matches(priorityList.get(j) + "(.*)")) {
                    if (subDirectoriesAfterSort[j] == null) {
                        subDirectoriesAfterSort[j] = subDirectories[i].getPath();
                    } else {
                        subDirectoriesAfterSort[j] += "," + subDirectories[i].getPath();
                    }

                }
            }
        }
        List<String> tempFinalSubDirectories = new ArrayList<>(Arrays.asList(subDirectoriesAfterSort));
        tempFinalSubDirectories.removeAll(Arrays.asList("", null));
        List<String> finalSubDirectories = new ArrayList<>();
        tempFinalSubDirectories.forEach(subPath -> {
            if (subPath.contains(",")) {
                String[] pathSplit = subPath.split(",");
                finalSubDirectories.addAll(Arrays.asList(pathSplit));
            } else {
                finalSubDirectories.add(subPath);
            }
        });

        log.info("====> Final: ");
        finalSubDirectories.forEach(path -> log.info(path));
        return finalSubDirectories;

    }
}


