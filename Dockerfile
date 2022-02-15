FROM adoptopenjdk/openjdk11:alpine-jre

ARG version=0.0.1
ARG tag=undefined

LABEL app=turf-route
LABEL version=$version
LABEL tag=$tag

EXPOSE 8080

WORKDIR /

ENV DB_URL="jdbc:mariadb://localhost:3306/turf_route"
ENV DB_PASS="redacted"
ENV START_MEM="256m"
ENV MAX_MEM="1024m"

COPY build/libs/turf-route-${version}.jar turf-route.jar

CMD java -Xms$START_MEM -Xmx$MAX_MEM -jar turf-route.jar
