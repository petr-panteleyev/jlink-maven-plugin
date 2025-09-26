# Basic Example

```xml
<plugin>
    <groupId>org.panteleyev</groupId>
    <artifactId>jlink-maven-plugin</artifactId>
    <configuration>
        <output>${project.build.directory}/jlink</output>
        <modulePaths>
            <modulePath>${project.build.directory}/jmods</modulePath>
        </modulePaths>
        <addModules>
            <addModule>ALL-MODULE-PATH</addModule>
        </addModules>
        <noHeaderFiles>true</noHeaderFiles>
        <noManPages>true</noManPages>
        <stripDebug>true</stripDebug>
    </configuration>
</plugin>
```