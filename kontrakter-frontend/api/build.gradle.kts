openApiGenerate {
    globalProperties.put("apis", "")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-web:4.0.0")
    api(project(":kontrakter-frontend:dtoer"))
}
