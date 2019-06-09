FROM maven:3.6.1-jdk-8-slim as builder

WORKDIR /build
COPY . .
RUN mkdir /package 
RUN mvn clean package -U
RUN cp ./target/*.jar /package/gcr-gc.jar

FROM google/cloud-sdk:249.0.0-alpine 

WORKDIR /app

RUN apk update \
    && apk add openjdk8 \
    && rm -rf /var/cache/apk/*

COPY --from=builder /package/gcr-gc.jar /app/gcr-gc.jar

ENTRYPOINT [ "java","-jar","/app/gcr-gc.jar" ]

