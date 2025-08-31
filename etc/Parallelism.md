# Parallelism

Requests are coming independently, and each request is handled in its own thread. Each request starts a command that is added to a **command queue** (implemented by **command store** and **outbox** component).

Command store publishes the commands to the **command queue**. Command messages are distributed as `one-of` to the available **command handlers**: _only one handler_ will get a command message and process it, regardless of how many handlers are available. WHY: to scale out the processing of commands.

```
N threads -> command queue -> M threads (command handlers).
```

**Service** is, essentially, a **command processor**.

## Events and Event Queue

When a command is processed, it may result in one or more **events** being generated. These events are published to the **event queue**.

Events are distributed as `fan-out` to all the available listeners:

+ **Projections** that update the read-only views of the state.
+ Other event handlers that may trigger additional events (`Event -> [Event]`).

⚠️ Event should not modify the state ?

⚠️ Events should not directly run commands! The idiomatic way to do the same is to use a **saga**.


## Projections

Projections are read-only views of the state. They are built by processing **events** that are published to the **event queue** by services.

Projection is quite simple concept. It is a function that takes an event and updates the projection state. That's it.

We don't care how the projection state is stored. It can be in-memory, in a database, in a file, etc. We just need to ensure that the projection state is updated in a consistent manner.

## Service Registry and Services

Service operates on a **State**. No matter how the state is constructed, it is always **owned** by a **Service**.

⚠️ Since services run in parallel, we need to ensure that only one service instance is operating on a given state at any time.

> We must ensure that only one service instance is operating on a given state at any time!

That's the whole gist of it.

In order to achieve that, the state must have a _unique identifier_ so we can differentiate between different states.

To achieve above:

1. Have a LOCK per state (e.g. per user, per account, etc.). Equivalent to table-lock. When multiple states need to be updated in a single command, it would need to lock multiple queues. This is not scalable.
2. Have a LOCK per state identifier. Equivalent to row-lock. This is scalable.

Lock is either a 1) queue or 2) a distributed lock (e.g. Redis, Zookeeper, etc.). Queue is preferred as it provides better scalability. However, we often cant create that many queues (e.g. millions of users). In that case, we can use a combination of both.

Distributed lock may be pessimistic (just lock) or optimistic (dont lock, check the state at the time of operation).

So far nothing new.

🔥 Is there a better way to operate with state in parallel?

For example, command `StudentEnrollInCourse(studentId, courseId)` operates on Student state. We must be sure if `TuitionPaid` event is processed before `StudentEnrollInCourse` command is processed. The command need a _valid_ state to operate on.

Default approach: explicit, manual lock per `studentId & courseId`.

Different approach, but first - what do we want to change? We want to change the ownership of the state, _towards actor idea_. Instead of having a "general" entity-first state, how about to split it?
