read -p "DB Host: " DB_HOST
read -p "DB Port: " DB_PORT
read -p "DB User: " DB_USER
read -s -p "DB Password: " DB_PASS
echo
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-3306}
export DB_URL="jdbc:mariadb://${DB_HOST}:${DB_PORT}/turf_route"
export DB_USER=${DB_USER:-test}
export DB_PASS=${DB_PASS:-so_secret}
java -jar build/libs/turf-route-0.0.1.jar
