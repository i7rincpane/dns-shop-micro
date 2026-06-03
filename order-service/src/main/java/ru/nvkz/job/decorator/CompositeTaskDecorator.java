package ru.nvkz.job.decorator;

import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskDecorator;

import java.util.List;

@RequiredArgsConstructor
public class CompositeTaskDecorator implements TaskDecorator {

    private final List<TaskDecorator> decorators;

    @Override
    public Runnable decorate(Runnable runnable) {
        Runnable decorated = runnable;

        for (int i = decorators.size() - 1; i >= 0; i--) {
            decorated = decorators.get(i).decorate(decorated);
        }

        return decorated;
    }

}
