rem Run this script from the project root directory.
rem Assumes that mongo and mongoimport are in your path.
rem Note: this script hasn't been tested!! Caveat Emptor!
mongo < bin/data-import-prep.js
scala -classpath lib/joda-time-1.6.jar bin/data-import-transform.scala
mongoimport --db stocks_yahoo_NYSE --collection A_prices --file datatmp/stocks_yahoo_NYSE_A_prices.json
mongoimport --db stocks_yahoo_NYSE --collection B_prices --file datatmp/stocks_yahoo_NYSE_B_prices.json
mongoimport --db stocks_yahoo_NYSE --collection C_prices --file datatmp/stocks_yahoo_NYSE_C_prices.json
mongoimport --db stocks_yahoo_NYSE --collection D_prices --file datatmp/stocks_yahoo_NYSE_D_prices.json
mongoimport --db stocks_yahoo_NYSE --collection E_prices --file datatmp/stocks_yahoo_NYSE_E_prices.json
mongoimport --db stocks_yahoo_NYSE --collection F_prices --file datatmp/stocks_yahoo_NYSE_F_prices.json
mongoimport --db stocks_yahoo_NYSE --collection G_prices --file datatmp/stocks_yahoo_NYSE_G_prices.json
mongoimport --db stocks_yahoo_NYSE --collection H_prices --file datatmp/stocks_yahoo_NYSE_H_prices.json
mongoimport --db stocks_yahoo_NYSE --collection I_prices --file datatmp/stocks_yahoo_NYSE_I_prices.json
mongoimport --db stocks_yahoo_NYSE --collection J_prices --file datatmp/stocks_yahoo_NYSE_J_prices.json
mongoimport --db stocks_yahoo_NYSE --collection K_prices --file datatmp/stocks_yahoo_NYSE_K_prices.json
mongoimport --db stocks_yahoo_NYSE --collection L_prices --file datatmp/stocks_yahoo_NYSE_L_prices.json
mongoimport --db stocks_yahoo_NYSE --collection M_prices --file datatmp/stocks_yahoo_NYSE_M_prices.json
mongoimport --db stocks_yahoo_NYSE --collection N_prices --file datatmp/stocks_yahoo_NYSE_N_prices.json
mongoimport --db stocks_yahoo_NYSE --collection O_prices --file datatmp/stocks_yahoo_NYSE_O_prices.json
mongoimport --db stocks_yahoo_NYSE --collection P_prices --file datatmp/stocks_yahoo_NYSE_P_prices.json
mongoimport --db stocks_yahoo_NYSE --collection Q_prices --file datatmp/stocks_yahoo_NYSE_Q_prices.json
mongoimport --db stocks_yahoo_NYSE --collection R_prices --file datatmp/stocks_yahoo_NYSE_R_prices.json
mongoimport --db stocks_yahoo_NYSE --collection S_prices --file datatmp/stocks_yahoo_NYSE_S_prices.json
mongoimport --db stocks_yahoo_NYSE --collection T_prices --file datatmp/stocks_yahoo_NYSE_T_prices.json
mongoimport --db stocks_yahoo_NYSE --collection U_prices --file datatmp/stocks_yahoo_NYSE_U_prices.json
mongoimport --db stocks_yahoo_NYSE --collection V_prices --file datatmp/stocks_yahoo_NYSE_V_prices.json
mongoimport --db stocks_yahoo_NYSE --collection W_prices --file datatmp/stocks_yahoo_NYSE_W_prices.json
mongoimport --db stocks_yahoo_NYSE --collection X_prices --file datatmp/stocks_yahoo_NYSE_X_prices.json
mongoimport --db stocks_yahoo_NYSE --collection Y_prices --file datatmp/stocks_yahoo_NYSE_Y_prices.json
mongoimport --db stocks_yahoo_NYSE --collection Z_prices --file datatmp/stocks_yahoo_NYSE_Z_prices.json

mongoimport --db stocks_yahoo_NYSE --collection A_dividends --file datatmp/stocks_yahoo_NYSE_A_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection B_dividends --file datatmp/stocks_yahoo_NYSE_B_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection C_dividends --file datatmp/stocks_yahoo_NYSE_C_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection D_dividends --file datatmp/stocks_yahoo_NYSE_D_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection E_dividends --file datatmp/stocks_yahoo_NYSE_E_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection F_dividends --file datatmp/stocks_yahoo_NYSE_F_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection G_dividends --file datatmp/stocks_yahoo_NYSE_G_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection H_dividends --file datatmp/stocks_yahoo_NYSE_H_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection I_dividends --file datatmp/stocks_yahoo_NYSE_I_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection J_dividends --file datatmp/stocks_yahoo_NYSE_J_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection K_dividends --file datatmp/stocks_yahoo_NYSE_K_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection L_dividends --file datatmp/stocks_yahoo_NYSE_L_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection M_dividends --file datatmp/stocks_yahoo_NYSE_M_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection N_dividends --file datatmp/stocks_yahoo_NYSE_N_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection O_dividends --file datatmp/stocks_yahoo_NYSE_O_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection P_dividends --file datatmp/stocks_yahoo_NYSE_P_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection Q_dividends --file datatmp/stocks_yahoo_NYSE_Q_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection R_dividends --file datatmp/stocks_yahoo_NYSE_R_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection S_dividends --file datatmp/stocks_yahoo_NYSE_S_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection T_dividends --file datatmp/stocks_yahoo_NYSE_T_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection U_dividends --file datatmp/stocks_yahoo_NYSE_U_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection V_dividends --file datatmp/stocks_yahoo_NYSE_V_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection W_dividends --file datatmp/stocks_yahoo_NYSE_W_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection X_dividends --file datatmp/stocks_yahoo_NYSE_X_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection Y_dividends --file datatmp/stocks_yahoo_NYSE_Y_dividends.json
mongoimport --db stocks_yahoo_NYSE --collection Z_dividends --file datatmp/stocks_yahoo_NYSE_Z_dividends.json
mongo < bin/data-import-finish.js
