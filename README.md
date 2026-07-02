# JLink Maven Plugin

Maven plugin for [jlink](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jlink.html).

```xml
<plugin>
    <groupId>org.panteleyev</groupId>
    <artifactId>jlink-maven-plugin</artifactId>
    <version>1.3.0</version>
</plugin>
```

## Goals

### jlink:jlink

#### Required Parameters

##### `<output>`

--output _path_ \
Specifies the location of the generated runtime image.

#### Optional Parameters

<table>
</table>

##### `<addModules>`

--add-modules _mod[,mod]_ \
Adds the named modules, mod, to the default set of root modules. The default set of root modules is empty. \
```xml
<addModules>
    <module>app.module</module>
</addModules>
```

##### `<bindServices>`

--bind-services \
Link service provider modules and their dependencies. \
**Default**: false


##### `<endian>`

--endian {little|big} \
Specifies the byte order of the generated image. The default value is the format of your system's architecture. \
Possible values:

| Plugin | jlink  |
|--------|--------|
| LITTLE | little |
| BIG    | big    |

##### `<generateCdsArchive>`

--generate-cds-archive \
Generate CDS archive if the runtime image supports the CDS feature. \
**Default**: false

##### `<ignoreSigningInformation>`

--ignore-signing-information \
Suppresses a fatal error when signed modular JARs are linked in the runtime image. The signature-related files of the 
signed modular JARs aren't copied to the runtime image. \
**Default**: false

##### `<launchers>`

--launcher command=module or --launcher command=module/main \
Specifies the launcher command name for the module or the command name for the module and main class.
```xml
<launchers>
    <launcher>
        <command>command</name>
        <module>module</module>
        <mainClass>optionalMainClass</mainClass>
    </launcher>
</launchers>
```

##### `<limitModules>`

--limit-modules mod[,mod...] \
Limits the universe of observable modules to those in the transitive closure of the named modules, mod, plus the main 
module, if any, plus any further modules specified in the addModules option. \
Each module is specified by a separate `<limitModule>` parameter.
```xml
<limitModules>
    <limitModule>java.base</limitModule>
</limitModules>
```

##### `<modulePaths>`

--module-path _modulepath_ \
Specifies the module path. \
If this option is not specified, then the default module path is `$JAVA_HOME/jmods`. This directory contains the 
java.base module and the other standard and JDK modules. If this option is specified but the java.base module cannot 
be resolved from it, then the jlink command appends `$JAVA_HOME/jmods` to the module path. \
Each module path is specified by a separate <modulePath> parameter.
```xml
<modulePaths>
    <modulePath>>${project.build.directory}/jmods</modulePath>
</modulePaths>
```

##### `<noHeaderFiles>`

--no-header-files \
Excludes header files. \
**Default**: false

##### `<noManPages>`

--no-man-pages \
Excludes man pages. \
**Default**: false

##### `<skip>`
Skips plugin execution. \
**Default**: false

##### `<stripDebug>`

--strip-debug \
Strips debug information from the output image. \
**Default**: false

`<stripNativeCommands>` : `boolean` \
--strip-native-commands \
Excludes native commands (such as java/java.exe) from the image. \
**Default**: false

---

`<verbose>` : boolean \
--verbose \
**Default**: false

---

[Plugin Documentation](https://www.panteleyev.org/javadoc/jlink-maven-plugin/)
