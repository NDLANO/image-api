#!/bin/sh

apt-get -y install unattended-upgrades
cat <<EOF > /etc/apt/apt.conf.d/20auto-upgrades
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
EOF

/etc/init.d/unattended-upgrades restart

wget -qO- https://get.docker.com | sh

cat <<EOF > /etc/default/docker
DOCKER_OPTS="-H=unix:///var/run/docker.sock -d --tlscacert=/var/ssl/ca.pem --tlscert=/var/ssl/server-cert.pem --tlskey=/var/ssl/server-key.pem -H 0.0.0.0:4243"
EOF
