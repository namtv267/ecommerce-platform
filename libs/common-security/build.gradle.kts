plugins { `java-library` }

dependencies {
    api(platform(libs.spring.boot.bom))
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    compileOnly("org.springframework.boot:spring-boot-starter-data-redis")
}
