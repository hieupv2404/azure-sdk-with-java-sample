package com.hieupv.azuresdkwithjavasample.utils;

import lombok.extern.log4j.Log4j2;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

@Log4j2
public class RetrieveConfig {
    public static Map<String,String> config = new HashMap<>();
    public static final String ROOT_PATH_DEPLOY = System.getenv("root-path-deploy");

    public static File[] getAllProject(String rootPath){
        return new File(rootPath).listFiles(File::isDirectory);
    }
    public static void retrieveConfig() {
        File[] directories = getAllProject(ROOT_PATH_DEPLOY);
        for (int i = 0; i < directories.length; i++) {
            String projectName = directories[i].getName();
            JSONParser parser = new JSONParser();
            try {
                FileReader reader = new FileReader(directories[i].getPath() + "/config.json");

                int readSize = reader.read();
                if (readSize == -1) {
                    log.error("File " + directories[i].getPath() + "/config.json is not found!");
                    continue;
                }
                else {
                    log.info("File " + directories[i].getPath() + "/config.json is OK!");
                }

                Object obj = parser.parse(new FileReader(directories[i].getPath() + "/config.json"));
                JSONObject jsonObject = (JSONObject)obj;
                config.put(projectName+"-rgName",(String)jsonObject.get("rg-name"));
                config.put(projectName+"-rgRegionName",(String)jsonObject.get("rg-region-name"));
                config.put(projectName+"-rgRegionLabel",(String)jsonObject.get("rg-region-label"));
                config.put(projectName+"-deploymentName",(String)jsonObject.get("deployment-name"));
                config.put(projectName+"-schedulerToCreate",(String)jsonObject.get("scheduler-to-create"));
                config.put(projectName+"-schedulerToDelete",(String)jsonObject.get("scheduler-to-delete"));

                config.forEach((k, v) -> {
                    System.out.println("Key: " + k + ", Value: " + v);
                });

            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

}
