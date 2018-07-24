# Blackbox tests

This is a refinement and globalization of [us-blackbox](https://github.com/fundingcircle/us-blackbox).

## Vision & Features
* Teams can author tests on timelines that match their product's
* Provide confidence in deploys
* Support continuous deployment
* Greate developer experience for new tests and troubleshooting
* Rich test reporting
* Resumability of test runs
* Rich diagnostics
* Support red/yellow/green test status
* Support eUATs

## Running the tests
You'll need to link your cvseeds:
```
ln -s ~/code/cvseeds .cvseeds
```

To run the tests locally against an environment:
```
scripts/tunnel -e staging lein scenari
```

(You'll need to update your /etc/hosts with the entries printed by `tunnel`.)

After a test run, diagnistics are produced for each step:
```
჻ tree diagnostics
diagnostics
└── steps
    ├── 0-create-investor-success-postmortem
    │   ├── chrome-127.0.0.1-30386-2018-07-24-09-11-09.html
    │   ├── chrome-127.0.0.1-30386-2018-07-24-09-11-09.json
    │   └── chrome-127.0.0.1-30386-2018-07-24-09-11-09.png
    ├── 0-create-investor-success.clj
    ├── 1-post-wire-deposit-failure-postmortem
    ├── 1-post-wire-deposit-failure.clj
    ├── 2-deposit-status-failure-postmortem
    │   ├── chrome-127.0.0.1-47723-2018-07-24-09-11-31.html
    │   ├── chrome-127.0.0.1-47723-2018-07-24-09-11-31.json
    │   └── chrome-127.0.0.1-47723-2018-07-24-09-11-31.png
    └── 2-deposit-status-failure.clj
```

This includes screenshots, javascript logs, HTML, and clojure code containing fixtures and code to execute each step. To work with the test run fixtures and steps, copy the test output into <troubleshooting/troubleshooting.clj>
```
cat diagnostics/steps/*.clj | pbcopy
```
