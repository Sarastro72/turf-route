tag=test
push=false
# Multi-platform builds need a docker-container builder; the default docker driver can't do them
builder=container-builder

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
    docker buildx inspect $builder > /dev/null 2>&1 || \
    docker buildx create --name $builder --driver docker-container > /dev/null && \
    docker buildx build --builder $builder --platform linux/amd64,linux/arm64 -t sarastro72/turf-route:$tag --push . && \
    echo "Deployed sarastro72/turf-route:$tag to docker hub"
else
    docker build -t sarastro72/turf-route:$tag . && \
    echo "Built sarastro72/turf-route:$tag"
fi
