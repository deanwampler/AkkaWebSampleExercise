#!/bin/bash
#----------------------------------------------------------------
# data-import.sh - Import the InfoChimps CSV data into MongoDB.
# Invoke with the "--help" option for details.

help() {
  echo "$@"
  cat <<EOF 
usage:
  $0 [--help] [--min=letter] [--max=letter] [--db=db_name] [--dbpath=path] infochimps-data-dir
  
where 
  --help                This information.
  --min=letter
  --max=letter          specifies the start and/or end letters of data to use, 
                        inclusive (defaults: A and Z, respectively).
                        The stock data is organized by the first letter of the 
                        stock symbol. If you want to limit the disk space consumed,
                        then specify a range like --max=E to import the data from
                        'A' to 'E', for example.
  --db=db_name          specifies the name of the database in MongoDB (default:
                        stocks_yahoo_NYSE). Note: If you use a different name,
                        make the corresponding change in "src/main/resources/Akka.conf".
  --dbpath=path         specifies the path to the MongoDB database (default: /data/db).
  infochimps-data-dir   is the directory that holds the CSV files.

At the time of this writing (Jan 15, 2011), that directory would be
  \$INFOCHIMPS/infochimps_dataset_4778_download_16677/NYSE,
where \$INFOCHIMPS is the location on your hard drive where you unzipped the
files. If \$INFOCHIMPS is your home directory, then you would run this command:
  data-import.sh \$HOME/infochimps_dataset_4778_download_16677/NYSE
EOF
}

for m in mongo mongoimport
do
  which $m > /dev/null 2>&1
  if [ $? -ne 0 ]
  then
    echo "$0: Could not find the $m command. Is MongoDB installed and on your path?"
    exit 1
  fi
done

min_letter="A"
max_letter="Z"
db=stocks_yahoo_NYSE
while [ $# -ne 0 ]
do
  case $1 in
    -h*|--h*)
      help
      exit 0
      ;;
    --min)
      shift
      min_letter=$1
      ;;
    --min=?*)
      min_letter=${1#--min=}
      ;;
    --max)
      shift
      max_letter=$1
      ;;
    --max=?*)
      max_letter=${1#--max=}
      ;;
    --db)
      shift
      db=$1
      ;;
    --db=?*)
      db=${1#--db=}
      ;;
    --dbpath)
      shift
      dbpath=$1
      ;;
    --dbpath=?*)
      dbpath=${1#--db=}
      ;;
    -*)
      help "Unrecognized option $1"
      exit 1
      ;;
    *)
      import_data_dir=$1
      ;;
  esac
  shift
done

if [ -z "$import_data_dir" ]
then 
  help "Must specify the directory for the InfoChimps files!"
  exit 1
fi
if [ "$import_data_dir" != $(echo $import_data_dir | cut -f2 -d' ') ]
then
  help "The import data directory path cannot have spaces. (Implementation limitation.)"
  exit 1
fi

echo "Reading data from $import_data_dir."
if [ -n "$dbpath" ]
then
  dbpathopt="--dbpath $dbpath"
  echo "Using database path $dbpath"
fi
echo "Inserting data for stock symbols from A to $max_letter."
echo "Invoking 'mongo' to create the database and 'collections':"

mongo $dbpathopt <<EOF
use $db;

var str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
for (i=0; i<str.length; i=i+1) {
   var nextChar = str[i];
   if (nextChar >= "$min_letter" && nextChar <= "$max_letter") {
     print("Creating collections "+nextChar+"_prices and "+nextChar+"_dividends")
     db.createCollection(""+nextChar+"_prices");
     db.createCollection(""+nextChar+"_dividends");
   }
}
EOF

# Import the munged data. Hack to handle the letter range.
in_range=
for n in {A..Z}
do
  if [ -n "$in_range" -o $n = $min_letter ]
  then
    in_range="yes"
    for x in "prices" "dividends"
    do
      file="$import_data_dir/*${x}_${n}.csv"
      if [ -f $file ]
      then
        mongoimport $dbpathopt --db $db --collection ${n}_${x} --file $file --type csv --headerline --drop
      else
        echo "$0: Error: Data file "$file" not found."
        exit 1
      fi
    done
  fi
  if [ $n = $max_letter ]
  then
    break
  fi
done

echo "Finishing: indexing the collections by date:"
mongo $dbpathopt <<EOF
use $db;

var str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
for (i=0; i<str.length; i=i+1) {
   var nextChar = str[i];
   if (nextChar >= "$min_letter" && nextChar <= "$max_letter") {
     print("Indexing collections "+nextChar+"_prices and "+nextChar+"_dividends")
     db[""+nextChar+"_prices"].ensureIndex({date: 1})
     db[""+nextChar+"_dividends"].ensureIndex({date: 1})
   }
}
EOF


# A sanity check; Does the following produce "sensible" results?
echo "Import finished. Let's run some test queries. If you see 'null' then"
echo "something went wrong above!"
mongo $dbpathopt <<EOF
use $db
db.${min_letter}_prices.findOne()
db.${min_letter}_dividends.findOne()
EOF
echo "Finished!"