# juke-cucumber

This is a library that integrates cucumber with [juke](..)

## Usage

Add the library to your project dependencies:

```clojure
:dependencies [[fundingcircle/juke "0.1.0"]
               [fundingcircle/juke-cucumber "0.1.0"]]
```

Run the cucumber driver, providing it the path to your clojure "glue"
and feature definitions. A lein alias like this in your project.clj
will run your tests with `lein cucumber`.
```clojure
{:aliases {"cucumber" ["run" "-m" "cucumber.api.cli.Main"
                       "--glue" "test/example"
                       "--plugin" "json:cucumber.json"
                       "test/features"]}}
```

## License

Copyright © 2018 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
