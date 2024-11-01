## Usage

### Finding jlink

Plugin searches for ```jlink``` executable using the following priority list:

1. ```maven-toolchains-plugin``` configured in the project. Toolchain "jdk" will be queried for 
tool = "jlink".

2. ```java.home``` system property.

### Configuration

#### Mandatory Parameters

To enable various configuration approaches mandatory parameters are validated during plugin execution:

* output

#### Relative Path Resolution

The following plugin parameters define directory or file location:

* output
* modulePath
 
If path is not absolute is will be resolved as relative to ```${project.basedir}```.

#### Additional Launchers

Additional launchers provide the opportunity to install alternative ways to start an application.

_Example:_

```xml
<launchers>
    <launcher>
        <name>App1</name>
        <file>src/resources/App1.properties</file>
    </launcher>
    <launcher>
        <name>App2</name>
        <file>src/resources/App2.properties</file>
    </launcher>
</launchers>
```

### Assembling Dependencies

Before executing ```jlink``` all runtime dependencies should be copied into a single folder together with main
application jar. This example shows how to do this via ```maven-dependency-plugin```.

```xml
<plugins>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <configuration>
            <outputDirectory>target/jmods</outputDirectory>
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
                    <outputDirectory>target/jmods</outputDirectory>
                </configuration>
            </execution>
        </executions>
    </plugin>

    <plugin>
        <groupId>org.panteleyev</groupId>
        <artifactId>jlink-maven-plugin</artifactId>
        <configuration>
            <modulePaths>
                <modulePath>target/jmods</modulePath>
            </modulePaths>
            <output>target/jlink</output>
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
