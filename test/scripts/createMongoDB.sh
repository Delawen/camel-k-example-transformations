#!/bin/bash

function waitFor() {
  for i in {1..30}; do
    sleep 5
    ("$@") && return
    echo "$i Waiting for exit code of command \"$@\"."
  done
  exit 1
}

# install Operator
sed "s/YAKS_NAMESPACE/${YAKS_NAMESPACE}/" resources/mongoOperator.yaml | oc create -f -
#oc create -f resources/mongoOperator.yaml -n ${YAKS_NAMESPACE} 
oc create -f resources/mongoSubscription.yaml -n ${YAKS_NAMESPACE}

# ensure operator pod is deployed and Ready
waitFor oc wait pod -l name=percona-server-mongodb-operator --for condition=Ready --timeout=100s -n ${YAKS_NAMESPACE}

# Add user and password to Secrets
oc create -f resources/mongoSecrets.yaml -n ${YAKS_NAMESPACE}

#create database
oc create -f resources/mongoDB.yaml -n ${YAKS_NAMESPACE}
waitFor oc wait pod -l name=mongodb --for=condition=Ready --timeout=100s -n ${YAKS_NAMESPACE}
