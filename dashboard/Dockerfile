FROM gradle:jdk17 AS build
RUN mkdir /src
COPY build.gradle /src
COPY ./src /src/src
WORKDIR /src
RUN gradle shadowJar --no-daemon

FROM openjdk:17
RUN mkdir /app
COPY --from=build /src/build/libs/*.jar /app/application.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "/app/application.jar", "/app/app.properties"]