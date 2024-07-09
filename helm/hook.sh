#!/bin/bash
cat <&0 > all.yaml
/agent/_work/1/kustomize build . --path /agent/_work/1/helm && rm all.yaml