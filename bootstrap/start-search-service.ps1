Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$PSDefaultParameterValues["*:ErrorAction"] = "Stop"

$docker_network = "sitesearch"
$service_name = "sitesearch-search-service"
$docker_image = "intrafind/$service_name"

docker network create $docker_network

docker load -i "~/tmp/$service_name.tar"

sudo chown -R 1000:1000 ~/srv/$service_name
sudo chmod -R 744 ~/srv/$service_name

docker rm -f $service_name
docker run -d --name $service_name --network $docker_network `
    -p 9605:9605 `
    -v $HOME/srv/$service_name/logs:/home/app_user/logs `
    $docker_image