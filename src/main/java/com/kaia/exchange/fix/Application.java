package com.kaia.exchange.fix;

import com.kaia.exchange.core.engine.Engine;
import com.kaia.exchange.core.stp.TradeCache;
import com.kaia.exchange.fix.market.BookFactoryImpl;
import com.kaia.exchange.fix.market.MarketFactoryImpl;
import com.kaia.exchange.fix.reader.FIXFileReader;
import com.kaia.exchange.fix.stp.TradePersistenceImpl;
import com.kaia.exchange.fix.utils.SettingsAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;

import java.io.FileInputStream;
import java.io.InputStream;

public class Application {
    public static final String REPLAY_ENABLED = "ReplayEnabled";
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        try {
            InputStream inputStream = null;
            if (args.length == 1) {
                inputStream = new FileInputStream(args[0]);
            }
            if (inputStream == null) {
                System.out.println("usage: " + Application.class.getName() + " [configFile].");
                return;
            }

            // open the database and load persisted trades
            TradePersistenceImpl persistence = new TradePersistenceImpl(System.getProperty("ecn.database.path"));
            TradeCache.initialise(persistence);
            TradeCache tradeCache = TradeCache.getInstance();
            persistence.load(tradeCache);

            // create the application and quickfix components
            SessionSettings settings = new SessionSettings(inputStream);
            SettingsAccessor.createInstance(settings);
            MessageStoreFactory storeFactory = new FileStoreFactory(settings);
            LogFactory logFactory = new FileLogFactory(settings);
            Engine engine = new Engine(new MarketFactoryImpl(new BookFactoryImpl()));
            MessageHandler messageHandler = new MessageHandler(engine);
            final SocketAcceptor acceptor = new SocketAcceptor(messageHandler, storeFactory, settings,
                    logFactory, new DefaultMessageFactory());

            // add a shutdown hook to stop the acceptor on shutdown
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    LOGGER.info("Stopping acceptor in shutdown hook");
                    try {
                        acceptor.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // start the replay reader if enabled
            if  (settings.isSetting(REPLAY_ENABLED) && settings.getBool(REPLAY_ENABLED)) {
                try {
                    FIXFileReader reader = new FIXFileReader(engine, settings);
                    messageHandler.addSubscriptionObserver(reader);

                    Thread runner = new Thread(reader, "FIXFileReader");
                    runner.start();
                }
                catch (Exception e) {
                    LOGGER.warn("Unable to create FIXFileReader ", e);
                }
            }

            // start receiving connections
            acceptor.block();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}