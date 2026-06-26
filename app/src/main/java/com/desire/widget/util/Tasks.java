package com.desire.widget.util;

import com.google.android.gms.tasks.Task;

public class Tasks {
    public static <T> T await(Task<T> task) throws Exception {
        while (!task.isComplete()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (task.isSuccessful()) {
            return task.getResult();
        } else if (task.getException() != null) {
            throw task.getException();
        }
        return null;
    }
}
