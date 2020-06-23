#!/bin/bash

# install Operator
oc create -f resources/postgresSubscription.yaml -n ${YAKS_NAMESPACE}
oc create -f resources/postgresOperator.yaml -n ${YAKS_NAMESPACE}

# ensure operator pod is deployed and Ready
oc wait pod -l name=postgres-operator --for condition=Ready --timeout=120s -n ${YAKS_NAMESPACE}

#create database
oc create -f resources/postgres.yaml -n ${YAKS_NAMESPACE} 

# wait for the postgres pod to be created
export PGPOD=""

while [[ -z $PGPOD ]]
do
  echo "Waiting for PostgreSQL pod to become alive"
  sleep 5
  export PGPOD=$(oc get pods -n ${YAKS_NAMESPACE} -o custom-columns=POD:.metadata.name --no-headers | \
  grep mypostg | grep -v deploy)
done

oc wait pod/$PGPOD --for condition=Ready --timeout=120s -n ${YAKS_NAMESPACE}

# populate database
oc rsync -n ${YAKS_NAMESPACE} sql $PGPOD:/tmp/
oc exec -n ${YAKS_NAMESPACE} $PGPOD -- chmod +x /tmp/sql/populate.sh
oc exec -n ${YAKS_NAMESPACE} $PGPOD -- ls -l /tmp/sql/
oc exec -n ${YAKS_NAMESPACE} $PGPOD -- /tmp/sql/populate.sh
