package io.github.susimsek.springgrpcsamples.config.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

class GrpcLocaleServerInterceptorTest {

    private static final String ACCEPT_LANGUAGE_HEADER = "accept-language";

    private static final Metadata.Key<String> ACCEPT_LANGUAGE =
            Metadata.Key.of(ACCEPT_LANGUAGE_HEADER, Metadata.ASCII_STRING_MARSHALLER);

    private final GrpcLocaleServerInterceptor interceptor = new GrpcLocaleServerInterceptor();

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void setsLocaleFromAcceptLanguageMetadata() {
        Metadata headers = new Metadata();
        headers.put(ACCEPT_LANGUAGE, "tr");
        AtomicReference<Locale> locale = new AtomicReference<>();
        ServerCall.Listener<String> listener = intercept(headers, locale);

        listener.onMessage("payload");
        listener.onHalfClose();
        listener.onCancel();
        listener.onComplete();

        assertThat(locale.get()).isEqualTo(Locale.forLanguageTag("tr"));
    }

    @Test
    void setsLocaleDuringStartCall() {
        Metadata headers = new Metadata();
        headers.put(ACCEPT_LANGUAGE, "tr");
        AtomicReference<Locale> locale = new AtomicReference<>();
        ServerCall<String, String> call = mock();
        ServerCallHandler<String, String> next =
                (serverCall, metadata) -> {
                    locale.set(LocaleContextHolder.getLocale());
                    return new ServerCall.Listener<>() {};
                };

        interceptor.interceptCall(call, headers, next);

        assertThat(locale.get()).isEqualTo(Locale.forLanguageTag("tr"));
    }

    @Test
    void restoresPreviousLocaleAfterStartCall() {
        LocaleContextHolder.setLocale(Locale.GERMAN);
        Metadata headers = new Metadata();
        headers.put(ACCEPT_LANGUAGE, "tr");
        ServerCall<String, String> call = mock();
        ServerCallHandler<String, String> next =
                (serverCall, metadata) -> new ServerCall.Listener<>() {};

        interceptor.interceptCall(call, headers, next);

        assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMAN);
    }

    @Test
    void setsLocaleFromAcceptLanguageMetadataWithWeightedValues() {
        Metadata headers = new Metadata();
        headers.put(ACCEPT_LANGUAGE, "tr-TR,tr;q=0.9,en;q=0.8");
        AtomicReference<Locale> locale = new AtomicReference<>();
        ServerCall.Listener<String> listener = intercept(headers, locale);

        listener.onHalfClose();

        assertThat(locale.get()).isEqualTo(Locale.forLanguageTag("tr"));
    }

    @Test
    void usesEnglishWhenAcceptLanguageIsInvalid() {
        Metadata headers = new Metadata();
        headers.put(ACCEPT_LANGUAGE, "invalid;q=broken");
        AtomicReference<Locale> locale = new AtomicReference<>();
        ServerCall.Listener<String> listener = intercept(headers, locale);

        listener.onReady();

        assertThat(locale.get()).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void usesEnglishWhenAcceptLanguageIsUnsupported() {
        Metadata headers = new Metadata();
        headers.put(ACCEPT_LANGUAGE, "fr");
        AtomicReference<Locale> locale = new AtomicReference<>();
        ServerCall.Listener<String> listener = intercept(headers, locale);

        listener.onReady();

        assertThat(locale.get()).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void usesEnglishWhenAcceptLanguageIsMissing() {
        AtomicReference<Locale> locale = new AtomicReference<>();
        ServerCall.Listener<String> listener = intercept(new Metadata(), locale);

        listener.onReady();

        assertThat(locale.get()).isEqualTo(Locale.ENGLISH);
    }

    private ServerCall.Listener<String> intercept(
            Metadata headers, AtomicReference<Locale> locale) {
        ServerCall<String, String> call = mock();
        ServerCall.Listener<String> delegate =
                new ServerCall.Listener<>() {
                    @Override
                    public void onHalfClose() {
                        locale.set(LocaleContextHolder.getLocale());
                    }

                    @Override
                    public void onMessage(String message) {
                        locale.set(LocaleContextHolder.getLocale());
                    }

                    @Override
                    public void onCancel() {
                        locale.set(LocaleContextHolder.getLocale());
                    }

                    @Override
                    public void onComplete() {
                        locale.set(LocaleContextHolder.getLocale());
                    }

                    @Override
                    public void onReady() {
                        locale.set(LocaleContextHolder.getLocale());
                    }
                };
        ServerCallHandler<String, String> next = (serverCall, metadata) -> delegate;
        return interceptor.interceptCall(call, headers, next);
    }
}
