tag=test
push=false

while getopts "pt:" opt; do
  case $opt in
    p)
      push=true
      ;;
    t)
      tag=$OPTARG
      ;;
    \?)
      echo "Invalid option: -$OPTARG"
      ;;
  esac
done

./gradlew clean build && \

if $push; then
    docker buildx build --platform linux/amd64,linux/arm64 -t sarastro72/turf-route:$tag --push .
    echo "Deployed sarastro72/turf-route:$tag to docker hub"
else
    docker build -t sarastro72/turf-route:$tag . && \
    echo "Built sarastro72/turf-route:$tag"
fi
