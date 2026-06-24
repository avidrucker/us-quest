(ns adventure.db
  "The re-frame app-db: a single source of truth for the whole UI.")

(def default-db
  "Initial app-db. `:route` selects the active view; `:library` holds saved
   adventures keyed by id; `:editor` and `:player` hold per-view working state."
  {:route   :library     ; :library | :editor | :player
   :library {}           ; <adventure-id> -> adventure (see adventure.domain)
   :editor  {}
   :player  {}})
