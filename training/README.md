# Training authoring

The English course material is authored with Quarto for graduate computer
science students and software professionals. The current material is a draft:
trainers and learners should test it before any module becomes immutable.

## Current curriculum

Modules 01 to 13 form the testable course. Each module contains a RevealJS
deck and a lab. Modules 12 and 13 distinguish aggregate persistence from a
query projection; Redis Streams remains a future comparison in `roadmap.qmd`.

The NoSQL sequence has three parts:

1. data models, aggregate design, consistency, and technology selection;
2. Redis data structures, persistence, and messaging semantics;
3. MongoDB document manipulation, aggregate persistence, and query projections.

Tutorial 08b bridges Redis CLI and Java with two isolated Spring Boot services
connected by one Redis List. Its standalone code lives under
`training/demos/redis-list-spring` and is intentionally separate from the
rental platform implementation. The companion 08b+ assignment evolves this
deliberately naive delivery into acknowledgement, recovery, and idempotence;
its optional tracks expose the limits of building messaging semantics on Lists.

Tutorial 11b introduces MongoDB through `mongosh` before Spring Data hides the
storage operations. Learners manipulate an isolated `rental_training` database
to practise BSON, nested documents, CRUD, update operators, indexes, query
plans, aggregation pipelines, and single-document atomicity. Tutorial 11c then
maps the same ideas to an isolated Spring Boot application using `@Document`,
embedded values, `MongoRepository`, derived queries, and index-backed
integrity. Modules 12 and 13 apply that foundation to the insurance aggregate
and rental projection.

## Review workflow

```bash
quarto preview training
quarto render training
```

During evaluation, edit any deck, lab, or supporting code as needed. The CI
pipeline renders all active modules but does not compare checksums.

## Publication workflow, deferred

After classroom testing:

1. revise the module from learner feedback;
2. render and inspect the complete deck;
3. create a checksum manifest with `training-ci create-manifest`;
4. enable `training-ci check-frozen` in CI;
5. tag the published state.

No manifest should be created before that decision.

## Public solutions

Only the reference solutions for Lab 08 and Lab 11b are published with the
learner material. All other instructor solutions remain local and excluded
from Git.
