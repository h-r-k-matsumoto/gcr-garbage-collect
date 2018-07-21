/**
 *
 */
package com.hrkm;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import com.hrkm.gcp.ContainerRegistry;

@SpringBootApplication
public class Application implements CommandLineRunner {

  private Logger log = LoggerFactory.getLogger(Application.class);

  @Autowired
  private ContainerRegistry containerRegistry;

  @Value("${dry-run}")
  private boolean isDryRun;

  @Value("${parallelism}")
  private int parallelism;

  public static void main(String[] args) {
    new SpringApplicationBuilder(Application.class).logStartupInfo(false).run(args);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run(String... args) throws Exception {
    List<String> repositories = containerRegistry.list();
    log.info("repository size {}", repositories.size());
    // repositories.parallelStream().map(this.containerRegistry::listTags)
    // .forEach(System.out::println);
    ForkJoinPool forkJoinPool = null;
    try {
      forkJoinPool = new ForkJoinPool(this.parallelism);
      forkJoinPool.submit(() -> {
        repositories.parallelStream().map(this.containerRegistry::listTags).forEach((x) -> {
          x.parallelStream().forEach((image) -> {
            if (isDryRun) {
              log.info("image delete: {}", image);
            } else {
              this.containerRegistry.delete(image);
            }

          });
        });
      }).get(); // this makes it an overall blocking call
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    } finally {
      if (forkJoinPool != null) {
        forkJoinPool.shutdown(); // always remember to shutdown the pool
      }
    }
  }
}
