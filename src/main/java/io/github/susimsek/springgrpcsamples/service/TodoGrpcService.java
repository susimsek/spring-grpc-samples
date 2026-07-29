package io.github.susimsek.springgrpcsamples.service;

import io.github.susimsek.springgrpcsamples.domain.Todo;
import io.github.susimsek.springgrpcsamples.exception.TodoNotFoundException;
import io.github.susimsek.springgrpcsamples.mapper.TodoMapper;
import io.github.susimsek.springgrpcsamples.proto.CreateTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.DeleteTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.DeleteTodoResponse;
import io.github.susimsek.springgrpcsamples.proto.GetTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.ListTodosRequest;
import io.github.susimsek.springgrpcsamples.proto.ListTodosResponse;
import io.github.susimsek.springgrpcsamples.proto.PatchTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.TodoApiGrpc;
import io.github.susimsek.springgrpcsamples.proto.TodoResponse;
import io.github.susimsek.springgrpcsamples.proto.UpdateTodoRequest;
import io.github.susimsek.springgrpcsamples.repository.TodoRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TodoGrpcService extends TodoApiGrpc.TodoApiImplBase {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final TodoRepository todoRepository;
    private final TodoMapper todoMapper;

    @Override
    public void createTodo(
            CreateTodoRequest request, StreamObserver<TodoResponse> responseObserver) {
        Todo created = todoRepository.save(todoMapper.toEntity(request));
        responseObserver.onNext(todoMapper.toResponse(created));
        responseObserver.onCompleted();
    }

    @Override
    public void getTodo(GetTodoRequest request, StreamObserver<TodoResponse> responseObserver) {
        Todo todo = findTodo(request.getId());

        responseObserver.onNext(todoMapper.toResponse(todo));
        responseObserver.onCompleted();
    }

    @Override
    public void listTodos(
            ListTodosRequest request, StreamObserver<ListTodosResponse> responseObserver) {
        int pageSize = request.getSize() == 0 ? DEFAULT_PAGE_SIZE : request.getSize();

        Page<Todo> page = todoRepository.findAll(PageRequest.of(request.getPage(), pageSize));
        ListTodosResponse.Builder response =
                ListTodosResponse.newBuilder()
                        .setPage(page.getNumber())
                        .setSize(page.getSize())
                        .setTotalElements(page.getTotalElements())
                        .setTotalPages(page.getTotalPages())
                        .setFirst(page.isFirst())
                        .setLast(page.isLast());
        page.stream().map(todoMapper::toResponse).forEach(response::addItems);
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateTodo(
            UpdateTodoRequest request, StreamObserver<TodoResponse> responseObserver) {
        Todo updated = findTodo(request.getId());

        todoMapper.update(request, updated);
        updated = todoRepository.save(updated);
        responseObserver.onNext(todoMapper.toResponse(updated));
        responseObserver.onCompleted();
    }

    @Override
    public void patchTodo(PatchTodoRequest request, StreamObserver<TodoResponse> responseObserver) {
        Todo existing = findTodo(request.getId());
        todoMapper.partialUpdate(request, existing);
        Todo patched = todoRepository.save(existing);

        responseObserver.onNext(todoMapper.toResponse(patched));
        responseObserver.onCompleted();
    }

    @Override
    public void deleteTodo(
            DeleteTodoRequest request, StreamObserver<DeleteTodoResponse> responseObserver) {
        Todo existing = findTodo(request.getId());
        todoRepository.delete(existing);

        responseObserver.onNext(
                DeleteTodoResponse.newBuilder().setId(request.getId()).setDeleted(true).build());
        responseObserver.onCompleted();
    }

    private Todo findTodo(Long id) {
        return todoRepository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
    }
}
