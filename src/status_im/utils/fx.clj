(ns status-im.utils.fx
  (:refer-clojure :exclude [defn]))

(defmacro defn
  "Defines an fx producing function
  Takes the same arguments as the defn macro
  Produces a 2 arity function:
  - first arity takes the declared parameters and returns a function that takes cofx as
  single argument, for use in composition of effects
  - second arity takes cofx as first arguments and declared parameters as next arguments,
  for use in repl or direct call
  Note: destructuring of cofx is possible
  TODO(yenda): add support for metadata and docstring"
  [name args & fdecl]
  (let [[cofx & args] args
        cofx-sym (gensym "cofx")]
    `(clojure.core/defn ~name
       ([~@args] (fn [~cofx-sym] (~name ~cofx-sym ~@args)))
       ([~cofx ~@args] ~@fdecl))))
