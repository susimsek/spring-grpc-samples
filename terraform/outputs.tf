output "cluster_name" {
  description = "kind cluster name."
  value       = kind_cluster.local.name
}

output "kubeconfig_path" {
  description = "Generated kubeconfig path for this cluster."
  value       = local.kubeconfig_absolute_path
}

output "grpc_ingress_host" {
  description = "Hostname exposed through ingress-nginx."
  value       = var.ingress_host
}

output "grpc_ingress_url" {
  description = "Ingress endpoint for the gRPC application."
  value       = "http://${var.ingress_host}:${var.ingress_http_host_port}"
}

output "grpcurl_ingress_command" {
  description = "Example grpcurl command using ingress-nginx."
  value       = "grpcurl -plaintext ${var.ingress_host}:${var.ingress_http_host_port} list"
}

output "grpc_port_forward_command" {
  description = "Fallback command to expose the ClusterIP gRPC service on localhost:9090."
  value       = "kubectl --kubeconfig=${local.kubeconfig_absolute_path} -n ${var.namespace} port-forward svc/spring-grpc-samples 9090:9090"
}
