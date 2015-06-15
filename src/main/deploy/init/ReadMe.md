In order to initalize deployment you need to do the following:
 
1. Generate certificates for communicating with remote Docker daemen by running docker/generateDockerCerts.sh
2. Make sure you av a key-pair on amazon, and store the private key in the folder amazon/
3. Create and initialize server by running amazon/amazonaws.sh
4. Copy Docker certificates by running amazon/copyDockerCertificates.sh