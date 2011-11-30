(ns photon.opengl.shaders
  (use [photon.opengl.jogl]
       [clojure.contrib.string :only (as-str)])
  (require [clojure.string]
	   [clojure.contrib.str-utils2 :as str-utils :only [blank?]])
  (import [com.jogamp.opengl.util.glsl ShaderUtil]))

(defglconst gl3 gl-vertex-shader)
(defglconst gl3 gl-fragment-shader)
(defglconst gl3 gl-geometry-shader)



(def fixed-pipeline
     "Fixed pipeline shader"
     {:gl 0})

(defmacro with [program & body]
  "Use a specific shader to render this section"
  `(let [current# *program*]
     (binding [*program* ~program]
       (. *gl* glUseProgram (:gl *program*))
       ~@body
       (. *gl* glUseProgram (:gl current#)))))

(defn- compile-and-attach [shader shader-source program]
  "Compiles shader code onto shader and attaches shader to program"
  (ShaderUtil/shaderSource *gl* shader (into-array String
						   (if (string? shader-source) ;This is to work with both an array and a single string
						     [shader-source]
						     shader-source)))
  (. *gl* glCompileShader shader)
  (. *gl* glAttachShader program shader)
  (ShaderUtil/getShaderInfoLog *gl* shader))

(defn make [ & {:keys [vertex fragment geometry]}]
  "This will compile the string provided for each named shader component it will return a shader program."
  (if (not (and vertex fragment))
    (throw "Need both vertex and fragment shader to make a shader."))
  (let [program (. *gl* glCreateProgram)
	dict (let [vert-shader (. *gl* glCreateShader gl-vertex-shader)
		   frag-shader (. *gl* glCreateShader gl-fragment-shader)
		   vert-log (compile-and-attach vert-shader vertex program)
		   frag-log (compile-and-attach frag-shader fragment program)
		   dict {:gl program :gl-vert vert-shader :gl-frag frag-shader :log-vertex vert-log :log-fragment frag-log}]
	       (if geometry ; Optional part
		 (let [geom-shader (. *gl* glCreateShader gl-geometry-shader)
		       geom-log (compile-and-attach geom-shader geometry program)]
		   (-> dict (assoc :gl-geom geom-shader) (assoc :log-geometry geom-log)))
		 dict))]
    (. *gl* glLinkProgram program)
    (assoc dict :log (ShaderUtil/getProgramInfoLog *gl* program))))

(defn getUniform [program location]
  "Get a shader location"
  (. *gl* glGetUniformLocation (:gl program) location))

(defn getAttrib [program location]
  "Get a shader location"
  (. *gl* glGetAttribLocation (:gl program) location))

(defn bindMatrix4 [location value]
  (let [location (getUniform *program* location)]
    (. *gl* glUniformMatrix4fv location 1 false value 0)))

(defn bindVec3 [location value & [count]]
  (let [location (getUniform *program* location)]
    (. *gl* glUniform3fv location (if count count 1) value 0)))

(defn delete [shader]
  "Deletes a shader from graphics memory"
  ())

(defn valid? [shader]
  "Verify if a shader is valid"
  (if (:log shader)
    (str-utils/blank? (:log shader))
    true))

(defn format [shader]
  "Format the shader"
  (str
   (:log shader)))