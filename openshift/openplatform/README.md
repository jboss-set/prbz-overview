# prbz-overview on Open Platform

## Login into Open Platform
* Before accessing the Open Platform, you must accept the Terms of Service and Acceptable Use Policies:
  Go to https://open.paas.redhat.com, and follow the link to Sign Up.
  Sign in through your Red Hat google account.
* Once you have accepted the policies, your account will automatically be provisioned:
  Log in to the Open Platform at https://open.paas.redhat.com/console/ with your LDAP/Kerberos login.
* Click the upper right conncer **Copy Login Command** in the broswer to generate a new token, and execute the copied command in a new terminal. E.g.

    oc login https://open.paas.redhat.com --token=your_generated_token.

## Create a new project in OpenShift.
    oc new-project PROJECT_NAME

## Registry Service Account
You will need a valid OpenShift user pull secret. Copy or download the pull secret from https://access.redhat.com/terms-based-registry/

Create a secret based on the genereated secret file.

    oc create -f your-pull-secret-secret.yaml
    
## Import the latest JBoss EAP for OpenShift Imagestreams

    oc import-image jboss-eap-7/eap73-openjdk11-openshift-rhel8 --from=registry.redhat.io/jboss-eap-7/eap73-openjdk11-openshift-rhel8 --confirm
    oc import-image jboss-eap-7/eap73-openjdk11-runtime-openshift-rhel8 --from=registry.redhat.io/jboss-eap-7/eap73-openjdk11-runtime-openshift-rhel8 --confirm

## PersistentVolumeClaim

In order to make use of it's HTTP response cache, you need to create a PersistentVolumeClaim with prbz-overview-pvc.yaml as below:

    oc create -f prbz-overview-pvc.yaml 

The prbz-overview-pvc.yaml is defined as follow:
   

    apiVersion: v1
    kind: PersistentVolumeClaim
    metadata:
      annotations:
        volume.beta.kubernetes.io/storage-provisioner: kubernetes.io/glusterfs
      finalizers:
        - kubernetes.io/pvc-protection
      name: prbz-overview-pvc
      labels:
        application: prbz-overview-pvc
    spec:
      accessModes:
        - ReadWriteOnce
      resources:
        requests:
          storage: 1Gi
          
You will reuse the PVC name **prbz-overview-pvc** as the volume persistentVolumeClaim claimName in DeploymentConfig object defined in the template.

## Secrets
Like the standalone server, you need to upload some issue tracker systems and version control systems metadata for Aphrodite initialization as below:

    oc create secret generic aphrodite-secret --from-file=aphrodite.properties.json.example
    
(**Optional**), If you need to enable https, one more secret creation is also required.

Example:

    $ keytool -genkey -keyalg RSA -alias eapdemo-selfsigned -keystore keystore.jks -validity 360 -keysize 2048
    $ oc secrets new eap7-app-secret keystore.jks
 
Example secrets may also be found here: https://github.com/jboss-openshift/application-templates/tree/master/secrets

    oc create -n your_namespace -f https://raw.githubusercontent.com/jboss-openshift/application-templates/master/secrets/eap7-app-secret.json

## ConfigMaps
After you created the secret, you also need to define below configuration as system properties.

    oc create configmap prbz-overview-config --from-literal=aphrodite.config=/etc/secret/aphrodite.properties.json.example --from-literal=released-disabled=true --from-literal=cacheDir=/home/jboss --from-literal=cacheName=github-cache  --from-literal=cacheSize=100

Otherwise, you can create it from the aphrodite-configmap.yaml

    oc create -f prbz-overview-config.yaml

You will reuse **/etc/secret** as the mount volume path later in DeploymentConfig.

## JBoss EAP 7.3 based application
Last, you need to create several OpenShift resource for the application from template prbz-overview-eap73-basic-s2i.yaml

* First, replace the **pullSecret** name with new generated pullSecret name.
* Second, replace the **IMAGE_STREAM_NAMESPACE** default value OpenShift with current working namespace, usually it's the same name as project name
* Last, from the web console Import YAML/JSON, import the template and create your application.
