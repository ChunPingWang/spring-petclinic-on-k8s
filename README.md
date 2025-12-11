# Distributed version of the Spring PetClinic Sample Application built with Spring Cloud and Spring AI

[![Build Status](https://github.com/spring-petclinic/spring-petclinic-microservices/actions/workflows/maven-build.yml/badge.svg)](https://github.com/spring-petclinic/spring-petclinic-microservices/actions/workflows/maven-build.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

This microservices branch was initially derived from [AngularJS version](https://github.com/spring-petclinic/spring-petclinic-angular1) to demonstrate how to split sample Spring application into [microservices](http://www.martinfowler.com/articles/microservices.html).
To achieve that goal, we use Spring Cloud Gateway, Spring Cloud Circuit Breaker, Micrometer Tracing, Resilience4j, and Open Telemetry.

> **Note**: This branch supports **Kubernetes-native deployment**. Spring Cloud Config Server, Eureka Discovery Server, and Admin Server have been removed in favor of Kubernetes-native solutions (ConfigMap/Secret, K8s Service Discovery, Prometheus + Grafana).

[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://codespaces.new/spring-petclinic/spring-petclinic-microservices)

[![Open in Codeanywhere](https://codeanywhere.com/img/open-in-codeanywhere-btn.svg)](https://app.codeanywhere.com/#https://github.com/spring-petclinic/spring-petclinic-microservices)

## Deploying to Kubernetes (Recommended)

This application is designed to run on Kubernetes. The K8s manifests are located in the `k8s/` directory.

### Prerequisites

- Kubernetes cluster (kind, minikube, Docker Desktop, or cloud provider)
- kubectl CLI configured
- NGINX Ingress Controller installed

### Quick Start with Kind

```bash
# Create a kind cluster with ingress support
cat <<EOF | kind create cluster --config=-
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
    extraPortMappings:
      - containerPort: 80
        hostPort: 80
        protocol: TCP
      - containerPort: 443
        hostPort: 443
        protocol: TCP
EOF

# Install NGINX Ingress Controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=90s
```

### Build and Deploy

```bash
# Build Docker images for all services
./mvnw clean spring-boot:build-image -pl spring-petclinic-customers-service -DskipTests
./mvnw clean spring-boot:build-image -pl spring-petclinic-vets-service -DskipTests
./mvnw clean spring-boot:build-image -pl spring-petclinic-visits-service -DskipTests
./mvnw clean spring-boot:build-image -pl spring-petclinic-api-gateway -DskipTests

# Load images into kind cluster
kind load docker-image petclinic/customers-service:latest
kind load docker-image petclinic/vets-service:latest
kind load docker-image petclinic/visits-service:latest
kind load docker-image petclinic/api-gateway:latest

# Deploy to Kubernetes (dev environment)
kubectl apply -k k8s/overlays/dev

# Wait for pods to be ready
kubectl wait --namespace petclinic --for=condition=ready pod --all --timeout=300s
```

### Access the Application

Add the following to your `/etc/hosts` file:
```
127.0.0.1 petclinic.local
```

Then access:
- **Frontend UI**: http://petclinic.local/
- **Customers API**: http://petclinic.local/api/customer/owners
- **Vets API**: http://petclinic.local/api/vet/vets
- **Visits API**: http://petclinic.local/api/visit/pets/visits?petId=1
- **Zipkin Tracing**: http://petclinic.local/zipkin
- **Grafana**: http://petclinic.local/grafana (admin/admin)
- **Prometheus**: Port-forward with `kubectl port-forward -n petclinic svc/prometheus 9090:9090`

### Kubernetes Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    NGINX Ingress Controller                  │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│  API Gateway  │   │   Customers   │   │     Vets      │
│    :8080      │   │    Service    │   │    Service    │
└───────────────┘   │    :8081      │   │    :8083      │
        │           └───────────────┘   └───────────────┘
        │                     │
        │           ┌───────────────┐   ┌───────────────┐
        │           │    Visits     │   │    GenAI      │
        │           │    Service    │   │    Service    │
        │           │    :8082      │   │    :8084      │
        │           └───────────────┘   └───────────────┘
        │                     │
        ▼                     ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│     MySQL     │   │   Prometheus  │   │    Grafana    │
│    :3306      │   │    :9090      │   │    :3000      │
└───────────────┘   └───────────────┘   └───────────────┘
                            │
                    ┌───────────────┐
                    │    Zipkin     │
                    │    :9411      │
                    └───────────────┘
```

### K8s Directory Structure

```
k8s/
├── base/                    # Base manifests (namespace, configmap, secret)
├── services/                # Service deployments
│   ├── api-gateway/
│   ├── customers-service/
│   ├── vets-service/
│   ├── visits-service/
│   ├── genai-service/
│   ├── tracing-server/
│   └── mysql/
├── monitoring/              # Prometheus + Grafana
│   ├── prometheus/
│   └── grafana/
├── ingress/                 # Ingress configuration
└── overlays/                # Kustomize overlays
    ├── dev/                 # Development environment
    └── prod/                # Production environment
```

## Starting services locally without Docker

Every microservice is a Spring Boot application and can be started locally using IDE or `../mvnw spring-boot:run` command.
Services can be started independently without any infrastructure dependencies (Config Server and Discovery Server have been removed).

If everything goes well, you can access the following services at given location:
* AngularJS frontend (API Gateway) - http://localhost:8080
* Customers Service - http://localhost:8081
* Visits Service - http://localhost:8082
* Vets Service - http://localhost:8083
* GenAI Service - http://localhost:8084 (requires OpenAI API key)
* Tracing Server (Zipkin) - http://localhost:9411/zipkin/ (we use [openzipkin](https://github.com/openzipkin/zipkin/tree/main/zipkin-server))
* Grafana Dashboards - http://localhost:3030
* Prometheus - http://localhost:9091

Configuration is managed via environment variables or `application.yml` files in each service.

## Starting services locally with docker-compose

Build images using Spring Boot's build-image plugin:
```bash
./mvnw clean spring-boot:build-image -DskipTests
```

This requires Docker to be installed and running. Once images are ready, start them with:
```bash
docker compose up
```

The docker-compose setup includes:
- MySQL database
- All business services (customers, vets, visits, api-gateway, genai)
- Zipkin tracing server
- Prometheus and Grafana for monitoring

Services will connect to each other using Docker's internal DNS. No Config Server or Discovery Server is required.

*NOTE: Under MacOSX or Windows, make sure that the Docker VM has enough memory (at least 4GB) to run the microservices.*


## Starting services locally with docker-compose and Java
If you experience issues with running the system via docker-compose you can try running the `./scripts/run_all.sh` script that will start the infrastructure services via docker-compose and all the Java based applications via standard `nohup java -jar ...` command. The logs will be available under `${ROOT}/target/nameoftheapp.log`. 

Each of the java based applications is started with the `chaos-monkey` profile in order to interact with Spring Boot Chaos Monkey. You can check out the (README)[scripts/chaos/README.md] for more information about how to use the `./scripts/chaos/call_chaos.sh` helper script to enable assaults.

## Understanding the Spring Petclinic application

[See the presentation of the Spring Petclinic Framework version](http://fr.slideshare.net/AntoineRey/spring-framework-petclinic-sample-application)

[A blog post introducing the Spring Petclinic Microsevices](http://javaetmoi.com/2018/10/architecture-microservices-avec-spring-cloud/) (french language)

You can then access petclinic here: http://localhost:8080/

## Microservices Overview

This project consists of several microservices:
- **Customers Service**: Manages customer and pet data (port 8081)
- **Vets Service**: Handles information about veterinarians (port 8083)
- **Visits Service**: Manages pet visit records (port 8082)
- **GenAI Service**: Provides a chatbot interface to the application (port 8084)
- **API Gateway**: Routes client requests and serves the frontend UI (port 8080)

Infrastructure services (deployed separately):
- **MySQL**: Database for persistent storage
- **Zipkin**: Distributed tracing
- **Prometheus**: Metrics collection
- **Grafana**: Metrics visualization

> **Note**: Config Server, Discovery Server, and Admin Server have been removed. Configuration is managed via environment variables/K8s ConfigMaps, service discovery via K8s Services, and monitoring via Prometheus + Grafana.

Each service has its own specific role and communicates via REST APIs.


![Spring Petclinic Microservices screenshot](docs/application-screenshot.png)


**Architecture diagram of the Spring Petclinic Microservices**

![Spring Petclinic Microservices architecture](docs/microservices-architecture-diagram.jpg)

## Integrating the Spring AI Chatbot

Spring Petclinic integrates a Chatbot that allows you to interact with the application in a natural language. Here are some examples of what you could ask:

1. Please list the owners that come to the clinic.
2. Are there any vets that specialize in surgery?
3. Is there an owner named Betty?
4. Which owners have dogs?
5. Add a dog for Betty. Its name is Moopsie.
6. Create a new owner.

![Screenshot of the chat dialog](docs/spring-ai.png)

This `spring-petlinic-genai-service` microservice currently supports **OpenAI** (default) or **Azure's OpenAI** as the LLM provider.
In order to start the microservice, perform the following steps:

1. Decide which provider you want to use. By default, the `spring-ai-openai-spring-boot-starter` dependency is enabled. 
   You can change it to `spring-ai-azure-openai-spring-boot-starter`in the `pom.xml`.
2. Create an OpenAI API key or a Azure OpenAI resource in your Azure Portal.
   Refer to the [OpenAI's quickstart](https://platform.openai.com/docs/quickstart) or [Azure's documentation](https://learn.microsoft.com/en-us/azure/ai-services/openai/) for further information on how to obtain these.
   You only need to populate the provider you're using - either openai, or azure-openai.
   If you don't have your own OpenAI API key, don't worry!
   You can temporarily use the `demo` key, which OpenAI provides free of charge for demonstration purposes.
   This `demo` key has a quota, is limited to the `gpt-4o-mini` model, and is intended solely for demonstration use.
   With your own OpenAI account, you can test the `gpt-4o` model by modifying the `deployment-name` property of the `application.yml` file.
3. Export your API keys and endpoint as environment variables:
    * either OpenAI:
    ```bash
    export OPENAI_API_KEY="your_api_key_here"
    ```
    * or Azure OpenAI:
    ```bash
    export AZURE_OPENAI_ENDPOINT="https://your_resource.openai.azure.com"
    export AZURE_OPENAI_KEY="your_api_key_here"
    ```

## In case you find a bug/suggested improvement for Spring Petclinic Microservices

Our issue tracker is available here: https://github.com/spring-petclinic/spring-petclinic-microservices/issues

## Database configuration

In its default configuration, Petclinic uses an in-memory database (HSQLDB) which gets populated at startup with data.
A similar setup is provided for MySql in case a persistent database configuration is needed.
Dependency for Connector/J, the MySQL JDBC driver is already included in the `pom.xml` files.

### Start a MySql database

You may start a MySql database with docker:

```
docker run -e MYSQL_ROOT_PASSWORD=petclinic -e MYSQL_DATABASE=petclinic -p 3306:3306 mysql:8.4.5
```
or download and install the MySQL database (e.g., MySQL Community Server 8.4.5 LTS), which can be found here: https://dev.mysql.com/downloads/

### Use the Spring 'mysql' profile

To use a MySQL database, you have to start 3 microservices (`visits-service`, `customers-service` and `vets-services`)
with the `mysql` Spring profile. Add the `--spring.profiles.active=mysql` as program argument.

By default, at startup, database schema will be created and data will be populated.
You may also manually create the PetClinic database and data by executing the `"db/mysql/{schema,data}.sql"` scripts of each 3 microservices. 
In the `application.yml` of the [Configuration repository], set the `initialization-mode` to `never`.

If you are running the microservices with Docker, you have to add the `mysql` profile into the (Dockerfile)[docker/Dockerfile]:
```
ENV SPRING_PROFILES_ACTIVE docker,mysql
```
In the `mysql section` of the `application.yml` from the [Configuration repository], you have to change 
the host and port of your MySQL JDBC connection string. 

## Custom metrics monitoring

Grafana and Prometheus are included in the `docker-compose.yml` configuration, and the public facing applications
have been instrumented with [MicroMeter](https://micrometer.io) to collect JVM and custom business metrics.

A JMeter load testing script is available to stress the application and generate metrics: [petclinic_test_plan.jmx](spring-petclinic-api-gateway/src/test/jmeter/petclinic_test_plan.jmx)

![Grafana metrics dashboard](docs/grafana-custom-metrics-dashboard.png)

### Using Prometheus

* Prometheus can be accessed from your local machine at http://localhost:9091

### Using Grafana with Prometheus

* An anonymous access and a Prometheus datasource are setup.
* A `Spring Petclinic Metrics` Dashboard is available at the URL http://localhost:3030/d/69JXeR0iw/spring-petclinic-metrics.
You will find the JSON configuration file here: [docker/grafana/dashboards/grafana-petclinic-dashboard.json]().
* You may create your own dashboard or import the [Micrometer/SpringBoot dashboard](https://grafana.com/dashboards/4701) via the Import Dashboard menu item.
The id for this dashboard is `4701`.

### Custom metrics
Spring Boot registers a lot number of core metrics: JVM, CPU, Tomcat, Logback... 
The Spring Boot auto-configuration enables the instrumentation of requests handled by Spring MVC.
All those three REST controllers `OwnerResource`, `PetResource` and `VisitResource` have been instrumented by the `@Timed` Micrometer annotation at class level.

* `customers-service` application has the following custom metrics enabled:
  * @Timed: `petclinic.owner`
  * @Timed: `petclinic.pet`
* `visits-service` application has the following custom metrics enabled:
  * @Timed: `petclinic.visit`

## Looking for something in particular?

| Component                       | Resources  |
|---------------------------------|------------|
| Kubernetes Deployment           | [K8s manifests](k8s/) and [Kustomize overlays](k8s/overlays/) |
| API Gateway                     | [Spring Cloud Gateway starter](spring-petclinic-api-gateway/pom.xml) and [Configuration](/spring-petclinic-api-gateway/src/main/resources/application.yml) |
| Docker Compose                  | [Spring Boot with Docker guide](https://spring.io/guides/gs/spring-boot-docker/) and [docker-compose file](docker-compose.yml) |
| Circuit Breaker                 | [Resilience4j fallback method](spring-petclinic-api-gateway/src/main/java/org/springframework/samples/petclinic/api/boundary/web/ApiGatewayController.java)  |
| Grafana / Prometheus Monitoring | [Micrometer implementation](https://micrometer.io/), [K8s monitoring manifests](k8s/monitoring/) |
| Distributed Tracing             | [Zipkin deployment](k8s/services/tracing-server/) and [Micrometer Tracing](https://micrometer.io/docs/tracing) |

|  Front-end module | Files |
|-------------------|-------|
| Node and NPM      | [The frontend-maven-plugin plugin downloads/installs Node and NPM locally then runs Bower and Gulp](spring-petclinic-ui/pom.xml)  |
| Bower             | [JavaScript libraries are defined by the manifest file bower.json](spring-petclinic-ui/bower.json)  |
| Gulp              | [Tasks automated by Gulp: minify CSS and JS, generate CSS from LESS, copy other static resources](spring-petclinic-ui/gulpfile.js)  |
| Angular JS        | [app.js, controllers and templates](spring-petclinic-ui/src/scripts/)  |

## Pushing to a Docker registry

You can build and push images to your own Docker registry for Kubernetes deployment.

### Choose your Docker registry

You need to define your target Docker registry.
Make sure you're already logged in by running `docker login <endpoint>` or `docker login` if you're just targeting Docker hub.

Setup the `REPOSITORY_PREFIX` env variable to target your Docker registry.
If you're targeting Docker hub, simple provide your username, for example:
```bash
export REPOSITORY_PREFIX=springcommunity
```

For other Docker registries, provide the full URL to your repository, for example:
```bash
export REPOSITORY_PREFIX=harbor.myregistry.com/petclinic
```

To push Docker image for the `linux/amd64` and the `linux/arm64` platform to your own registry, please use the command line:
```bash
mvn clean install -Dmaven.test.skip -P buildDocker -Ddocker.image.prefix=${REPOSITORY_PREFIX} -Dcontainer.build.extraarg="--push" -Dcontainer.platform="linux/amd64,linux/arm64"
```

The `scripts/pushImages.sh` and `scripts/tagImages.sh` shell scripts could also be used once you build your image with the `buildDocker` maven profile.
The `scripts/tagImages.sh` requires to declare the `VERSION` env variable.

## Compiling the CSS

There is a `petclinic.css` in `spring-petclinic-api-gateway/src/main/resources/static/css`.
It was generated from the `petclinic.scss` source, combined with the [Bootstrap](https://getbootstrap.com/) library.
If you make changes to the `scss`, or upgrade Bootstrap, you will need to re-compile the CSS resources
using the Maven profile `css` of the `spring-petclinic-api-gateway`module.
```bash
cd spring-petclinic-api-gateway
mvn generate-resources -P css
```

## Interesting Spring Petclinic forks

The Spring Petclinic `main` branch in the main [spring-projects](https://github.com/spring-projects/spring-petclinic)
GitHub org is the "canonical" implementation, currently based on Spring Boot and Thymeleaf.

This [spring-petclinic-microservices](https://github.com/spring-petclinic/spring-petclinic-microservices/) project is one of the [several forks](https://spring-petclinic.github.io/docs/forks.html) 
hosted in a special GitHub org: [spring-petclinic](https://github.com/spring-petclinic).
If you have a special interest in a different technology stack
that could be used to implement the Pet Clinic then please join the community there.


## Contributing

The [issue tracker](https://github.com/spring-petclinic/spring-petclinic-microservices/issues) is the preferred channel for bug reports, features requests and submitting pull requests.

For pull requests, editor preferences are available in the [editor config](.editorconfig) for easy use in common text editors. Read more and download plugins at <http://editorconfig.org>.


[Configuration repository]: https://github.com/spring-petclinic/spring-petclinic-microservices-config
[Spring Boot Actuator Production Ready Metrics]: https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-metrics.html

## Supported by

[![JetBrains logo](https://resources.jetbrains.com/storage/products/company/brand/logos/jetbrains.svg)](https://jb.gg/OpenSourceSupport)
