prbz-overview
=============

Overview page for GitHub repository pull request and payload tracker issues.

#Configuration
------------
Before deploying the war file,  please provide required properties in the configuration file "aphrodite.properties.json" and payload tracker metadata in "payload.properties", then specify files path as system property "aphrodite.config" and "payload.properties".

    </extensions>

    <system-properties>
        <property name="aphrodite.confige" value="/path/to/aphrodite.properties.json"/>
        <property name="payload.properties" value="/path/to/payload.properties"/>
    </system-properties>

    <management>

Or, You can also use -Daphrodite.config=/path/to/aphrodite.properties.json and -Dpayload.properties=/path/to/payload.properties in your server start-up command as parameter.

Make sure you have also provide streams.json and its file path inside aphrodite.properties.json.

Note： Unfortunately，it depends on a SNAPSHOT version of [assistant](https://github.com/soul2zimate/assistant) by the moment. Therefore, it needs a pre-build assistant into your local maven repository.

#Deployment
------------

```
mvn clean package
```

Copy generated war file to $JBOSS_HOME/standalone/deployments/ and visit http://localhost:8080/prbz-overview-${version}/index. Normally, it takes a few minutes to collect and display all information from GitHub and issue tracker systems.
