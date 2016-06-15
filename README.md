# RethinkDB ORM for Java

[![Build Status](https://img.shields.io/travis/foxylion/rethinkdb-orm-java/master.svg?style=flat-square)](https://travis-ci.org/foxylion/rethinkdb-orm-java)
[![Maven Version](https://img.shields.io/maven-central/v/de.jakobjarosch.rethinkdb/rethinkdb-orm.svg?style=flat-square)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22de.jakobjarosch.rethinkdb%22)
![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg?style=flat-square)
![Maintenance](https://img.shields.io/maintenance/yes/2016.svg?style=flat-square)

This is a lightweight OR mapper for [RethinkDB](https://www.rethinkdb.com/) and Java.
It automatically maps your POJOs to a RethinkDB compatible data structure and vice versa.

## How to use?

The integration is using annotation processors to generate the DAO classes.
Ensure that your IDE has [enabled annotation processing](https://immutables.github.io/apt.html).

To get started you've to annotate your POJO.

```java
@RethinkDBModel(
   tableName = "my_table",
   indices = {
      @Index(fields = {"location"}, geo = true)
   }
)
public MyModel {
   @PrimaryKey private String id;
   private String location;
}
```

The annotation processor will automatically generate a `MyModelDAO` class which
can be used to create, read, update, delete your model (CRUD). It is also possible
to stream the change sets. [Here](rethinkdb-orm-test/src/main/java/EntryPoint.java) you can find a sample implementation.

### Configure as a dependency

#### Maven
```xml
<dependency>
    <groupId>de.jakobjarosch.rethinkdb</groupId>
    <artifactId>rethinkdb-orm</artifactId>
    <version>{{ current-version }}</version>
</dependency>
```

#### Gradle
```
compile 'de.jakobjarosch.rethinkdb:rethinkdb-orm:{{ current-version }}'
```
