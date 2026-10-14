# ===== 1) BUILD STAGE =====
# 빌드에 필요한 JDK 21 환경을 기반으로 시작합니다.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Gradle 의존성 캐싱 (소스코드 변경 시 매번 라이브러리를 받지 않도록 최적화)
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

# 소스 복사 후 실행 가능한 .jar 파일로 빌드
COPY . .
RUN ./gradlew clean bootJar --no-daemon

# ===== 2) RUNTIME STAGE =====
# 실행에 필요한 가벼운 JRE 21 환경만 가져옵니다.
FROM eclipse-temurin:21-jre
ENV TZ=Asia/Seoul \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Seoul"
WORKDIR /opt/app

# 1단계(build)에서 완성된 결과물 .jar 파일만 쏙 복사 (이미지 용량 최소화)
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# (Actuator 없으면 HEALTHCHECK는 없어도 OK)
# HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]