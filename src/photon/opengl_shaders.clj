(ns photon.opengl
  "#Shaders
This file handels shaders, programs of shaders, and uniform binding")

;;; ##GL Constants

;; Shader types
(defglconst gl2 gl-vertex-shader)
(defglconst gl2 gl-fragment-shader)
(defglconst gl3 gl-geometry-shader)

;; Special Values
(def fixed-pipeline-program
     "The defualt fixed pipeline program."
     {:gl-type :program
      :gl-int 0})

;; Dynamic vars
(def ^{:dynamic true} *program*
     "This is part of the current global state, it is handy to have it available"
     fixed-pipeline-program)

;;; ##Shaders
;; Attributes, ins, outs, and uniforms are a mapping from a string to the symbol that represents their type. Valid types are as per glsl
;;   - `:bool`
;;   - `:int`, `:uint`
;;   - `:float`
;;   - `:vec2`, `:vec3`, `:vec4`
;;   - `:bvec2`, `:bvec3`, `:bvec4`
;;   - `:ivec2`, `:ivec2`, `:ivec3`
;;   - `:uvec2`, `:uvec2`, `:uvec3`
;;   - `:mat2`, `:mat3`, `:mat4`
;;   - `:mat2x2`, `:mat2x3`, `:mat2x4`
;;   - `:mat3x2`, `:mat3x3`, `:mat3x4`
;;   - `:mat4x2`, `:mat4x3`, `:mat4x4`

(defn make-program-part
  "Makes a shader from source, type may either be `:vertex`, `:fragment`, `:geometry`."
  [type source & {:keys [attributes uniforms ins outs]}]
  (let [shader-type (condp = type
			:vertex gl-vertex-shader
			:fragment gl-fragment-shader
			:geometry gl-geometry-shader)
	shader (.glCreateShader *gl* shader-type)]
    (ShaderUtil/shaderSource *gl* shader
			     (into-array String
					 (if (string? source) ;This is to work with both a seq and a single string
					   [source]
					   source)))
    (.glCompileShader *gl* shader)
    {:gl-type :program-part
     :gl-int shader
     :source source
     :type type
     :ins (merge attributes ins)
     :outs outs
     :uniforms uniforms}))

(defmethod get-log :program-part [s]
  (ShaderUtil/getShaderInfoLog *gl* (:gl-int s)))

;;; ##Programs

(defn make-program
  "Makes a program from a number of shaders."
  [vertex-shader fragment-shader & geometry-shader]
  (let [program (.glCreateProgram *gl*)
	shaders (conj geometry-shader [vertex-shader fragment-shader])]
    (.glAttachShader *gl* program (:gl-int vertex-shader))
    (.glAttachShader *gl* program (:gl-int fragment-shader))
    (when (not (nil? geometry-shader))
      (.glAttachShader *gl* program (:gl-int geometry-shader)))
    (.glLinkProgram *gl* program)
    {:gl-type :program
     :gl-int program
     :shaders {:vertex vertex-shader :fragment fragment-shader :geometry geometry-shader}
     :attributes (:ins vertex-shader)
     :uniforms (reduce merge (map :uniforms shaders))}))

(defmethod get-log :program [p]
  (ShaderUtil/getProgramInfoLog *gl* (:gl-int p)))

(defn use-shader
  "Explicitly use a shader, it is recommeneded to avoid this method. Use `with-shader` instead."
  [program]
  (.glUseProgram *gl* (:gl-int program)))

(defmacro with-shader
  "A macro that helps manageopengl state"
  [program & body]
  `(let [prev-program# *program*]
     (binding [*program* ~program]
       (use-shader *program*)
       ~@body
       (use-shader prev-program#))))

;;; ##Uniforms

(defn get-uniform
  "Get a shader uniform location"
  [location & program]
  {:gl-type :uniform
   :gl-int (.glGetUniformLocation *gl* (:gl-int (if (nil? program) *program* program)) location)})

(defn- uniform-type-to-glfn-bind-name
  [type array]
  (format (if array
	    "glUniform%sv"
	    "glUniform%s")
	  (condp = type
	      "float" "1f"
	      "int" "1i"
	      "uint" "1ui"
	      (condp #(= %1 (subs %2 0 3)) type
		  "vec" (format "%sf" (subs type 3 4))
		  "uve" (format "%sui" (subs type 4 5))
		  "ive" (format "%si" (subs type 4 5))
		  "mat" (format "Matrix%sf" (subs type 3))))))
						 

(defn- uniform-type-to-glfn-bind
  [type array]
  (symbol (uniform-type-to-glfn-bind-name
	   (opengl-str type)
	   array)))

(defmacro bind-uniform
  "Binds a uniform of the glsl type given to a location (gotten with `get-uniform`) in the current program of the given value."
  [type location values]
  (if (= ":mat" (subs (str type) 0 4))
    `() ;Throw exeception
    `(. *gl* ~(uniform-type-to-glfn-bind type false) (:gl-int ~location) ~@values)))

(defmacro bind-uniforms
  "Binds a uniform of the glsl type given to a location (gotten with `get-uniform`) in the current program of the given values, values must be a clojure array-type that matches the given type."
  [type location values count]
  (if (= ":mat" (subs (str type) 0 4))
    `(. *gl* ~(uniform-type-to-glfn-bind type true) (:gl-int ~location) ~count true (matrix/to-opengl ~values) 0)
    `(. *gl* ~(uniform-type-to-glfn-bind type true) (:gl-int ~location) ~count (matrix/to-opengl ~values) 0)))

;;; ##Attributes

(defn get-attrib
  "Get a shader attribute location"
  [location & program]
  (.glGetAttribLocation *gl* (:gl-int (if (nil? program) *program* program)) location))

;;; #Helper Macros

(defmacro defshader
  "A helpful shader macro"
  [name & {:keys [attributes vertex-uniforms vertex fragment-uniforms fragment] :as data}]
  `(def ~name (agent ~data)))

(defn load-defshader
  "Loads a defshader onto the card"
  [defedshader]
  (make-program
   (make-program-part :vertex (:vertex defedshader) :uniforms (:vertex-uniforms defedshader) :attributes (:attributes defedshader))
   (make-program-part :fragment (:fragment defedshader) :uniforms (:fragment-uniforms defedshader))))
	       