variable "cluster_name" {
  description = "Local kind cluster name."
  type        = string
  default     = "local"
}

variable "kubernetes_version" {
  description = "kind node image tag."
  type        = string
  default     = "v1.33.1"
}

variable "namespace" {
  description = "Namespace for the application release."
  type        = string
  default     = "apps"
}

variable "image_repository" {
  description = "Container image repository for the application."
  type        = string
  default     = "docker.io/suayb/spring-grpc-samples"
}

variable "image_tag" {
  description = "Container image tag for the application."
  type        = string
  default     = "latest-native"
}

variable "spring_profile" {
  description = "Spring profile passed to the Helm release."
  type        = string
  default     = "prod"
}

variable "jwt_secret" {
  description = "JWT secret passed to the Helm release."
  type        = string
  default     = "GOnGieF8gvSk01KtlZUwkpQB8U4tZWTrSt/BJm0+2Mk="
  sensitive   = true
}
