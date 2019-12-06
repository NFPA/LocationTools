---
id: docker_setup
title: Docker Setup
sidebar_label: Docker Setup
---

## Building Docker Image


### Sample Build (MA)

- Clone the project on to your machine and build the docker image. This needs ~3 GB disk space to download libpostal data

```
git clone https://github.com/NFPA/LocationTools.git
cd LocationTools
docker image build -t nfpa-location-tools .
docker container run -p 8080:8080 nfpa-location-tools
```

This will start the location tools API on port 8080. The first port of -p parameter is the post of localhost you want to bind to container's port, which is the second part of -p parameter.

For example, if you want to run the API on port 8088 you would execute

```
docker container run -p 8088:8080 nfpa-location-tools
```

### Complete USA Build

```
git clone https://github.com/NFPA/LocationTools.git
cd LocationTools
```

Edit the `onstart-docker.sh` file and change `https://tiger-lucene-index.s3.us-east-2.amazonaws.com/lucene-sample-states-index.zip` to `https://tiger-lucene-index.s3.us-east-2.amazonaws.com/lucene-all-states-index.zip` and then proceed  to:

```
docker image build -t nfpa-location-tools .
docker container run -p 8080:8080 nfpa-location-tools
```

### Troubleshooting

- If you get permission errors, use `sudo docker` instead

