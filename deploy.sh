TAG=${1:-alpha}
gradle clean build && \
docker build -t sarastro72/turf-route:$TAG . && \
docker push sarastro72/turf-route:$TAG
echo "Deployed sarastro72/turf-route:$TAG"
