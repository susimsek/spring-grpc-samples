package io.github.susimsek.springgrpcsamples.service;

import io.github.susimsek.springgrpcsamples.exception.InvalidCredentialsException;
import io.github.susimsek.springgrpcsamples.proto.AuthApiGrpc;
import io.github.susimsek.springgrpcsamples.proto.LoginRequest;
import io.github.susimsek.springgrpcsamples.proto.LoginResponse;
import io.github.susimsek.springgrpcsamples.security.JwtService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthGrpcService extends AuthApiGrpc.AuthApiImplBase {

    private static final String BEARER_TOKEN_TYPE = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        Authentication authentication = authenticate(request);
        responseObserver.onNext(
                LoginResponse.newBuilder()
                        .setAccessToken(jwtService.generateToken(authentication))
                        .setTokenType(BEARER_TOKEN_TYPE)
                        .setExpiresIn(jwtService.getExpiresInSeconds())
                        .build());
        responseObserver.onCompleted();
    }

    private Authentication authenticate(LoginRequest request) {
        try {
            return authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.getUsername(), request.getPassword()));
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException();
        }
    }
}
