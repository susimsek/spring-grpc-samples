locals {
  kubeconfig_path          = "${path.module}/kubeconfig"
  kubeconfig_absolute_path = abspath(local.kubeconfig_path)
  node_image               = "kindest/node:${var.kubernetes_version}"
}

provider "kind" {}

resource "kind_cluster" "local" {
  name            = var.cluster_name
  node_image      = local.node_image
  kubeconfig_path = local.kubeconfig_path
  wait_for_ready  = true

  kind_config {
    kind        = "Cluster"
    api_version = "kind.x-k8s.io/v1alpha4"

    node {
      role = "control-plane"
    }

    node {
      role = "worker"
    }
  }
}

provider "kubernetes" {
  host                   = kind_cluster.local.endpoint
  client_certificate     = kind_cluster.local.client_certificate
  client_key             = kind_cluster.local.client_key
  cluster_ca_certificate = kind_cluster.local.cluster_ca_certificate
}

provider "helm" {
  kubernetes = {
    host                   = kind_cluster.local.endpoint
    client_certificate     = kind_cluster.local.client_certificate
    client_key             = kind_cluster.local.client_key
    cluster_ca_certificate = kind_cluster.local.cluster_ca_certificate
  }
}

resource "kubernetes_namespace_v1" "app" {
  metadata {
    name = var.namespace
  }
}

resource "helm_release" "spring_grpc_samples" {
  name              = "spring-grpc-samples"
  chart             = "${path.module}/../helm/spring-grpc-samples"
  namespace         = kubernetes_namespace_v1.app.metadata[0].name
  dependency_update = true
  wait              = true
  timeout           = 600

  set = [
    {
      name  = "image.repository"
      value = var.image_repository
    },
    {
      name  = "image.tag"
      value = var.image_tag
    },
    {
      name  = "spring.profiles.active"
      value = var.spring_profile
    },
    {
      name  = "security.jwt.secret"
      value = var.jwt_secret
    }
  ]

  depends_on = [kind_cluster.local]
}
