import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import ru.tinkoff.invest.openapi.model.rest.SandboxCurrency
import ru.tinkoff.invest.openapi.model.rest.SandboxRegisterRequest
import ru.tinkoff.invest.openapi.model.rest.SandboxSetCurrencyBalanceRequest
import ru.tinkoff.invest.openapi.model.rest.SandboxSetPositionBalanceRequest
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

private fun readToken(): String? {
    val classLoader: ClassLoader = object {}.javaClass.classLoader
    return classLoader.getResourceAsStream("token")?.bufferedReader()?.readText()
}

fun main() = runBlocking {
    initLogger()
    val logger = LoggerFactory.getLogger("main")
    val token = readToken() ?: throw Error("Place your token in resources/token")
    val sandboxMode = true
    val ticker = "AAPL"
    val candleInterval = CandleInterval._1MIN

    OkHttpOpenApi(token, sandboxMode).use { api ->
        val instrumentsList = api.marketContext.searchMarketInstrumentsByTicker(ticker).join()
        val instrument = instrumentsList.instruments.stream().findFirst().get()

        if (api.isSandboxMode) {
            api.sandboxContext.performRegistration(SandboxRegisterRequest()).join()
            api.sandboxContext.clearAll(null)
            api.sandboxContext.setCurrencyBalance(SandboxSetCurrencyBalanceRequest().apply {
                currency = SandboxCurrency.USD
                balance = 1000.0.toBigDecimal()
            }, null)
            api.sandboxContext.setPositionBalance(SandboxSetPositionBalanceRequest().apply {
                figi = instrument.figi
                balance = 10.toBigDecimal()
            }, null)
        }
        launch {
            logger.info("Run events consumer")
            api.streamingContext.asFlow().collect { event ->
                logger.info("Got event: $event")
            }
        }

        val currentOrders = api.ordersContext.getOrders(null).join()
        logger.info("Current orders: $currentOrders")
        val currentPositions = api.portfolioContext.getPortfolio(null).join()
        logger.info("Current positions: $currentPositions")

        val portfolioCurrencies = api.portfolioContext.getPortfolioCurrencies(null).join()
        logger.info("Portfolio currency: $portfolioCurrencies")

        api.streamingContext.sendRequest(StreamingRequest.subscribeCandle(instrument.figi, candleInterval))
    }
}