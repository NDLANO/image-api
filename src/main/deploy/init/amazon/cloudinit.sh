#!/bin/sh

apt-get -y install unattended-upgrades
cat <<EOF > /etc/apt/apt.conf.d/20auto-upgrades
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
EOF

/etc/init.d/unattended-upgrades restart

wget -qO- https://get.docker.com | sh
