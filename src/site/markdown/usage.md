# Usage

## Finding jlink

Plugin searches for ```jlink``` executable using the following priority list:

1. ```maven-toolchains-plugin``` configured in the project. Toolchain "jdk" will be queried for 
tool = "jlink".

2. ```java.home``` system property.

## Configuration

### Relative Path Resolution

Parameters of type ```File``` are resolved to absolute paths. To avoid unexpected results it is advised to supply
absolute paths explicitly using Maven variables such as ```${project.basedir}```.

## Assembling Dependencies

Before executing ```jlink``` all runtime dependencies should be copied into a single folder together with main
application jar. This example shows how to do this via ```maven-dependency-plugin```.

```xml
<plugins>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <configuration>
            <outputDirectory>${project.build.directory}/jmods</outputDirectory>
        </configuration>
    </plugin>
    
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <executions>
            <execution>
                <id>copy-dependencies</id>
                <phase>package</phase>
                <goals>
                    <goal>copy-dependencies</goal>
                </goals>
                <configuration>
                    <includeScope>runtime</includeScope>
                    <outputDirectory>${project.build.directory}/jmods</outputDirectory>
                </configuration>
            </execution>
        </executions>
    </plugin>

    <plugin>
        <groupId>org.panteleyev</groupId>
        <artifactId>jlink-maven-plugin</artifactId>
        <configuration>
            <modulePaths>
                <modulePath>${project.build.directory}/jmods</modulePath>
            </modulePaths>
            <output>${project.build.directory}/jlink</output>
        </configuration>
    </plugin>
</plugins>
```

## Dry Run Mode

To print jlink parameters without executing jpackage set ```jlink.dryRun``` property to ```true```.

_Example:_

```
mvn clean package jlink:jlink -jlink.dryRun=true
```
