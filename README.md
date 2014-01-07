# Mule Kick: SFDC to SFDC Custom Objects Sync

+ [Use Case](#usecase)
+ [Run it!](#runit)
    * [Running on CloudHub](#runoncloudhub)
    * [Running on premise](#runonopremise)
        * [Properties to be configured](#propertiestobeconfigured)
+ [Customize It!](#customizeit)
    * [config.xml](#configxml)
    * [endpoints.xml](#endpointsxml)
    * [businessLogic.xml](#businesslogicxml)
    * [errorHandling.xml](#errorhandlingxml)


# Use Case <a name="usecase"/>
As a Salesforce admin I want to syncronize custom objects between two Salesfoce orgs.

This Kick (template) should serve as a foundation for the process of migrating contacts from one Salesfoce instance to another, being able to specify filtering criterias and desired behaviour when a contact already exists in the destination org. 

As implemented, for each one of the contacts from one instance of Salesforce, determines if it meets the requirements to be synced and if so, checks if the contact already exists syncing only if the record from the target instance is older. 

# Run it! <a name="runit"/>

Simple steps to get SFDC to SFDC Custom Objects Sync running.

## Running on CloudHub <a name="runoncloudhub"/>

While [creating your application on CloudHub](http://www.mulesoft.org/documentation/display/current/Hello+World+on+CloudHub) (Or you can do it later as a next step), you need to go to Deployment > Advanced to set all environment variables detailed in **Properties to be configured** as well as the **mule.env**. 

Once your app is all set and started, supposing you choose as domain name `sfdccustomobjectsync` to trigger the use case you just need to hit `http://sfdccustomobjectsync.cloudhub.io/synccustomobjects` and report will be sent to the emails configured.

## Running on premise <a name="runonopremise"/>
Complete all properties in one of the property files, for example in [mule.prod.properties] (../blob/master/src/main/resources/mule.prod.properties) and run your app with the corresponding environment variable to use it. To follow the example, this will be `mule.env=prod`.

After this, to trigger the use case you just need to hit the local http endpoint with the port you configured in your file. If this is, for instance, `9090` then you should hit: `http://localhost:9090/synccustomobjects` and this will create a CSV report and send it to the mails set.

## Properties to be configured (With examples)<a name="propertiestobeconfigured"/>

In order to use this Mule Kick you need to configure properties (Credentials, configurations, etc.) either in properties file or in CloudHub as Environment Variables. Detail list with examples:

### Application configuration
+ http.port `9090` 

#### SalesForce Connector configuration for company A
+ sfdc.a.username `bob.dylan@orga`
+ sfdc.a.password `DylanPassword123`
+ sfdc.a.securityToken `avsfwCUl7apQs56Xq2AKi3X`
+ sfdc.a.url `https://login.salesforce.com/services/Soap/u/26.0`

#### SalesForce Connector configuration for company B
+ sfdc.b.username `joan.baez@orgb`
+ sfdc.b.password `JoanBaez456`
+ sfdc.b.securityToken `ces56arl7apQs56XTddf34X`
+ sfdc.b.url `https://login.salesforce.com/services/Soap/u/26.0`




# Customize It!<a name="customizeit"/>

This brief guide intends to give a high level idea of how this Kick is built and how you can change it according to your needs.
As mule applications are based on XML files, this page will be organised by describing all the XML that conform the Kick.
Of course more files will be found such as Test Classes and [Mule Application Files](http://www.mulesoft.org/documentation/display/current/Application+Format), but to keep it simple we will focus on the XMLs.

Here is a list of the main XML files you'll find in this application:

* [config.xml](#configxml)
* [endpoints.xml](#endpointsxml)
* [businessLogic.xml](#businesslogicxml)
* [errorHandling.xml](#errorhandlingxml)


## config.xml<a name="configxml"/>
Configuration for Connectors and [Properties Place Holders](http://www.mulesoft.org/documentation/display/current/Configuring+Properties) are set in this file. **Even you can change the configuration here, all parameters that can be modified here are in properties file, and this is the recommended place to do it so.** Of course if you want to do core changes to the logic you will probably need to modify this file.

In the visual editor they can be found on the *Global Element* tab.

## endpoints.xml<a name="endpointsxml"/>
This is the file where you will found the inbound and outbound sides of your integration app.
This Kick has only an [HTTP Inbound Endpoint](http://www.mulesoft.org/documentation/display/current/HTTP+Endpoint+Reference) as the way to trigger the use case.

###  Inbound Flow
**HTTP Inbound Endpoint** - Start Report Generation
+ `${http.port}` is set as a property to be defined either on a property file or in CloudHub environment variables.
+ The path configured by default is `synccontacts` and you are free to change for the one you prefer.
+ The host name for all endpoints in your CloudHub configuration should be defined as `localhost`. CloudHub will then route requests from your application domain URL to the endpoint.
+ The endpoint is configured as a *request-response* since as a result of calling it the response will be the total of Contacts synced and filtered by the criteria specified.


## businessLogic.xml<a name="businesslogicxml"/>
Functional aspect of the kick is implemented on this XML, directed by one flow responsible of conducting the generation of the report.
The *mainFlow* organises the job in two different steps and finally sets the payload that will be the response for the HTTP Call.
This flow has Exception Strategy that basically consists on invoking the *defaultChoiseExceptionStrategy* defined in *errorHandling.xml* file.


###  Gather Data Flow
Mainly consisting of one call (Query) to bring all Contacts from SFDC Org A and setting the variable that would count the total of synced.

###  Filter And Insert Data Flow
The main component of this flow is a *For Each* processor that will try to either update or create a contact in the target SFDC org in bulks defined by the property `page.size`. Running this process in batch mode is key for performance, mainly to reduce the number of calls to SFDC API making use of their Batch API Methods.

Before calling the *Bulk-Upsert* (Upsert since the action will be either an Update or a Insert) to SFDC Target company, Contacts will be filtered and the responsible for that will be the Sub Flow name *filterFlow*:
+ A bulk (List) of Contacts will be received and for each one a *SFDCCustomObjectFilter* [Java Transformer](http://www.mulesoft.org/documentation/display/current/Java+Transformer+Reference) will determine (With a help of a [Filter Expression](http://www.mulesoft.org/documentation/display/current/Using+Filters) as the next processor) if the Contact has to be synced on not. If meets the requirements, it will be added to the list *filteredContactList* that will turn to be the payload at the end of the execution of this flow. 
+ In this Kick a Custom Object to be synced will be filtered only if it exists already in target SFDC org and the data from Org A is older. In order to change this behaviour you just need to change logic on the *SFDCCustomObjectFilter* Java Transformer.

The *filterFlow* as explained will return a list that will be filtered if it is empty, and if not, Bulk Sync method will take place. To check the results/status of this batch process you should go into SFDC UI to **Setup >  Monitoring > Bulk Data Load Jobs**.


## errorHandling.xml<a name="errorhandlingxml"/>
Contains a [Catch Exception Strategy](http://www.mulesoft.org/documentation/display/current/Catch+Exception+Strategy) that is only Logging the exception thrown (If so). As you imagine, this is the right place to handle how your integration will react depending on the different exceptions. 


