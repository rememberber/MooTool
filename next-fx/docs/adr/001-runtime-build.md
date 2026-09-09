# ADR 001 — Runtime and build

Status: Accepted  
Date: 2026-09-09

## Context

P0 must lock an independently redistributable OpenJDK + OpenJFX combination, a Maven Wrapper, and an app-image that starts without a system Java install.

## Decision

- Language level and bytecode: Java 25 (`release=25`). No preview language features.
- JDK used to compile and package on this machine: Azul Zulu 25.0.4.1 (`x86_64`), `JAVA_HOME=/Users/zhoubo/Library/Java/JavaVirtualMachines/azul-25.0.4.1/Contents/Home`.
- OpenJFX: 26.0.2 Maven modules for development; matching Gluon jmods for `jlink`.
- Maven Wrapper 3.3.4 distributing Apache Maven 3.9.16. The project does not inherit the repository root POM.
- Application code stays on the classpath. JavaFX modules are on the module path at runtime and inside the jlinked runtime. RichTextFX, Jackson, JSONPath, and sqlite-jdbc remain regular jars.
- Packaging path: `jlink` (keep `bin/java`) then `jpackage --type app-image`. Installers (DMG/MSI/DEB) are deferred to P7.
- macOS `jpackage --app-version` cannot start with `0` on JDK 25. Product version stays `0.1.0-SNAPSHOT` in `pom.xml` / `version.properties`; the app-image numeric version is mapped to `1.0.0` (see `packaging/version-mapping.txt`).
- Development profile (`--profile=dev`) writes to `*.dev` identity directories so later release data is not mixed.

## Consequences

Ordinary `./mvnw javafx:run` uses the system/dev JDK plus Maven JavaFX artifacts. The shipped app-image must be verified with a PATH that does not include another `java`. Windows/Linux app-images remain unbuilt on this macOS x64 host.
