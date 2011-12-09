;; You can find most of OpenGL Java classes at:
;; <http://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/>

(defproject photon "0.0.2"
  :description "An idiomatic wrapper for OpenGL using JOGL."
  :dependencies [[org.clojure/clojure "1.3.0"]
		 [org.clojure/core.incubator "0.1.0"]
		 [org.clojure/tools.macro "0.1.1"]
		 [org.clojars.nakkaya/vecmath "1"]]
  :dev-dependencies [[swank-clojure "1.3.0"]]
  :main photon.examples.dungeon
  :disable-deps-clean true)