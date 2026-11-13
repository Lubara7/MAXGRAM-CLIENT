plugins { `java-library` }

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.msgpack:msgpack-core:0.9.10")
    implementation("org.lz4:lz4-java:1.8.0")
    implementation("io.airlift:aircompressor:0.27")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

tasks.register("writeAdapterClasspath") {
    dependsOn(tasks.jar)
    doLast {
        layout.buildDirectory.file("adapter-classpath.txt").get().asFile.writeText(
            files(tasks.jar.get().archiveFile, configurations.runtimeClasspath).asPath
        )
    }
}
