openApiGenerate {
    globalProperties.put("apis", "")
}

dependencies {
    compileOnly("org.springframework.boot:spring-boot-starter-web:4.0.2")
    api(project(":kontrakter-frontend:dtoer"))
}
