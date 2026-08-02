package gatling;

import static io.gatling.javaapi.grpc.GrpcDsl.grpc;

import io.gatling.javaapi.grpc.GrpcProtocolBuilder;
import java.time.Duration;
import java.util.Optional;

public final class GatlingDefaults {

    private GatlingDefaults() {}

    public static String host() {
        return Optional.ofNullable(System.getProperty("grpcHost")).orElse("localhost");
    }

    public static int port() {
        return Integer.getInteger("grpcPort", 9090);
    }

    public static int users() {
        return Integer.getInteger("users", 5);
    }

    public static Duration rampDuration() {
        return Duration.ofMinutes(Integer.getInteger("ramp", 1));
    }

    public static Duration testDuration() {
        return Duration.ofMinutes(Integer.getInteger("duration", 1));
    }

    public static Duration maxDuration() {
        return rampDuration().plus(testDuration()).plusSeconds(30);
    }

    public static Duration minPause() {
        return Duration.ofSeconds(Long.getLong("minPauseSeconds", 10));
    }

    public static Duration maxPause() {
        return Duration.ofSeconds(Long.getLong("maxPauseSeconds", 20));
    }

    public static Duration pause() {
        return Duration.ofSeconds(Long.getLong("pauseSeconds", 10));
    }

    public static String username() {
        return Optional.ofNullable(System.getProperty("username")).orElse("admin");
    }

    public static String password() {
        return Optional.ofNullable(System.getProperty("password")).orElse("admin");
    }

    public static GrpcProtocolBuilder grpcProtocol() {
        return grpc.serverConfigurations(
                grpc.serverConfiguration("default").forAddress(host(), port()).usePlaintext());
    }
}
