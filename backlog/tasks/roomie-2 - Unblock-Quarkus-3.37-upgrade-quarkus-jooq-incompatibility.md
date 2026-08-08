---
id: ROOMIE-2
title: Unblock Quarkus 3.37+ upgrade (quarkus-jooq incompatibility)
status: To Do
assignee: []
created_date: '2026-08-08 12:54'
labels: []
dependencies: []
ordinal: 2000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The Quarkus platform is pinned to 3.36.3 because io.quarkiverse.jooq:quarkus-jooq 2.1.0 (its latest release, built against Quarkus 3.15.1) calls the deprecated ReflectiveClassBuildItem(boolean, boolean, Class...) constructor that Quarkus removed in 3.37.0. On 3.37+ the augmentation step fails with NoSuchMethodError in JooqProcessor#build, so the application cannot be built at all. This blocks every future Quarkus upgrade until it is resolved, either by a new quarkus-jooq release, or by dropping the extension and producing the jOOQ DSLContext from a small CDI producer of our own.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The project builds and boots on the latest Quarkus 3.x release
- [ ] #2 jOOQ DSLContext remains injectable in the same places it is injected today
- [ ] #3 The pin comment on quarkus.platform.version in pom.xml is removed once the constraint no longer applies
<!-- AC:END -->
