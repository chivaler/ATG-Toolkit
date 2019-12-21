# Contributing to ATG-Toolkit
:+1::tada: First off, thanks for taking the time to contribute! :tada::+1:

## Disclaimer
Given project was started as useful PET project which makes ATG development more convenient by providing benefits and capabilities similar to Spring plugin (IoC related ones)
At this moment plugin lacks test, some capabilities are very raw.
I had experience with only two ATG codebases with their conventions/standards, where I was able to test give plugin.
I Assume, that plugin could require adaptations to become either more generic or vice-versa support smth project-specific like own-written annotations.

## How Can I Contribute?
### Reporting Bugs
* **Check if you can reproduce** the problem in latest IDEA/plugin versions
* **Raise an issue** at [Issues page](https://github.com/chivaler/ATG-Toolkit/issues) if nothing related was raised/discussed before.
* **Use a clear and descriptive title** for the issue to identify the problem
* **Describe the exact steps which reproduce the problem** in as many details as possible
* **Attach stacktraces** if exception is thrown during reproducing
* **Provide specific examples to demonstrate the steps**. Include links to files or GitHub projects, or copy/pasteable snippets, which you use in those examples. If you're providing snippets in the issue, use [Markdown code blocks](https://help.github.com/articles/markdown-basics/#multiple-lines)
* **Describe the behavior you observed after following the steps** and point out what exactly is the problem with that behavior.
* **Explain which behavior you expected to see instead and why.**
* **Include screenshots and animated GIFs** (if suitable) which help you demonstrate the steps. You can use [this tool](https://www.cockos.com/licecap/) to record GIFs on macOS and Windows, and [this tool](https://github.com/colinkeenan/silentcast) or [this tool](https://github.com/GNOME/byzanz) on Linux.

### Suggesting Enhancements
* **Raise an issue** at [Issues page](https://github.com/chivaler/ATG-Toolkit/issues) if nothing related was raised/discussed before.
* **Use a clear and descriptive title** for the issue to identify the suggestion.
* **Provide a step-by-step description of the suggested enhancement** in as many details as possible.
* **Provide specific examples to demonstrate the steps**. Include copy/pasteable snippets which you use in those examples, as [Markdown code blocks](https://help.github.com/articles/markdown-basics/#multiple-lines).
* **Describe the current behavior** and **explain which behavior you expected to see instead** and why.
* **Include screenshots and animated GIFs** (if suitable) which help you demonstrate the steps. You can use [this tool](https://www.cockos.com/licecap/) to record GIFs on macOS and Windows, and [this tool](https://github.com/colinkeenan/silentcast) or [this tool](https://github.com/GNOME/byzanz) on Linux.

### Code Contribution / Local development
* It's recommended to **raise an issue and agree with repo maintainers** before development to avoid redundant work
* Given plugin can be developed locally. You need just `gradle` and `IDEA` with enabled `Plugin DevKit` plugin
* Basic SDK capabilities 
* After changes were implemented, they could be tested(debugged) via `runIde` gradle task
* Perform at least smoke testing of main capabilities like navigation over Nucleus components, navigation to setters, search of given component usages

### Pull Requests
* After changes were tested, MR could be created against `develop` branch
* Attached screenshots to MR (how changes look like) are welcome, as due to plugin specifics it's hard to reproduce most of provided capabilities

## Styleguides
1. Code should be formatted with default IntelliJ IDE Java/XML formatters
1. Java Beans naming standard is preferred over using of "my" prefixes on class fields
1. New code should avoid usages of deprecated APIs
1. New Util/Component classes, reusable public methods, and non-trivial methods should have javadocs
1. Avoid to use local-specific paths or OS-specific separators. Consider that plugin could be developed/used under Win/Mac/Lin systems
1. Strive to remain `since-build` version at least as possible for better compatibility
1. Strive to write capabilities in a generic way so they can be useful for larger number of projects
1. Create/update proper page on [WIKI](https://github.com/chivaler/ATG-Toolkit/wiki) for new capabilities after MR is accepted

## Tests coverage

Unit tests are useless there, however IntelliJ provides framework for testing provided capabilities.
Starting point for getting acquainted - [Testing Plugins](http://www.jetbrains.org/intellij/sdk/docs/basics/testing_plugins/testing_plugins.html)
I had no capacity to cover capabilities whilst it was a PET project.
However I hope this will be fixed if plugin comes back to active development stage.
