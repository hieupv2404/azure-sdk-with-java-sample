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
import java.util.Arrays;
import java.util.List;

import static org.spongycastle.asn1.iana.IANAObjectIdentifiers.directory;


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
        RetrieveConfig.setPriority();
        File[] directories = RetrieveConfig.getAllProject(RetrieveConfig.ROOT_PATH_DEPLOY);

        for (int i = 0; i < directories.length; i++) {
            String projectName = directories[i].getName();
            File[] subDirectories = RetrieveConfig.getAllProject(directories[i].getPath());

            subDirectoriesAfterSort(subDirectories).forEach(directory ->{
                log.info("====>>> Start deploy resource path " + directory);
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
            });

            Scheduler create = new Scheduler();
            create.schedule(RetrieveConfig.config.get(projectName + "-schedulerToCreate"), () -> {
                log.info("====>>> 1st Run job to deploy for " + projectName);
                subDirectoriesAfterSort(subDirectories).forEach(directory ->{
                    log.info("====>>> Start deploy resource path " + directory);
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
                });

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

    public static List<String> subDirectoriesAfterSort(File[] subDirectories){
        List<String> priorityList = RetrieveConfig.priorityList;
        String[] subDirectoriesAfterSort = new String[priorityList.size()];
        for (int i = 0; i < subDirectories.length; i++) {
            for(int j =0;j<priorityList.size();j++) {
                if(subDirectories[i].getName().matches(priorityList.get(j)+"(.*)")){
                    if(subDirectoriesAfterSort[j]==null){
                        subDirectoriesAfterSort[j] = subDirectories[i].getPath();
                    }else{
                        subDirectoriesAfterSort[j] += ","+subDirectories[i].getPath();
                    }
                }
            }
        }
        List<String> tempFinalSubDirectories = new ArrayList<>(Arrays.asList(subDirectoriesAfterSort));
        tempFinalSubDirectories.removeAll(Arrays.asList("", null));
        List<String> finalSubDirectories = new ArrayList<>();
        tempFinalSubDirectories.forEach(subPath->{
           if(subPath.contains(",")){
               String[] pathSplit = subPath.split(",");
               finalSubDirectories.addAll(Arrays.asList(pathSplit));
           }else{
               finalSubDirectories.add(subPath);
           }
        });

        log.info("====> Final: ");
        finalSubDirectories.forEach(path -> log.info(path));
        return finalSubDirectories;

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


