[Purpose](#h.3luhptgttygw)  
[Prerequisites](#h.6fqfp5l75beu)  
[Initial setup of the Mule project](#h.xgb56x1lmym4)  
    [Step 1: Salesforce Developer account creation](#h.mm4lty59p5jz)  
    [Step 2: Install the Salesforce connector from the update site](#h.hazhd6a6xbvi)  
    [Step 3: Create a new Mule project](#h.u2054v3mxr49)  
    [Step 4: Store the credentials](#h.sg4utm9fhsyr)  
    [Step 5: Create a Salesforce Global element](#h.ybl7nbt51z6x)  
[Building the demo](#h.a06pdv59bpy)  
    [Step 1: Building the "create" flow](#h.b5h03zvnorg8)  
    [Step 2: Building the "query" flow](#h.b5rqxh9ur2qz)  
    [Step 3: Building the "update" flow](#h.3ohy7796y3c8)  
    [Step 4: Building the "delete" flow](#h.ynvegarhzyf1)  
[Running the application](#h.jjcipepf5hgv)  
[Complete example](#h.t76x1vxakq1x)  
[Resources](#h.wewt0o3apul9) 

Purpose
=======

This video features MuleSoft´s Salesforce connector and displays how to meet configuration requirements for using the Connector in a Mule project and how to incrementally build a "create-retrieve-update-delete" use case through the connector operations.

Prerequisites
=============

In order to build and run this project you'll need:  
●     A [Developer Force platform](http://www.google.com/url?q=http%3A%2F%2Fdeveloper.force.com%2F&sa=D&sntz=1&usg=AFQjCNF4KH7T8O4e3fqSJtNjeyC0GZqOcA) account.  
●     To download and install [MuleStudio Community edition](http://www.mulesoft.org/download-mule-esb-community-edition).  
●     A browser to make a request to your Mule application.  

Initial setup of the Mule project
=================================

### Step 1: Salesforce Developer account creation

1.      Create an account on [http://developer.force.com](http://developer.force.com)  
2.      Login to [https://login.salesforce.com](https://login.salesforce.com)  
3.      Click **Your Name** \> **Setup** \> **My Personal Information** \> **Reset Security Token**, and click the **Reset My Security Token** button. The new security token is sent via email to your registered email address.  
> 
### Step 2: Install the Salesforce connector from the update site

To begin building this application, start Mule Studio and  
1.      Go to **Help \> Install New Software**. From the Work with dropdown select **MuleStudio Cloud Connectors Update Site.**  
2.      Expand the community Option and look for the **Mule Salesforce Connector Mule Studio Extension.**  
3.      Check its checkbox and click **Next**. Complete the installation process. After the connector is installed you'll be required to restart MuleStudio.

### Step 3: Create a new Mule project

Once MuleStudio has launched, create a new project:  
1.      Go to **File \> New \> Mule Project**  
2.      In the New Mule Project configuration menu, provide a name for this project: **salesforce-demo**  
3.      Click **Next** and provide a name for the flow: **salesforce-demo.**  
4.      Click **Finish**.  

### Step 4: Store the credentials

In src/main/app/mule-app.properties file that's on your project put the following key/value pairs and replace what's displayed **bold** with your credentials values.  

salesforce.username=&lt;username\>  
salesforce.password=&lt;password\>  
salesforce.securityToken=&lt;securityToken\>  

![](images/image001.png)

### Step 5: Create a Salesforce Global element

1.      Click on "Global Elements" tab.  
2.      Click on "Create" to bring up Global Type dialog box.  
3.      Filter by "Salesforce".  
4.      Select "Salesforce" from "Cloud Connectors" section.  
5.      Populate the fields with property placeholders.  

\${salesforce.username}  
\${salesforce.password}  
\${salesforce.securityToken}  

6.      Click Ok.

![](images/image002.png)

Building the demo
=================

This demo will be incrementally creating a "create-retrieve-update-delete" type of application. Groovy and Property Transformers are used to put the necessary information on the Mule Message for subsequent operations to consume.

### Step 1: Building the "create" flow

1.      Filter the Palette by "http" and drag and drop an **HTTP Inbound Endpoint** in the canvas.  
2.      Filter the Palette by "groovy" and drag a **Groovy Transformer**next to the Http Inbound Endpoint.  
3.      Filter the Palette by "salesforce" and drag a **Salesforce Cloud Connector**next to the Groovy Transformer.  
4.      Again, filter the Palette by "groovy" and drag a **Groovy Transformer**next to the Salesforce Connector.  
5.      Filter the Palette by "property" and drag a **Property Transformer**next to the last Groovy Component.  
6.      Filter the Palette by "logger" and place a **Logger Component**next to the Message Properties Transformer.  
7.      Filter the Palette by "flow reference" and place a **Flow Reference Component**after the Logger.  
8.      At last, filter the Palette by "flow" and drag a **Flow Scope**below the flow that's already in the canvas.  

Now let's setup the individual components:  
1.      Double click the **empty flow,**once its properties dialog is displayed set "query" as its Name and click Ok.  
2.      Double click the **other flow,**once its properties dialog is displayed set "create" as its Name and click Ok.  
3.      Double click the **Groovy Transformer** next to the Http Inbound Endpoint**.**This will set the payload that the next operation will be consuming. Configure it as follows:  
    a.      Set "Generate create input" as its Displayed Name,  
    b.      Make sure the Script Text radio button is select and then paste the following code in the box:

    import java.util.\*  
    HashMap<String,Object\> accountSObjectFields = new HashMap<String,Object\>()  
    // sObject is defined as a map  
    accountSObjectFields.put("Name", "The Brick Hut")  
    accountSObjectFields.put("BillingStreet", "403 McAdoo St")  
    accountSObjectFields.put("BillingCity", "Truth or Consequences")  
    accountSObjectFields.put("BillingState", "NM")  
    accountSObjectFields.put("BillingPostalCode", "87901")  
    accountSObjectFields.put("BillingCountry", "US")  
    // list of sobjects to be created  
    List<HashMap<String,Object\>\> objects = new ArrayList<HashMap<String,Object\>\>()  
    objects.add(accountSObjectFields)    
    // map that will be placed as payload  
    HashMap<String,Object\> payload = new HashMap<String,Object\>()  
    payload.put("type", "Account")
    payload.put("objects", objects)
    return payload

    c.      Click Ok.

4.      Double click on the **Salesforce Cloud Connector** and configure it as follows:  
    a.      Set "Create Account sObject" as Display Name.  
b.      Select Salesforce from the Config Reference dropdown.  
c.      Select "Create" as Operation value and click Ok.  
d.      Enter \#[payload.type] in the sObject Type field.  
e.      In the sObject Field Mappings section make sure that From Message is selected and enter \#[payload.objects] in its input box.

f.       Click Ok.  
5.      Open the **Logger Component** pattern properties by double clicking it.  
a.      Set "\#\#\# create operation payload \#[payload]" as Message.  
b.      Click Ok.  
6.      Double click the **Groovy Transformer** next to the Salesforce Cloud Connector. The id of the sObject we created is now in the payload. Configure it as follows:  
a.      Set "Get sObject Id" as its Displayed Name,  
b.      Make sure the Script Text radio button is select and then paste the following code in the box:

    import com.sforce.soap.partner.\*
    import java.util.\*
    // get the message payload
    List<SaveResult\> saveResults =  (List<SaveResult\>) message.getPayload()
                           
    Iterator<SaveResult\> iter = saveResults.iterator()
    SaveResult saveResult = iter.next()
    
    return saveResult.getId()

c.      Click Ok.  
7.      Double click on the **Property Transformer** to bring up properties dialog. Configure it as follows:  
a.      Set "Store sObject Id" as Display Name.  
b.      Select "Set Property" as operation.  
c.      Enter sObjectId as Name and \#[payload] as Value .  
d.      Click Ok.  
8.      Open the **Flow Reference Component** pattern properties by double clicking it.  
a.      Set "To query" as Display Name.  
b.      Select "query" from the Flow Name dropdown.  
c.      Click Ok.  

### Step 2: Building the "query" flow

1.      Filter the Palette by "salesforce" and drag a **Salesforce Cloud Connector**into the "query" flow created on Step 1.  
2.      Filter the Palette by "logger" and place a **Logger Component**next to the Salesforce Cloud Connector.  
3.      Filter the Palette by "flow reference" and place a **Flow Reference Component**after the Logger.  
4.      At last, filter the Palette by "flow" and drag a **Flow Scope**below the "query" Flow.

Now let's setup the individual components:

1.      Double click the **empty flow,**once its properties dialog is displayed set "update" as its Name and click Ok.  
2.      Bring up the pattern properties of the **Salesforce Cloud Connector** by double clicking it and configure it as follows:  
a.      Set "Query" as Display Name.  
b.      Select Salesforce from the Config Reference dropdown.  
c.      Select "Query" as Operation value and click Ok.  
d.      Enter "SELECT Id FROM Account WHERE Name = 'The Brick Hut'" in the Query field.  
e.      Click Ok.  
3.      Open the **Logger Component** pattern properties by double clicking it.  
a.      Set "\#\#\# query operation payload \#[payload]" as Message.  
b.      Click Ok.  
4.      Open the **Flow Reference Component**pattern properties by double clicking it.  
a.      Set "To update" as Display Name.  
b.      Select "update" from the Flow Name dropdown.  
c.      Click Ok.  

### Step 3: Building the "update" flow

1.      Filter the Palette by "groovy" and drag a **Groovy Transformer **into the "update" flow created on Step 2.  
2.      Filter the Palette by "salesforce" and drag a **Salesforce Cloud Connector**next to the Groovy Component.  
3.      Filter the Palette by "logger" and place a **Logger Component**next to the Salesforce Cloud Connector.  
4.      Filter the Palette by "flow reference" and place a **Flow Reference Component**after the Logger.  
5.      At last, filter the Palette by "flow" and drag a **Flow Scope**below the "update" Flow.  
6.      
Now let's setup the individual components:

1.      Double click the **empty flow,**once its properties dialog is displayed set "delete" as its Name and click Ok.  
2.      Double click the **Groovy Transformer** that was just added**.**This will set the payload that the next operation will be consuming. Configure it as follows:  
a.      Set "Generate update input" as its Displayed Name,  
b.      Make sure the Script Text radio button is select and then paste the following code in the box:  

    import java.util.\*
    // salesforce sobject to be passed to the operation
    HashMap<String,Object\> object = new HashMap<String,Object\>()
    object.put("Name", "The New Brick Hut")
    object.put("Id", message.getProperty('sObjectId').toString())
    // map that will be placed as payload
    HashMap<String,Object\> payload = new HashMap<String,Object\>()
    payload.put("type", "Account")
    payload.put("object", object)
    return payload

c.      Click Ok.  
3.      Bring up the pattern properties of the **Salesforce Cloud Connector** by double clicking it and configure it as follows:  
a.      Set "Update single Account sObject" as Display Name.  
b.      Select Salesforce from the Config Reference dropdown.  
c.      Select "Update single" as Operation value and click Ok.   
d.      Enter "\#[payload.type]" in the sObject Type field.  
e.      In the Salesforce Object section make sure that From Message is selected and enter \#[payload.object] in its input box.  
f.       Click Ok.  
4.      Open the **Logger Component** pattern properties by double clicking it.  
a.      Set "\#\#\# update-single operation payload \#[payload]" as Message.  
b.      Click Ok.  
5.      Open the **Flow Reference Component**pattern properties by double clicking it.  
a.      Set "To delete" as Display Name.  
b.      Select "delete" from the Flow Name dropdown.  
c.      Click Ok.  

### Step 4: Building the "delete" flow

1.      Filter the Palette by "salesforce" and drag a **Salesforce Cloud Connector** into the "query" flow created on Step 3.  
2.      Filter the Palette by "logger" and place a **Logger Component** next to the Salesforce Cloud Connector.  
3.      At last, filter the Palette by "set payload" and drag a **Set Payload Transformer** after the Logger Component.  
Now let's setup the individual components:  
1.      Bring up the pattern properties of the **Salesforce Cloud Connector** by double clicking it and configure it as follows:  
a.      Set "Delete" as Display Name.  
b.      Select Salesforce from the Config Reference dropdown.  
c.      Select "Delete" as Operation value and click Ok.  
d.      In the Ids to Delete section select Create Object manually.  
e.      Click on the icon with three dots(...), then click the plus icon on the following window.
f.      Select the new entry (labeled Entry:String) and click the Edit icon, and on the new window enter "\#[groovy:message.getProperty('sObjectId')]" in the Value field.
g.       Click Ok.  
2.      Open the **Logger Component** pattern properties by double clicking it.  
a.      Set "\#\#\# delete operation payload \#[payload]" as Message.  
b.      Click Ok.  
3.      Open the **Set Payload Transformer**pattern properties by double clicking it.  
a.      Set "Demo is completed. Check the operation payloads in the console." as Value.  
b.      Click Ok.  

Running the application
=======================

1.     Right click on salesforce-demo.mflow and select **Run As Mule Application**.  
2.     Check the console to see when the application starts.

    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

     + Started app 'salesforce-demo'                          +

    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

1.     Hit the endpoint at<http://localhost:8081/crud> and check the operation payload.

    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    + Started app 'salesforce-demo'                          +
    
    ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    INFO  2013-02-04 16:40:30,894 [[salesforce-demo].connector.http.mule.default.receiver.02] org.mule.api.processor.LoggerMessageProcessor: \#\#\# create operation payload [[SaveResult  errors='{[0]}
     id='001d000000XnZx5AAF'
     success='true'
    ]
    ]
    
    INFO  2013-02-04 16:40:31,461 [[salesforce-demo].connector.http.mule.default.receiver.02] org.mule.api.processor.LoggerMessageProcessor: \#\#\# query operation payload [{Id=001d000000XnZx5AAF, type=Account}]
    
    INFO  2013-02-04 16:40:32,131 [[salesforce-demo].connector.http.mule.default.receiver.02] org.mule.api.processor.LoggerMessageProcessor: \#\#\# update-single operation payload [SaveResult  errors='{[0]}'
     id='001d000000XnZx5AAF'
     success='true'
    ]
    
    INFO  2013-02-04 16:40:32,831 [[salesforce-demo].connector.http.mule.default.receiver.02] org.mule.api.processor.LoggerMessageProcessor: \#\#\# delete operation payload [[DeleteResult  errors='{[0]}'
     id='001d000000XnZx5AAF'
     success='true'
    ]
    ]

As you can see the payload of each operations is printed to the console.

Complete example
================

If you click the **Configuration XML** tab this is how the code should look like

    <?xml version="1.0" encoding="UTF-8"?\>
    
    <mule xmlns:mulexml="http://www.mulesoft.org/schema/mule/xml" xmlns:json="http://www.mulesoft.org/schema/mule/json" xmlns:scripting="http://www.mulesoft.org/schema/mule/scripting" xmlns:http="http://www.mulesoft.org/schema/mule/http" xmlns:sfdc="http://www.mulesoft.org/schema/mule/sfdc" xmlns="http://www.mulesoft.org/schema/mule/core" xmlns:doc="http://www.mulesoft.org/schema/mule/documentation" xmlns:spring="http://www.springframework.org/schema/beans" version="CE-3.3.1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="
    http://www.mulesoft.org/schema/mule/http http://www.mulesoft.org/schema/mule/http/current/mule-http.xsd  
    http://www.mulesoft.org/schema/mule/sfdc http://www.mulesoft.org/schema/mule/sfdc/5.0/mule-sfdc.xsd  
    http://www.mulesoft.org/schema/mule/scripting http://www.mulesoft.org/schema/mule/scripting/current/mule-scripting.xsd
    http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-current.xsd
    http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd
    http://www.mulesoft.org/schema/mule/xml http://www.mulesoft.org/schema/mule/xml/current/mule-xml.xsd
    http://www.mulesoft.org/schema/mule/json http://www.mulesoft.org/schema/mule/json/current/mule-json.xsd "\>
    
        <sfdc:config name="Salesforce" username="\${salesforce.username}" password="\${salesforce.password}" doc:name="Salesforce" securityToken="\${salesforce.securityToken}"\>
            <sfdc:connection-pooling-profile initialisationPolicy="INITIALISE\_ONE" exhaustedAction="WHEN\_EXHAUSTED\_GROW"/\>
        </sfdc:config\>
    
        <flow name="create" doc:name="create"\>
            <http:inbound-endpoint exchange-pattern="request-response" host="localhost" port="8081" doc:name="HTTP" path="crud"/\>
            <scripting:transformer doc:name="Generate create input"\>
                <scripting:script engine="Groovy"\>
                    <scripting:text\><![CDATA[import java.util.\*
    HashMap<String,Object\> accountSObjectFields = new HashMap<String,Object\>()
    // sObject is defined as a map
    accountSObjectFields.put("Name", "The Brick Hut")
    accountSObjectFields.put("BillingStreet", "403 McAdoo St")
    accountSObjectFields.put("BillingCity", "Truth or Consequences")
    accountSObjectFields.put("BillingState", "NM")
    accountSObjectFields.put("BillingPostalCode", "87901")
    accountSObjectFields.put("BillingCountry", "US")
    // list of sobjects to be created
    List<HashMap<String,Object\>\> objects = new ArrayList<HashMap<String,Object\>\>()
    objects.add(accountSObjectFields)
    // map that will be placed as payload
    HashMap<String,Object\> payload = new HashMap<String,Object\>()
    payload.put("type", "Account")
    payload.put("objects", objects)
    return payload]]\></scripting:text\>
                </scripting:script\>
            </scripting:transformer\>
            <sfdc:create config-ref="Salesforce"  doc:name="Create Account sObject" type="\#[payload.type]"\>
                <sfdc:objects ref="\#[payload.objects]"/\>
            </sfdc:create\>
            <logger message="\#\#\# create operation payload \#[payload]" level="INFO" doc:name="Logger"/\>
            <scripting:transformer doc:name="Get sObject Id"\>
                <scripting:script engine="Groovy"\>
                    <scripting:text\><![CDATA[import com.sforce.soap.partner.\*
    import java.util.\*
    // get the message payload
    List<SaveResult\> saveResults =  (List<SaveResult\>) message.getPayload()
                          
    Iterator<SaveResult\> iter = saveResults.iterator()
    SaveResult saveResult = iter.next()
    return saveResult.getId()]]\></scripting:text\>
                </scripting:script\>
            </scripting:transformer\>
            <set-property propertyName="sObjectId" value="\#[payload]" doc:name="Store sObject Id"/\>
            <flow-ref name="query" doc:name="To query"/\>
        </flow\>
        <flow name="query" doc:name="query"\>
            <sfdc:query config-ref="Salesforce" query="SELECT Id FROM Account WHERE Name = 'The Brick Hut'" doc:name="Query"/\>
            <logger message="\#\#\# query operation payload \#[payload]" level="INFO" doc:name="Logger"/\>
            <flow-ref name="update" doc:name="To update"/\>
        </flow\>
        <flow name="update" doc:name="update"\>
            <scripting:transformer doc:name="Generate update input"\>
                <scripting:script engine="Groovy"\>
                    <scripting:text\><![CDATA[import java.util.\*
    // salesforce sobject to be passed to the operation
    HashMap<String,Object\> object = new HashMap<String,Object\>()
    object.put("Name", "The New Brick Hut")
    object.put("Id", message.getProperty('sObjectId').toString())
    // map that will be placed as payload
    HashMap<String,Object\> payload = new HashMap<String,Object\>()
    payload.put("type", "Account")
    payload.put("object", object)
    return payload]]\></scripting:text\>
                </scripting:script\>
            </scripting:transformer\>
            <sfdc:update-single config-ref="Salesforce" type="\#[payload.type]" doc:name="Update single Account sObject"\>
                <sfdc:object ref="\#[payload.object]"/\>
            </sfdc:update-single\>
            <logger message="\#\#\# update-single operation payload \#[payload]" level="INFO" doc:name="Logger"/\>
            <flow-ref name="delete" doc:name="To delete"/\>
        </flow\>
        <flow name="delete" doc:name="delete"\>
            <sfdc:delete config-ref="Salesforce" doc:name="Delete"\>
                <sfdc:ids \>
                    <sfdc:id value-ref="\#[groovy:message.getProperty('sObjectId')]"/\>
                </sfdc:ids\>
            </sfdc:delete\>
            <logger message="\#\#\# delete operation payload \#[payload]" level="INFO" doc:name="Logger"/\>
            <set-payload value="Demo is completed. Check the operation payloads in the console." doc:name="Set Payload"/\>
        </flow\>
    </mule\>

Project **Message Flow**

![](images/image003.jpg)

Resources
=========

For additional information related to the Salesforce Cloud Connector you can check the[API docs](http://mulesoft.github.com/salesforce-connector/mule/modules.html) that are available in [MuleSoft's Salesforce Connector Github repository.](https://github.com/mulesoft/salesforce-connector)

Here's a list of features used in this demo with a link to their documentation

●     [Mule Expression Language](http://www.mulesoft.org/documentation/display/MULE3USER/Mule+Expression+Language)  
●     [Configuring Endpoints](http://www.mulesoft.org/documentation/display/MULE3USER/Configuring+Endpoints)  
●     [Studio transformers](http://www.mulesoft.org/documentation/display/MULE3STUDIO/Studio+Transformers)  
●     [Flow references](http://www.mulesoft.org/documentation/display/MULE3STUDIO/Flow+Ref+Component+Reference)

 

Webinars and additional documentation related to Mule ESB can be found under [Resources](http://www.mulesoft.com/resources) menu option.
