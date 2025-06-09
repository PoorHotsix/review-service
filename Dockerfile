# multi-stage build를 사용하여 Spring Boot 애플리케이션을 Docker 이미지로 패키징

# 1단계 : 빌드
FROM openjdk:17-slim as build

WORKDIR /application

# ARG : Dockerfile에서 빌드 타임(Build time)에 값을 설정할 수 있는 변수를 정의하는 명령어
ARG JAR_FILE=build/libs/review-service-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE}  application.jar

RUN java -Djarmode=layertools -jar application.jar extract

# 2단계 : 실행

FROM openjdk:17-slim

WORKDIR /application

COPY --from=build application/dependencies/ ./
COPY --from=build application/spring-boot-loader/ ./
COPY --from=build application/snapshot-dependencies/ ./
COPY --from=build application/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]