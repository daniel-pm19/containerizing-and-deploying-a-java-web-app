FROM amazoncorretto:21

WORKDIR /app

COPY target/*.jar app.jar

ENV PORT=8000

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "app.jar"]