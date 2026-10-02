# Training authoring

The English course material is authored with Quarto for graduate computer
science students and software professionals. The current material is a draft:
trainers and learners should test it before any module becomes immutable.

## Current curriculum

Modules 01 to 11 form the testable course. Each module contains a RevealJS
deck and a lab. Modules 12 and 13 are planned in `roadmap.qmd`; they depend on a
future Redis Streams and CQRS implementation.

The NoSQL sequence has two parts:

1. data models, aggregate design, consistency, and technology selection;
2. Redis data structures, persistence, and messaging semantics.

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
