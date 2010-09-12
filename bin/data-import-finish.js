use stocks_yahoo_NYSE;

var str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
for (i=0; i<str.length; i=i+1) {
   var nextChar = str[i];
   print("Indexing collections "+nextChar+"_prices and "+nextChar+"_dividends")
   db[""+nextChar+"_prices"].ensureIndex({date: 1})
   db[""+nextChar+"_dividends"].ensureIndex({date: 1})
}
