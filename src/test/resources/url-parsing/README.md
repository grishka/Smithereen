# web-platform-tests

This directory contains URL parsing test data from the [WPT](https://github.com/web-platform-tests/wpt) project.

See the corresponding [README]([https://github.com/web-platform-tests/wpt/blob/master/url/README.md](https://github.com/web-platform-tests/wpt/blob/a6f29b0bedaf3f1edba7b6739127fe8e713bfcb3/url/README.md)) for more.

### How to run

Run the `smithereen.URLNormalizationTest#testNormalizeUrl` test. It will load the JSON file from this directory
and try to parse the URLs.

Note that some test cases are muted (indicated by `"muted": true` in the JSON file), and some are disabled.
Disabled tests are never run, and muted tests are always run, but their result is inverted.

We disable tests when it doesn't make sense to support a particular corner case in Smithereen.

We mute tests if we currently parse the URL incorrectly, and this is a bug that we plan to fix. When the bug is fixed,
the test will start failing, reminding us to unmute it.