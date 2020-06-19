#!/bin/bash

oc new-app -n ${YAKS_NAMESPACE} --name="postgres" --template=postgresql-ephemeral \
-e POSTGRESQL_USER=camel-k-example \
-e POSTGRESQL_PASSWORD=transformations \
-e POSTGRESQL_DATABASE=example

# wait for the postgres pod to be created 
export PGPOD=""

while [[ -z $PGPOD ]]
do
  sleep 2
  export PGPOD=$(oc get pods -n ${YAKS_NAMESPACE} -o custom-columns=POD:.metadata.name --no-headers | \
  grep postgresql | grep -v deploy)
done

# ensure postgresql pod is deployed and Ready
oc wait pod $PGPOD  -n ${YAKS_NAMESPACE}  --for condition=Ready

oc rsync $PGPOD:/tmp/sql/ sql

oc rsh  -n ${YAKS_NAMESPACE} $PGPOD /tmp/sql/populate.sh