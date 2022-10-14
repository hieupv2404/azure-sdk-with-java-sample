package com.hieupv.azuresdkwithjavasample;

import static com.hieupv.azuresdkwithjavasample.AzureSdkWithJavaSampleApplication.azureResourceManager;
import static com.hieupv.azuresdkwithjavasample.AzureSdkWithJavaSampleApplication.rgName;

public class DeleteResourceGroup {
    public static void main(String[] args){
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
