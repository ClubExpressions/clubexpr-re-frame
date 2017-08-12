(defproject club "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.854"]
                 [reagent  "0.6.0" :exclusions [cljsjs/react
                                                cljsjs/react-dom]]
                 [re-frame "0.9.4"]]

  :plugins [[lein-cljsbuild "1.1.7"]]

  ;:plugins [[lein-figwheel "0.5.12"]
  ;          [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]

  :min-lein-version "2.5.3"

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]
             :server-port 3451
             :repl true}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.8.2"]]

    :plugins      [[lein-figwheel "0.5.9"]]
    }}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "club.core/mount-root"}
     :compiler     {:main club.core
                    :foreign-libs
                      [{:file "public/js/bundle.js"
                        :provides ["cljsjs.react" "cljsjs.react.dom" "webpack.bundle"]}]
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}
                    }}

    {:id           "min"
     :source-paths ["src/cljs"]
     :compiler     {:main club.core
                    :foreign-libs
                      [{:file "public/js/bundle.js"
                        :provides ["cljsjs.react" "cljsjs.react.dom" "webpack.bundle"]}]
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :simple
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}


    ]}

  )
