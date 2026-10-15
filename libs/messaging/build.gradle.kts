plugins { `java-library` }

dependencies {
    api(platform(libs.spring.boot.bom))
    api(project(":libs:contracts"))
    api("org.springframework.boot:spring-boot-starter-jdbc")
    api("org.springframework.boot:spring-boot-starter-json")
    api("org.springframework.kafka:spring-kafka")
}
