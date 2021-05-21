read -p "DB Host: " DB_HOST
read -p "DB Password: " DB_PASS
DB_HOST=${DB_HOST:localhost}
export DB_URL="jdbc:mariadb://${DB_HOST}:3306/turf_route"
export DB_PASS=${DB_PASS:-so_secret}
java -jar build/libs/turf-route-0.0.1.jar
