(defproject photon "0.0.2"
  :description "An idiomatic wrapper for OpenGL using JOGL"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/clojure-contrib "1.2.0"]]
  :dev-dependencies [[swank-clojure "1.3.0"]]
  :main photon.examples.parametric-viewer
  :disable-deps-clean true)
