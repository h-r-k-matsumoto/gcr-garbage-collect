# gcr-garbage-collect

google container registry parallel image delete tool.
This tool used. Google Cloud SDK.

google cloud console image delete

## Why is this tool required?

I made this tool for that.

-  When deleting images collectively from the google cloud console, an error occurs with high probability.
-  When deleting with gcloud command, serial execution is very slow.

## Proccessing Summary

```
1.gcloud container images list
  └ (parallel)-> gcloudcontainer images list-tags {repository-name}
      └ (parallel) -> gcloud container images delete {imagename@sha256:digest}
```


# System Requirements

 - [Google Cloud SDK](https://cloud.google.com/sdk/)
 - Java 8
 - [Apache Maven](https://maven.apache.org/) 3.5,or later.

# Packaging

```
$ mvn package
```

`target/gcr-garbage-collect-0.0.1-SNAPSHOT.jar` this is execute files.


## Usage


**dry-run**

```
java -jar gcr-garbage-collect-0.0.1-SNAPSHOT.jar
```

this output is delete targte images.

```
2018-07-22 00:49:15.994  INFO 8152 --- [Pool-1-worker-5] com.hrkm.Application                     : image delete: asia.gcr.io/xxx/yyy@sha256:90bf09121
```

**delete execute**

```
java -jar gcr-garbage-collect-0.0.1-SNAPSHOT.jar --dry-run=false
```

**target repository change**

```
java -jar gcr-garbage-collect-0.0.1-SNAPSHOT.jar --gcloud.container.repository=asia.gcr.io/{gcp-project-id}
```


**deletes only the specified image.**

```
java -jar gcr-garbage-collect-0.0.1-SNAPSHOT.jar --gcloud.container.image=asia.gcr.io/{gcp-project-id}/{image-name}
```


### Options

All of the options are defined in [application.yaml](src/main/resource/application.yaml).

|option |description|
|:------|:----------|
|dry-run                                |if true, it will not be deleted. It is only output to be deleted.|
|gcloud.command                         |please set the path to gcloud command.<br>If this is windows, please set the path to gcloud.cmd.|
|gcloud.container.repository            |target gcp project repository.|
|gcloud.container.delete.none-tags-only |if true,filter is `NOT tags:*` . |
|gcloud.container.delete.before-month   |seletes the image before the specified number of months.|
|gcloud.container.repository            |deletes only the specified image.|




