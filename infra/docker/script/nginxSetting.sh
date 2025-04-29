#!/bin/bash

# Nginx를 기본 설정으로 실행 (SSL 인증서가 준비될 때까지 대기)
echo "Starting Nginx with default config..."
nginx -g "daemon off;" &
echo "Nginx started, waiting for SSL certificate..."

# 인증서 디렉토리 확인
echo "Checking if SSL certificates exist..."

if [ ! -f "/etc/letsencrypt/live/app.sangchu.xyz/fullchain.pem" ]; then
    echo "No certificate found, requesting certificate from certbot..."

    # certbot을 실행하여 인증서를 발급받음
    certbot certonly --webroot --webroot-path=/var/www/certbot --email ${CERTBOT_EMAIL} --agree-tos --no-eff-email -d app.sangchu.xyz

    # 인증서가 발급된 후 확인
    if [ -f "/etc/letsencrypt/live/app.sangchu.xyz/fullchain.pem" ]; then
        echo "Certificate issued successfully by certbot."
    else
        echo "Failed to issue certificate."
        sleep 1000000
        exit 1
    fi
else
    echo "Certificate found, skipping certbot."
fi

# 인증서가 생성되면 Nginx 설정 파일을 config.conf로 교체
echo "Updating Nginx configuration to use SSL..."
mv config.conf /etc/nginx/conf.d/default.conf

sudo chmod 777 /home/ubuntu/LLL3/certbot/conf/accounts

# Nginx를 다시 시작하여 새로운 SSL 인증서와 설정을 적용
echo "Restarting Nginx with the new SSL config..."
nginx -s reload

# 이후에는 Nginx가 계속 실행되도록 함
wait
