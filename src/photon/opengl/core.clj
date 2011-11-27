(ns photon.opengl.core
  (:use [clojure.contrib.def :only (defn-memo defmacro-)]
	[clojure.contrib.string :only (as-str)])
  (:require [clojure.contrib.str-utils2 :as str-util])
  (:import [javax.media.opengl GL4bc]))

;;; Global
(def ^:dynamic *gl*
     "The current GL context"
     nil)

;;; Shaders

(def ^:dynamic *program*
     "The current shader program"
     {:gl 0})


;;;; Universal helper functionality

(defn- lisp-symbol-to-gl-const [symbol]
  "Converts a lisp symbol to a GL constant string"
  (str-util/replace (str-util/upper-case (as-str symbol)) \- \_ ))

(defmacro def-gl-const [import-name]
  "Defines a symbol with the value of the gl constant based on the lispy name"
  (let [gl-name (lisp-symbol-to-gl-const import-name)
	value `(. GL4bc ~(symbol gl-name))]
    `(def ^:const ^int ~import-name ~value)))