package io.github.susimsek.springgrpcsamples.config.aot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.susimsek.springgrpcsamples.exception.GlobalGrpcExceptionHandler;
import io.github.susimsek.springgrpcsamples.proto.DeleteTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.LoginRequest;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.core.io.Resource;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

class NativeRuntimeHintsTest {

    @Test
    void registersApplicationReflectionHints() throws NoSuchMethodException {
        RuntimeHints hints = new RuntimeHints();
        Method handlerMethod =
                GlobalGrpcExceptionHandler.class.getMethod("handleException", Exception.class);
        Method loginRequestMethod = LoginRequest.class.getMethod("getUsername");
        Method loginRequestBuilderMethod = LoginRequest.Builder.class.getMethod("getUsername");
        Method deleteTodoRequestMethod = DeleteTodoRequest.class.getMethod("getId");

        new NativeRuntimeHints().registerHints(hints, getClass().getClassLoader());

        assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(handlerMethod))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(loginRequestMethod))
                .accepts(hints);
        assertThat(
                        RuntimeHintsPredicates.reflection()
                                .onMethodInvocation(loginRequestBuilderMethod))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(deleteTodoRequestMethod))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.resource().forResource("i18n/messages_tr.properties"))
                .accepts(hints);
    }

    @Test
    void failsWhenProtoRequestTypeScanFails() throws NoSuchMethodException {
        Method scanMethod =
                NativeRuntimeHints.class.getDeclaredMethod(
                        "findApplicationProtoRequestTypes", ClassLoader.class);
        scanMethod.setAccessible(true);
        ClassLoader classLoader =
                new ClassLoader() {
                    @Override
                    public Enumeration<URL> getResources(String name) throws IOException {
                        throw new IOException("boom");
                    }
                };

        assertThatThrownBy(() -> scanMethod.invoke(null, classLoader))
                .cause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to scan application proto request types");
    }

    @Test
    void failsWhenProtoRequestMetadataCannotBeRead() throws Exception {
        Method classNameMethod =
                NativeRuntimeHints.class.getDeclaredMethod(
                        "className", Resource.class, SimpleMetadataReaderFactory.class);
        classNameMethod.setAccessible(true);
        Resource resource = mock();
        when(resource.getInputStream()).thenThrow(new IOException("boom"));

        assertThatThrownBy(
                        () ->
                                classNameMethod.invoke(
                                        null,
                                        resource,
                                        new SimpleMetadataReaderFactory(
                                                getClass().getClassLoader())))
                .cause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to read proto request metadata");
    }
}
