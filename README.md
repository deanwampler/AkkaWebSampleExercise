# Akka Sample Exercise README 

This is a a sample exercise for a web app based on [Akka](http://akkasource.org) that
used for the May 20th, 2010 meeting of the [Chicago-Area Scala Enthusiasts](http://www.meetup.com/chicagoscala/) (CASE).
It has since been ported to Scala 2.8.0.final, Akka 0.10, and enhanced in other ways. It demonstrates building an Actor based, distributed application with a web interface and optional MongoDB persistence.

For a blog post on setting up a similar Akka web app, see [this blog post](http://roestenburg.agilesquad.com/2010/04/starting-with-akka-and-scala.html).

See the Akka [docs](http://doc.akkasource.org) for details on the Akka API.

## Disclaimer

This is completely free and open source, with no warranty of any kind. I hacked it together quickly, so it is certainly buggy!

# Setup

use the following *nix shell commands to get started. (On windows, use an environment like Cygwin or make the appropriate command shell substitutions.) Everything after a `#` is a comment.

    git clone git://github.com/deanwampler/AkkaWebSampleExercise.git
    cd AkkaWebSampleExercise
    ./sbt             # start sbt. On Windows, use sbt.bat
    update            # update the dependencies on teh interwebs. This will take a whiiiiile
    exit              # leave sbt (^D also works)
    
The last line (after the `./sbt` line) is a command at the `sbt` prompt (`>`, by default). Note that the `sbt` script has some options; type `sbt --help` for details. When I say that `update` might take a long time, I'm not kidding... Fortunately, you rarely need to run it.

This application requires a [NYSE stock ticker data set](http://infochimps.org/datasets/daily-1970-current-open-close-hi-low-and-volume-nyse-exchange-up--2) from [infochimps](http://infochimps.org). Select the YAML format. Note that there are similar data sets on the site; use this one! Put the files in a `data` directory at the root of this project.

There are two data persistence options, a MongoDB-backed persistent map and an in-memory map. Currently, the in-memory map is really the only supported option, because of the need to import the data into some form of persistent storage.

The persistence option is set in `src/main/resources/akka.conf`. Change the following statement around line 13,

    type = in-memory
    
to

    type = MongoDB

Download and install MongoDB from [here](http://www.mongodb.org/display/DOCS/Downloads). In another terminal window, go to the installation directory, which we'll call `$MONGODB_HOME`, and run this command:

    $MONGODB_HOME/bin/mongod --dbpath some_directory/data/db
    
Pick a `some_directory` that's convenient or you can omit the --dbpath option and MongoDB will use the default location (`/data/db` on *nix systems, including OS X).

Now, start up `./sbt` again, so you can build the app and run the tests. (As before, sbt's `>` prompt is not shown.)
     
    test              # run the test suite (after compiling as needed). It should end with "success"

Helpful hint: When your working on code, run this version of test:

    ~test             # run the test suite (after compiling as needed). It should end with "success"

When the `~` appears before any `sbt` action, it loops, watching for file system changes, then it runs the action every time you save changes. If you've used `autotest` for Ruby development (or a similar tool), you'll know how useful this is.

# Import the Data

We'll implement the data import feature in subsequent weeks. Stay tuned...


# The Web App

The web tier is incomplete. We'll add it in subsequent weeks. The following discussion will apply then:

In `sbt`, start the Jetty web server

    jetty-run                            # run the Jetty web server
    open http://localhost:8080/finance   # open the UI in a browser

If you're working on the web pages (HTML, JavaScript, or CSS), use this command in sbt.

    ~prepare-webapp   # automatically load any changes in the running server.
    
Avoid those server restarts! Note that Scala code changes will also get picked up, but the turn around time is slower.
    
While we're at it, you can stop or restart jetty thusly:

    jetty-stop         # stop the Jetty web server
    jetty-restart      # restart the Jetty web server

Enter a comma-separate list of NYSE stock symbols, start and end dates, then the `Go!` button. The results are presented below.


The `Ping` button is a diagnostic tool. It checks whether or not the Akka "actors" are still responsive in the application.


# TODO

## Mine More of the Data

Currently the app just returns price data. There are other items in the data files that can be exploited, and various analytics can be applied to the data.

## Implement a Clustered Solution

How does the performance scale up, especially any analytics, if you use Akka's support for clustering?

# Notes

## Contributions Welcome!

Please fork the [repo](git://github.com/deanwampler/AkkaWebSampleExercise.git) and commit improvements, updates, *etc.*

## Scala Version

This version requires Scala 2.8.0.final and Akka 0.10 or later.
