FROM ubuntu:18.04

RUN apt-get update && apt-get install -y openjdk-8-jdk curl autoconf automake libtool pkg-config git


ARG COMMIT
ENV COMMIT ${COMMIT:-master}
ENV DEBIAN_FRONTEND noninteractive

# Build libpostal
RUN git clone https://github.com/openvenues/libpostal -b $COMMIT
COPY build_libpostal.sh /libpostal/build_libpostal.sh
RUN ["chmod", "+x", "/libpostal/build_libpostal.sh"]
WORKDIR /libpostal
RUN ./build_libpostal.sh

# Build jpostal
WORKDIR /
RUN git clone https://github.com/openvenues/jpostal -b $COMMIT
COPY build_jpostal.sh /jpostal/build_jpostal.sh
RUN ["chmod", "+x", "/jpostal/build_jpostal.sh"]
WORKDIR /jpostal
RUN ./build_jpostal.sh

RUN ["mv", "/jpostal/src/main/jniLibs", "/usr/src/jniLibs"]

# Copy jars and config files
COPY target/location-tools-1.0-SNAPSHOT.jar /usr/src/location-tools-1.0-SNAPSHOT.jar
COPY src/conf/vertx-docker-conf.json /usr/src/conf/vertx-docker-conf.json

# Change the location of lucene index
ADD TIGER_RAW/index /usr/src/index

COPY onstart-docker.sh /usr/src/onstart-docker.sh
RUN ["chmod", "+x", "/usr/src/onstart-docker.sh"]
CMD ["/usr/src/onstart-docker.sh"]

#COPY docker-driver.ini /usr/src/docker-driver.ini
#CMD \
#java -cp /usr/src/location-tools-1.0-SNAPSHOT.jar org.nfpa.spatial.Driver --download /usr/src/docker-driver.ini ;\
#java -cp /usr/src/location-tools-1.0-SNAPSHOT.jar org.nfpa.spatial.Driver --process /usr/src/docker-driver.ini ;

EXPOSE 8080
