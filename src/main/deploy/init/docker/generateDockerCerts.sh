#!/bin/bash
# Generates certificates and puts in folder certs
set -ex
mkdir certs && cd certs
echo "Creating server keys..."
echo 01 > ca.srl
openssl genrsa -des3 -out ca-key.pem
openssl req -new -x509 -days 3650 -key ca-key.pem -out ca.pem
openssl genrsa -des3 -out server-key.pem
openssl req -new -key server-key.pem -out server.csr
openssl x509 -req -days 365 -in server.csr -CA ca.pem -CAkey ca-key.pem \
    -out server-cert.pem

echo "Creating client keys..."
openssl genrsa -des3 -out client-key.pem
openssl req -new -key client-key.pem -out client.csr
echo extendedKeyUsage = clientAuth > extfile.cnf
openssl x509 -req -days 365 -in client.csr -CA ca.pem -CAkey ca-key.pem \
    -out client-cert.pem -extfile extfile.cnf

echo "Stripping passwords from keys..."
openssl rsa -in server-key.pem -out server-key.pem
openssl rsa -in client-key.pem -out client-key.pem
