/* 
Use (and create) a database named stocks_yahoo_NYSE.
Change the database name in the next line (and the other files) if desired.
Change the "str" to limit the files you import, if desired (e.g., "ABCDE").
*/
use stocks_yahoo_NYSE;

var str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
for (i=0; i<str.length; i=i+1) {
   var nextChar = str[i];
   print("Creating collections "+nextChar+"_prices and "+nextChar+"_dividends")
   db.createCollection(""+nextChar+"_prices");
   db.createCollection(""+nextChar+"_dividends");
}
