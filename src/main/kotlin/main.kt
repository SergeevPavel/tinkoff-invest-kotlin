import com.sun.org.slf4j.internal.LoggerFactory
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.LoggerFactory
import ru.tinkoff.invest.openapi.model.rest.*
import ru.tinkoff.invest.openapi.model.streaming.CandleInterval
import ru.tinkoff.invest.openapi.model.streaming.StreamingRequest
import ru.tinkoff.invest.openapi.okhttp.OkHttpOpenApi
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.logging.LogManager


private fun initLogger() {
    val logManager = LogManager.getLogManager()
    val classLoader: ClassLoader = object {}.javaClass.classLoader
    classLoader.getResourceAsStream("logging.properties").use { input ->
        if (input == null) {
            throw FileNotFoundException()
        }
        Files.createDirectories(Paths.get("./logs"))
        logManager.readConfiguration(input)
    }
}

suspend fun main()  {
    initLogger()
    val logger = LoggerFactory.getLogger("main")
    val token = ""
    val sandboxMode = true
    val ticker = "AAPL"
    val candleInterval = CandleInterval._1MIN
    OkHttpOpenApi(token, sandboxMode).use { api ->
        if (api.isSandboxMode) {
            api.sandboxContext.performRegistration(SandboxRegisterRequest()).join()
        }

        val currentOrders = api.ordersContext.getOrders(null).join()
        logger.info("Current orders: $currentOrders")

        val currentPositions = api.portfolioContext.getPortfolio(null).join()
        logger.info("Current positions: $currentPositions")

        val instrumentsList = api.marketContext.searchMarketInstrumentsByTicker(ticker).join()
        val instrument = instrumentsList.instruments.stream().findFirst().get()
        logger.info("Instrument: $instrument")

        val portfolioCurrencies = api.portfolioContext.getPortfolioCurrencies(null).join()
        logger.info("Portfolio currency: $portfolioCurrencies")

        api.streamingContext.sendRequest(StreamingRequest.subscribeCandle(instrument.figi, candleInterval))
        api.streamingContext.sendRequest(StreamingRequest.unsubscribeCandle(instrument.figi, candleInterval))
    }
//    OkHttpOpenApi(token, sandboxMode).use { api ->
//        if (api.isSandboxMode) {
//            api.sandboxContext.performRegistration(SandboxRegisterRequest()).join()
//        }
////        launch {
////            logger.info("Run events consumer")
////            api.streamingContext.asFlow().collect { event ->
////                println("Got event: $event")
////            }
////        }
//        val currentOrders = api.ordersContext.getOrders(null).join()
//        logger.info("Current orders: $currentOrders")
//        val currentPositions = api.portfolioContext.getPortfolio(null).join()
//        logger.info("Current positions: $currentPositions")
//
//        val instrumentsList = api.marketContext.searchMarketInstrumentsByTicker(ticker).join()
//        val instrument = instrumentsList.instruments.stream().findFirst().get()
//
//        val portfolioCurrencies = api.portfolioContext.getPortfolioCurrencies(null).join()
//        logger.info("Portfolio currency: $portfolioCurrencies")
//
//        api.streamingContext.sendRequest(StreamingRequest.subscribeCandle(instrument.figi, candleInterval))
//    }
    logger.info("Main ended")
}