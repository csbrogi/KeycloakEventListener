# Introduction
Keycloak Event-Listener.
Listen to user and group specific events.
# Getting Started
1.	Installation process
2.	Software dependencies
3.	Latest releases
4.	API references

# Build and Test
Build with _mvn package_

#Installation
Buid the targe and copy the file target/keycloak-event-listener-jar-with-dependencies.jar to keycloak's standalone/deployments directory 
 
#Environment
- <i>KAFKA_PORT</i>, <i>KAFKA_HOST</i> when set, events are send to this kafka
- <i>EVENT_SOURCE</i> if set to "Keycloak", events from keycloak are handled, otherwise from SCIM


If you want to learn more about creating good readme files then refer the following [guidelines](https://docs.microsoft.com/en-us/azure/devops/repos/git/create-a-readme?view=azure-devops). You can also seek inspiration from the below readme files:
- [ASP.NET Core](https://github.com/aspnet/Home)
- [Visual Studio Code](https://github.com/Microsoft/vscode)
- [Chakra Core](https://github.com/Microsoft/ChakraCore)
