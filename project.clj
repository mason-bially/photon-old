;; You can find most of OpenGL Java classes at:
;; <http://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/>

(defproject photon "0.0.5"
  :description "An idiomatic wrapper for OpenGL using JOGL."
  :dependencies [[org.clojure/clojure "1.4.0"]
		 [org.clojure/core.incubator "0.1.1"]
		 [org.clojure/tools.macro "0.1.1"]
		 [org.clojars.nakkaya/vecmath "1"]
                 [org.clojars.biallym/jogl.all "2.0-rc9"]
                 [org.clojars.biallym/gluegen-rt "2.0-rc9"]
                 [org.clojars.biallym/jogl-native-all "2.0-rc9"]]
  :main photon.examples.dungeon
  )
