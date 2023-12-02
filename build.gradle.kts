plugins {
    `java-library`
    signing
    `maven-publish`
    //application
}

data class PublicationRepo(
    val name: String,
    val usernameFrom: List<String>,
    val passwordFrom: List<String>,
    val snapshotRepo: String,
    val releaseRepo: String,
    val snapshotPredicate: (String) -> Boolean = { it.endsWith("-SNAPSHOT") }
)

val hasPublication: String by rootProject
val publicationSigning: String by rootProject
val publicationRepo: PublicationRepo? = if (hasPublication.toBoolean()) PublicationRepo(
    name = "OSSRH",
    usernameFrom = listOf("OSSRH_USERNAME", "ossrhUsername"),
    passwordFrom = listOf("OSSRH_PASSWORD", "ossrhPassword"),
    snapshotRepo = "https://s01.oss.sonatype.org/content/repositories/snapshots/",
    releaseRepo = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
) else null

data class Organization(
    val name: String,
    val url: String
)

data class Developer(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val url: String? = null,
    val organization: Organization? = projOrg,
    val roles: Set<String>? = null,
    val timezone: String? = null,
    val properties: Map<String, String?>? = null
)

val projDevelopers = arrayOf(
    Developer("example")
)

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
val projOrg = Organization(orgName, orgUrl)

val jdkVersion: String by rootProject
val jdkEnablePreview: String by rootProject
val jdkEarlyAccessDoc: String? by rootProject

val targetJavaVersion = jdkVersion.toInt()

group = projGroupId
version = projVersion

repositories {
    mavenCentral()
    // snapshot repositories
    //maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    //maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots") }

    //maven { url = uri("https://oss.oss.sonatype.org/content/repositories/releases") }
    //maven { url = uri("https://s01.oss.sonatype.org/content/repositories/releases") }
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
                links("https://docs.oracle.com/en/java/javase/$jdkVersion/docs/api/")
            } else {
                links("https://download.java.net/java/early_access/$jdkEarlyAccessDoc/docs/api/")
            }
            if (jdkEnablePreview.toBoolean()) {
                addBooleanOption("-enable-preview", true)
                addStringOption("source", jdkVersion)
            }
        }
    }
}

//application {
//    applicationName = projName
//    mainClass = "org.example.Main"
//}

tasks.named<Jar>("jar") {
    manifestContentCharset = "utf-8"
    setMetadataCharset("utf-8")
    manifest.attributes(
        "Specification-Title" to projName,
        "Specification-Vendor" to projOrg.name,
        "Specification-Version" to projVersion,
        "Implementation-Title" to projName,
        "Implementation-Vendor" to projOrg.name,
        "Implementation-Version" to projVersion
        //"Main-Class" to "org.example.Main"
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
    from(rootProject.file(projLicenseFileName)).rename(projLicenseFileName, "${projLicenseFileName}_$projArtifactId")
}

if (hasPublication.toBoolean() && publicationRepo != null) {
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
                    name = projOrg.name
                    url = projOrg.url
                }
                developers {
                    projDevelopers.forEach {
                        developer {
                            id = it.id
                            it.name?.also { name = it }
                            it.email?.also { email = it }
                            it.url?.also { url = it }
                            it.organization?.also {
                                organization = it.name
                                organizationUrl = it.url
                            }
                            it.roles?.also { roles = it }
                            it.timezone?.also { timezone = it }
                            it.properties?.also { properties = it }
                        }
                    }
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
            name = publicationRepo.name
            credentials {
                username = publicationRepo.usernameFrom.firstNotNullOf { rootProject.findProperty(it) }.toString()
                password = publicationRepo.passwordFrom.firstNotNullOf { rootProject.findProperty(it) }.toString()
            }
            url = uri(
                if (publicationRepo.snapshotPredicate(projVersion)) publicationRepo.snapshotRepo
                else publicationRepo.releaseRepo
            )
        }
    }

    signing {
        if (!publicationRepo.snapshotPredicate(projVersion) && publicationSigning.toBoolean()) {
            sign(publishing.publications["mavenJava"])
        }
    }
}
