package io.github.susimsek.springgrpcsamples.config.i18n;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@GlobalServerInterceptor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GrpcLocaleServerInterceptor implements ServerInterceptor {

    private static final String ACCEPT_LANGUAGE_HEADER = "accept-language";

    private static final List<Locale> SUPPORTED_LOCALES =
            List.of(Locale.ENGLISH, Locale.forLanguageTag("tr"));

    private static final Metadata.Key<String> ACCEPT_LANGUAGE =
            Metadata.Key.of(ACCEPT_LANGUAGE_HEADER, Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        Locale locale = resolveLocale(headers);
        ServerCall.Listener<ReqT> listener =
                callWithLocale(locale, () -> next.startCall(call, headers));
        return new LocaleAwareServerCallListener<>(listener, locale);
    }

    private static Locale resolveLocale(Metadata headers) {
        String language = headers.get(ACCEPT_LANGUAGE);
        if (!StringUtils.hasText(language)) {
            return Locale.ENGLISH;
        }
        try {
            Locale locale = Locale.lookup(Locale.LanguageRange.parse(language), SUPPORTED_LOCALES);
            return locale != null ? locale : Locale.ENGLISH;
        } catch (IllegalArgumentException ex) {
            return Locale.ENGLISH;
        }
    }

    private static <T> T callWithLocale(Locale locale, Supplier<T> action) {
        LocaleContext previous = LocaleContextHolder.getLocaleContext();
        try {
            LocaleContextHolder.setLocaleContext(new SimpleLocaleContext(locale));
            return action.get();
        } finally {
            LocaleContextHolder.setLocaleContext(previous);
        }
    }

    private static final class LocaleAwareServerCallListener<ReqT>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

        private final Locale locale;

        private LocaleAwareServerCallListener(ServerCall.Listener<ReqT> delegate, Locale locale) {
            super(delegate);
            this.locale = locale;
        }

        @Override
        public void onMessage(ReqT message) {
            runWithLocale(() -> super.onMessage(message));
        }

        @Override
        public void onHalfClose() {
            runWithLocale(super::onHalfClose);
        }

        @Override
        public void onCancel() {
            runWithLocale(super::onCancel);
        }

        @Override
        public void onComplete() {
            runWithLocale(super::onComplete);
        }

        @Override
        public void onReady() {
            runWithLocale(super::onReady);
        }

        private void runWithLocale(Runnable action) {
            callWithLocale(
                    locale,
                    () -> {
                        action.run();
                        return null;
                    });
        }
    }
}
