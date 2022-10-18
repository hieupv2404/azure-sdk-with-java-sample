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
    static int i;
    public static void main(String[] args) {
        SpringApplication.run(AzureSdkWithJavaSampleApplication.class, args);
        File[] directories = RetrieveConfig.getAllProject(RetrieveConfig.ROOT_PATH_DEPLOY);

        for (i = 0; i < directories.length; i++) {
            String projectName = directories[i].getName();
            AutomationDeployResources automation = AutomationDeployResources.builder()
                    .parentPath(directories[i].getPath())
                    .projectName(projectName)
                    .azureResourceManager(azureResourceManager)
                    .build();
            automationDeployResourcesList.add(automation);
            Scheduler create = new Scheduler();
            create.schedule(RetrieveConfig.config.get(projectName+"-schedulerToCreate"), () -> automationDeployResourcesList.get(i).runCreateResources());
            // Starts the scheduler to create resources.
            create.start();

            Scheduler delete = new Scheduler();
            delete.schedule(RetrieveConfig.config.get(projectName+"-schedulerToDelete"), () -> automationDeployResourcesList.get(i).deleteResourceGroup());
            // Starts the scheduler to delete resources.
            delete.start();
            schedulerList.add(create);
            schedulerList.add(delete);
        }
    }

    static AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);
    static TokenCredential credential = new DefaultAzureCredentialBuilder()
            .authorityHost(profile.getEnvironment().getActiveDirectoryEndpoint())
            .build();

    static AzureResourceManager azureResourceManager = AzureResourceManager
            .configure()
            .withLogLevel(HttpLogDetailLevel.BASIC)
            .authenticate(credential, profile)
            .withDefaultSubscription();
}


