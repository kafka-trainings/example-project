# Builder image
FROM gradle:7.6 as builder
RUN wget https://github.com/Aiven-Open/opensearch-connector-for-apache-kafka/archive/refs/tags/v3.1.1.tar.gz && \
    tar -xzf v3.1.1.tar.gz && \
    cd opensearch-connector-for-apache-kafka-3.1.1 && \
    ./gradlew installDist

# Base image
FROM quay.io/strimzi/kafka:latest-kafka-3.7.0
USER root:root
# Add the connector
COPY --from=builder opensearch-connector-for-apache-kafka-3.1.1/build/install/opensearch-connector-for-apache-kafka/ /opt/kafka/libs/
USER 1001