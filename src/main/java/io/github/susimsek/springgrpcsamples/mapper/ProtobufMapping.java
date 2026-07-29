package io.github.susimsek.springgrpcsamples.mapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.mapstruct.Mapping;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@Mapping(target = "allFields", ignore = true)
@Mapping(target = "clearField", ignore = true)
@Mapping(target = "clearOneof", ignore = true)
@Mapping(target = "mergeFrom", ignore = true)
@Mapping(target = "mergeUnknownFields", ignore = true)
@Mapping(target = "unknownFields", ignore = true)
public @interface ProtobufMapping {}
