package com.hrkm.gcp;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import com.hrkm.util.ProcessUtils;

@Component
public class ContainerRegistry {

  @Value("${gcloud.command}")
  private String gcloudCommand;

  @Value("${gcloud.container.repository:}")
  private String gcloudRepository;

  @Value("${gcloud.container.delete.none-tags-only:false}")
  private boolean noneTagsOnly;

  @Value("${gcloud.container.delete.before-month:-1}")
  private int beforeMonth;

  @Value("${gcloud.container.delete.filter:}")
  private String filter;

  @Value("${gcloud.container.image:}")
  private String image;


  public List<String> list() {
    List<String> names = new ArrayList<>();
    if (!StringUtils.isEmpty(image)) {
      names.add(image);
      return names;
    }
    List<String> args =
        new ArrayList<>(Arrays.asList(gcloudCommand, "container", "images", "list"));

    if (!StringUtils.isEmpty(gcloudRepository)) {
      args.add("--repository=" + gcloudRepository);
    }
    Consumer<String> parser = (x) -> {
      if (x.indexOf("gcr.io/") != -1) {
        names.add(x);
      }
    };
    ProcessUtils.process(parser, args);
    return names;
  }


  public List<String> listTags(String repositoryName) {
    List<String> digestList = new ArrayList<>();
    List<String> args = new ArrayList<>(Arrays.asList(gcloudCommand, "container", "images",
        "list-tags", repositoryName, "--limit=999999", listTagsFormat()));

    if (StringUtils.isEmpty(filter)) {
      String filters = this.listTagsFilter();
      if (!StringUtils.isEmpty(filters)) {
        String osname = System.getProperty("os.name", "windows").toLowerCase();
        if (osname.indexOf("windows") != -1) {
          // FIXME: 2018/07/21 hiroki.matsumoto --filter="NOT tags:*" is not works.
          // ProcessBuilder is addtional double qoute, because option has spaces.
          // args.add(filters); //This is not execution.
          String[] tokens = filters.split(" ");
          for (int i = 0; i < tokens.length; i++) {
            args.add(tokens[i]);
          }
        } else {
          args.add(filters);
        }
      }
    } else {
      args.add(filter);
    }
    Consumer<String> parser = (x) -> {
      if (x.startsWith("sha256:")) {
        StringBuilder builder = new StringBuilder(128);
        builder.append(repositoryName).append('@').append(x);
        digestList.add(builder.toString());
      }
    };
    ProcessUtils.process(parser, args);
    return digestList;
  }

  public void delete(String imageName) {
    List<String> args = new ArrayList<>(Arrays.asList(gcloudCommand, "container", "images",
        "delete", imageName, "--quiet", "--force-delete-tags"));
    ProcessUtils.process(null, args);
  }

  private String listTagsFormat() {
    String osname = System.getProperty("os.name", "windows").toLowerCase();
    if (osname.indexOf("windows") != -1) {
      return "--format=\"get('digest')\"";
    } else {
      return "--format=get('digest')";
    }
  }

  private String listTagsFilter() {
    StringBuilder filters = new StringBuilder();
    String osname = System.getProperty("os.name", "windows").toLowerCase();
    if (this.noneTagsOnly) {
      filters.append("NOT tags:*");
    }
    if (this.beforeMonth > 0) {
      if (filters.length() > 0)
        filters.append(" AND ");
      String date = LocalDate.now().minusMonths(beforeMonth)
          .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      if (osname.indexOf("windows") != -1) {
        // windows env escape.
        filters.append("timestamp.datetime^<").append(date);
      } else {
        filters.append("timestamp.datetime<").append(date);
      }
    }
    if (filters.length() > 0) {
      if (osname.indexOf("windows") != -1) {
        filters.insert(0, "--filter=\"");
        filters.append("\"");
      } else {
        filters.insert(0, "--filter=");
      }
    }
    return filters.toString();
  }
}
