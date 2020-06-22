#!/bin/bash
oc delete template mypostgresql -n openshift
oc apply -f resources/postgresql-ephemeral.yaml

oc new-app -n ${YAKS_NAMESPACE} --name="postgres" --template=mypostgresql \
-e POSTGRESQL_USER=camel-k-example \
-e POSTGRESQL_PASSWORD=transformations \
-e POSTGRESQL_DATABASE=example

# wait for the postgres pod to be created 
export PGPOD=""

while [[ -z $PGPOD ]]
do
  echo "Waiting for PostgreSQL pod to become alive"
  sleep 5
  export PGPOD=$(oc get pods -n ${YAKS_NAMESPACE} -o custom-columns=POD:.metadata.name --no-headers | \
  grep postgresql | grep -v deploy)
done

# ensure postgresql pod is deployed and Ready
oc wait pod $PGPOD  -n ${YAKS_NAMESPACE}  --for condition=Ready

# populate database
oc rsync -n ${YAKS_NAMESPACE} sql $PGPOD:/tmp/
oc exec -n ${YAKS_NAMESPACE} $PGPOD -- chmod +x /tmp/sql/populate.sh
oc exec -n ${YAKS_NAMESPACE} $PGPOD -- ls -l /tmp/sql/
oc exec -n ${YAKS_NAMESPACE} $PGPOD -- /tmp/sql/populate.sh