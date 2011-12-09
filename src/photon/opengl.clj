(ns photon.opengl
  "Wraps JOGL and Opengl core graphics opengl graphics include. This is the core namespace, and provides many of the helper functions and macros."
  (use [clojure.core.incubator :only (defmacro-)]
       [clojure.tools.macro :only (macrolet)])
  (require [clojure.string :as str-util]
	   [photon.matrix :as matrix])
  (import [java.nio FloatBuffer IntBuffer]
	  [javax.media.opengl GL2 GL3bc GL4bc]
	  [com.jogamp.opengl.util.glsl ShaderUtil]))

;; The different versions of opengl
(def gl2 GL2)
(def gl3 GL3bc)
(def gl4 GL4bc)

(def ^{:dynamic true
       :doc "The current gl context, because this is so transient it is a bound variable."}
     *gl* nil)

(defmacro with-gl
  "Bind the `*gl*` var."
  [gl-context & body]
  `(binding [*gl* ~gl-context]
     ~@body))


;;; #Stateful Protocol

(defprotocol GLStateful
  "A protocol for representing opengl state-machine changes. Specifically ones which can be wrapped around code and then restored."
  (begin [this gl] "Returns the information needed to end this state later")
  (end [this gl state] "Takes information from begin to recover the old state"))

(defmacro with-state*
  "Wraps a GLStateful object, using the given gl context, around a body of code"
  [gl gl-stateful & body]
  `(let [gl-state# (GLState/begin ~gl-stateful ~gl)]
	 ~@body
	 (GLState/end ~gl-stateful ~gl gl-state#)))

(defmacro with-state
  "Like `with-state*` but resolves the gl-context using `*gl*`"
  [gl-stateful & body]
  `(with-state* *gl* ~gl-stateful ~@body))


;;; #GL Based Defs

(defn lisp-symbol-to-gl-const
  "Converts a lisp symbol (like `gl-color-buffer-bit`) to a OpenGL style symbol string (like `GL_COLOR_BUFFER_BIT`)."
  [symbol]
  (str-util/replace (str-util/upper-case (str symbol)) \- \_ ))

(defmacro defglconst
  "Uses the given symbol to generate a OpenGL style symbol, and attempts to resolve it against gl-import-class, which should be one of [`gl2`, `gl3`, `gl4`] defined above.

It is also notable the way that this code gets at these static classes is a bit convoluted due to the fact that `.` treats symbols of static classes, and those actual static classes differently. Namely `(. StaticClass StaticMember)` and `(. (identity StaticClass) StaticMember)` are not equivelent. Hence this macro splices the symbol of the static class into an exper which is inserted into the def and evaluated as the intial value."
  [gl-import-class import-name]
  (let [gl-name (lisp-symbol-to-gl-const import-name)
	value `(. ~(symbol (.getName (eval gl-import-class)))
		  ~(symbol gl-name))]
    `(def ^:const ^int ~import-name ~value)))

;;; #GL Types
;; All opengl backed objects in this library have the following fields:
;;  - `:gl-type` This is the type of the gl-backed object. This is how our multimethods are dispatched.
;;  - `:gl-int` This is the integer assigned to the object by opengl.

(defmulti get-log
  "Get any logs that this object may have, from opengl" :gl-type)


;;; #Other helpers
(defmacro- opengl-str [symbol]
  `(if (keyword? ~symbol)
     (str-util/replace-first (str ~symbol) ":" "")
     (str ~symbol)))
     
;;; #Loading other sections

(load "opengl_shaders")
(load "opengl_buffers")