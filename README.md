prbz-overview
=============

Pull Request and Bugzilla Overview

#Configuration
------------
Before deploying the war file,  please update required properties in the configuration file and provide pull.helper.property.file path as system properties used by pull shared.

    </extensions>

    <system-properties>
        <property name="pull.helper.property.file" value="/path/to/processor-eap-6.x.properties"/>
    </system-properties>

    <management>

#Deployment
------------

```
mvn clean install
```

Then, copy generated war file to jboss-eap-6.4/standalone/deployments/ and visit http://localhost:8080/prbz-overview/. Normally, it takes a few minutes to collect and display all information from Github and issue tracker systems.
