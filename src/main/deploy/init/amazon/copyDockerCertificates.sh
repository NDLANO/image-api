#!/bin/sh
if [ $# -ne 4 ]
  then
    echo "Usage: copyDockerCertificates [ipforamazonserver] [sshportonamazonserver] [usernameonamazonserver] [pathtoamazoncertificate]"
    exit 1
fi
echo "Copying scripts to /var/ssl/ on $1 on port $2 with username $3 using cert $4"
ssh -p$2 -l $3 -i $4 $1 sudo mkdir -p /var/ssl
rsync -e "ssh -p$2 -i $4 -l $3" --rsync-path="sudo rsync" ../docker/certs/ca.pem ../docker/certs/server-cert.pem ../docker/certs/server-key.pem $1:/var/ssl/
ssh -p$2 -l $3 -i $4 $1 sudo chmod 600 /var/ssl/server-key.pem
ssh -p$2 -l $3 -i $4 $1 sudo groupadd docker
ssh sudo gpasswd -a $3 docker
ssh -p$2 -l $3 -i $4 $1 sudo service docker restart
