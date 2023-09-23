plugins {
    `java-library`
    signing
    `maven-publish`
}

data class ProjPublication(
    val name: String,
    val usernameFrom: String,
    val passwordFrom: String,
    val snapshotRepo: String,
    val releaseRepo: String,
    val snapshotPredicate: (String) -> Boolean = { it.endsWith("-SNAPSHOT") }
)

val hasPublication: String by rootProject
val publicationSigning: String by rootProject
val publication: ProjPublication? = if (hasPublication.toBoolean()) ProjPublication(
    name = "OSSRH",
    usernameFrom = "OSSRH_USERNAME",
    passwordFrom = "OSSRH_PASSWORD",
    snapshotRepo = "https://s01.oss.sonatype.org/content/repositories/snapshots/",
    releaseRepo = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
) else null

val hasJavadocJar: String by rootProject
val hasSourcesJar: String by rootProject

val projGroupId: String by rootProject
val projArtifactId: String by rootProject
val projName: String by rootProject
val projVersion: String by rootProject
val projDesc: String by rootProject
val projUrl: String? by rootProject
val projLicenseUrl: String? by rootProject
val projScmConnection: String? by rootProject
val projScmUrl: String? by rootProject
val projLicense: String by rootProject
val projLicenseFileName: String by rootProject

val orgName: String by rootProject
val orgUrl: String by rootProject

val jdkVersion: String by rootProject
val jdkEnablePreview: String by rootProject
val jdkEarlyAccessDoc: String? by rootProject

val targetJavaVersion = jdkVersion.toInt()

group = projGroupId
version = projVersion

repositories {
    mavenCentral()
    // snapshot repositories
    // maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    // maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots") }

    // maven { url = uri("https://oss.oss.sonatype.org/content/repositories/releases") }
    // maven { url = uri("https://s01.oss.sonatype.org/content/repositories/releases") }
}

dependencies {
    // add your dependencies
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release = targetJavaVersion
    }
    if (jdkEnablePreview.toBoolean()) options.compilerArgs.add("--enable-preview")
}

tasks.withType<Test> {
    if (jdkEnablePreview.toBoolean()) jvmArgs("--enable-preview")
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
    if (hasJavadocJar.toBoolean()) withJavadocJar()
    if (hasSourcesJar.toBoolean()) withSourcesJar()
}

tasks.withType<Javadoc> {
    isFailOnError = false
    options {
        encoding = "UTF-8"
        locale = "en_US"
        windowTitle = "$projName $projVersion Javadoc"
        if (this is StandardJavadocDocletOptions) {
            charSet = "UTF-8"
            isAuthor = true
            if (jdkEarlyAccessDoc == null) {
                links("https://docs.oracle.com/en/java/javase/$targetJavaVersion/docs/api/")
            } else {
                links("https://download.java.net/java/early_access/$jdkEarlyAccessDoc/docs/api/")
            }
        }
    }
}

tasks.named<Jar>("jar") {
    manifestContentCharset = "utf-8"
    setMetadataCharset("utf-8")
    manifest.attributes(
        "Specification-Title" to projName,
        "Specification-Vendor" to orgName,
        "Specification-Version" to projVersion,
        "Implementation-Title" to projName,
        "Implementation-Vendor" to orgName,
        "Implementation-Version" to projVersion
    )
}

if (hasSourcesJar.toBoolean()) {
    tasks.named<Jar>("sourcesJar") {
        dependsOn(tasks["classes"])
        archiveClassifier = "sources"
        from(sourceSets["main"].allSource)
    }
}

if (hasJavadocJar.toBoolean()) {
    tasks.named<Jar>("javadocJar") {
        val javadoc by tasks
        dependsOn(javadoc)
        archiveClassifier = "javadoc"
        from(javadoc)
    }
}

tasks.withType<Jar> {
    archiveBaseName = projArtifactId
    from(projLicenseFileName).rename { "${projLicenseFileName}_$projArtifactId" }
}

if (hasPublication.toBoolean() && publication != null) {
    publishing.publications {
        register<MavenPublication>("mavenJava") {
            groupId = projGroupId
            artifactId = projArtifactId
            version = projVersion
            description = projDesc
            from(components["java"])
            pom {
                name = projName
                description = projDesc
                projUrl?.also { url = it }
                licenses {
                    license {
                        name = projLicense
                        url = projLicenseUrl
                    }
                }
                organization {
                    name = orgName
                    url = orgUrl
                }
                scm {
                    projScmConnection?.also {
                        connection = it
                        developerConnection = it
                    }
                    projScmUrl?.also { url = it }
                }
            }
        }
    }

    publishing.repositories {
        maven {
            name = publication.name
            credentials {
                username = rootProject.findProperty(publication.usernameFrom).toString()
                password = rootProject.findProperty(publication.passwordFrom).toString()
            }
            url = uri(
                if (publication.snapshotPredicate(projVersion)) publication.snapshotRepo
                else publication.releaseRepo
            )
        }
    }

    signing {
        if (!publication.snapshotPredicate(projVersion) && publicationSigning.toBoolean()) {
            sign(publishing.publications["mavenJava"])
        }
    }
}
