---
id: build
title: Building the project
sidebar_label: Building JAR
---

## Building JAR

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

Clone the Location Tools repository and build fat jar from source

```
git clone https://github.com/NFPA/LocationTools.git
mvn clean install
```

You should now have `location-tools-1.0-SNAPSHOT.jar` in `target` directory.
