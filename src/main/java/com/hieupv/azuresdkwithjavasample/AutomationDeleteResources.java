package com.hieupv.azuresdkwithjavasample;

import com.azure.resourcemanager.AzureResourceManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Builder
@Log4j2
public class AutomationDeleteResources {
    private String projectName;

    private AzureResourceManager azureResourceManager;

    private String rgName;
    private String deploymentName;

    private String parentPath;

    private String configDeleteFilePath;

    public void createScriptDeleteResource(List<String> directories) throws IOException {
        // Create new file to config delete
        AtomicInteger folderCount = new AtomicInteger();
        File f = new File(configDeleteFilePath);
        if (f.exists() && f.isDirectory()) {
            FileUtils.deleteDirectory(f);

        }
        if (!f.exists()) {
            f.mkdir();
        }

        directories.forEach(directory -> {
            folderCount.getAndIncrement();
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode template;
            String resourceName = null;
            String resourceType = null;

            try {
                template = mapper.readTree(new File(directory + "/template.json"));
                JsonNode resourcesNode = template.get("resources").get(0);
                if (!resourcesNode.get("type").isNull()) {
                    resourceType = resourcesNode.get("type").toString().replaceAll("\"", "");
                }
                if (!resourcesNode.get("name").isNull()) {
                    String[] tempArrayString = resourcesNode.get("name").toString().split("\'");
                    String paramName = tempArrayString[1];
                    JsonNode paramNodeName = template.get("parameters").get(paramName);
                    if (!paramNodeName.get("defaultValue").isNull()) {
                        resourceName = paramNodeName.get("defaultValue").toString().replaceAll("\"", "");
                    }
                }
                File fileScript = new File(configDeleteFilePath + folderCount.get());
                if (fileScript.exists() && fileScript.isDirectory()) {
                    fileScript.delete();
                }
                if (!fileScript.exists()) {
                    fileScript.mkdir();
                    Files.write(Paths.get(configDeleteFilePath + folderCount.get() + "/config-delete.bat"), "".getBytes(), StandardOpenOption.CREATE_NEW);
                    Files.write(Paths.get(configDeleteFilePath + folderCount.get() + "/config-delete.sh"), "".getBytes(), StandardOpenOption.CREATE_NEW);

                }
                Files.write(Paths.get(configDeleteFilePath + folderCount.get() + "/config-delete.bat"), ("az resource delete --resource-group " + rgName + " --name " + resourceName + " --resource-type " + resourceType + "\n").getBytes(), StandardOpenOption.APPEND);
                Files.write(Paths.get(configDeleteFilePath + folderCount.get() + "/config-delete.sh"), ("az resource delete --resource-group " + rgName + " --name " + resourceName + " --resource-type " + resourceType + "\n").getBytes(), StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    int iExitValue;

    public void runScriptDeleteResource(String pathScriptFile) {
        boolean isWindows = System.getProperty("os.name")
                .toLowerCase().startsWith("windows");
        if (isWindows) {
            String sCommandString = pathScriptFile + "/config-delete.bat";
            CommandLine oCmdLine = CommandLine.parse(sCommandString);
            DefaultExecutor oDefaultExecutor = new DefaultExecutor();
            oDefaultExecutor.setExitValue(0);
            try {
                iExitValue = oDefaultExecutor.execute(oCmdLine);
            } catch (ExecuteException e) {
                log.error("Execution failed.");
                e.printStackTrace();
            } catch (IOException e) {
                log.error("Permission denied.");
                e.printStackTrace();
            }
        } else {
            String sCommandString = "sh " + pathScriptFile +"/config-delete.sh";
            String chmodString = "chmod +x " + pathScriptFile +"/config-delete.sh";
            CommandLine oCmdLine = CommandLine.parse(sCommandString);
            CommandLine chmodLine = CommandLine.parse(chmodString);
            DefaultExecutor oDefaultExecutor = new DefaultExecutor();
            oDefaultExecutor.setExitValue(0);
            try {
                iExitValue = oDefaultExecutor.execute(chmodLine);
                iExitValue = oDefaultExecutor.execute(oCmdLine);
            } catch (ExecuteException e) {
                log.error("Execution failed.");
                e.printStackTrace();
            } catch (IOException e) {
                log.error("Permission denied.");
                e.printStackTrace();
            }
        }


    }

}
