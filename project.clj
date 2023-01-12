(defproject d-qqt "0.1.0-SNAPSHOT"
  :repositories [["clojars" {:url "https://clojars.org/repo"
                             :sign-releases false}]]
  :clean-targets ^{:protect false} ["target"]
  :main d-qqt.start
  :aot [d-qqt.start])
