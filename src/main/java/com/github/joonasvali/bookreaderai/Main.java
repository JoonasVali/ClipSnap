package com.github.joonasvali.bookreaderai;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class Main {
  public static void main(String[] args) throws IOException, InterruptedException, InvocationTargetException {
    SwingUtilities.invokeAndWait(() -> {
      MainFrame app = new MainFrame();
    });
  }
}