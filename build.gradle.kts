
plugins {
    java
    application

    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.nikipo"
version = "1.0.0"

repositories {
    mavenCentral()
}

javafx {
    version = "21.0.8"

    modules = listOf(
        "javafx.controls",
        "javafx.fxml"
    )
}
dependencies {

    // ============================
    // JavaFX
    // ============================

    implementation("org.openjfx:javafx-controls:21.0.8")
    implementation("org.openjfx:javafx-fxml:21.0.8")

    // ============================
    // Video (FFmpeg)
    // ============================

    implementation("org.bytedeco:javacv-platform:1.5.12")

    // ============================
    // JSON
    // ============================

    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.0")

    // ============================
    // CSV
    // ============================

    implementation("org.apache.commons:commons-csv:1.14.1")

    // ============================
    // Extra JavaFX Controls
    // ============================

    implementation("org.controlsfx:controlsfx:11.2.2")

    // ============================
    // File Utilities
    // ============================

    implementation("commons-io:commons-io:2.20.0")

    // ============================
    // Logging
    // ============================

    implementation("org.slf4j:slf4j-api:2.0.17")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.18")

    // ============================
    // App Directories
    // ============================

    implementation("net.harawata:appdirs:1.2.2")

    // ============================
    // Testing
    // ============================

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.nikipo.ardupilotoverlay.App")
}
