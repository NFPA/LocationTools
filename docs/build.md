---
id: build
title: Building the project
sidebar_label: Building JAR
---

## Building the Project
- Install GIT and Maven

<!--DOCUSAURUS_CODE_TABS-->
<!--JavaScript-->
```js
console.log('Hello, world!');
```
<!--Python-->
```py
print('Hello, world!')
```

<!--C-->
```C
#include <stdio.h>

int main() {
   printf("Hello World!");
   return 0;
}
```

<!--Pascal-->
```Pascal
program HelloWorld;
begin
  WriteLn('Hello, world!');
end.
```

<!--END_DOCUSAURUS_CODE_TABS-->
On Ubuntu:

```
$ sudo apt-get update
$ sudo apt-get -y git maven
```

- Clone the Location Tools Repo

```git clone https://github.com/NFPA/LocationTools.git```

- Build jar from source

```mvn clean install```

You should now have Locationtools-1.0-snapshot.jar in `target` directory.
