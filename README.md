# Anypoint Template: SFDC to SFDC Custom Objects Migration

+ [License Agreement](#licenseagreement)
+ [Use Case](#usecase)
+ [Run it!](#runit)
    * [Create the Custom Object schemas in both organizations](#createcustomobjects)
    * [Running on CloudHub](#runoncloudhub)
    * [Running on premise](#runonopremise)
        * [Properties to be configured](#propertiestobeconfigured)
+ [Customize It!](#customizeit)
    * [config.xml](#configxml)
    * [endpoints.xml](#endpointsxml)
    * [businessLogic.xml](#businesslogicxml)
    * [errorHandling.xml](#errorhandlingxml)


# License Agreement <a name="licenseagreement"/>
Note that using this template is subject to the conditions of this [License Agreement](AnypointTemplateLicense.pdf).
Please review the terms of the license before downloading and using this template. In short, you are allowed to use the template for free with Mule ESB Enterprise Edition, CloudHub, or as a trial in Anypoint Studio.

# Use Case <a name="usecase"/>
As a Salesforce admin I want to migrate custom objects between two Salesfoce orgs.

This Template should serve as a foundation for the process of migrating contacts from one Salesfoce instance to another, being able to specify filtering criterias and desired behaviour when a custom object already exists in the destination org. 

As implemented, this Template leverage the [Batch Module](http://www.mulesoft.org/documentation/display/current/Batch+Processing).
The batch job is divided in Input, Process and On Complete stages.
During the Input stage the Template will go to the SalesForce Org A and query all the existing custom objects that match the filter criteria.
During the Process stage, each SFDC Custom Object will be filtered depending on, if it has an existing matching custom object in the SFDC Org B and if the last updated date of the later is greater than the one of SFDC Org A.
The last step of the Process stage will group the custom objects and create them in SFDC Org B.
Finally during the On Complete stage the Temaplate will both otput statistics data into the console and send a notification email with the results of the batch excecution. 

# Run it! <a name="runit"/>

Simple steps to get SFDC to SFDC Custom Objects Migration running.

## Create the Custom Object schemas in both organizations <a name="createcustomobjects" />

In order to run the Template as is, you'll need to create the custom objects provided in your Salesforce accounts. In order to do so, [please follow the steps documented in SalesForce documentation](http://www.salesforce.com/us/developer/docs/apexcode/Content/apex_qs_customobject.htm).

The custom objects and custom fields created for this application are the following:
1. SalesForce org A
MusicAlbum
	interpreter
	year
2. SalesForce org B
MusicAlbum
	interpreter
	genre

**Note:** Please, take into account that this sample application uses SalesForce Object Query Language which, when querying for custom objects and fields, requires you to append `__c` to your query. So for example, to query the music albums' interptreters, the query would be this way: `SELECT interpreter__c FROM MusicAlbum__c`.

## Running on CloudHub <a name="runoncloudhub"/>

While [creating your application on CloudHub](http://www.mulesoft.org/documentation/display/current/Hello+World+on+CloudHub) (Or you can do it later as a next step), you need to go to Deployment > Advanced to set all environment variables detailed in **Properties to be configured** as well as the **mule.env**. 

Once your app is all set and started, supposing you choose as domain name `sfdccustomobjectmigration` to trigger the use case you just need to hit `http://sfdccustomobjectmigration.cloudhub.io/migratecustomobjects` and report will be sent to the emails configured.

## Running on premise <a name="runonopremise"/>
Complete all properties in one of the property files, for example in [mule.prod.properties] (../blob/master/src/main/resources/mule.prod.properties) and run your app with the corresponding environment variable to use it. To follow the example, this will be `mule.env=prod`.

After this, to trigger the use case you just need to hit the local http endpoint with the port you configured in your file. If this is, for instance, `9090` then you should hit: `http://localhost:9090/migratecustomobjects` and this will create a CSV report and send it to the mails set.

## Properties to be configured (With examples)<a name="propertiestobeconfigured"/>

In order to use this Mule Template you need to configure properties (Credentials, configurations, etc.) either in properties file or in CloudHub as Environment Variables. Detail list with examples:

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

#### eMail Details
+ mail.from `batch.contact.migration%40mulesoft.com`
+ mail.to `your.username@youremaildomain.com`
+ mail.subject `Batch Job Finished Report`

# Customize It!<a name="customizeit"/>

This brief guide intends to give a high level idea of how this Template is built and how you can change it according to your needs.
As mule applications are based on XML files, this page will be organised by describing all the XML that conform the Template.
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
This Template has only an [HTTP Inbound Endpoint](http://www.mulesoft.org/documentation/display/current/HTTP+Endpoint+Reference) as the way to trigger the use case.

###  Inbound Flow
**HTTP Inbound Endpoint** - Start Report Generation
+ `${http.port}` is set as a property to be defined either on a property file or in CloudHub environment variables.
+ The path configured by default is `migratecustomobjects` and you are free to change for the one you prefer.
+ The host name for all endpoints in your CloudHub configuration should be defined as `localhost`. CloudHub will then route requests from your application domain URL to the endpoint.
+ The endpoint is configured as a *request-response* since as a result of calling it the response will be the total of Contacts migrated and filtered by the criteria specified.


## businessLogic.xml<a name="businesslogicxml"/>
Functional aspect of the Template is implemented on this XML, directed by one flow responsible of excecuting the logic.
For the purpouse of this particular Template the *mainFlow* just excecutes the Batch Job which handles all the logic of it.
This flow has Exception Strategy that basically consists on invoking the *defaultChoiseExceptionStrategy* defined in *errorHandling.xml* file.


## errorHandling.xml<a name="errorhandlingxml"/>
Contains a [Catch Exception Strategy](http://www.mulesoft.org/documentation/display/current/Catch+Exception+Strategy) that is only Logging the exception thrown (If so). As you imagine, this is the right place to handle how your integration will react depending on the different exceptions. 


