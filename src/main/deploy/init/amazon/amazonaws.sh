#!/bin/sh

## Safety feature: exit script if error is returned, or if variables not set.
## Exit if a pipeline results in an error.
#set -ue
#set -o pipefail
#
## Setter nøkler og region for skriptet
#export AWS_ACCESS_KEY_ID=AKIAJWIYRNSCEPICV6DQ
#export AWS_SECRET_ACCESS_KEY=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
#export AWS_DEFAULT_REGION=eu-central-1
#
## Oppretter security group, henter ut id og tagger denne med NDLA
#SEC_GROUP_ID=$(aws ec2 create-security-group --group-name ImageAPI --description "NDLA_Image-API" --output=text)
#aws ec2 create-tags --resource $SEC_GROUP_ID --tags Key=NDLA,Value=
#
## Konfigurerer security group
## TODO: vurder å sperre ned tilgangen ift. IP-adresser
#aws ec2 authorize-security-group-ingress --group-id $SEC_GROUP_ID --protocol tcp --port 22 --cidr 0.0.0.0/0
#aws ec2 authorize-security-group-ingress --group-id $SEC_GROUP_ID --protocol tcp --port 80 --cidr 0.0.0.0/0

# Starter instans
# TODO: Finn annen håndtering enn manuell generering på AWS for nøkkelpar
aws ec2 run-instances --image-id ami-accff2b1 --count 1 --instance-type t2.micro --key-name NDLA --security-group-ids sg-8ffb55e6 --user-data file://./cloudinit.sh
# Opprydding
#aws ec2 delete-security-group --group-name ImageAPI