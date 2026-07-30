package io.github.susimsek.springgrpcsamples.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.github.susimsek.springgrpcsamples.exception.InvalidCredentialsException;
import io.github.susimsek.springgrpcsamples.proto.LoginRequest;
import io.github.susimsek.springgrpcsamples.proto.Token;
import io.github.susimsek.springgrpcsamples.security.AuthoritiesConstants;
import io.github.susimsek.springgrpcsamples.security.JwtService;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class AuthGrpcServiceTest {

    @Mock private AuthenticationManager authenticationManager;

    @Mock private JwtService jwtService;

    @InjectMocks private AuthGrpcService service;

    @Test
    void loginReturnsJwtForEnabledUser() {
        Authentication authentication = authenticatedAdmin();
        RecordingObserver<Token> observer = new RecordingObserver<>();
        when(authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated("admin", "admin")))
                .thenReturn(authentication);
        when(jwtService.generateToken(authentication)).thenReturn("jwt-token");
        when(jwtService.getExpiresInSeconds()).thenReturn(3600L);

        service.login(
                LoginRequest.newBuilder().setUsername("admin").setPassword("admin").build(),
                observer);

        assertThat(observer.values())
                .singleElement()
                .satisfies(
                        response -> {
                            assertThat(response.getAccessToken()).isEqualTo("jwt-token");
                            assertThat(response.getTokenType()).isEqualTo("Bearer");
                            assertThat(response.getExpiresIn()).isEqualTo(3600L);
                        });
        assertThat(observer.completed()).isTrue();
        assertThat(observer.error()).isNull();
    }

    @Test
    void loginRejectsAuthenticationFailures() {
        when(authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated("admin", "wrong")))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(
                        () ->
                                service.login(
                                        LoginRequest.newBuilder()
                                                .setUsername("admin")
                                                .setPassword("wrong")
                                                .build(),
                                        new RecordingObserver<>()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    private static Authentication authenticatedAdmin() {
        return UsernamePasswordAuthenticationToken.authenticated(
                "admin", null, List.of(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)));
    }

    private static final class RecordingObserver<T> implements StreamObserver<T> {

        private final List<T> values = new ArrayList<>();
        private Throwable error;
        private boolean completed;

        @Override
        public void onNext(T value) {
            values.add(value);
        }

        @Override
        public void onError(Throwable throwable) {
            error = throwable;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }

        private List<T> values() {
            return values;
        }

        private Throwable error() {
            return error;
        }

        private boolean completed() {
            return completed;
        }
    }
}
