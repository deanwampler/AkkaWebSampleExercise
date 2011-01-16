rem Run this script from the project root directory.
rem Assumes that mongo and mongoimport are in your path.
rem Edit to be consistent with data-import-prep.js and
rem data-import-finish.js.
rem Note: this script hasn't been tested!! Caveat Emptor!

mongo < data-import-prep.js

set data_dir=%HOME%\infochimps_dataset_4778_download_16677\NYSE

FOR %%c IN (A B C D E F G H I J K L M N O P Q R S T U V W X Y Z) DO (
  mongoimport --db stocks_yahoo_NYSE --collection %c%_prices --file %data_dir%\NYSE_daily_prices_%c%.csv --type csv --headerline --drop
  mongoimport --db stocks_yahoo_NYSE --collection %c%_dividends --file %data_dir%\NYSE_dividends_%c%.csv --type csv --headerline --drop
)

mongo < data-import-finish.js
