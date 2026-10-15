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
    implementation(variantOf(libs.clickhouse.jdbc) { classifier("all") })

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
