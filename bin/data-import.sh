#!/bin/bash
#------------------------------------------------
# data-import.sh - Transform the YAML data files and import them into mongo.
# Designed to be run in the root directory of the project.

dir=$(dirname $0)
mongo < $dir/data-import-prep.js 
scala $dir/data-import-transform.scala

for n in {A..Z}
do
  prefix=$dir/../datatmp/stocks_yahoo_NYSE
  file1=${prefix}_${n}_prices.json
  file2=${prefix}_${n}_dividends.json
  if [ -f $file1 -a -f $file2 ] ; then
    mongoimport --db stocks_yahoo_NYSE --collection ${n}_prices --file $file1
    mongoimport --db stocks_yahoo_NYSE --collection ${n}_dividends --file $file2
  else
    echo "Error: Temporary data files $file1 and/or $file2 not found."
    exit 1
  fi
done
mongo < $dir/data-import-finish.js

echo "Test queries:"
mongo <<EOF
use stocks_yahoo_NYSE
db.Z_prices.findOne()
db.Z_dividends.findOne()
EOF
echo "Finished. Delete the datetmp directory."