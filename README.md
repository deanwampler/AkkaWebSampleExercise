# Akka Sample Exercise README 

This is a a sample exercise for a web app based on [Akka](http://akkasource.org) that
used for the May 20th, 2010 meeting of the [Chicago-Area Scala Enthusiasts](http://www.meetup.com/chicagoscala/) (CASE).
It has since been ported to Scala 2.8.0.final, Akka 0.10, and enhanced in other ways. It demonstrates building an Actor based, distributed application with a web interface and optional MongoDB persistence. Actually, it's a "framework" for such exploration; there's a lot that can be done with it to play with data analysis. Currently, it only does a few things (discussed below).

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
    
The last 2 lines (after the `./sbt` line) are commands at the `sbt` prompt (`>`, by default). Note that the `sbt` script has some options; type `sbt --help` for details. When I say that `update` might take a long time, I'm not kidding... Fortunately, you rarely need to run it.

This application requires a [NYSE stock ticker data set](http://infochimps.org/datasets/daily-1970-current-open-close-hi-low-and-volume-nyse-exchange-up--2) from [infochimps](http://infochimps.org). Select the YAML format. Note that there are similar data sets on the site; use this one! Put the files in a `data` directory at the root of this project.

Download and install MongoDB from [here](http://www.mongodb.org/display/DOCS/Downloads), following the instructions for your operating system. In another terminal window, go to the installation directory, which we'll call `$MONGODB_HOME`, and run this command:

    $MONGODB_HOME/bin/mongod --dbpath some_directory/data/db
    
Pick a `some_directory` that's convenient or you can omit the --dbpath option and MongoDB will use the default location (`/data/db` on *nix systems, including OS X).

Now, start up `./sbt` again, so you can build the app and run the tests. (As before, sbt's `>` prompt is not shown.)
     
    test              # run the test suite (doing any required compilations first). It should end with "success"

Helpful hint: When your working on code, run this version of test:

    ~test

When the `~` appears before any `sbt` action, it loops, watching for file system changes, then it runs the action every time you save changes. If you've used `autotest` for Ruby development (or a similar tool), you'll know how useful this is.

You can exit this infinite loop by entering a carriage return. The sbt `>` prompt should return.

# Import the Data

We'll implement the data import feature in subsequent weeks. Stay tuned...


# The Web App

The web tier is partially complete. It talks to the server, but until we implement data importing, no data is returned to the UI. However, you can still play with the UI now.

In `sbt`, start the Jetty web server

    jetty-run                            # run the Jetty web server
    
Then open the home page: [localhost:8080/finance](http://localhost:8080/finance).

Note: When ever you're working on the web pages (HTML, JavaScript, or CSS), use this command in sbt.

    ~prepare-webapp   # automatically load any changes in the running server.
    
Avoid those server restarts! Note that Scala code changes will also get picked up, but the turn around time is slower. (See also the **Notes** section below.)
    
While we're at it, you can stop or restart jetty thusly:

    jetty-stop         # stop the Jetty web server
    jetty-restart      # restart the Jetty web server

In the web UI, enter a comma-separate list of NYSE stock symbols, start and end dates, then click the `Go!` button. The results are presented below in a table. The table will be empty until we get that data importing working... Also, we plan to add google charts instead of tabular output (or both).

The `Ping` button is a diagnostic tool. It checks whether or not the Akka "actors" are still responsive in the application. It will return a list of actors running. If you click it before asking for stock data (with the `Go!` button), only one actor will be listed. Afterwards, 5 or more actors will be listed.

I added a `Bogus` button just to show what happens if you ask the server to do something it doesn't understand. It returns an error message that's presented to the browser.

Internally, all these calls are made using AJAX and the server returns JSON-formatted responses.

# TODO

## More Data Analysis

Currently the app just returns price data. There are other items in the data files that can be exploited, and various analytics can be applied to the data. For example, some "starter" code is already in the server for requesting 50- and 200-day moving average calculations.

## Implement a Clustered Solution

How does the performance scale up, especially any analytics, if you use Akka's support for clustering?

## Clean up the JSON Handling

It's a bit messy and in your face. Lot's of room for code cleanup here!

# Notes

## Persistence Options

There are two data persistence options, MongoDB and an in-memory hash map. Currently, MongoDB is really the only supported option, because of the need to import the data into some form of persistent storage first.

The persistence option is set in `src/main/resources/akka.conf`. Look for these lines around line 13,

    type = MongoDB
    # type = in-memory

"Toggle" the comments if you want to flip the persistence option. Note that the unit tests (mostly) ignore this flag, so MongoDB needs to be installed for all the tests to pass.

## Permgen Exhaustion

When you keep reloading code changes into Jetty, e.g., using the `~prepare-webapp` feature, you can eventually exhaust the JVM's "permgen space". The `sbt` script raises the size of this space, but it can still happen. If so, kill the JVM process and restart.

## Contributions Welcome!

Please fork the [repo](git://github.com/deanwampler/AkkaWebSampleExercise.git) and commit improvements, updates, *etc.*

## Scala Version

This version requires Scala 2.8.0.final and Akka 0.10 or later.
