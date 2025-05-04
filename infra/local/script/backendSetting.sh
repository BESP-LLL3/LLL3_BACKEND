#!/bin/bash

# Elasticsearch가 준비될 때까지 대기
until curl -s http://elasticsearch:9200 > /dev/null; do
  echo "Waiting for Elasticsearch to be ready..."
  sleep 5
done

# Elasticsearch 준비 완료 후 애플리케이션 실행
exec java -jar /app/app.jar --spring.profiles.active=${PROFILE}
