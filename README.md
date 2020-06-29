[![Build Status](https://travis-ci.org/jboss-set/prbz-overview.svg?branch=master)](https://travis-ci.org/jboss-set/prbz-overview)
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

### Dev profile

For development purposes you can use a dev profile that will retrieve a small portion of the data and not perform any updates. To enable it add this system property:

    <property name="prbz-dev" value="true"/>

#Deployment
------------

```
mvn clean package -Dnorpm
```

Copy generated war file to $JBOSS_HOME/standalone/deployments/ and visit http://localhost:8080/prbz-overview-${version}/prbz-overview.
For instance on thunder: [OverviewPage](https://thunder.sin2.redhat.com/prbz-overview)

In order to avoid potential Github API rate limitation, the scheduled update task updates one payload and one stream Github repositories per hour. Therefore, it may take a bit longer to see all streams / payloads information for the first deployment.

#REST endpoint
---------------
/rest/api/ exposes a few management operations:

- `GET /rest/api/payload/{stream}/{release}` - produces JSON representation of issues in payload (using cached data)
```
[{
    "summary": "(7.1.z) Upgrade Infinispan to 8.2.9.Final",
    "url": "https://issues.redhat.com/browse/JBEAP-14221",
    "type": "component upgrade",
    "acks": {
      "QE": "+",
      "DEV": "+",
      "PM": "+"
    },
    "status": "VERIFIED",
    "priority": "BLOCKER"
  },
  {
    "summary": "(7.1.z) RPM - Setting JAVA_HOME not effective in RHEL-6 init scripts",
    "url": "https://issues.redhat.com/browse/JBEAP-14193",
    "type": "bug",
    "acks": {
      "QE": "+",
      "DEV": "+",
      "PM": "?"
    },
    "status": "VERIFIED",
    "priority": "MAJOR"
  }]
```
 - `GET /rest/api/status` - returns current status of PRBZ cache
```
{"refreshStatus":"Complete","lastRefresh":"2020-06-27T16:16:54.507"}
```
 - `POST /rest/api/refresh` - schedules a full refresh of all PRBZ caches. The operation is executed asynchronously and can be monitored through `/rest/api/status`.
```
{"refreshStatus":"Scheduled","lastRefresh":"2020-06-27T16:16:54.507"}
```
 - `POST /rest/api/refresh/{stream}/{release}` - schedules a refresh of cached informations for selected release only. The operation is executed asynchronously and can be monitored through `/rest/api/status`.
```
{"refreshStatus":"Scheduled","lastRefresh":"2020-06-27T16:16:54.507"}
```
