ARG BUILD_IMAGE=usgs/centos:7
ARG FROM_IMAGE=usgs/centos:7

FROM ${BUILD_IMAGE} as build

# install java; which is used by gradle to find java
RUN yum install -y java-11-openjdk-devel which

# install gradle
COPY ./gradlew /project/
COPY ./gradle /project/gradle
COPY ./build.gradle /project/.
WORKDIR /project
RUN ./gradlew tasks

# see .dockerignore for what is not COPYed
COPY . /project
RUN ./gradlew --no-daemon build

# use consistent jar name
RUN cp /project/build/libs/neic-locator-*-all.jar /project/build/neic-locator-service.jar


FROM ${FROM_IMAGE}

# install java
RUN yum install -y java-11-openjdk-headless

# copy shadow jar
COPY --from=build /project/build/neic-locator-service.jar /project/
# copy models
COPY --from=build /project/build/models /project/models

# set environment
ENV locator.model.path=/project/models/
ENV locator.serialized.path=/project/local/

# run as root to avoid volume writing issues
USER root
EXPOSE 8080
WORKDIR /project
ENTRYPOINT [ "/usr/bin/java", "-jar", "neic-locator-service.jar" ]
CMD [ "--mode=service" ]
