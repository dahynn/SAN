FROM eclipse-temurin:21-jdk-alpine AS build

# working directory를 /app으로 설정
WORKDIR /app

# gradle 파일들을 /app으로 복사, 권한 설정
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN sed -i 's/\r//' gradlew && chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스 파일 복사 및 빌드
COPY src ./src
RUN ./gradlew build -x test --no-daemon

# JRE
FROM eclipse-temurin:21-jre-alpine

# curl 설치
RUN apk add --no-cache curl

# working directory를 /app으로 설정
WORKDIR /app

# 생성된 파일을 /app에 복사
COPY --from=build /app/build/libs/app.jar app.jar

# 포트 노출 및 실행
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
