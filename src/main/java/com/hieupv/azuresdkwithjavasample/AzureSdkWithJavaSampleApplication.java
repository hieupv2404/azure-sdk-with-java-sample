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
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


@SpringBootApplication
@Configuration
@EnableScheduling
@Log4j2
public class AzureSdkWithJavaSampleApplication {
    static List<AutomationDeployResources> automationDeployResourcesList = new ArrayList<>();
    static List<Scheduler> schedulerList = new ArrayList<>();
    public static void main(String[] args) {
        SpringApplication.run(AzureSdkWithJavaSampleApplication.class, args);
        RetrieveConfig.retrieveConfig();
        File[] directories = RetrieveConfig.getAllProject(RetrieveConfig.ROOT_PATH_DEPLOY);

        for (int i = 0; i < directories.length; i++) {
            String projectName = directories[i].getName();
            File[] subDirectories = RetrieveConfig.getAllProject(directories[i].getPath());
            log.info("Project Name: " + projectName);
            log.info("Sub Directory: " + subDirectories.toString());
            Scheduler create = new Scheduler();
            create.schedule(RetrieveConfig.config.get(projectName + "-schedulerToCreate"), () -> {
                log.info("====>>> 1st Run job to deploy for " + projectName);
                for (int j = 0; j < subDirectories.length; j++) {
                    AutomationDeployResources automation = AutomationDeployResources.builder()
                            .parentPath(subDirectories[j].getPath())
                            .projectName(projectName)
                            .azureResourceManager(azureResourceManager)
                            .rgName(RetrieveConfig.config.get(projectName + "-rgName"))
                            .deploymentName(RetrieveConfig.config.get(projectName + "-deploymentName"))
                            .build();
                    automationDeployResourcesList.add(automation);
                    automation.runCreateResources();
                }
            });
            create.start();
            schedulerList.add(create);

//                Scheduler delete = new Scheduler();
//                delete.schedule(RetrieveConfig.config.get(projectName + "-schedulerToDelete"), () -> automationDeployResourcesList.get(currentIndex).deleteResourceGroup());
//                // Starts the scheduler to delete resources.
//                delete.start();
//                schedulerList.add(delete);


        }

    }

    static AzureProfile profile = new AzureProfile("16f290ef-2fe2-43b8-b8b5-2b74d9257f50", "0e57272d-f2d7-4c7b-ad01-4632eee6454f",AzureEnvironment.AZURE);
    static TokenCredential credential = new DefaultAzureCredentialBuilder()
            .authorityHost(profile.getEnvironment().getActiveDirectoryEndpoint())
            .build();

    static AzureResourceManager azureResourceManager = AzureResourceManager
            .configure()
            .withLogLevel(HttpLogDetailLevel.BASIC)
            .authenticate(credential, profile)
            .withDefaultSubscription();
}


