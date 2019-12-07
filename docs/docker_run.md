---
id: docker_run
title: Running Pre-Built Docker Image
sidebar_label: Pre-Built Image
---

If you have a pre built docker image `nfpa-location-tools.tar`, you can load it into you Docker with

```bash
docker load --input nfpa-location-tools.tar
```

And then run the image with

```bash
docker container run -p 8080:8080 nfpa-location-tools:latest
```

