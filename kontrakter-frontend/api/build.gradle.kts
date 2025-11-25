openApiGenerate {
    globalProperties.put("apis", "")
}

dependencies {
    compileOnly("org.springframework.boot:spring-boot-starter-web:3.5.4")
    api(project(":kontrakter-frontend:dtoer"))
}
