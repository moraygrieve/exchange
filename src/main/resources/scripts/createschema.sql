DROP TABLE IF EXISTS Trade;

CREATE TABLE Trade (
  tradeID            varchar(255) PRIMARY KEY NOT NULL,
  tradeTime          datetime NOT NULL,
  symbol             varchar(50) NOT NULL,
  price              float(50,5) NOT NULL,
  quantity           float(50,5) NOT NULL,
  buyOrderID         varchar(255),
  buyClientOrderID   varchar(255),
  buyAccount         varchar(255),
  buyTraderID        varchar(255),
  buyType            varchar(255),
  sellOrderID        varchar(255),
  sellClientOrderID  varchar(255),
  sellAccount        varchar(255),
  sellTraderID       varchar(255),
  sellType           varchar(255),
  aggressorID        varchar(255)
);


