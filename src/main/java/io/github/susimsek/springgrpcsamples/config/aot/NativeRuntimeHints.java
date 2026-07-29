package io.github.susimsek.springgrpcsamples.config.aot;

import io.github.susimsek.springgrpcsamples.exception.GlobalGrpcExceptionHandler;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

public class NativeRuntimeHints implements RuntimeHintsRegistrar {

    private static final String APPLICATION_PROTO_REQUEST_PATTERN =
            "classpath*:io/github/susimsek/springgrpcsamples/proto/*Request.class";

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("i18n/**");
        hints.reflection()
                .registerTypes(
                        findApplicationProtoRequestTypes(classLoader).stream()
                                .flatMap(
                                        typeName ->
                                                Stream.of(
                                                        TypeReference.of(typeName),
                                                        TypeReference.of(typeName + "$Builder")))
                                .toList(),
                        builder -> builder.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));
        hints.reflection()
                .registerType(
                        GlobalGrpcExceptionHandler.class, MemberCategory.INVOKE_PUBLIC_METHODS);
    }

    private static List<String> findApplicationProtoRequestTypes(ClassLoader classLoader) {
        PathMatchingResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver(classLoader);
        SimpleMetadataReaderFactory metadataReaderFactory =
                new SimpleMetadataReaderFactory(classLoader);
        try {
            return Stream.of(resolver.getResources(APPLICATION_PROTO_REQUEST_PATTERN))
                    .filter(Resource::isReadable)
                    .map(resource -> className(resource, metadataReaderFactory))
                    .sorted()
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to scan application proto request types", ex);
        }
    }

    private static String className(
            Resource resource, SimpleMetadataReaderFactory metadataReaderFactory) {
        try {
            MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
            return metadataReader.getClassMetadata().getClassName();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read proto request metadata", ex);
        }
    }
}
