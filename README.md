```
USER ->
COMMAND STORE [OUTBOX] -> CMD BUS -> HANDLER ->
SERVICE [STATE R/W, DISTRIBUTED_LOCK] -> 
EVENT STORE [OUTBOX] -> EVENT BUS -> PROJECTOR

-----------------------

USER -> PROJECTOR
```


## User vs EDDI

User only sees `CommandStore` and `Projector`.

