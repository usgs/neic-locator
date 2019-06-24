FROM gradle:jdk11 as build
# see .dockerignore for what is not COPYed
COPY . /project
# run gradle build
WORKDIR /project
RUN gradle --no-daemon build


FROM adoptopenjdk/openjdk11:latest
# copy shadow jar
COPY --from=build /project/build/libs/neic-locator-0.1.0-all.jar /project/
# copy models
ENV locator.model.path=/project/models/
COPY --from=build /project/build/models/* /project/models/
# run as unprivileged user
USER nobody
EXPOSE 8080
WORKDIR /project
CMD [ "/opt/java/openjdk/bin/java", "-jar", "neic-locator-0.1.0-all.jar" ]
