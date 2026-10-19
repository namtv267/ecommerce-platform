plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

tasks.named<Jar>("jar") { enabled = false }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // ClickHouse + MyBatis (không dùng JPA)
    implementation(libs.mybatis.spring.boot.starter)
    implementation(libs.clickhouse.jdbc)
    implementation("com.clickhouse:clickhouse-client:0.6.5")
    implementation("com.clickhouse:clickhouse-http-client:0.6.5")
    implementation("org.apache.httpcomponents.client5:httpclient5")
    implementation("org.lz4:lz4-java:1.8.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
