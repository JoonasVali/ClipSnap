package com.github.joonasvali.bookreaderai;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ImageLoader {


  public static List<BufferedImage> loadImagesFromPaths(Path[] paths) throws IOException {
    List<CompletableFuture<BufferedImage>> futures = new ArrayList<>();
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    for (Path path : paths) {
      futures.add(CompletableFuture.supplyAsync(() -> {
        try {
          return ImageIO.read(path.toFile());
        } catch (IOException e) {
          System.err.println("Error reading image at path: " + path);
          e.printStackTrace();
          return null;
        }
      }, executor));
    }

    List<BufferedImage> images = futures.stream()
        .map(CompletableFuture::join)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    executor.shutdown();
    return images;
  }
}