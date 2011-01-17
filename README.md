# Akka Sample Exercise README 

This is a "sample exercise" for a web app based on [Akka](http://akka.io) that I presented at the May 20th, 2010 meeting of the [Chicago-Area Scala Enthusiasts](http://www.meetup.com/chicagoscala/) (CASE). It uses NYSE historical stock data, stored in a MongoDB database.

It has recently been ported to Scala 2.8.1 and Akka 1.0-RC2 (and enhanced in other ways). I wouldn't call it perfect ;), but it demonstrates building an Actor-based, distributed application with a web interface and a persistence tier (MongoDB). There are many possible enhancements that could be done with it, especially in the area of data analysis and distributed processing. Currently, it only does a few things (discussed below).

For a blog post on setting up a similar Akka web app, see [this blog post](http://roestenburg.agilesquad.com/2010/04/starting-with-akka-and-scala.html).

See the Akka [docs](http://doc.akka.io) for details on the Akka API.

## Disclaimer

This is completely free and open source, with no warranty of any kind. It is certainly buggy, so use it at your own discretion.

# Setup

use the following *nix shell commands to get started. (On windows, use an environment like Cygwin or make the appropriate command shell substitutions.) Everything after a `#` is a comment.

    git clone git://github.com/deanwampler/AkkaWebSampleExercise.git
    cd AkkaWebSampleExercise
    ./sbt     # start sbt. On Windows, use sbt.bat
    update    # update the dependencies on teh interwebs. This will take a whiiiiile
    exit      # leave sbt (^D also works)

The last 2 lines (after the `./sbt` line) are commands at the `sbt` prompt (`>`, by default). Note that the `sbt` script has some options; type `sbt --help` for details. When I say that `update` might take a long time, I'm not kidding... Fortunately, you rarely need to run it.

Download and install MongoDB from [here](http://www.mongodb.org/display/DOCS/Downloads), following the instructions for your operating system. In another terminal window, go to the installation directory, which we'll call `$MONGODB_HOME`, and run this command:

    $MONGODB_HOME/bin/mongod --dbpath some_directory/data/db
    
Pick a `some_directory` that's convenient or you can omit the --dbpath option and MongoDB will use the default location (`/data/db` on *nix systems, including OS X).

Now, start up `./sbt` again, so you can build the app and run the tests. (As before, sbt's `>` prompt is not shown.) The test run should end with `[success]`.

    test    # run the test suite (doing any required compilations first).

Helpful hint: When you're working on code, run this version of test:

    ~test

When the `~` appears before any `sbt` action, it loops, watching for file system changes, then it runs the action every time you save changes. If you've used `autotest` or `watchr` for Ruby development (or similar tools), you'll know how useful this is.

You can exit this infinite loop by entering a carriage return. The sbt `>` prompt should return.

# Import the Data

**UPDATE:** InfoChimps has changed the location of the data set and removed the YAML version. I updated the following instructions to use the new location and CSV format. (I also simplified process and made it more flexible...)

The import process uses the `bash` script in `bin/data-import.sh`. There is a preliminary, *but untested*, windows shell script in `bin/windows`. If you try it and find bugs, let me know. (Or better, provide patches!)

You must also install **MongoDB** before proceeding.

This application requires a [NYSE stock ticker data set](http://infochimps.com/datasets/daily-1970-2010-open-close-hi-low-and-volume-nyse-exchange) from [infochimps](http://infochimps.org). The files are in CSV format. Download and expand the ZIP file somewhere convenient on your system.

The script `bin/data-import.sh` creates the `stocks_yahoo_NYSE` database, creates many tables, and imports the data. It takes a long time to run.

To see the options for this script, run:

    bin/data-import.sh --help

The only required option is the directory where the CSV files are located, as described. You can also limit the data imported with the `--min_letter=C` and/or
`--max_letter=C` options. If you are loading this data on a small machine (like a Netbook), I strongly recommend that you use these options, say for example, `--max_letter=E`, to only load data for stock symbols that begin with the letters A through E.

**Before you run it, make sure `mongod` is running,** per the instructions above. Also, if you're running `mongod` with the `--dbpath some_directory/data/db` argument, you'll need to invoke `bin/data-import.sh` with the same argument.

When `bin/data-import.sh` is finished, you should have 52 collections in `stocks_yahoo_NYSE`, of the form `A_prices`, `A_dividends`, ... `Z_prices`, `Z_dividends` (or less, if you decided to import a subset of the data). 

To get a sense of the installed data, start the `mongo` interactive shell (it's in the same directory as `mongod`) and run the following commands. The `>` is the `mongo` prompt, which you don't type, and the rest of the lines are the output that I got in response. Your results should be similar, but not identical.

    > show dbs
    admin
    local
    play
    stocks_yahoo_NYSE
    
    > use stocks_yahoo_NYSE
    switched to db stocks_yahoo_NYSE
    
    > db.A_prices.count()              
    693733     // Or some similar, largish number
    
    > db.A_prices.findOne()
    {
      "_id" : ObjectId("4c89b27c48bc853f23fc87ae"),
      "exchange" : "NYSE",
      "date" : "2008-03-07",
      "stock_symbol" : "ATU",
      "stock_price_close" : 27.13,
      "stock_price_high" : 27.4,
      "stock_price_adj_close" : 27.13,
      "stock_price_low" : 26.18,
      "stock_price_open" : 26.18,
      "stock_volume" : 591000
    }
    
    > db.A_dividends.count()           
    8322     // Or some similar, middling number
    
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
            
If you encounter any problems with these commands, the data import might have failed. If you want to try again with a clean slate, the `mongo` command `db.dropDatabase()` will drop the whole database you're in (e.g., `stocks_yahoo_NYSE`). Similarly, the command `db.A_prices.drop()` will drop just the `A_prices` collection, etc.

**NOTE:** To get help in the `mongo` console, start with `help()`, `db.help()` and `db.coll.help()`. See the [MongoDB](http://mongodb.org) web site for more details. The console is quite powerful!

# The Web App

The web tier is functional, but there is room for enhancements. It queries the server for data, per user input, and displays it in "candlestick" graphs or tables.

In `sbt`, start the Jetty web server

    jetty-run                            # run the Jetty web server
    
Then open the home page: [localhost:8080/finance](http://localhost:8080/finance).

Note: Whenever you're working on the web pages (HTML, JavaScript, or CSS), use this command in sbt.

    ~prepare-webapp   # automatically load any web-tier changes in the running server.

Avoid those server restarts! Note that Scala code changes will also get picked up, but the turn around time is slower. (See also the **Notes** section below.)
    
While we're at it, you can stop or restart jetty thusly:

    jetty-stop         # stop the Jetty web server
    jetty-restart      # restart the Jetty web server

In the web UI, enter a comma-separate list of NYSE stock symbols, start and end dates, then click the `Go!` button (or hit `return` in one of the text fields). The results are presented below in a graph or table. If no data is returned, the UI will indicate that fact. Usually that indicates you specified a time range for which there is no data. (The data range in the data set is not that big). Try date ranges in 2007. Also, if you use a wide range, you will get a **lot** of data back and the query will take a while. 

**Note:** Start with small queries first to get a sense of the computation load.

When you display a table of data. You can click on the column headers to sort the data by that column. Click again to reverse the sort.

The `Ping` button is a diagnostic tool. It checks whether or not the Akka "actors" are still responsive in the application. It will return a list of actors running. If you click it before asking for stock data (with the `Go!` button), only one actor will be listed. After a `Go!` command, 5 or more actors will be listed.

The `List Stocks` button returns a list of stocks for the letters you enter in the `Symbols` field.

Internally, all these calls are made using AJAX and the server returns JSON-formatted responses.

# TODO

## More Data Analysis

Currently the app just returns daily price and volume data. There are other items in the data files that can be exploited, and various analytics can be applied to the data. For example, some "starter" code is already in the server for requesting 50- and 200-day moving average calculations.

It's reasonably straightforward to implement many of these calculations in MongoDB's query language. A challenge is structuring such queries for good performance and also integrating them into the `DataStore` class hierarchy.

## Richer UI ##

Zooming in the UI and tooltips with data for each point would be nice.

## Implement a Clustered Solution

How does the performance scale up, especially any analytics, if you use Akka's support for clustering?

## Clean Up the JSON Handling

It's a bit messy and in your face in the server code. Lot's of room for code cleanup and encapsulation here!

## Clean Up the MongoDB-related Code

In `MongoDBDataStore.scala`, which interacts with MongoDB, there is a mixture of `mongo-scala-driver` code and the standard Mongo Java driver. This could be cleaned up substantially. For example, I want to replace this code with  [Casbah](http://github.com/novus/casbah).

## Simplify? 

The server-side code is more complex than it needs to be. I was experimenting with ideas... 

## Testing

The testing of the server-side Scala code is pretty good. The testing of the web-tier JavaScript is non-existent.

# Notes

## Global Configuration

Many global configuration properties are set in `src/main/resources/akka.conf`. Some more complex global configuration items are setup in `src/main/scala/boot/BootAWSE.scala` and  `src/main/scala/server/finance/InstrumentAnalysisServerSupervisor.scala`.

## Persistence Options

While MongoDB is the primary persistence option, this is abstracted to allow other options. There is an in-memory hash map implementation, useful primarily for testing. Note that it is not as feature complete as the MongoDB code. A straightforward exercise would be to replace this with a key-value store, like Redis.

The persistence option is set in `src/main/resources/akka.conf`. Look for these lines around line 13,

    type = MongoDB
    # type = in-memory

"Toggle" the comments if you want to flip the persistence option. Note that the unit tests (mostly) ignore this flag, so MongoDB needs to be installed for all the tests to pass.

## Permgen Exhaustion and Other "Hangups"

When you keep reloading code changes into Jetty, e.g., using the `sbt ~prepare-webapp` feature, you can eventually exhaust the JVM's "permgen space". The `./sbt` script raises the size of this space to reduce these occurrences. When they happen, just kill the JVM/sbt process and restart. 

Similarly, especially large queries (watch those large date ranges!) can bog down the server until it becomes unresponsive. Restart `jetty` or `sbt`.

## Scala Version

This version requires Scala 2.8.1.final and Akka 1.0-RC2 or later. Note that these values are configured in the `sbt` project file `project/build/AkkaWebSampleExercise.scala` and in `src/main/resources/akka.conf`. 

## Contributions Welcome!

Please fork the [repo](git://github.com/deanwampler/AkkaWebSampleExercise.git) and commit improvements, updates, *etc.*

Thanks for your interest.

Dean Wampler
