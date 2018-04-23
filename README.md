[![Codacy Badge](https://api.codacy.com/project/badge/Grade/b50fddd11ce24251b546f1de9f4854e2)](https://app.codacy.com/app/jpatton-USGS/neic-locator?utm_source=github.com&utm_medium=referral&utm_content=usgs/neic-locator&utm_campaign=badger)
[![Build Status](https://travis-ci.org/usgs/neic-locator.svg?branch=master)](https://travis-ci.org/usgs/neic-locator)
[![Documentation](https://usgs.github.io/neic-locator/codedocumented.svg)](https://usgs.github.io/neic-locator/)

# neic-locator
Port of the NEIC locator from FORTRAN77 to Java

Dependencies
------
* neic-locator was written in Oracle Java 1.8
* neic-locator was depends on the [neic-traveltime](https://github.com/usgs/neic-traveltime)
package. A copy of this package is automatically downloaded and built as part of
the neic-locator build.
* neic-locator is built with [Apache Ant](http://ant.apache.org/), and was
written using Eclipse and netbeans.  Netbeans project files, source files,
and an ant build.xml are included in this project

Building
------
The steps to get and build neic-locator.jar using ant are as follows:

1. Clone neic-locator.
2. Open a command window and change directories to /neic-locator/
3. To build the jar file, run the command `ant jar`
4. To generate javadocs, run the command `ant javadoc`
5. To compile, generate javadocs, build jar, run the command `ant all`

Using
-----
Once you are able to build the neic-locator jar, simply include the jar
file in your application, or call using the LocMain class.

Further Information and Documentation
------
For further information and documentation please check out the [neic-locator Documentation Site](https://usgs.github.io/neic-locator/).

File bug reports, feature requests and questions using [GitHub Issues](https://github.com/usgs/neic-locator/issues)
