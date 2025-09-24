terraform {
  required_providers {
    kubernetes = { source = "hashicorp/kubernetes", version = "~> 2.30" }
    helm       = { source = "hashicorp/helm",       version = "~> 2.13" }
  }
}

provider "kubernetes" {
  # Uses KUBECONFIG from environment by default
}

provider "helm" {
  kubernetes {}
}

variable "image_repository" { type = string }
variable "image_tag" { type = string }

resource "helm_release" "api" {
  name       = "hmcts-api-service"
  chart      = "${path.module}/../../helm/api"
  namespace  = "default"

  set {
    name  = "image.repository"
    value = var.image_repository
  }
  set {
    name  = "image.tag"
    value = var.image_tag
  }
}
