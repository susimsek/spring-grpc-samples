package gatling.simulations;

import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.grpc.GrpcDsl.grpc;
import static io.gatling.javaapi.grpc.GrpcDsl.response;
import static io.gatling.javaapi.grpc.GrpcDsl.statusCode;

import gatling.GatlingDefaults;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.grpc.GrpcProtocolBuilder;
import io.github.susimsek.springgrpcsamples.proto.AuthServiceGrpc;
import io.github.susimsek.springgrpcsamples.proto.CreateTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.DeleteTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.DeleteTodoResponse;
import io.github.susimsek.springgrpcsamples.proto.GetTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.ListTodosRequest;
import io.github.susimsek.springgrpcsamples.proto.LoginRequest;
import io.github.susimsek.springgrpcsamples.proto.PageRequest;
import io.github.susimsek.springgrpcsamples.proto.PatchTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.Todo;
import io.github.susimsek.springgrpcsamples.proto.TodoList;
import io.github.susimsek.springgrpcsamples.proto.TodoServiceGrpc;
import io.github.susimsek.springgrpcsamples.proto.Token;
import io.github.susimsek.springgrpcsamples.proto.UpdateTodoRequest;
import io.grpc.Status;
import java.util.UUID;

public class TodoSimulation extends Simulation {

    private final GrpcProtocolBuilder grpcProtocol = GatlingDefaults.grpcProtocol();

    private final ChainBuilder login =
            exec(grpc("Login")
                            .unary(AuthServiceGrpc.getLoginMethod())
                            .send(
                                    LoginRequest.newBuilder()
                                            .setUsername(GatlingDefaults.username())
                                            .setPassword(GatlingDefaults.password())
                                            .build())
                            .check(
                                    statusCode().is(Status.Code.OK),
                                    response(Token::getAccessToken)
                                            .exists()
                                            .saveAs("access_token")))
                    .exitHereIfFailed();

    private final ChainBuilder todoCrudFlow =
            exec(session -> session.set("todoTitle", "Perf Todo " + UUID.randomUUID()))
                    .exec(
                            grpc("Create Todo")
                                    .unary(TodoServiceGrpc.getCreateTodoMethod())
                                    .send(
                                            session ->
                                                    CreateTodoRequest.newBuilder()
                                                            .setTitle(
                                                                    session.getString("todoTitle"))
                                                            .build())
                                    .asciiHeader("authorization")
                                    .valueEL("Bearer #{access_token}")
                                    .check(
                                            statusCode().is(Status.Code.OK),
                                            response(Todo::getId).saveAs("todoId"),
                                            response(Todo::getTitle).isEL("#{todoTitle}")))
                    .exitHereIfFailed()
                    .pause(GatlingDefaults.pause())
                    .exec(
                            grpc("Get Todo")
                                    .unary(TodoServiceGrpc.getGetTodoMethod())
                                    .send(
                                            session ->
                                                    GetTodoRequest.newBuilder()
                                                            .setId(session.getLong("todoId"))
                                                            .build())
                                    .asciiHeader("authorization")
                                    .valueEL("Bearer #{access_token}")
                                    .check(
                                            statusCode().is(Status.Code.OK),
                                            response(Todo::getId).isEL("#{todoId}")))
                    .pause(GatlingDefaults.minPause(), GatlingDefaults.maxPause())
                    .exec(
                            grpc("List Todos")
                                    .unary(TodoServiceGrpc.getListTodosMethod())
                                    .send(
                                            ListTodosRequest.newBuilder()
                                                    .setPageRequest(
                                                            PageRequest.newBuilder()
                                                                    .setPage(0)
                                                                    .setSize(5)
                                                                    .build())
                                                    .build())
                                    .asciiHeader("authorization")
                                    .valueEL("Bearer #{access_token}")
                                    .check(
                                            statusCode().is(Status.Code.OK),
                                            response(TodoList::getItemsCount).gte(0)))
                    .pause(GatlingDefaults.pause())
                    .exec(
                            grpc("Update Todo")
                                    .unary(TodoServiceGrpc.getUpdateTodoMethod())
                                    .send(
                                            session ->
                                                    UpdateTodoRequest.newBuilder()
                                                            .setId(session.getLong("todoId"))
                                                            .setTitle(
                                                                    session.getString("todoTitle")
                                                                            + " Updated")
                                                            .setCompleted(true)
                                                            .build())
                                    .asciiHeader("authorization")
                                    .valueEL("Bearer #{access_token}")
                                    .check(
                                            statusCode().is(Status.Code.OK),
                                            response(Todo::getCompleted).is(true)))
                    .pause(GatlingDefaults.pause())
                    .exec(
                            grpc("Patch Todo")
                                    .unary(TodoServiceGrpc.getPatchTodoMethod())
                                    .send(
                                            session ->
                                                    PatchTodoRequest.newBuilder()
                                                            .setId(session.getLong("todoId"))
                                                            .setTitle(
                                                                    session.getString("todoTitle")
                                                                            + " Patched")
                                                            .build())
                                    .asciiHeader("authorization")
                                    .valueEL("Bearer #{access_token}")
                                    .check(
                                            statusCode().is(Status.Code.OK),
                                            response(Todo::getTitle).isEL("#{todoTitle} Patched")))
                    .pause(GatlingDefaults.pause())
                    .exec(
                            grpc("Delete Todo")
                                    .unary(TodoServiceGrpc.getDeleteTodoMethod())
                                    .send(
                                            session ->
                                                    DeleteTodoRequest.newBuilder()
                                                            .setId(session.getLong("todoId"))
                                                            .build())
                                    .asciiHeader("authorization")
                                    .valueEL("Bearer #{access_token}")
                                    .check(
                                            statusCode().is(Status.Code.OK),
                                            response(DeleteTodoResponse::getDeleted).is(true)))
                    .exitHereIfFailed();

    private final ScenarioBuilder users =
            scenario("Todo gRPC CRUD")
                    .exec(login)
                    .pause(GatlingDefaults.pause())
                    .repeat(2)
                    .on(todoCrudFlow);

    {
        setUp(
                        users.injectOpen(
                                rampUsers(GatlingDefaults.users())
                                        .during(GatlingDefaults.rampDuration())))
                .protocols(grpcProtocol)
                .maxDuration(GatlingDefaults.maxDuration());
    }
}
