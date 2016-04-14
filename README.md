prbz-overview
=============

Overview page for GitHub repository pull request and payload tracker issues.

#Configuration
------------
Before deploying the war file,  please update required properties in the configuration file "aphrodite.properties.json" and specify file path as a system property "aphrodite.config".

    </extensions>

    <system-properties>
        <property name="aphrodite.confige" value="/path/to/aphrodite.properties.json"/>
    </system-properties>

    <management>

Or, You can also use -Daphrodite.config=/path/to/aphrodite.properties.json in your server start-up command parameter.

Make sure you have also provide streams.json and its file path inside aphrodite.properties.json.

Note： Unfortunately，it depends on a SNAPSHOT version of [assistant](https://github.com/soul2zimate/assistant) by the moment. Therefore, it needs a pre-build assistant into your local maven repository.

#Deployment
------------

```
mvn clean package
```

Copy generated war file to $JBOSS_HOME/standalone/deployments/ and visit http://localhost:8080/prbz-overview-${version}/pullrequestoverview. Normally, it takes a few minutes to collect and display all information from GitHub and issue tracker systems.
