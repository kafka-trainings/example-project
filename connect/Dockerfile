FROM ubuntu/kafka:3.1-22.04_beta
ADD connector.tar.gz /connectors
ENTRYPOINT ["connect-distributed.sh", "/connect-distributed.properties"]