---
id: requirements
title: Installing Prerequisites
sidebar_label: Installing Prerequisites
---

Building the project has only been tested on Linux/Ubuntu. For Windows, [running a pre-built Docker](docker_run.md) image is the preferred method.


### libpostal

We use [libpostal](https://github.com/openvenues/libpostal) to parse input addresses into different components. You can install libpostal from [here](https://github.com/openvenues/libpostal#installation-maclinux)

### jpostal

[jpostal](https://github.com/openvenues/jpostal), the Java bindings for libpostal, can be installed from [here](https://github.com/openvenues/jpostal#building-jpostal)

> Shared libraries will be available at `jpostal/src/main/jniLibs`
> You'll need to use this as a value in the [driver configuration](config.md)

### git(>=2.17) & maven(>=3.6.0)

```
sudo apt-get update
sudo apt-get install git maven
```
