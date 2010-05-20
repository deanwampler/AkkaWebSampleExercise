# Akka Sample Exercise README 

This is a a sample exercise for a web app based on [Akka](http://akkasource.org) that
used for the May 20th, 2010 meeting of the [Chicago-Area Scala Enthusiasts](http://www.meetup.com/chicagoscala/) (CASE). It demonstrates building an Actor based, distributed application with a web interface (forthcoming...) and MongoDB persistence.

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

There are two data persistence options, a MongoDB-backed persistent map and an in-memory map. By default,
the MongoDB-backed map is used, but using the in-memory map makes running the app easier. 

If you don't want to use MongoDB persistence, do the following.

Edit `src/main/resources/akka.conf`. Change the following statement around line 13,

    type = MongoDB
    
to

    type = in-memory

If you want to use MongoDB persistence, download and install MongoDB from [here](http://www.mongodb.org/display/DOCS/Downloads). In another terminal window, go to the installation directory, which we'll call `$MONGODB_HOME`, and run this command:

    $MONGODB_HOME/bin/mongod --dbpath some_directory/data/db
    
Pick a `some_directory` that's convenient.

Now, back in `sbt` you can run the tests and run the app. (sbt's `>` prompt is not shown.)
     
    test              # run the test suite. It should end with "success"
    jetty-run         # run the Jetty web server
    curl http://localhost:8080/primes/ajax/ajax/start   # start calculating primes
    curl http://localhost:8080/primes/ajax/ajax/primes  # profit!

If you don't have the `curl` command or the equivalent, use a browser. (The "ajax/ajax" stuff is both redundant and used for the planned AJAX-based UI...) There is one other supported "action":

    curl http://localhost:8080/primes/ajax/ajax/ping    # You there?


# Improvements You Can Make (a.k.a. Exercises)

Here are some things you might try to better understand how Akka works.

## Fix the Bugs!!

It breaks down after calculating a few runs of primes. Help me debug it. ;)

## Simplify the Actors

This sample exercise is roughly based on a more involved production system, but we don't need all the bells and whistles. Try simplifying the structure. Here are some suggestions.

### "Throttle" the Behavior

Right now, it runs hot and fast. Using the Akka [docs](http://doc.akkasource.org/), can you figure out ways to slow it down, *i.e.,* by inserting pauses between runs of calculating primes?

### Combine the `DataStoreServer` and `PrimeCalculatorServer`

There is actually a one-to-one mapping between the an actor instance of each `DataStoreServer` and `PrimeCalculatorServer`. You could move the prime calculation into the `DataStoreServer` or move the persistence logic into the calculator. What are the relative benefits and disadvantages of this refactoring?

## Improve the Supervision Logic

This goes with the previous point. If a `DataStoreServer` dies, the corresponding `PrimeCalculatorServer` should be restarted with it. Assuming you *don't* do the refactoring in the previous point, how can you manage these server pairs together? (Hint: hard code creation of the `DataStoreServers` and `PrimeCalculatorServers` in the `BootAWSESupervisor`, rather than use the current "dynamic-creation" logic.)

### Make It Clustered

Look at the Akka docs page for [remote actors](http://doc.akkasource.org/remote-actors). Can you make this a truly distributed application?

### Fix the TODOs

There are a few `TODO` comments in the code base. Try fixing them.

## Use the New PubSub Features

Akka's `head` branch has support for building "PubSub" systems using Redis and other options. Try rewriting the app to use these features.

## Substitute a Different Problem Domain

Instead of calculating primes, do something else. I considered building a distributed version of the recent Akka version of Clojure's "ants" demo. There are two Scala variants, [here](http://github.com/azzoti/ScalaAkkaAnts) and [here](http://github.com/pvlugter/ants). Try building a larger version of ants using distributed/clustered actors.

## Web Tier

The web tier uses simple HTML pages, JavaScript and AJAX. Possible enhancements would include using the Lift or Play web framework. Also, the AJAX calls could be replaced with HTML 5 WebSockets (only supported currently in Google Chrome and some other prerelease versions of other browsers).

### Testing

There aren't a lot of tests for this example, especially for the JavaScript used. The Scala is poorly tested, too. (There's no test for MongoDB persistence, for example.) Try writing more tests to better understand the code (and find bugs for me!!)


# Notes

## Contributions Welcome!

Please fork the [repo](git://github.com/deanwampler/AkkaWebSampleExercise.git) and commit improvements, updates, *etc.*

## Scala Version

At the time of this writing, Scala 2.8.0.Beta1 is the latest release with released builds of Akka and some other tools we're using.
