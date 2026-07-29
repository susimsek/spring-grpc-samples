package io.github.susimsek.springgrpcsamples.mapper;

import com.google.protobuf.Timestamp;
import java.time.Instant;

public final class ProtobufMapper {

    private ProtobufMapper() {}

    public static Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
