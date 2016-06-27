# RethinkDB ORM for Java

[![Build Status](https://img.shields.io/travis/foxylion/rethinkdb-orm-java/master.svg?style=flat-square)](https://travis-ci.org/foxylion/rethinkdb-orm-java)
[![Maven Version](https://img.shields.io/maven-central/v/de.jakobjarosch.rethinkdb/rethinkdb-orm.svg?style=flat-square)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22de.jakobjarosch.rethinkdb%22)
![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg?style=flat-square)
![Maintenance](https://img.shields.io/maintenance/yes/2016.svg?style=flat-square)

This is a lightweight OR mapper for [RethinkDB](https://www.rethinkdb.com/) and Java.
It automatically maps your POJOs to a RethinkDB compatible data structure and vice versa.

## What do I get?

- Lightweight OR mapper using annotation processors
  - Support to map fields to other database field names
  - Support to ignore fields from model or database
- Possibility to automatically create tables and inidices
- Connection pooling support across threads (also standalone available)

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
public class MyModel {
   @PrimaryKey private String id;
   private String location;
}
```

The annotation processor will automatically generate a `MyModelDAO` class which
can be used to create, read, update, delete your model (CRUD). It is also possible
to stream the change sets.

The DAO can be used very easily.

```java
Connection connection = RethinkDB.r.connection().connect();
MyModelDAO dao = new MyModelDAO(connection);
MyModel model = dao.read("2a22fda0");
model.location = "12.234,23.764";
dao.update(model);
connection.close();
```

**More examples can be found [here](rethinkdb-orm-samples/src/main/java/EntryPoint.java).**

### Configure as a dependency

The current version can be found [here](releases).

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
