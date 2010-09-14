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

This application requires a [NYSE stock ticker data set](http://infochimps.org/datasets/daily-1970-current-open-close-hi-low-and-volume-nyse-exchange-up--2) from [infochimps](http://infochimps.org). Select the YAML format. Note that there are similar data sets on the site; use this one! Put the files in a `data` directory at the root of this project.

The script `bin/data-import.sh` munges this YAML data into a format that `mongoimport` likes, creates the `stocks_yahoo_NYSE` database, creates many tables, and imports the data. It takes a long time to run. 

**Before you run it, make sure `mongod` is running,** per the instructions above. Also, if you're running `mongod` with the `--dbpath some_directory/data/db` argument, you'll need to add the same argument to the invocations of `mongoimport` and `mongo` in the script.

Also, before you run the script, you'll need to install the Scala distribution (if it isn't already installed), as this script runs Scala and it is not set up to use the Scala distribution embedded in the `sbt` project. Go to [scala-lang.org/downloads](http://scala-lang.org/downloads) and follow the instructions to install Scala. (I prefer the _IzPack_ installer myself; you just double click and go. However, on Windows, the Windows-specific installer might be best.)
 
(**Note:** there is `bin/data-import.bat` script for windows that attempts to do the same steps, but it is untested! Feedback is welcome!)

When `bin/data-import.sh` is finished, you should have 52 collections in `stocks_yahoo_NYSE`, of the form `A_prices`, `A_dividends`, ... `Z_prices`, `Z_dividends`. To get a sense of the installed data, start the `mongo` interactive shell (it's in the same directory as `mongod`) and run the following commands. The `>` is the `mongo` prompt, which you don't type, and the rest of the lines are the output that I got in response. Your results should be similar, but not identical.

    > show dbs
    admin
    local
    play
    stocks_yahoo_NYSE
    
    > use stocks_yahoo_NYSE
    switched to db stocks_yahoo_NYSE
    
    > db.A_prices.count()              
    693733
    
    > db.A_prices.findOne()
    {
      "_id" : ObjectId("4c89b27c48bc853f23fc87ae"),
      "close" : 27.13,
      "high" : 27.4,
      "date" : "2008-03-07",
      "stock_symbol" : "ATU",
      "exchange" : "NYSE",
      "volume" : 591000,
      "adj close" : 27.13,
      "low" : 26.18,
      "open" : 26.18
    }
    
    > db.A_dividends.count()           
    8322
    
    > db.A_dividends.findOne()
    {
      "_id" : ObjectId("4c89b2ab48bc853f24071d93"),
      "date" : "2007-09-26",
      "dividends" : 0.04,
      "stock_symbol" : "ATU",
      "exchange" : "NYSE"
    }
    
    
Repeat the last `count()` and `findOne()` commands for any of the collections that interest you. Here's a command that is also very useful; it shows all the stock symbols in a given table. It's useful because each stock has one entry for every day that it was traded.

    > db.A_prices.distinct("stock_symbol")  
    [
      "AA",
      "AAI",
      "AAP",
      "AAR",
      ...
      "AZ",
      "AZN",
      "AZO",
      "AZZ"
    ]
            
If you encounter any problems with these commands, the data import might have failed. If you want to try again with a clean slate, the commands `db.dropDatabase()` will drop the whole database you're in (e.g., `stocks_yahoo_NYSE`), while a command like `db.A_prices.drop()` will drop just the `A_prices` collection.

**NOTE:** To get help in the `mongo` console, start with `help()`. 

Finally, the last message of the import script tells you to delete the `datatmp` directory. This is where temporary data files were staged. The script doesn't delete them automatically, in case you need to do some diagnostics...

# The Web App

The web tier is partially complete. It queries the server for data, and displays it in tables. Lots of enhancements will be made over the coming months.

In `sbt`, start the Jetty web server

    jetty-run                            # run the Jetty web server
    
Then open the home page: [localhost:8080/finance](http://localhost:8080/finance).

Note: Whenever you're working on the web pages (HTML, JavaScript, or CSS), use this command in sbt.

    ~prepare-webapp   # automatically load any changes in the running server.
    
Avoid those server restarts! Note that Scala code changes will also get picked up, but the turn around time is slower. (See also the **Notes** section below.)
    
While we're at it, you can stop or restart jetty thusly:

    jetty-stop         # stop the Jetty web server
    jetty-restart      # restart the Jetty web server

In the web UI, enter a comma-separate list of NYSE stock symbols, start and end dates, then click the `Go!` button (or hit `return` in one of the text fields). The results are presented below in a table. If no data is returned, the table will indicate that fact. Usually that indicates you specified a time range for which there is no data. (The data range in the data set is not that big). Try date ranges in February, 2008. Also, if you use a wide range, you will get a **lot** of data back and the query will take a while. 

**Note:** Currently, data for *all* stocks in the corresponding A-Z table is returned, not just for the symbols you entered! 

When you get a table of data. You can click on the column headers to sort the data by that column. Click again to reverse the sort.

We will add better filtering of stock symbols and graphing the data over the next several weeks.

The `Ping` button is a diagnostic tool. It checks whether or not the Akka "actors" are still responsive in the application. It will return a list of actors running. If you click it before asking for stock data (with the `Go!` button), only one actor will be listed (There is a bug in the display currently...). Afterwards, 5 or more actors will be listed.

I added a `Bogus` button just to show what happens if you ask the server to do something it doesn't understand. It returns an error message that's presented in the browser.

Internally, all these calls are made using AJAX and the server returns JSON-formatted responses.

# TODO

## More Data Analysis

Currently the app just returns closing price data. There are other items in the data files that can be exploited, and various analytics can be applied to the data. For example, some "starter" code is already in the server for requesting 50- and 200-day moving average calculations.

## Implement a Clustered Solution

How does the performance scale up, especially any analytics, if you use Akka's support for clustering?

## Clean Up the JSON Handling

It's a bit messy and in your face in the server code. Lot's of room for code cleanup and encapsulation here!

## Clean Up the MongoDB-related Code

In `MongoDBDataStore.scala`, which interacts with MongoDB, there is a mixture of `mongo-scala-driver` code and the standard Mongo Java driver. This could be cleaned up substantially. For example, the newer [Casbah](http://github.com/novus/casbah) API looks promising.

# Notes

## Global Configuration

Many global configuration properties are set in `src/main/resources/akka.conf`. Some more complex global configuration items are setup in `src/main/scala/boot/BootAWSE.scala` and  `src/main/scala/server/finance/InstrumentAnalysisServerSupervisor.scala`.

## Persistence Options

There are two data persistence options, MongoDB and an in-memory hash map. Currently, MongoDB is really the only supported option, because of the need to import the data into some form of persistent storage first.

The persistence option is set in `src/main/resources/akka.conf`. Look for these lines around line 13,

    type = MongoDB
    # type = in-memory

"Toggle" the comments if you want to flip the persistence option. Note that the unit tests (mostly) ignore this flag, so MongoDB needs to be installed for all the tests to pass.

## Permgen Exhaustion and Other "Hangups"

When you keep reloading code changes into Jetty, e.g., using the `~prepare-webapp` feature, you can eventually exhaust the JVM's "permgen space". The `sbt` script raises the size of this space, but it can still happen. If so, kill the JVM process and restart. 

Similarly, especially large queries (watch those large date ranges!) can bog down the server until it becomes unresponsive. Just restart `jetty` or `sbt`.

## Scala Version

This version requires Scala 2.8.0.final and Akka 0.10 or later.

## Contributions Welcome!

Please fork the [repo](git://github.com/deanwampler/AkkaWebSampleExercise.git) and commit improvements, updates, *etc.*

