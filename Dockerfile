ARG BUILD_IMAGE=usgs/java:11
ARG FROM_IMAGE=usgs/java:11

# === Stage 1: Compile and Build java codebase ===
FROM ${BUILD_IMAGE} as build

# install gradle
COPY ./gradlew /neic-locator/
COPY ./gradle /neic-locator/gradle
COPY ./build.gradle /neic-locator/.
WORKDIR /neic-locator
RUN ./gradlew tasks

# see .dockerignore for what is not COPYed
COPY . /neic-locator

# don't run tests and checks since this is a deployment
# container, we run these elsewhere in the pipeline
RUN ./gradlew --no-daemon build -x test -x check

# use consistent jar name
RUN cp /neic-locator/build/libs/neic-locator-*-all.jar /neic-locator/build/neic-locator-service.jar

# === Stage 2: Create image to serve java locator service app ===
FROM ${FROM_IMAGE}

# copy shadow jar
COPY --from=build /neic-locator/build/neic-locator-service.jar /neic-locator/
# copy models
COPY --from=build /neic-locator/build/models /neic-locator/models
# copy entrypoint
COPY --from=build /neic-locator/docker-entrypoint.sh /neic-locator/

# set environment
ENV locator.model.path=/neic-locator/models/
ENV locator.serialized.path=/neic-locator/local/

# run as root to avoid volume writing issues
USER root
WORKDIR /neic-locator

# create entrypoint, needs double quotes
ENTRYPOINT [ "/neic-locator/docker-entrypoint.sh" ]
EXPOSE 8080
