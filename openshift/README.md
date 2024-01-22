# prbz-overview on OpenShift

## Login into OpenShift from CLI
* Log in to your Openshift instance.
* Click the upper right corner `Copy Login Command` in the browser to generate a new token, and execute the copied command in a new terminal.

      oc login --token=your_generated_token --server=--server=https://api.gpc.xxx.com:6443

## Switch to your Openshift project.

    oc project your_project_name

## Registry Service Account
You need a valid OpenShift user pull secret. Copy or download the pull secret from https://access.redhat.com/terms-based-registry/

Create a secret based on the generated secret file.

    oc create -f your-pull-secret-secret.yaml
    
## Import the latest JBoss EAP 7.4 for OpenShift Image streams

    oc import-image jboss-eap-7/eap74-openjdk11-openshift-rhel8 --from=registry.redhat.io/jboss-eap-7/eap74-openjdk11-openshift-rhel8 --confirm
    oc import-image jboss-eap-7/eap74-openjdk11-runtime-openshift-rhel8 --from=registry.redhat.io/jboss-eap-7/eap74-openjdk11-runtime-openshift-rhel8 --confirm

## PersistentVolumeClaim

In order to make use of it's HTTP response cache, you need to create a `PersistentVolumeClaim` with `prbz-overview-pvc.yaml` as below:

    oc create -f prbz-overview-pvc.yaml 

The `prbz-overview-pvc.yaml` is defined as below:

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
          
You will reuse the PVC name `prbz-overview-pvc` as the volume `persistentVolumeClaim` claimName in `DeploymentConfig` object defined in the template.

## Secrets
Like the standalone server, you need to upload some issue tracker systems and version control systems metadata for Aphrodite initialization as below:

    oc create secret generic aphrodite-secret --from-file=aphrodite.properties.json.example
    
To enable https, you either need to create a keystore that contains the self-signed certificate or use a service generated signed certificate and put it in a secret in OpenShift.

### Generate a keystore contains the self-signed certificate

    keytool -genkey -keyalg RSA -alias <alias_name> -keystore keystore.jks  -validity 360 -keysize 2048

Note the `keystore` password that you are typing when prompted as we will need it later.

In order to use that keystore on OpenShift, we need to store it in a `Secret`. We will also store the keystore password in that secret in order to read from the keystore.

Create a secret from the previously created keystore and its secret using the following command:

    oc create secret generic eap7-app-secret --from-file=keystore.jks --from-literal=keystore-password=your_keystore_password

### Using service serving certificate secrets

 - Add a service certificate as described in this [document](https://docs.openshift.com/container-platform/4.11/security/certificates/service-serving-certificate.html#add-service-certificate_service-serving-certificate)
 - Log in the cluster and find the generated secret, copy and save generated certificate and key stored in `tls.crt` and `tls.key` files to local filesystem.
 - Create an empty keystore.

       keytool -genkey -keyalg RSA -alias eap-servicesigned -keystore tls-keystore.jks -validity 360 -keysize 2048 -dname "CN=Developer, OU=Department O=Company, L=City, ST=State, C=CA"
       keytool -delete -alias eap-servicesigned -storepass your_keystore_password -keystore tls-keystore.jks
       keytool -list -keystore tls-keystore.jks

- Convert the x.509 cert and key to a pkcs12 file

      openssl pkcs12 -export -in tls.crt -inkey tls.key  -out server.p12 -name myalias -CAfile ca.crt -caname root

- Convert the pkcs12 file to the Java keystore

      keytool -importkeystore -deststorepass your_dest_keystore_password -destkeypass your_key_password -destkeystore tls-keystore.jks -srckeystore server.p12 -srcstoretype PKCS12 -srcstorepass your_src_keystore_password -alias myalias
      keytool -list -keystore tls-keystore.jks

- Finally, create the secret

      oc create secret generic tls-app-secret --from-file=tls-keystore.jks --from-literal=keystore-password=your_keystore_password

## ConfigMaps
After you created the secret, you also need to define the below configuration as system properties.

    oc create configmap prbz-overview-config --from-literal=aphrodite.config=/etc/secret/aphrodite.properties.json.example --from-literal=released-disabled=true --from-literal=cacheDir=/home/jboss --from-literal=cacheName=github-cache  --from-literal=cacheSize=100

Otherwise, you can create it from the aphrodite-configmap.yaml

    oc create -f prbz-overview-config.yaml

You will reuse `/etc/secret` as the mount volume path later in `DeploymentConfig`.

## Launch the application on OpenShift
Last, you need to create several OpenShift resource for the application from template prbz-overview-eap74-https-s2i.yaml

* First, replace the `pullSecret` name with a new generated pullSecret name.
* Second, replace the `IMAGE_STREAM_NAMESPACE` default value OpenShift with current working namespace, usually it's the same name as project name
* Last, create the application from the template.

      oc new-app -f prbz-overview-eap74-https-s2i.yaml --env HTTPS_PASSWORD=your_keystore_password IMAGE_STREAM_NAMESPACE=your_openshift_namespace HOSTNAME_HTTPS=your_expected_hostname
