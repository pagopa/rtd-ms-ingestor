#!/bin/bash
cat <&0 > all.yaml
/agent/_work/1/kustomize build /agent/_work/1/helm/ && rm all.yaml