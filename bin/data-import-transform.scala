import java.io._
import org.joda.time._

/** 
 * Assumes you're running this script from the project root directory:
 *   scala bin/data-import-transform.scala
 */

val pricesRE    = """^\s*(\{\s*"close"[^}]+\}),?\s*$""".r
val dividendsRE = """^\s*(\{\s*"date"[^}]+\}),?\s*$""".r

(new File("datatmp")).mkdir
val prefix = "stocks_yahoo_NYSE_"

for (c <- 'A' to 'Z') {
  // Ugly Java for file I/O...
  val inputFileName = "data/stocks_yahoo_NYSE-yaml/" + prefix + c + ".yaml"
  println("Processing: data/stocks_yahoo_NYSE-yaml/" + prefix + c + ".yaml")
  val input = 
    new BufferedReader(new FileReader(inputFileName))
  val printOutput = 
    new PrintWriter(new BufferedWriter(new FileWriter("datatmp/" + prefix + c + "_prices.json")))
  val dividendsOutput = 
    new PrintWriter(new BufferedWriter(new FileWriter("datatmp/" + prefix + c + "_dividends.json")))
  val otherOutput = 
    new PrintWriter(new BufferedWriter(new FileWriter("datatmp/" + prefix + c + "_other.json")))

  // Define this helper function here, so the reader and writers become part of the closure
  // and don't have to passed as arguments. 
  def process(): Unit = input.readLine match {
    case null => // finished
    case string => 
      string match {
        case pricesRE(json) => printOutput.println(json)
        case dividendsRE(json) => dividendsOutput.println(json)
        case _ => otherOutput.println(string)
      }
      process() // tail recursive call; optimized by scala compiler
  }

  try {
    process()
  } finally {
    input.close
    printOutput.close
    dividendsOutput.close
    otherOutput.close
  }
}

