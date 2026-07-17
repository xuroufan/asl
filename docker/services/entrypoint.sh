#!/bin/bash
set -e
JAVA_OPTS="-Xmx256m"
SPRING_OPTS="--spring.cloud.nacos.discovery.enabled=false --spring.cloud.nacos.config.enabled=false --seata.enabled=false"
EXCLUDES="--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.LettuceConnectionConfiguration,org.springframework.boot.actuate.autoconfigure.redis.RedisHealthContributorAutoConfiguration"
CONFIG="--spring.config.additional-location=file:config/health-fix.yml"

if [ -n "$EXTRA_EXCLUDES" ]; then
    EXCLUDES="$EXCLUDES,$EXTRA_EXCLUDES"
fi

echo "Starting ASL Futures Service..."
echo "Extra excludes: ${EXTRA_EXCLUDES:-none}"
exec java $JAVA_OPTS -jar app.jar $SPRING_OPTS $EXCLUDES $CONFIG "$@"
