FROM adoptopenjdk/openjdk11:latest

COPY target/uberjar/sample3.jar /sample3/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/sample3/app.jar"]
