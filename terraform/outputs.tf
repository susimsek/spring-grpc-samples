output "cluster_name" {
  description = "kind cluster name."
  value       = kind_cluster.local.name
}

output "kubeconfig_path" {
  description = "Generated kubeconfig path for this cluster."
  value       = local.kubeconfig_absolute_path
}

output "grpc_port_forward_command" {
  description = "Command to expose the ClusterIP gRPC service on localhost:9090."
  value       = "kubectl --kubeconfig=${local.kubeconfig_absolute_path} -n ${var.namespace} port-forward svc/spring-grpc-samples 9090:9090"
}
