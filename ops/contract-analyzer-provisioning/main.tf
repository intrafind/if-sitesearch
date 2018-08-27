module "upstream" {
  source = "modules/upstream/internet_module"
}

// Can be added only once per project, which leads to a singleton problem.
// Hence hcloud_ssh_key, should be a default workspace singleton only.
//resource "hcloud_ssh_key" "minion" {
//  name = "minion"
//  public_key = "${file("~/.ssh/if-minion-id_rsa.pub")}"
//}

variable "tenant" {
  type = "string"
  default = "node"
  description = "Customer Name"
}

variable "password" {
  type = "string"
  default = "invalid dummy"
  description = "Default Password"
}

variable "hetzner_cloud_analyze_law" {
  type = "string"
  default = "invalid dummy"
  description = "Analyze Law API Token"
}

locals {
  hcloud_token = "${var.hetzner_cloud_analyze_law}"
  server_image = "ubuntu-18.04"
  datacenter_prefix = "nbg1"
}

//output "floating_ip" "IPv4" {
//  value = "${hcloud_floating_ip.main.ip_address}"
//  sensitive = false
//}

output "ip" "IPv4" {
  value = "${hcloud_server.node.ipv4_address}"
  sensitive = false
}

output "workspace" "server" {
  value = "${terraform.workspace}-${var.tenant}"
}

provider "hcloud" "Hetzner" {
  token = "${var.hetzner_cloud_analyze_law}"
}

resource "hcloud_server" "node" {
  name = "${terraform.workspace}-${var.tenant}-${count.index}"
  count = "1"
  datacenter = "${local.datacenter_prefix}-dc3"
  image = "${local.server_image}"
  server_type = "cx31-ceph"
  ssh_keys = [
    //    "minion",
    "alex",
    "bachka",
  ]

  provisioner "file" "al-license" {
    source = "~/my/project/intrafind/docker-container/intrafind-dev.license"
    destination = "/srv/al-contract-analyzer.license"
  }

  provisioner "file" "al-demo-data" {
    source = "~/my/project/intrafind/docker-container/al-demo-data.tgz"
    destination = "/opt/al-demo-data.tgz"
  }

  //  provisioner "file" "iFinder" {
  //    source = "~/Desktop/contract-analyzer/iFinder5-Installer/iFinder_linux_v2.zip"
  //    destination = "/srv/iFinder_linux_v2.zip"
  ////    connection {
  ////      type = "ssh"
  ////      user = "root"
  ////      host = "195.201.100.226"
  ////      private_key = "${file("~/.ssh/id_rsa")}"
  ////    }
  //  }

  provisioner "remote-exec" "install" {
    inline = [
      "sleep 20 && apt-get update && apt-get install docker.io certbot -y",
      "docker login docker-registry.sitesearch.cloud --username sitesearch --password ${var.password}",
      "docker network create main",
      "cd /opt && tar xfz al-demo-data.tgz && chmod -R 777 /opt/al-demo-data && mv /opt/al-demo-data/volumes/analyzelaw_esdata1/_data /opt/al-data",
      "docker run --name al-elasticsearch -d -v /opt/al-data:/usr/share/elasticsearch/data --env discovery.type=single-node --restart unless-stopped --network main docker.elastic.co/elasticsearch/elasticsearch-oss:6.2.4",
      "docker run --name al-api -d -p 8080:8080 --restart unless-stopped --network main docker-registry.sitesearch.cloud/intrafind/al-api:latest",
      "docker run --name al-tagger -d -v /srv/contract-analyzer:/srv/contract-analyzer -p 9603:9603 --network main docker-registry.sitesearch.cloud/intrafind/al-tagger:release",
      "docker run --name al-router -d -p 443:443 --restart unless-stopped --network main docker-registry.sitesearch.cloud/intrafind/al-router:latest",
      "docker run --name al-ui -d -p 80:80 --restart unless-stopped --network main docker-registry.sitesearch.cloud/intrafind/al-ui:latest",
      "docker ps",
      "sed -i -e 's/%sudo\tALL=(ALL:ALL) ALL/%sudo\tALL=(ALL:ALL) NOPASSWD:ALL/g' /etc/sudoers",
      "adduser --disabled-password --gecos '' minion && usermod -aG sudo minion && usermod --lock minion",
      "docker exec -it al-api ./ingest-essential-data.sh",
      //      "java -jar if-sv-clausedetection-0.0.0.2-SNAPSHOT-jar-with-dependencies.jar -mode excel -convert http://localhost:9602/hessian/converter -tag http://localhost:9603/hessian/tagger -mode excel -excelformat multiple -categorize None -input ./input"
    ]
  }
}

provider "docker" "container runtime" {
  host = "unix:///var/run/docker.sock"
}

resource "docker_container" "foo" {
  image = "${docker_image.ubuntu.latest}"
  name = "my"
}

resource "docker_image" "ubuntu" {
  name = "ubuntu:latest"
}

provider "google" "GCE Cloud" {
  credentials = "${file("~/.ssh/analyze-law-owner-service-account.json")}"
  project = "analyze-law"
}

data "google_dns_managed_zone" "analyze-law" {
  name = "analyzelaw-com"
}

resource "google_dns_record_set" "tenant-domain" {
  name = "${var.tenant}.${data.google_dns_managed_zone.analyze-law.dns_name}"
  type = "A"
  ttl = 300

  managed_zone = "${data.google_dns_managed_zone.analyze-law.name}"

  rrdatas = [
    //    "${hcloud_floating_ip.main.ip_address}",
    "${hcloud_server.node.ipv4_address}",
  ]

  provisioner "local-exec" {
    command = "ssh root@${hcloud_server.node.ipv4_address}"
  }
}

//resource "hcloud_floating_ip" "main" {
//  type = "ipv4"
//  server_id = "${hcloud_server.node.id}"
//  home_location = "${local.datacenter_prefix}"
//
//  provisioner "remote-exec" "setup" {
//    inline = [
//      "ip addr add ${hcloud_floating_ip.main.ip_address} dev eth0",
//      "docker run --name al-tagger -d -v /srv/contract-analyzer:/srv/contract-analyzer -p 9603:9603 docker-registry.sitesearch.cloud/intrafind/al-tagger:release",
//      "docker run --name al-api -d -p 8001:8001 docker-registry.sitesearch.cloud/intrafind/if-sitesearch",
//    ]
//
//    connection {
//      host = "${hcloud_server.node.ipv4_address}"
//    }
//  }
//}
