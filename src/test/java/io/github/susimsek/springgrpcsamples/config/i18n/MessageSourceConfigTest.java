package io.github.susimsek.springgrpcsamples.config.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;

class MessageSourceConfigTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(MessageSourceAutoConfiguration.class)
                    .withPropertyValues(
                            "spring.messages.basename=i18n/messages",
                            "spring.messages.encoding=UTF-8",
                            "spring.messages.fallback-to-system-locale=false");

    @Test
    void configuresMessageSourceFromI18nBundle() {
        contextRunner.run(
                context -> {
                    MessageSource messageSource = context.getBean(MessageSource.class);

                    String message =
                            messageSource.getMessage(
                                    "grpc.todo.notFound",
                                    new Object[] {"1"},
                                    Locale.forLanguageTag("tr"));

                    assertThat(message).isEqualTo("id'si 1 olan todo bulunamad\u0131");
                });
    }

    @Test
    void doesNotApplyNumberGroupingToStringArguments() {
        contextRunner.run(
                context -> {
                    MessageSource messageSource = context.getBean(MessageSource.class);

                    String message =
                            messageSource.getMessage(
                                    "grpc.todo.notFound", new Object[] {"999999"}, Locale.ENGLISH);

                    assertThat(message).isEqualTo("todo not found with id: 999999");
                });
    }
}
