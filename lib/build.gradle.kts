import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import sp.kx.gradlex.GitHub
import sp.kx.gradlex.Markdown
import sp.kx.gradlex.Maven
import sp.kx.gradlex.add
import sp.kx.gradlex.asFile
import sp.kx.gradlex.assemble
import sp.kx.gradlex.buildDir
import sp.kx.gradlex.buildSrc
import sp.kx.gradlex.check
import sp.kx.gradlex.create
import sp.kx.gradlex.dir
import sp.kx.gradlex.eff
import sp.kx.gradlex.get
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

version = "0.2.3"

val maven = Maven.Artifact(
    group = "com.github.kepocnhh",
    id = rootProject.name,
)

val gh = GitHub.Repository(
    owner = "StanleyProjects",
    name = rootProject.name,
)

repositories {
    mavenCentral()
    maven("https://central.sonatype.com/repository/maven-snapshots") // todo
}

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.gradle.jacoco")
    id("io.gitlab.arturbosch.detekt") version Version.detekt
    id("org.jetbrains.dokka") version Version.dokka
}

tasks.getByName<JavaCompile>("compileJava") {
    targetCompatibility = Version.jvmTarget
}

val compileKotlinTask = tasks.getByName<KotlinCompile>("compileKotlin") {
    kotlinOptions {
        jvmTarget = Version.jvmTarget
        freeCompilerArgs += setOf("-module-name", maven.moduleName(separator = '-'))
    }
}

tasks.getByName<JavaCompile>("compileTestJava") {
    targetCompatibility = Version.jvmTarget
}

tasks.getByName<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = Version.jvmTarget
}

sourceSets.create("jmh") {
    project.kotlin.target.compilations.also {
        it[name].associateWith(it["main"])
    }
}

dependencies {
    implementation("com.github.kepocnhh:Bytes:0.4.1u-SNAPSHOT")
    implementation("com.github.kepocnhh:Hashes:0.1.0-SNAPSHOT")
    implementation("com.github.kepocnhh:Ids:0.0.1-SNAPSHOT")
    implementation("com.github.kepocnhh:Storages:0.13.2u-SNAPSHOT")
    implementation("com.github.kepocnhh:Streamers:0.1.0-SNAPSHOT")
    implementation("com.github.kepocnhh:Times:0.0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Version.jupiter}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Version.jupiter}")
    "jmhImplementation"("org.openjdk.jmh:jmh-core:${Version.jmh}")
    "jmhImplementation"("org.openjdk.jmh:jmh-generator-bytecode:${Version.jmh}")
}

fun Test.getExecutionData(): File {
    return buildDir()
        .dir("jacoco")
        .asFile("$name.exec")
}

val taskUnitTest by tasks.register<Test>("checkUnitTest") {
    useJUnitPlatform()
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED") // https://github.com/gradle/gradle/issues/18647
    doLast {
        getExecutionData().eff()
    }
}

jacoco.toolVersion = Version.jacoco

val taskCoverageReport by tasks.register<JacocoReport>("assembleCoverageReport") {
    dependsOn(taskUnitTest)
    reports {
        csv.required = false
        html.required = true
        xml.required = false
    }
    sourceDirectories.setFrom(file("src/main/kotlin"))
    classDirectories.setFrom(sourceSets.main.get().output.classesDirs)
    executionData(taskUnitTest.getExecutionData())
    doLast {
        val report = buildDir()
            .dir("reports/jacoco/$name/html")
            .eff("index.html")
        println("Coverage report: ${report.absolutePath}")
    }
}

tasks.register<JacocoCoverageVerification>("checkCoverage") {
    dependsOn(taskCoverageReport)
    violationRules {
        rule {
            limit {
                minimum = BigDecimal(0.96)
            }
        }
    }
    classDirectories.setFrom(taskCoverageReport.classDirectories)
    executionData(taskCoverageReport.executionData)
}

tasks.register<Detekt>("checkCodeQuality") {
    buildUponDefaultConfig = true
    allRules = true
    jvmTarget = Version.jvmTarget
    val sourceSet = sourceSets.getByName("main")
    source = sourceSet.allSource
    val configs = setOf(buildSrc.dir("src/main/resources/detekt").eff("config.yml"))
    config.setFrom(configs)
    val report = buildDir()
        .dir("reports/analysis/code/quality/${sourceSet.name}/html")
        .asFile("index.html")
    reports {
        html {
            required = true
            outputLocation = report
        }
        md.required = false
        sarif.required = false
        txt.required = false
        xml.required = false
    }
    val detektTask = tasks.get<Detekt>("detekt", sourceSet.name)
    classpath.setFrom(detektTask.classpath)
    doFirst {
        println("Analysis report: ${report.absolutePath}")
    }
}

tasks.register<Detekt>("checkDocs") {
    buildUponDefaultConfig = false
    allRules = false
    jvmTarget = Version.jvmTarget
    val sourceSet = sourceSets.getByName("main")
    source = sourceSet.allSource
    val configs = setOf(buildSrc.dir("src/main/resources/detekt").eff("docs.yml"))
    config.setFrom(configs)
    val report = buildDir()
        .dir("reports/analysis/docs/html")
        .asFile("index.html")
    reports {
        html {
            required = true
            outputLocation = report
        }
        md.required = false
        sarif.required = false
        txt.required = false
        xml.required = false
    }
    val detektTask = tasks.get<Detekt>("detekt", sourceSet.name)
    classpath.setFrom(detektTask.classpath)
    doFirst {
        println("Analysis report: ${report.absolutePath}")
    }
}

fun tasks(variant: String, version: String, maven: Maven.Artifact, gh: GitHub.Repository) {
    tasks.create("assemble", variant, "MavenMetadata") {
        doLast {
            val target = buildDir().dir("yml").file("maven-metadata.yml")
            val file = maven.assemble(version = version, target = target)
            println("Maven metadata: ${file.absolutePath}")
        }
    }
    tasks.add<Jar>("assemble", variant, "Jar") {
        dependsOn(compileKotlinTask)
        archiveBaseName = maven.id
        archiveVersion = version
        from(compileKotlinTask.destinationDirectory.asFileTree)
    }
    tasks.add<Jar>("assemble", variant, "Source") {
        archiveBaseName = maven.id
        archiveVersion = version
        archiveClassifier = "sources"
        from(sourceSets.main.get().allSource)
    }
    tasks.create("assemble", variant, "Metadata") {
        doLast {
            val target = buildDir().dir("yml").file("metadata.yml")
            val file = gh.assemble(version = version, target = target)
            println("Metadata: ${file.absolutePath}")
        }
    }
}

project.kotlin.target.compilations.getByName("jmh") {
    val issuer = name
    val dir = buildDir().dir("${issuer}Generated")
    val outputSourceDir = dir.asFile("sources")
    val outputResourceDir = dir.asFile("resources")
    val outputClassesDir = dir.dir("classes")
    val generatorType = "default"
    val generators = output.classesDirs.map {
        val compiledBytecodePath = it.absolutePath
        // Usage: generator <compiled-bytecode-dir> <output-source-dir> <output-resource-dir> [generator-type]
        tasks.register<JavaExec>("${issuer}RunBytecodeGenerator${compiledBytecodePath.hashCode()}") {
            dependsOn("classes")
            mainClass.set("org.openjdk.jmh.generators.bytecode.JmhBytecodeGenerator")
            classpath = sourceSets[issuer].runtimeClasspath
            args(
                compiledBytecodePath,
                outputSourceDir.absolutePath,
                outputResourceDir.absolutePath,
                generatorType,
            )
        }
    }
    val compileGeneratedTask = tasks.register<JavaCompile>("${issuer}CompileGenerated") {
        dependsOn(generators)
        classpath = sourceSets[issuer].runtimeClasspath
        source(outputSourceDir)
        destinationDirectory.set(outputClassesDir)
    }
    tasks.register<JavaExec>("runBenchmark") {
        val benchmarks: String? by project
        dependsOn(compileGeneratedTask)
        val reports = buildDir().asFile("reports/jmh")
        doFirst { reports.mkdirs() }
        mainClass.set("org.openjdk.jmh.Main")
        classpath(
            sourceSets[issuer].runtimeClasspath,
            outputResourceDir,
            outputClassesDir,
        )
        val timeout = 10.seconds
        val iterations = 1
        val time = 1.seconds
        val forks = 1
        val wf = 0
//        val wf = 1
        val wi = 1
        val wt = Duration.ZERO
//        val wt = 1.seconds
//        val mode = "Throughput"
        val mode = "AverageTime"
        val format = "text"
        val output = reports.resolve("result.txt")
        args(
            benchmarks.orEmpty(),
            "-to=${timeout.inWholeMilliseconds}ms",
            "-f=$forks",
            "-i=$iterations",
            "-r=${time.inWholeMilliseconds}ms",
            "-wf=$wf",
            "-wi=$wi",
            "-w=${wt.inWholeMilliseconds}ms",
            "-bm=$mode",
//            "-prof=cl",
//            "-prof=comp",
            "-rf=$format",
            "-rff=${output.absolutePath}",
            "-tu=ms",
            "-t=max",
        )
    }
}

"unstable".also { variant ->
    val version = "${version}u-SNAPSHOT"
    tasks(variant = variant, version = version, maven = maven, gh = gh)
    tasks.create("assemble", variant, "Pom") {
        doLast {
            val target = buildDir().dir("libs").file("${maven.name(version = version)}.pom")
            val text = maven.pom(version = version, packaging = "jar")
            val file = target.assemble(text = text)
            println("POM: ${file.absolutePath}")
        }
    }
    tasks.create("check", variant, "Readme") {
        doLast {
            val expected = setOf(
                "GitHub ${Markdown.link(text = version, uri = gh.release(version = version))}",
                "Maven ${Markdown.link("metadata", Maven.Snapshot.metadata(artifact = maven))}",
                "maven(\"${Maven.Snapshot.Host}\")",
                "implementation(\"${maven.moduleName(version = version)}\")",
                "gradle lib:assemble${variant.replaceFirstChar(Char::titlecase)}Jar",
            )
            rootDir.resolve("README.md").check(
                expected = expected,
                report = buildDir()
                    .dir("reports/analysis/readme")
                    .asFile("index.html"),
            )
        }
    }
}

"snapshot".also { variant ->
    val version = "$version-SNAPSHOT"
    tasks(variant = variant, version = version, maven = maven, gh = gh)
    tasks.create("assemble", variant, "Pom") {
        doLast {
            val target = buildDir().dir("libs").file("${maven.name(version = version)}.pom")
            val text = maven.pom(version = version, packaging = "jar")
            val file = target.assemble(text = text)
            println("POM: ${file.absolutePath}")
        }
    }
    tasks.create("check", variant, "Readme") {
        doLast {
            val expected = setOf(
                "GitHub ${Markdown.link(text = version, uri = gh.release(version = version))}",
                "Maven ${Markdown.link("metadata", Maven.Snapshot.metadata(artifact = maven))}",
                "maven(\"${Maven.Snapshot.Host}\")",
                "implementation(\"${maven.moduleName(version = version)}\")",
                "gradle lib:assemble${variant.replaceFirstChar(Char::titlecase)}Jar",
            )
            rootDir.resolve("README.md").check(
                expected = expected,
                report = buildDir()
                    .dir("reports/analysis/readme")
                    .asFile("index.html"),
            )
        }
    }
}

"release".also { variant ->
    val version = version.toString()
    tasks(variant = variant, version = version, maven = maven, gh = gh)
    tasks.create("assemble", variant, "Pom") {
        doLast {
            val target = buildDir().dir("libs").file("${maven.name(version = version)}.pom")
            val license = gh.uri("blob/$version/LICENSE")
            val developer = "Stanley Wintergreen" // todo
            val text = maven.pom(
                version = version,
                packaging = "jar",
                uri = gh.uri(),
                licenses = setOf(license),
                scm = gh.uri(),
                tag = version,
                developers = setOf(developer),
            )
            val file = target.assemble(text = text)
            println("POM: ${file.absolutePath}")
        }
    }
    tasks.create("check", variant, "Readme") {
        doLast {
            val expected = setOf(
                Markdown.link(text = "GitHub", uri = gh.release(version = version)),
                Markdown.link(text = "Maven", uri = maven.uri(version = version)),
                Markdown.link(text = "Docs", uri = gh.pages("docs/$version")),
                "implementation(\"${maven.moduleName(version = version)}\")",
                "gradle lib:assemble${variant.replaceFirstChar(Char::titlecase)}Jar",
            )
            rootDir.resolve("README.md").check(
                expected = expected,
                report = buildDir()
                    .dir("reports/analysis/readme")
                    .asFile("index.html"),
            )
        }
    }
    val docsTask = tasks.add<DokkaTask>("assemble", variant, "Docs") {
        outputDirectory = buildDir().dir("docs/$variant")
        moduleName = gh.name
        moduleVersion = version
        dokkaSourceSets.getByName("main") {
            val path = "src/$name/kotlin"
            reportUndocumented = false
            sourceLink {
                localDirectory = file(path)
                remoteUrl = gh.uri("tree/${moduleVersion.get()}/lib/$path").toURL()
            }
            jdkVersion = Version.jvmTarget.toInt()
        }
        doLast {
            val index = outputDirectory.get().eff("index.html")
            println("Docs: ${index.absolutePath}")
        }
    }
    tasks.add<Jar>("assemble", variant, "Javadoc") {
        dependsOn(docsTask)
        archiveBaseName = maven.id
        archiveVersion = version
        archiveClassifier = "javadoc"
        from(docsTask.outputDirectory)
    }
}
