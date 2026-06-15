openApiGenerate {
    globalProperties.put("apis", "")
}

dependencies {
    compileOnly("org.springframework.boot:spring-boot-starter-web:4.1.0")
    api(project(":kontrakter-frontend:dtoer"))
}
