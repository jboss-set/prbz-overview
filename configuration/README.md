### Customizing standalone-openshift.xml

* Get standalone-openshift.xml from original image. For example, you can refer to standalone-openshift.xml with following commands:
```
    $ podman login -u username -p password registry.redhat.io

    $ podman run -it --rm registry.redhat.io/jboss-eap-7/eap73-openjdk11-openshift-rhel8 /bin/bash
```
* This will open a prompt. Then run:
```
    $ cat /opt/eap/standalone/configuration/standalone-openshift.xml
```
* Then, copy and customize the standalone-openshift.xml, you can use it on your image by deploying the standalone-openshift.xml under configuration directory, For example:

    <YOUR_TOP_SOURCE_DIRECTORY>/configuration/standalone-openshift.xml

* In this case, we only need to adjust `default-timeout` value for the transaction subsystem.
```
    <coordinator-environment statistics-enabled="${wildfly.transactions.statistics-enabled:${wildfly.statistics-enabled:false}}" default-timeout="3600"/>
```
