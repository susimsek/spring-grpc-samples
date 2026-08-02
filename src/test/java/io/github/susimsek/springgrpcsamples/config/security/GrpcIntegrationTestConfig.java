package io.github.susimsek.springgrpcsamples.config.security;

import io.github.susimsek.springgrpcsamples.proto.AuthServiceGrpc;
import io.github.susimsek.springgrpcsamples.proto.TodoServiceGrpc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.GrpcChannelFactory;

@TestConfiguration(proxyBeanMethods = false)
public class GrpcIntegrationTestConfig {

    @Bean
    AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub(
            GrpcChannelFactory grpcChannelFactory) {
        return AuthServiceGrpc.newBlockingStub(
                grpcChannelFactory.createChannel("integration-test"));
    }

    @Bean
    TodoServiceGrpc.TodoServiceBlockingStub todoServiceBlockingStub(
            GrpcChannelFactory grpcChannelFactory) {
        return TodoServiceGrpc.newBlockingStub(
                grpcChannelFactory.createChannel("integration-test"));
    }
}
