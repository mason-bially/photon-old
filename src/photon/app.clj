(ns photon.app
  "Provides support for building applications. Namely creating photon (and opengl) drawable objects and providing helper functions to access those capabilities. In addition it provides support for event handlers and creating simple opengl only applications."
  (use [clojure.core.incubator :only (defmacro-)])
  (require [photon.opengl])
  (import [javax.media.opengl GLAutoDrawable GLEventListener GLProfile GLCapabilities]
	   [javax.media.opengl.awt GLCanvas]
	   [com.jogamp.opengl.util Animator]))

;; Intialize the jogl library.
;; Always call this before running anything else.
(defn init-photon []
  (. GLProfile initSingleton false))

;;; ##Capabilities

(defmacro- caps-not-nil
  [obj func var] ;;TODO: clean this up with macrolets
  `(let [v# ~var] (if (not (nil? v#)) (. ~obj ~func)))) 

(defn capabilities
  "Builds an immutable object describing a set of capabilities that may be desired of a canvas. Most variables are self explanatory. All the 'bits' and 'count' inputs take ints. All of the 'is' and 'has' inputs take bools.

 - `has-sample-buffers` -> If true then allocate `sample-buffer-count` buffers (if `sample-buffer-count` is `nil` then the system default will be allocated."
  [& {:keys [stencil-bits depth-bits sample-buffer-count
	     accum-alpha-bits accum-red-bits accum-green-bits accum-blue-bits
	     is-stereo is-double-buffered is-hardware-accelerated has-sample-buffers]}]
  (doto (new GLCapabilities nil)
    ;Bool options
    (caps-not-nil setStereo is-stereo)
    (caps-not-nil setSampleBuffers has-sample-buffers)
    (caps-not-nil setDoubleBuffered is-double-buffered)
    (caps-not-nil setHardwareAccelerated is-hardware-accelerated)
    ;Integer options
    (caps-not-nil setStencilBits stencil-bits)
    (caps-not-nil setDepthBits depth-bits)
    (caps-not-nil setNumSamples sample-buffer-count)
    (caps-not-nil setAccumAlphaBits accum-alpha-bits)
    (caps-not-nil setAccumRedBits accum-red-bits)
    (caps-not-nil setAccumGreenBits accum-green-bits)
    (caps-not-nil setAccumBlueBits accum-blue-bits)))


;;; ##Canvas

(defn canvas
  "Constructs an AWT canvas."
  [& {:keys [capabilities]}]
  (if capabilities (new GLCanvas capabilities) (new GLCanvas)))


;;; ##Event Listeners
;;; #GL Event Listener

(defmacro- reify-gl-event-listener
  "Helper macro to reduce number of fn's needed to reify a gl-event-listener"
  [init dispose reshape display]
  `(reify GLEventListener
	  (GLEventListener/display
	   [this# drawable#]
	   (try (~display drawable#)
		(catch java.lang.Exception e# (.printStackTrace e#))))
	  (GLEventListener/init
	   [this# drawable#]
	   (try (~init drawable#)
		(catch java.lang.Exception e# (.printStackTrace e#))))
	  (GLEventListener/dispose
	   [this# drawable#]
	   (try (~dispose drawable#)
		(catch java.lang.Exception e# (.printStackTrace e#))))
	  (GLEventListener/reshape
	   [this# drawable# x# y# width# height#]
	   (try (~reshape drawable# x# y# width# height#)
		(catch java.lang.Exception e# (.printStackTrace e#))))))

(defn- attach-listener-phony-fn
  "Simple phony fn for missing event handlers"
  ([drawable] nil)
  ([drawable x y w h] nil))

(defn attach-gl-listener
  "Takes gl related callbacks and attaches them to the canvas. All callbacks recive the canvas object. The reshape callback also recives the window's new x y coordinates and new width and height. For example (reshape canvas x y width height)."
  [^GLCanvas canvas &{:keys [init dispose reshape display] :or {init attach-listener-phony-fn
								dispose attach-listener-phony-fn
								reshape attach-listener-phony-fn
								display attach-listener-phony-fn}}]
  (. canvas GLCanvas/addGLEventListener
     (reify-gl-event-listener init dispose reshape display)))


;;; ##Context

(defn gl-context
  "Retrive the GL Context from a canvas for use with photon.opengl"
  [drawable]
  (. drawable GLCanvas/getGL))

;;; ##Frames
;;; #Start

(defn start
  "Start an app around the canvas, meant to be used for most simple apps and testing purposes.

Uses an `Animator` to run as fast as possible, it is recommended to use vsync to achive 60 fps.

Uses a new thread to stop the animator, perform the `on-close` callback and then dispose of the frame."
  [canvas & {:keys [name size on-close] :or {name "A Photon App" size '[800 600] on-close (fn [frame] nil)}}]
  (let [frame (new java.awt.Frame name)
	animator (new Animator)]
    (. frame setVisible true)
    (let [insets (.getInsets frame)
          x (+ (nth size 0) (.left insets) (.right insets))
          y (+ (nth size 1) (.top insets) (.bottom insets))]
      (doto frame
        (. setSize x y)
        (. setLayout (new java.awt.BorderLayout))
        (. addWindowListener
           (proxy [java.awt.event.WindowAdapter] []
	   (windowClosing
             [^java.awt.event.WindowEvent e]
             (. (new Thread
                     (proxy [Runnable] []
                       (run
                         []
                         (. animator stop)
                         (on-close frame)
                         (. frame dispose))))
                start))))
        (. add canvas java.awt.BorderLayout/CENTER)
        (. validate)))
    (doto animator
      (. add canvas)
      (. start))
    frame))