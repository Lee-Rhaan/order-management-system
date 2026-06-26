### Spring Boot Order Management System — CI/CD with GitHub Actions, Docker Hub & Render (PostgreSQL)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Lee-Rhaan_order-management-system&metric=coverage)](https://sonarcloud.io/summary/new_code?id=Lee-Rhaan_order-management-system)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Lee-Rhaan_order-management-system&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Lee-Rhaan_order-management-system)

### Project Overview:
### Tech: 
|Java 17|Spring Boot 3|Maven|JPA|Thymeleaf|PostgreSQL|Docker|Docker Hub|
|---|---|---|---|---|---|---|---|

|GitHub Actions|SonarCloud|JaCoCo|Render|Uptime Robot|MySQL|H2|
|---|---|---|---|---|---|---|

---
### Role: Solo developer (design, build, CI/CD, quality gates, deployment)
##### PROJECT LINK:
https://order-management-system-inoe.onrender.com

---

### Highlights

Designed and delivered a production-grade CI/CD pipeline that builds, tests (unit+integration), analyzes quality (SonarCloud + JaCoCo coverage), containerizes the app, pushes images to Docker Hub with date+latest tags, and auto-deploys to Render via deploy hooks.

Migrated the app from MySQL to PostgreSQL to streamline deployment on Render’s free tier; externalized configuration via environment variables and secure GitHub Secrets.

Implemented test coverage reporting (JaCoCo XML + HTML), quality gate on SonarCloud, dependency caching (Maven & Sonar cache), and reproducible builds.

Hardened the pipeline and runtime: least-privilege tokens, secret-based auth, immutable image tags, and no local build/storage dependency.

### Key outcomes

- Zero-touch deployments on push to main.

- Repeatable, portable builds via Docker; no local storage required.

- Actionable code quality metrics (bugs/vulns/smells + coverage trend) surfaced on every PR/commit.

- Cloud-hosted app backed by managed PostgreSQL, with environment-based configuration for easy rotation and rollback.

---
### The Story & Decisions (detailed, step-by-step)
#### Baseline app & initial constraints
##### Starting point:
> A Java Spring Boot + Thymeleaf Order Management System with MySQL, built with Maven.

##### Constraint:
> Local machine storage was limited; builds and image creation had to run entirely in the cloud.

##### Goal:
> Fully automated pipeline—code changes → tested → analyzed → containerized → pushed → deployed.

##### Why:
> Avoid local bottlenecks, ensure reproducibility, and get to a cloud-native, infrastructure-as-code workflow.

---
#### Containerization & registry
##### Dockerfile: 
- Simple, production-friendly image based on openjdk:17-jdk-slim.
- Copies the Spring Boot fat JAR → app.jar.
- Uses Java 17; minimal surface area.

##### Registry:
- Docker Hub chosen for public availability and easy integration with Render.

##### Tags:
- CI builds attach date tag (YYYY-MM-DD) and latest to each push for traceability and rollback.

##### Why:
- Docker images make the app portable across any host with a Docker runtime. Versioned tags enable deterministic rollbacks.

---
#### CI/CD pipeline (GitHub Actions) — “production-ish” standards

---
#### Job A: Build, Test & Static Analysis
##### Triggers:
- On push to main & manual dispatch.

##### Caching:
- Maven cache (~/.m2/repository) speeds up builds.
- Sonar cache (~/.sonar/cache) shortens analysis time.

##### Tests + Coverage:
- mvn clean verify runs unit/integration tests and JaCoCo (XML for Sonar, HTML for local viewing).

##### SonarCloud analysis:
- CI-based analysis using sonar-maven-plugin with -Dsonar.host.url, -Dsonar.organization, -Dsonar.projectKey.
- SONAR_TOKEN provided as a GitHub Secret.
- Quality gate provides fail-fast visibility on code issues.

##### Why:
> Fast feedback, automated quality controls, and measurable coverage—key qualities of robust CI.

---
#### Job B: Dockerize & Deploy
##### Dependency:
- Sonarcloud ensures we only ship if analysis completes.

##### Build:
- mvn clean package -DskipTests (tests already ran).

##### Docker login → build → push:
- Image pushed to Docker Hub with date and latest tags.

##### Render deploy hook:
- POST call tells Render to pull the new image and restart the service—no manual steps.

##### Why:
> Safe, gated delivery with clear artifact promotion from “built & tested” to “running in prod.”

---
#### Secrets & configuration (12-factor style)
##### GitHub Secrets:
- DOCKER_USERNAME, DOCKER_PASSWORD, SONAR_TOKEN, RENDER_DEPLOY_HOOK_URL.

##### App configuration:
- Moved database settings to environment variables consumed by Spring:
> DB_URL, DB_USERNAME, DB_PASSWORD (mapped to spring.datasource.*).

##### Render Environment:
Set variables from the Render PostgreSQL Connection Info page, not hard-coded in the repo.

##### Why:
> Security (no creds in code), easy rotation, and parity across environments.

---
#### Database migration: MySQL → PostgreSQL
##### Reason:
- Free and simpler to host on Render; avoids external MySQL service or disk mounting on free tier.

##### Changes:
- Switched driver to org.postgresql:postgresql.
- Updated application.properties to use jdbc:postgresql://… and dialect org.hibernate.dialect.PostgreSQLDialect.
- Ensured spring.jpa.hibernate.ddl-auto=update for first-run schema creation.

##### Result:
- One Render web service (Docker) + one Render managed PostgreSQL DB, with clean environment variable wiring.

##### Why:
> Lower operational overhead and simpler, cost-free deployment path.

---
#### Test strategy & pipeline stability
##### Integration tests:
- Avoided external DB by using H2 or testcontainers profile locally/CI, so tests run without external dependencies.

##### Fixes:
- Addressed Spring ApplicationContext test startup errors by ensuring test DB config is isolated from prod settings.

##### Why:
> Deterministic, fast, and isolated CI runs (no flaky DB dependencies).

---
#### Code quality program (SonarCloud + JaCoCo)
##### Coverage:
- JaCoCo XML report published for Sonar; HTML report for local inspection.

##### Rules:
- Sonar scans track bugs, vulnerabilities, code smells, duplication, and coverage.

##### Branch setup:
Corrected Sonar default branch (renamed master → main) to ensure results show without upgrade prompts.

##### Why:
> Maintainability, reliability, and long-term health of the codebase—measured, not guessed.

---
#### Deployment & runtime
#### Render (Docker deploy):

##### Deploy method:
- Docker → Render pulls image from Docker Hub.

##### Environment variables:
- Set DB credentials/URL from Render’s PostgreSQL.

##### Auto-deploy:
- Triggered by GitHub Actions after push.

##### Runtime behavior:
- Container runs on Render; no local machine resources required.
- External URL provided by Render.

##### Why:
> A lightweight, cloud-hosted runtime that fits the free tier and maintains separation of build and runtime concerns.

---
#### Security & reliability practices
- Secrets isolated in CI; no credentials in code.
- Immutable version tags for traceability and rollback.
- Clear separation of concerns: build/test/analyze vs. deliver/deploy.
- Externalized configuration for easy rotation.
- Minimal base image (slim JDK) to reduce attack surface.

---
#### What “production-level” means here
- Automated quality gates (SonarCloud + JaCoCo) before deploy.
- Fail-fast pipeline with job dependencies.
- Immutable Docker artifacts with version tags.
- Environment-based configuration & secret management.
- Reproducible builds via caching and pinned runtimes.
- Hands-off deployments on push to main.
