(ns photon.app
  (:use [clojure.contrib.def :only (defmacro-)])
  (:require [photon.opengl.core])
  (:import [javax.media.opengl GLAutoDrawable GLEventListener GLProfile GLCapabilities]
	   [javax.media.opengl.awt GLCanvas]
	   [com.jogamp.opengl.util Animator]))

(. GLProfile initSingleton false)

;;TODO clean this up with macrolets
(defmacro- caps-not-nil [obj func var]
  "If var is not nil then perform the if test on it"
  `(let [v# ~var] (if (not (nil? v#)) (. ~obj ~func)))) 

(defn capabilities [& {:keys [stencil-bits depth-bits sample-buffer-count
			      accum-alpha-bits accum-red-bits accum-green-bits accum-blue-bits          ;int params
			      is-stereo is-double-buffered is-hardware-accelerated has-sample-buffers   ;bool params
			      ]}]
  "Builds an immutable object describing a set of capabilities that may be desired of a canvas"
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
    
    
(defn canvas [& {:keys [init destroy reshape display
			capabilities]}]
  "Takes a variety of callbacks and a capabilities object, which are used to construct an AWT canvas"
  (let [^GLEventListener listener (proxy [GLEventListener] []
				    (display
				     [drawable]
				     (binding [photon.opengl.core/*gl* (. drawable getGL)]
				       (try (display drawable)
					    (catch java.lang.Exception e (.printStackTrace e)))))
				    (init
				     [drawable]
				     (binding [photon.opengl.core/*gl* (. drawable getGL)]
				       (try (init drawable)
					    (catch java.lang.Exception e (.printStackTrace e)))))
				    (destroy
				     [drawable]
				     (binding [photon.opengl.core/*gl* (. drawable getGL)]
				       (try (destroy drawable)
					    (catch java.lang.Exception e (.printStackTrace e)))))
				    (reshape
				     [drawable x y width height]
				     (binding [photon.opengl.core/*gl* (. drawable getGL)]
				       (try (reshape drawable x y width height)
					    (catch java.lang.Exception e (.printStackTrace e))))))
	^GLCanvas canvas (if capabilities (new GLCanvas capabilities) (new GLCanvas))]
    (.addGLEventListener canvas listener)
    canvas))


(defn start [canvas & {:keys [name size on-close] :or {name "A Photon App" size '[800 600] on-close (fn [] nil)}}]
  "Start an app around the canvas, meant to be used for most simple apps and testing purposes"
  (let [frame (new java.awt.Frame name)
	animator (new Animator)]
    (doto frame
      (. setSize (nth size 0) (nth size 1))
      (. setLayout (new java.awt.BorderLayout))
      (. addWindowListener (proxy [java.awt.event.WindowAdapter] []
			     (windowClosing
			      [^java.awt.event.WindowEvent e]
			      (. (new Thread (proxy [Runnable] []
					       (run
						[]
						(. animator stop)
						(on-close frame)
						(. frame dispose))))
				 start))))
      (. add canvas java.awt.BorderLayout/CENTER)
      (. validate)
      (. setVisible true))
    (doto animator
      (. add canvas)
      (. start))
    frame))
			       