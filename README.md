# Akka Sample Exercise README 

This is a a sample exercise for a web app based on [Akka](http://akkasource.org) that
used for the May 20th, 2010 meeting of the [Chicago-Area Scala Enthusiasts](http://www.meetup.com/chicagoscala/) (CASE). It demonstrates building an Actor based, distributed application with a web interface and MongoDB persistence.

For a blog post on setting up a similar Akka web app, see [this blog post](http://roestenburg.agilesquad.com/2010/04/starting-with-akka-and-scala.html).

# Setup

Everything after a `#` is a comment.

    git clone git://github.com/deanwampler/AkkaWebSampleExercise.git
    cd AkkaWebSampleExercise
    ./sbt             # start sbt. On Windows, use sbt.bat
    update            # update the dependencies. This will take a whiiiiile
    test              # run the test suite. It should end with "success"
    jetty-run         # run the Jetty web server
    open http://localhost:8080/case      # profit!

The 3 lines after `./sbt` are commands at the `sbt` prompt (`>`, by default). The last command, `open http://localhost:8080/case`, works on OS X. Use the appropriate command on your OS to open a web page in a browser or just copy and past the URL.

The `sbt` script has some options; type `sbt --help` for details. (The options are supported in the `sbt.bat` script.)

# Notes

## Contributions Welcome!

Please fork the repo and commit improvements, updates, *etc.*

## Scala Version

At the time of this writing, Scala 2.8.0.Beta1 is the latest release with stable builds of Akka and some other tools we're using.

## JavaScript Testing

Although this app uses some JavaScript, we haven't included tests for it, which should be made dependents of the `test` target.
