# neic-locator
The neic-locator project is a direct port of the [United States Geolgical Survey National Earthquake Information Center](https://earthquake.usgs.gov/contactus/golden/neic.php) production earthquake location software from FORTRAN to Java. 

The neic-locator project depends on the [neic-traveltime](https://github.com/usgs/neic-traveltime) package to produce seismic travel times and some additional metadata. In its current incarnation, the neic-locator package represents 55 years of evolution from roots in the early 1960s, Engahl, E. R., and R. H. Gunst, “Use of a High Speed Computer for the Preliminary Determination of Earthquake Hypocenters”, [BSSA, 1962, vol. 56, pp. 325-336](https://pubs.geoscienceworld.org/ssa/bssa/article/56/2/325/116393/use-of-a-high-speed-computer-for-the-preliminary). Evolutions include using a much wider selection of seismic phases and eliminating the use of pP-P times to fix depth in the 1980s. In the 1990s, travel-time statistics and Bayesian depth statistics were added to the algorithm. In the early 2000s, the Jeffreys reweighting scheme was replaced by a rank-sum regression to improve statistical properties and decorrelation was added to deal with significant inhomogeneity in the network of available stations.

In its current form, the neic-locator is a small piece of the much larger NEIC seismic processing system. This is significant because, the neic-locator is not responsible for event nucleation, phase association, or magnitudes. It is responsible for phase re-identification and does have some responsibility for handling analysis commands (e.g., phase use, phase identification, modifying the Bayesian depth parameters, fixing a hypocenter or depth, etc.). In the absence of other input, the Bayesian depth is guided by 40 years of earthquake statistics. The Locator is also aware of gross global crustal geology, which is used to modify Pg and Sg in tectonic areas where Pb and Sb are not observed.

Dependencies
------
* neic-locator was written in Java 1.8
* neic-locator is built with [Gradle](https://gradle.org/), a build.gradle file and gradlew are included in this project
* neic-locator depends on the [neic-traveltime](https://github.com/usgs/neic-traveltime) package. A copy of this package is automatically downloaded and built as part of the neic-locator build.
* neic-locator depends on the [earthquake-processing-formats](https://github.com/usgs/earthquake-processing-formats) package. A copy of this package is automatically downloaded and built as part of the neic-locator build.
* neic-locator uses the [Micronaut library](https://micronaut.io/) to implement a locator webservice. This package is automatically downloaded and built as part of the neic-locator build.
* neic-locator can be optionally built into a docker container using the supplied dockerfile. Docker is not included with neic-locator project.

Building
------
The steps to get and build neic-locator.jar using gradle are as follows:

1. Clone neic-locator.
2. Open a command window and change directories to /neic-locator/
3. To build the jar file, run the command `./gradlew build`
4. To build the jar file, run unit tests, and code coverage, run the command `./gradlew check`
5. To generate javadocs, run the command `./gradlew javadoc`

To build the docker image, with docker installed and neic-locator built:

1. In a command window, change directories to /neic-locator/
2. To build the docker container, run the command `docker build -t usgs/neic-locator:latest .`

Using
-----
Once you are able to build the neic-locator jar, simply include the jar
file in your application, or call using the LocMain class.

A set of model files used by the locator is stored in the models/ directory.
The locator also requires traveltime model files. These files are copied into
build/models/ as part of the gradle build.

An example legacy input file `LocOutput1000010563_23.txt` is provided in the
examples directory.

To run this example, run the command  `java -jar build/libs/neic-locator-0.3.0-all.jar --modelPath=./build/models/ --inputFile=./examples/raylocinput1000010563_23.txt  --logLevel=debug`

The results of this example (and associated log file) are expected to match the
results found in the legacy output file `LocOutput1000010563_23.txt` file
provided in the examples directory.

To run the neic-locator as a web service, run the command `java -jar build/libs/neic-locator-0.3.0-all.jar --mode=service`

To run the neic-locator from the docker container, first create a docker volume for the temporary files with the command `docker volume create loc-vol`, then run the command `docker run -d --name loc-test --mount source=loc-vol,target=/project/local/ -p 8070:8080 -it usgs/neic-locator:latest` or `docker-compose up` if docker-compose is available.

To attach to the neic-locator container for debugging, use the command `docker container exec -it loc-test /bin/bash`

Once the web service is running, either locally or out of the container, you can access the swagger ui in a browser at `http://localhost:8080/` or `http://localhost:8070/` (if running out of the container), and "try out" the locator service using the contents of examples/request.json.

Further Information and Documentation
------
For further information and documentation please check out the [neic-locator Documentation Site](https://usgs.github.io/neic-locator/).

File bug reports, feature requests and questions using [GitHub Issues](https://github.com/usgs/neic-locator/issues)
