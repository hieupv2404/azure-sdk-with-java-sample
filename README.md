**How to config and run Application**

**Be Prepared:**

- Java JDK 11
- Azure CLI
- Maven 3.x

1. **Login Azure CLI**

Using Azure shell on portal.azure.com.

Setup authen:

- Get ID: az account show
- Get Authen:

**az ad sp create-for-rbac --name [YourAppName] --role Contributor --scopes /subscriptions/[SubscriptionID]**

Output:

{

"appId": "28ae4550-xxxxxxxxxxxxxxxxxxxx",

"displayName": "YourAppName",

"password": "xxxxxxxxxxxxxxxxxxxxxxxx",

"tenant": "xxxxxxxxxxxxxxxxxxxxxxxx"

}

- AZURE\_SUBSCRIPTION\_ID: is Subscription ID
- AZURE\_CLIENT\_ID: is appID
- AZURE\_CLIENT\_SECRET: is password
- AZURE\_TENANT\_ID: is tenant

Using Azure CLI on your OS’s terminal, this command can help you login azure CLI:

**az login --service-principal -u xxxxxxxxxxxxxxxxxxxxxx -p xxxxxxxxxxxxxxxxxxxxxxxx --tenant xxxxxxxxxxxxxxxxxxxxxxxxxxx**

1. **Move all projects to a folder**

![](img-readme/Aspose.Words.2ec36de8-663a-4372-b8b4-8ce897bc2af8.001.png)

Example:

In root folder, D:\Document\dmoa\single

We move all projects (cmc, dkr, … ) to the root folder.

![](img-readme/Aspose.Words.2ec36de8-663a-4372-b8b4-8ce897bc2af8.002.png)

In every project, we have to export template on Azure to here, then unzip them.

1. **Config enviroment variable for OS**

We have to create 3 variables in your OS’s Enviroment Variables:

+ ROOT_PATH_DEPLOY: It’s root folder

Example: D:\Document\dmoa\single

![](img-readme/Aspose.Words.2ec36de8-663a-4372-b8b4-8ce897bc2af8.003.png)



+ SUBSCRIPTION_ID: can get from authen information in step 1

![](img-readme/Aspose.Words.2ec36de8-663a-4372-b8b4-8ce897bc2af8.004.png)



+ TENAN_ID: can get from authen information in step 1

![](img-readme/Aspose.Words.2ec36de8-663a-4372-b8b4-8ce897bc2af8.005.png)

1. **Config file: priority.json**

In your root path, create a new file: **priority.json**

![](img-readme/Aspose.Words.2ec36de8-663a-4372-b8b4-8ce897bc2af8.006.png)

Example:

![](img-readme/Aspose.Words.2ec36de8-663a-4372-b8b4-8ce897bc2af8.007.png)

1. **Config file: config.json for each project**

Create a new file: config.json for each your project.

We have to config some information for the automation, such as: resource group name, region name, scheduler to create or delete resources, …

![](img-readme/Aspose.Words.2ec36de8-663a-4372-b8b4-8ce897bc2af8.008.png)

![](img-readme/Aspose.Words.2ec36de8-663a-4372-b8b4-8ce897bc2af8.009.png)

1. **Run your application**
    - Run in IntelliJ
        - Open folder in IntelliJ.
        - Wait for import dependencies
        - Finally, Click Green Arrow “RUN” in the main application class

![](img-readme/Aspose.Words.2ec36de8-663a-4372-b8b4-8ce897bc2af8.010.png)

![](img-readme/Aspose.Words.2ec36de8-663a-4372-b8b4-8ce897bc2af8.011.png)

- Run with java command
    - Verify java: **java –version**

![](img-readme/Aspose.Words.2ec36de8-663a-4372-b8b4-8ce897bc2af8.012.png)



- Verify maven: **mvn –version**

![](img-readme/Aspose.Words.2ec36de8-663a-4372-b8b4-8ce897bc2af8.013.png)

- Verify you are in your project: **dir**

![](img-readme/Aspose.Words.2ec36de8-663a-4372-b8b4-8ce897bc2af8.014.png)



- Re-create folder target: **mvn -U clean package**

![](img-readme/Aspose.Words.2ec36de8-663a-4372-b8b4-8ce897bc2af8.015.png)



- Go to folder ./target: **cd target**

![](img-readme/Aspose.Words.2ec36de8-663a-4372-b8b4-8ce897bc2af8.016.png)



- Run the application: **java -jar azure-sdk-with-java-sample-0.0.1-SNAPSHOT.jar**

![](img-readme/Aspose.Words.2ec36de8-663a-4372-b8b4-8ce897bc2af8.017.png)
