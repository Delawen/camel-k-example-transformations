#!/bin/bash
oc delete template mymongodb -n openshift
oc apply -f resources/mongodb-ephemeral.yaml

oc new-app -n ${YAKS_NAMESPACE} --name="mongodb" --template=mymongodb  \
-e MONGODB_USER=camel-k-example -e MONGODB_PASSWORD=transformations  \
-e MONGODB_DATABASE=example -e MONGODB_ADMIN_PASSWORD=compleexpasswrd