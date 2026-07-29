package io.github.susimsek.springgrpcsamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springgrpcsamples.security.AuthoritiesConstants;
import org.junit.jupiter.api.Test;
import org.springframework.grpc.server.security.AuthenticationProcessInterceptor;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.grpc.server.security.OAuth2ResourceServerConfigurer;
import org.springframework.grpc.server.security.RequestMapperConfigurer;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void createsPasswordEncoder() {
        assertThat(config.passwordEncoder()).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void authenticationManagerAuthenticatesWithUserDetailsAndPasswordEncoder() {
        PasswordEncoder passwordEncoder = config.passwordEncoder();
        UserDetailsService userDetailsService =
                username ->
                        org.springframework.security.core.userdetails.User.withUsername(username)
                                .password(passwordEncoder.encode("admin"))
                                .authorities(AuthoritiesConstants.ADMIN)
                                .build();

        var authentication =
                config.authenticationManager(userDetailsService, passwordEncoder)
                        .authenticate(
                                UsernamePasswordAuthenticationToken.unauthenticated(
                                        "admin", "admin"));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo("admin");
    }

    @Test
    void authenticationManagerRejectsInvalidPassword() {
        PasswordEncoder passwordEncoder = config.passwordEncoder();
        UserDetailsService userDetailsService =
                username ->
                        org.springframework.security.core.userdetails.User.withUsername(username)
                                .password(passwordEncoder.encode("admin"))
                                .authorities(AuthoritiesConstants.ADMIN)
                                .build();

        assertThatThrownBy(
                        () ->
                                config.authenticationManager(userDetailsService, passwordEncoder)
                                        .authenticate(
                                                UsernamePasswordAuthenticationToken.unauthenticated(
                                                        "admin", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createsGrpcSecurityFilterChain() throws Exception {
        GrpcSecurity grpc = mock(GrpcSecurity.class);
        AuthenticationProcessInterceptor interceptor = mock(AuthenticationProcessInterceptor.class);
        RequestMapperConfigurer requests = mock(RequestMapperConfigurer.class);
        RequestMapperConfigurer.AuthorizedCall authorizedCall =
                mock(RequestMapperConfigurer.AuthorizedCall.class);
        OAuth2ResourceServerConfigurer resourceServer = mock(OAuth2ResourceServerConfigurer.class);
        OAuth2ResourceServerConfigurer.JwtConfigurer jwt =
                mock(OAuth2ResourceServerConfigurer.JwtConfigurer.class);
        when(requests.methods("AuthApi/Login", "grpc.*/*")).thenReturn(authorizedCall);
        when(requests.methods("TodoApi/*")).thenReturn(authorizedCall);
        when(authorizedCall.permitAll()).thenReturn(requests);
        when(requests.allRequests()).thenReturn(authorizedCall);
        when(authorizedCall.hasAuthority(AuthoritiesConstants.ADMIN)).thenReturn(requests);
        when(authorizedCall.authenticated()).thenReturn(requests);
        when(resourceServer.jwt(any()))
                .thenAnswer(
                        invocation -> {
                            Customizer<OAuth2ResourceServerConfigurer.JwtConfigurer> customizer =
                                    invocation.getArgument(0);
                            customizer.customize(jwt);
                            return resourceServer;
                        });
        when(grpc.authorizeRequests(any()))
                .thenAnswer(
                        invocation -> {
                            Customizer<RequestMapperConfigurer> customizer =
                                    invocation.getArgument(0);
                            customizer.customize(requests);
                            return grpc;
                        });
        when(grpc.oauth2ResourceServer(any()))
                .thenAnswer(
                        invocation -> {
                            Customizer<OAuth2ResourceServerConfigurer> customizer =
                                    invocation.getArgument(0);
                            customizer.customize(resourceServer);
                            return grpc;
                        });
        when(grpc.build()).thenReturn(interceptor);

        AuthenticationProcessInterceptor result = config.grpcSecurityFilterChain(grpc);

        assertThat(result).isSameAs(interceptor);
        verify(requests).methods("AuthApi/Login", "grpc.*/*");
        verify(requests).methods("TodoApi/*");
        verify(authorizedCall).hasAuthority(AuthoritiesConstants.ADMIN);
        verify(requests).allRequests();
        verify(authorizedCall).authenticated();
    }
}
