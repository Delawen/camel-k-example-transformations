#!/bin/bash

oc new-app -n ${YAKS_NAMESPACE} --name="mongodb" --template=mongodb-persistent  \
-e MONGODB_USER=camel-k-example -e MONGODB_PASSWORD=transformations  \
-e MONGODB_DATABASE=example -e MONGODB_ADMIN_PASSWORD=compleexpasswrd