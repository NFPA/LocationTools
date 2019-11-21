FROM ubuntu:18.04
#FROM openjdk:8-alpine

RUN apt-get update && apt-get install -y openjdk-8-jre curl autoconf automake libtool pkg-config git


ARG COMMIT
ENV COMMIT ${COMMIT:-master}
ENV DEBIAN_FRONTEND noninteractive
RUN git clone https://github.com/openvenues/libpostal -b $COMMIT
COPY build_libpostal.sh /libpostal/build_libpostal.sh

RUN ["chmod", "+x", "/libpostal/build_libpostal.sh"]
WORKDIR /libpostal
RUN ./build_libpostal.sh

COPY target/location-tools-1.0-SNAPSHOT.jar /usr/src/location-tools-1.0-SNAPSHOT.jar
COPY docker-driver.ini /usr/src/docker-driver.ini
COPY src/conf/vertx-docker-conf.json /usr/src/conf/vertx-docker-conf.json
ADD jniLibs /usr/src/jniLibs
ADD TIGER_RAW/index /usr/src/index

COPY onstart-docker.sh /usr/src/onstart-docker.sh
RUN ["chmod", "+x", "/usr/src/onstart-docker.sh"]
CMD ["/usr/src/onstart-docker.sh"]

#CMD \
#java -cp /usr/src/location-tools-1.0-SNAPSHOT.jar org.nfpa.spatial.Driver --download /usr/src/docker-driver.ini ;\

EXPOSE 8080
