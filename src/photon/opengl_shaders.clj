(ns photon.opengl
  "#Shaders

This file handels shaders, programs of shaders, and uniform binding")

;;; ##GL Constants

;; Shader types
(defglconst gl4 gl-vertex-shader)
(defglconst gl4 gl-fragment-shader)
(defglconst gl4 gl-geometry-shader)

(def gl-fixed-pipeline-program
     "The defualt fixed pipeline program."
     {:gl-int 0})

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

(defn make-shader
  "Makes a shader from source, type may either be `:vertex`, `:fragment`, `:geometry`."
  [source type & {:keys [attributes uniforms ins outs]}]
  (let [shader-type (condp = type
			:vertex gl-vertex-shader
			:fragment gl-fragment-shader
			:geometry gl-geometry-shader)
	shader (.glCreateShader *gl* shader-type)]
    (.glCompileShader *gl* shader)
    {:gl-type :shader
     :glint shader
     :source source
     :type type
     :ins (merge attributes ins)
     :outs outs
     :uniforms uniforms}))

(defmethod get-log :shader [s]
  (ShaderUtil/getShaderInfoLog *gl* s))

;;; ##Programs

(defn make-program
  "Makes a program from a number of shaders."
  [vertex-shader fragment-shader & geometry-shader]
  (let [program (.glCreateProgram *gl*)
	shaders (conj geometry-shader [vertex-shader fragment-shader])]
    (.glAttachShader *gl* program vertex-shader)
    (.glAttachShader *gl* program fragment-shader)
    (when (not (nil? geometry-shader))
      (.glAttachShader *gl* program geometry-shader))
    (.glLinkProgram *gl* program)
    {:gl-type :program
     :glint program
     :shaders {:vertex vertex-shader :fragment fragment-shader :geometry geometry-shader}
     :attributes (:ins vertex-shader)
     :uniforms (reduce merge (map :uniforms shaders))}))

(defmethod get-log :program [p]
  (ShaderUtil/getProgramInfoLog *gl* p))

(defn use-shader
  [program]
  (.glUseProgram *gl* (:gl-int program)))

;;; ##Locations

(defn glfn-get-uniform [program location]
  "Get a shader uniform location"
  (.glGetUniformLocation *gl* (:gl-int program) location))

(defn glfn-get-attrib [program location]
  "Get a shader attribute location"
  (.glGetAttribLocation *gl* (:gl-int program) location))