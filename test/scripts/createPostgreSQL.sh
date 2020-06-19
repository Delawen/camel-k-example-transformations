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

# populate database with dummy data
oc rsh -n ${YAKS_NAMESPACE} $PGPOD  \
 psql -U camel-k-example example \
 -c "CREATE TABLE descriptions (id varchar(10), info varchar(30));INSERT INTO descriptions (id, info) VALUES ('SO2', 'Nitric oxide is a free radical');INSERT INTO descriptions (id, info) VALUES ('NO2', 'Toxic gas');"
