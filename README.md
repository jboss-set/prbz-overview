prbz-overview
=============

Overview page for GitHub repository pull request and payload tracker issues.

#Configuration
------------
Before deploying the war file,  please provide required properties in the configuration file "aphrodite.properties.json" and payload tracker metadata in "payload.properties", then specify files path as system property "aphrodite.config" and "payload.properties".

    </extensions>

    <system-properties>
        <property name="aphrodite.config" value="/path/to/aphrodite.properties.json"/>
        <property name="payload.properties" value="/path/to/payload.properties"/>
    </system-properties>

    <management>

Or, You can also use -Daphrodite.config=/path/to/aphrodite.properties.json and -Dpayload.properties=/path/to/payload.properties in your server start-up command as parameter.

Make sure you have also provide streams.json and its file path inside aphrodite.properties.json as:

	"streamConfigs": [
        {
            "file": "/path/to/streams.json.example",
            "type": "JSON"
        }
    ]

If you need to obtain online streams.json from [jboss-streams](https://github.com/jboss-set/jboss-streams), it needs to change to:

	"streamConfigs": [
        {
            "url": "https://raw.githubusercontent.com/jboss-set/jboss-streams/master/streams.json",
            "type": "JSON"
        }
    ]

Since one payload update can sometimes takes several minutes due to server side network latency. It's required to increase default timeout value in transactions subsystem to a bigger value depends on the network condition.

	<coordinator-environment default-timeout="300"/>

#Deployment
------------

```
mvn clean package
```

Copy generated war file to $JBOSS_HOME/standalone/deployments/ and visit http://localhost:8080/prbz-overview-${version}/prbz-overview.
For instance on thunder: [OverviewPage](https://thunder.sin2.redhat.com/prbz-overview)

In order to avoid potential Github API rate limitation, the scheduled update task updates one payload and one stream Github repositories per hour. Therefore, it may take a bit longer to see all streams / payloads information for the first deployment.
