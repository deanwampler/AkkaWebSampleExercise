# Akka Sample Exercise README 

This is a a sample exercise for a web app based on [Akka](http://akkasource.org) that
used for the May 20th, 2010 meeting of the [Chicago-Area Scala Enthusiasts](http://www.meetup.com/chicagoscala/) (CASE). When finished, it will demonstrate building an Actor based, distributed application with a web interface and optional MongoDB persistence.

For a blog post on setting up a similar Akka web app, see [this blog post](http://roestenburg.agilesquad.com/2010/04/starting-with-akka-and-scala.html).

See the Akka [docs](http://doc.akkasource.org) for details on the Akka API.

## Disclaimer

This is completely free and open source, with no warranty of any kind. I hacked it together quickly, so it is certainly buggy!

# Setup

Everything after a `#` is a comment.

    git clone git://github.com/deanwampler/AkkaWebSampleExercise.git
    cd AkkaWebSampleExercise
    ./sbt             # start sbt. On Windows, use sbt.bat
    update            # update the dependencies. This will take a whiiiiile
    
The last line (after the `./sbt` line) is a command at the `sbt` prompt (`>`, by default). Note that the `sbt` script has some options; type `sbt --help` for details. (The options are supported in the `sbt.bat` script.)

There are two data persistence options, a MongoDB-backed persistent map and an in-memory map. Currently, the MongoDB persistence doesn't work (see the TODO items below), so the in-memory map is the default (it also makes running the app easier, when you're getting started...). 

The persistence option is set in `src/main/resources/akka.conf`. Change the following statement around line 13,

    type = in-memory
    
to

    type = MongoDB

If you use MongoDB persistence, download and install MongoDB from [here](http://www.mongodb.org/display/DOCS/Downloads). In another terminal window, go to the installation directory, which we'll call `$MONGODB_HOME`, and run this command:

    $MONGODB_HOME/bin/mongod --dbpath some_directory/data/db
    
Pick a `some_directory` that's convenient.

Now, back in `sbt` you can run the tests and run the app. (sbt's `>` prompt is not shown.)
     
    test              # run the test suite. It should end with "success"
    jetty-run         # run the Jetty web server
    open http://localhost:8080/primes   # open the UI in a browser
    
If you don't have the `curl` command or the equivalent, use a browser. Click the `Start` button to tell the server to start calculating primes. (Note that the `Stop` and `Restart` options do not yet work correctly.) AJAX is used to send these commands to the server. 

After you start it, the UI will poll for the calculated primes. Currently, the returned JSON strings are printed to the UI verbatim. Don't worry if you see some `{"error": "..."}` messages at first. 

You can run the AJAX calls from curl as well:

    curl http://localhost:8080/primes/ajax/start     # start calculating primes
    curl http://localhost:8080/primes/ajax/stop      # stop calculating primes (broken)
    curl http://localhost:8080/primes/ajax/restart   # restart calculating primes (broken)
    curl http://localhost:8080/primes/ajax/primes    # profit!
    curl http://localhost:8080/primes/ajax/ping      # you there?

# Improvements You Can Make (a.k.a. Exercises)

Here are some things you might try to better understand how Akka works.

## Simplify the Actors

This sample exercise is roughly based on a more involved production system, but we don't need all the bells and whistles. Try simplifying the structure. Here are some suggestions.

### "Throttle" the Behavior

Right now, it runs hot and fast. Using the Akka [docs](http://doc.akkasource.org/), can you figure out ways to slow it down, *i.e.,* by inserting pauses between runs of calculating primes?

### Combine the `DataStoreServer` and `PrimeCalculatorServer`

There is actually a one-to-one mapping between the an actor instance of each `DataStoreServer` and `PrimeCalculatorServer`. You could move the prime calculation into the `DataStoreServer` or move the persistence logic into the calculator. What are the relative benefits and disadvantages of this refactoring?

### Improve the Supervision Logic

This goes with the previous point. If a `DataStoreServer` dies, the corresponding `PrimeCalculatorServer` should be restarted with it. Assuming you *don't* do the refactoring in the previous point, how can you manage these server pairs together? (Hint: hard code creation of the `DataStoreServers` and `PrimeCalculatorServers` in the `BootAWSESupervisor`, rather than use the current "dynamic-creation" logic.)

### Improve the Server Management Logic

There are UI controls for starting, stopping, and restarting the server. The stop and restart logic is somewhat simplistic and could be improved. See for example `TODO` comments in `ServerFactories.scala`.

### Make It Clustered

Look at the Akka docs page for [remote actors](http://doc.akkasource.org/remote-actors). Can you make this a truly distributed application?

## Testing

There aren't a lot of tests. For example, there are no tests for MongoDB persistence. Try writing more tests to better understand the code (and find bugs for me!!)

### Fix the TODOs

There are `TODO` items below and a few `TODO` comments in the code base. Try fixing them.

## Use the New PubSub Features

Akka's `head` branch has support for building "PubSub" systems using Redis and other options. Try rewriting the app to use these features.

## Substitute a Different Problem Domain

Instead of calculating primes, do something else. I considered building a distributed version of the recent Akka version of Clojure's "ants" demo. There are two Scala variants, [here](http://github.com/azzoti/ScalaAkkaAnts) and [here](http://github.com/pvlugter/ants). Try building a larger version of ants using distributed/clustered actors.

# TODO

1. Add web pages that make AJAX calls to retrieve the data.
2. Clean up and simplify the actor code. In particular, fix bugs in the use of Transactors vs. Actors. Until this is sorted out, MongoDB-backed persistence won't work.
3. Fix the errors in the JSON data.
4. Exploit clustering.
5. Implement graceful shutdown.
6. Fix the `TODO` items in the code base
7. Others?

# Notes

## Contributions Welcome!

Please fork the [repo](git://github.com/deanwampler/AkkaWebSampleExercise.git) and commit improvements, updates, *etc.*

## Scala Version

At the time of this writing, Scala 2.8.0.Beta1 is the latest release with released builds of Akka and some other tools we're using.
