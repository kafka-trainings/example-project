FROM gradle:jdk17 AS build
RUN mkdir /src
COPY build.gradle /src
COPY ./src /src/src
WORKDIR /src
RUN gradle shadowJar --no-daemon

FROM openjdk:17
RUN mkdir /app
COPY jmx_exporter.jar /app/jmx_exporter.jar
COPY jmx_exporter.yaml /app/jmx_exporter.yaml
COPY --from=build /src/build/libs/*.jar /app/application.jar
ENTRYPOINT ["java", "-javaagent:/app/jmx_exporter.jar=3022:/app/jmx_exporter.yaml", "-jar", "/app/application.jar", "/app/app.properties"]