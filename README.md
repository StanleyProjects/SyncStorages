# SyncStorages
A library for storing, changing and synchronizing data between devices.

---

## Unstable

> GitHub [0.2.4u-SNAPSHOT](https://github.com/StanleyProjects/SyncStorages/releases/tag/0.2.4u-SNAPSHOT) release
>
> Maven [metadata](https://central.sonatype.com/repository/maven-snapshots/com/github/kepocnhh/SyncStorages/maven-metadata.xml)

### Build
```
$ gradle lib:assembleUnstableJar
```

### Import
```kotlin
repositories {
    maven("https://central.sonatype.com/repository/maven-snapshots")
}

dependencies {
    implementation("com.github.kepocnhh:SyncStorages:0.2.4u-SNAPSHOT")
}
```

---

## Benchmarks

### run all benchmarks
```
$ gradle lib:runBenchmark
```

### run all benchmarks in the class
```
$ gradle lib:runBenchmark -Pbenchmarks=${class}
```

### run one benchmark in the class
```
$ gradle lib:runBenchmark -Pbenchmarks=${class}.${method}
```

---
