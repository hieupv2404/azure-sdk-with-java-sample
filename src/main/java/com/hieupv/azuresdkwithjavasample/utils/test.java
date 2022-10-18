package com.hieupv.azuresdkwithjavasample.utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class test {
    public static void main(String[] args) {
        //Creating a File object for directory
//        String WORKING_PATH = "D:/Document/dmoa/cpl";
//        Map<String, String> arm = new HashMap<>(0);
//        File directoryPath = new File(WORKING_PATH);
//        //List of all files and directories
//        String contents[] = directoryPath.list();
//        System.out.println("List of files and directories in the specified directory:");
//        for(int i=0; i<contents.length; i++) {
//            System.out.println(contents[i]);
//            if(new File(WORKING_PATH+contents[i]).isDirectory()){
//
//            }
//        }
//        final String ROOT_PATH_DEPLOY = System.getenv("root-path-deploy");
//        File[] directories = new File(ROOT_PATH_DEPLOY).listFiles(File::isDirectory);
//
//        for (int i = 0; i < directories.length; i++) {
//            System.out.println(directories[i].getPath());
//            System.out.println(directories[i].getName());
//        }
        RetrieveConfig.retrieveConfig();
    }
}
