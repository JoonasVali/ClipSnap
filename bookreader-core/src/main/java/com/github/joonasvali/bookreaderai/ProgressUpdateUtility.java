package com.github.joonasvali.bookreaderai;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ProgressUpdateUtility {
  private final Logger logger = org.slf4j.LoggerFactory.getLogger(ProgressUpdateUtility.class);

  private final int totalTasks;
  private Boolean tasksCompleted[];

  private float progress;
  private int finalTaskComplete = 0;
  private List<Consumer<Float>> listeners;

  public ProgressUpdateUtility(int totalTasks) {
    this.totalTasks = totalTasks;
    tasksCompleted = new Boolean[totalTasks];
    Arrays.fill(tasksCompleted, false);
    progress = 0;
    listeners = new ArrayList<>();
  }

  public void setListener(Consumer<Float> listener) {
    listeners.add(listener);
  }

  public float getProgress() {
    return progress;
  }

  public void notifyListeners() {
    listeners.forEach(listener -> listener.accept(progress));
  }

  public void removeListener(Consumer<Float> listener) {
    listeners.remove(listener);
  }

  public void setTranscribeTaskComplete(int index, boolean isComplete) {
    tasksCompleted[index] = isComplete;
    updateProgress();
  }

  private void updateProgress() {
    int completedTasks = (int) Arrays.stream(tasksCompleted).filter(task -> task).count() + finalTaskComplete;
    progress = (float) completedTasks / (totalTasks + 1);
    notifyListeners();
  }

  public void setFinalTaskComplete() {
    finalTaskComplete = 1;
    updateProgress();
  }
}
