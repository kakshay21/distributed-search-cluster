FROM maven:3.8-jdk-11 AS MAVEN_TOOL_CHAIN_CONTAINER
RUN mkdir src
COPY src /tmp/src
COPY ./pom.xml /tmp/
RUN mkdir resources
COPY resources /tmp/resources
WORKDIR /tmp/
RUN --mount=type=cache,target=/root/.m2 mvn package
RUN ls -la /tmp

FROM openjdk:11
COPY --from=MAVEN_TOOL_CHAIN_CONTAINER /tmp/target/distributed.search.cluster-1.0-SNAPSHOT-jar-with-dependencies.jar /tmp/
COPY --from=MAVEN_TOOL_CHAIN_CONTAINER /tmp/resources /tmp/resources
WORKDIR /tmp/
ENTRYPOINT ["java","-jar", "distributed.search.cluster-1.0-SNAPSHOT-jar-with-dependencies.jar"]
CMD ["8080"]