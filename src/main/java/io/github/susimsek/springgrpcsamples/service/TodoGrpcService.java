package io.github.susimsek.springgrpcsamples.service;

import io.github.susimsek.springgrpcsamples.domain.TodoEntity;
import io.github.susimsek.springgrpcsamples.exception.TodoNotFoundException;
import io.github.susimsek.springgrpcsamples.mapper.TodoMapper;
import io.github.susimsek.springgrpcsamples.proto.CreateTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.DeleteTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.DeleteTodoResponse;
import io.github.susimsek.springgrpcsamples.proto.GetTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.ListTodosRequest;
import io.github.susimsek.springgrpcsamples.proto.PatchTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.Todo;
import io.github.susimsek.springgrpcsamples.proto.TodoList;
import io.github.susimsek.springgrpcsamples.proto.TodoServiceGrpc;
import io.github.susimsek.springgrpcsamples.proto.UpdateTodoRequest;
import io.github.susimsek.springgrpcsamples.repository.TodoRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TodoGrpcService extends TodoServiceGrpc.TodoServiceImplBase {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final TodoRepository todoRepository;
    private final TodoMapper todoMapper;

    @Override
    public void createTodo(CreateTodoRequest request, StreamObserver<Todo> responseObserver) {
        TodoEntity created = todoRepository.save(todoMapper.toEntity(request));
        responseObserver.onNext(todoMapper.toProto(created));
        responseObserver.onCompleted();
    }

    @Override
    public void getTodo(GetTodoRequest request, StreamObserver<Todo> responseObserver) {
        TodoEntity todo = findTodo(request.getId());

        responseObserver.onNext(todoMapper.toProto(todo));
        responseObserver.onCompleted();
    }

    @Override
    public void listTodos(ListTodosRequest request, StreamObserver<TodoList> responseObserver) {
        int pageSize = request.getSize() == 0 ? DEFAULT_PAGE_SIZE : request.getSize();

        Page<TodoEntity> page = todoRepository.findAll(PageRequest.of(request.getPage(), pageSize));
        TodoList.Builder response =
                TodoList.newBuilder()
                        .setPage(page.getNumber())
                        .setSize(page.getSize())
                        .setTotalElements(page.getTotalElements())
                        .setTotalPages(page.getTotalPages())
                        .setFirst(page.isFirst())
                        .setLast(page.isLast());
        page.stream().map(todoMapper::toProto).forEach(response::addItems);
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateTodo(UpdateTodoRequest request, StreamObserver<Todo> responseObserver) {
        TodoEntity updated = findTodo(request.getId());

        todoMapper.updateEntity(request, updated);
        updated = todoRepository.save(updated);
        responseObserver.onNext(todoMapper.toProto(updated));
        responseObserver.onCompleted();
    }

    @Override
    public void patchTodo(PatchTodoRequest request, StreamObserver<Todo> responseObserver) {
        TodoEntity existing = findTodo(request.getId());
        todoMapper.patchEntity(request, existing);
        TodoEntity patched = todoRepository.save(existing);

        responseObserver.onNext(todoMapper.toProto(patched));
        responseObserver.onCompleted();
    }

    @Override
    public void deleteTodo(
            DeleteTodoRequest request, StreamObserver<DeleteTodoResponse> responseObserver) {
        TodoEntity existing = findTodo(request.getId());
        todoRepository.delete(existing);

        responseObserver.onNext(
                DeleteTodoResponse.newBuilder().setId(request.getId()).setDeleted(true).build());
        responseObserver.onCompleted();
    }

    private TodoEntity findTodo(Long id) {
        return todoRepository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
    }
}
