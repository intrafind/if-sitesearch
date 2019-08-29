terraform {
  backend "gcs" {
    bucket = "intrafind-ops"
    prefix = "terraform"
  }
}
resource "hcloud_network" "cluster" {
  name     = "k8s-${terraform.workspace}"
  ip_range = "10.0.0.0/8"
}
resource "hcloud_network_route" "ingres" {
  network_id  = hcloud_network.cluster.id
  destination = "10.1.1.0/24"
  gateway     = "10.0.2.1"
}
resource "hcloud_network_subnet" "tenant" {
  network_id   = hcloud_network.cluster.id
  type         = "server"
  network_zone = "eu-central"
  ip_range     = "10.0.1.0/24"
}
//resource "hcloud_server_network" "node" {
//  network_id = hcloud_network.cluster.id
//  server_id  = hcloud_server.node[0].id
//  ip         = "10.0.1.20"
//}
//resource "hcloud_server_network" "master" {
//  network_id = hcloud_network.cluster.id
//  server_id  = hcloud_server.master[0].id
//  ip         = "10.0.1.10"
//}
variable "volumeHandle" {
  type        = string
  description = "Hetzner Volume ID to use for re-attachment"
  default     = ""
}
variable "docker_registry_k8s_secret" {
  type    = string
  default = ""
}
resource "null_resource" "update-migration" {
  depends_on = [
    hcloud_server.node
  ]
  connection {
    host        = hcloud_server.master[0].ipv4_address
    private_key = file("~/.ssh/id_rsa")
  }
  triggers = {
    updateTrigger = local.updateTrigger
  }
  provisioner "remote-exec" {
    inline = [
      "kubectl create secret docker-registry docker-registry --docker-server=docker-registry.intrafind.net --docker-username=sitesearch --docker-password=${var.password}",
      "helm install /srv/asset/sis-sitesearch --name sis-sitesearch --namespace ${terraform.workspace} --set app.tenant=${terraform.workspace},app.HETZNER_API_TOKEN=${var.hetzner_cloud_intrafind},app.password=${var.password},app.dockerRegistrySecret=${var.docker_registry_k8s_secret} --set-string app.volumeHandle=${var.volumeHandle}",
      "helm install --name ingress stable/nginx-ingress --set rbac.create=true,controller.hostNetwork=true,controller.kind=DaemonSet",
      "kubectl get svc,node,pvc,deployment,pods,pvc,pv,namespace,job -A && helm list",
    ]
  }
}

variable "password" {
  type = string
}

variable "hetzner_cloud_intrafind" {
  type = string
}

locals {
  //  dcLocation    = "nbg1"
  dc            = "nbg1-dc3"
  updateTrigger = timestamp()
  password      = var.password == "" ? uuid() : var.password
}
output "web" {
  value = "http://${hcloud_server.master.0.ipv4_address}:8001"
}
output "k8s_master" {
  value = hcloud_server.master.*.ipv4_address
}
output "k8s_master_node" {
  value = hcloud_server.master.0.ipv4_address
}
output "k8s_node" {
  value = hcloud_server.node.*.ipv4_address
}
output "password" {
  value = local.password
}
output "updateTrigger" {
  value = local.updateTrigger
}
output "k8s_ssh" {
  value = "ssh -q -o StrictHostKeyChecking=no root@${hcloud_server.master[0].ipv4_address}"
}
provider "hcloud" {
  token = var.hetzner_cloud_intrafind
}
variable "nodeCount" {
  type    = number
  default = 1
}
variable "masterCount" {
  type    = number
  default = 1
}
resource "hcloud_server" "node" {
  //  location = local.dcLocation
  labels = {
    password = local.password
  }
  name  = "sis-${terraform.workspace}-node-${count.index}"
  count = var.nodeCount
  image = "debian-9"
  // Debian 10 does not work with Hetzner volumes yet
  server_type = "cx21-ceph"
  ssh_keys = [
    "alex",
    "minion",
  ]

  provisioner "local-exec" {
    command = "cat << EOF >> ~/.bash_ssh_connections\nalias sis-${terraform.workspace}-node-${count.index}='ssh -o StrictHostKeyChecking=no root@${self.ipv4_address}'\n"
  }

  provisioner "remote-exec" {
    connection {
      host        = self.ipv4_address
      private_key = file("~/.ssh/id_rsa")
    }

    inline = [
      "echo 'root:${local.password}' | chpasswd",
      //      "apt-get update && sleep 2 && apt-get install curl software-properties-common -y",
      "curl -s https://download.docker.com/linux/debian/gpg | apt-key add -",
      "curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add -",
      "echo 'deb [arch=amd64] https://packages.cloud.google.com/apt kubernetes-xenial main' >> /etc/apt/sources.list",
      "echo \"deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable\" >> /etc/apt/sources.list",
      "apt-get update && apt-get install rsync docker-ce kubeadm sshpass busybox -y",
      "containerd config default > /etc/containerd/config.toml && systemctl restart containerd",
      "sshpass -p ${local.password} scp -o StrictHostKeyChecking=no root@${hcloud_server.master.0.ipv4_address}:/srv/kubeadm_join /tmp && eval $(cat /tmp/kubeadm_join)",
    ]
  }
}

resource "hcloud_server" "master" {
  //  location = local.dcLocation
  labels = {
    password = local.password
  }
  name  = "sis-${terraform.workspace}-master-${count.index}"
  count = var.masterCount
  image = "debian-9"
  // Debian 10 does not work with Hetzner volumes yet
  server_type = "cx21-ceph"
  ssh_keys = [
    "alex",
    "minion",
  ]

  provisioner "local-exec" {
    command = "cat << EOF >> ~/.bash_ssh_connections\nalias sis-${terraform.workspace}='ssh -o StrictHostKeyChecking=no root@${self.ipv4_address}'\n"
  }

  provisioner "file" {
    connection {
      host        = self.ipv4_address
      private_key = file("~/.ssh/id_rsa")
    }
    source      = "asset"
    destination = "/srv/asset"
  }

  provisioner "remote-exec" {
    connection {
      host        = self.ipv4_address
      private_key = file("~/.ssh/id_rsa")
    }

    inline = [
      "echo 'root:${local.password}' | chpasswd",
      //      "apt-get update && sleep 1 && apt-get install curl software-properties-common -y",
      "curl -s https://download.docker.com/linux/debian/gpg | apt-key add -",
      "curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add -",
      "echo 'deb [arch=amd64] https://packages.cloud.google.com/apt kubernetes-xenial main' >> /etc/apt/sources.list",
      "echo \"deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable\" >> /etc/apt/sources.list",
      "apt-get update && apt-get install rsync docker-ce kubeadm -y",
      "containerd config default > /etc/containerd/config.toml && systemctl restart containerd",
      "kubeadm init --cri-socket /run/containerd/containerd.sock",
      "mkdir -p $HOME/.kube && cp -i /etc/kubernetes/admin.conf $HOME/.kube/config && chown $(id -u):$(id -g) $HOME/.kube/config",
      "kubectl apply -f https://docs.projectcalico.org/v3.8/getting-started/kubernetes/installation/hosted/kubernetes-datastore/calico-networking/1.7/calico.yaml",
      "kubectl apply -f https://raw.githubusercontent.com/hetznercloud/csi-driver/master/deploy/kubernetes/hcloud-csi.yml",
      "kubectl taint nodes --all node-role.kubernetes.io/master- # override security and enable scheduling of pods on master",
      "echo $(kubeadm token create --print-join-command) --cri-socket /run/containerd/containerd.sock > /srv/kubeadm_join",
      "kubectl apply -f /srv/asset/init-helm-rbac-config.yaml",
      "curl -L https://git.io/get_helm.sh | bash && helm init --service-account tiller --upgrade",
    ]
  }
}

//resource "hcloud_rdns" "node" {
//  server_id  = hcloud_server.node[0].id
//  ip_address = hcloud_server.node[0].ipv4_address
//  dns_ptr    = "${hcloud_server.node[0].name}.intrafind.net"
//}
//
//resource "hcloud_rdns" "master" {
//  server_id  = hcloud_server.master[0].id
//  ip_address = hcloud_server.master[0].ipv4_address
//  dns_ptr    = "${hcloud_server.master[0].name}.intrafind.net"
//}
