(ns fundingcircle.jukebox.alias.snippets
  "Entry point for snippets command."
  (:import cucumber.api.cli.Main))

(defn -main [& args]
  (cucumber.api.cli.Main/main
   (into-array String (concat ["--glue" "regenerate_snippets"] args))))
