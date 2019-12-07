---
id: docker_setup
title: Docker Setup
sidebar_label: Docker Setup
---

## Building Docker Image


### Sample Build (Massachusetts)

Clone the project on to your machine and build the docker image. This needs ~3 GB disk space to download libpostal data

```bash
git clone https://github.com/NFPA/LocationTools.git
cd LocationTools
mvn clean install
docker image build -t nfpa-location-tools .
docker container run -p 8080:8080 nfpa-location-tools
```

This will start the location tools API on port 8080. The first port of -p parameter is the post of localhost you want to bind to container's port, which is the second part of -p parameter.

For example, if you want to run the API on port 8088 you would execute

```bash
docker container run -p 8088:8080 nfpa-location-tools
```

### U.S. Build

Edit the `onstart-docker.sh` file and change `BUILD` variable to `all` instead of `sample` and then proceed  to:

```bash
docker image build -t nfpa-location-tools .
docker container run -p 8080:8080 nfpa-location-tools
```

## Exporting Docker Image

To share this build with others you can export the docker image to a tar as follows

```bash
docker save -o nfpa-location-tools.tar nfpa-location-tools:latest
```


## Troubleshooting

- If you get permission errors, use `sudo docker` instead

