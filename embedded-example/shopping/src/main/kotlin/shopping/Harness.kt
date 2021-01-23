package shopping

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.statefun.flink.harness.Harness as StatefunHarness

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Harness {
    val log: Logger = LoggerFactory.getLogger(Module::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        log.info("Starting development harness")
        val params = ParameterTool.fromArgs(args)
        val config = params.toMap().toMutableMap().apply {
            putIfAbsent("kafka.bootstrap.servers", "localhost:9092")
        }

        StatefunHarness().run {
            withConfiguration("parallelism.default", "1")
            withSupplyingIngress(ModuleIO.RESTOCK_INGRESS_ID, RestockGenerator(5000))
            withSupplyingIngress(ModuleIO.ADD_TO_BASKET_INGRESS_ID, AddToBasketGenerator(5000))
            withConsumingEgress(ModuleIO.SUPPLY_CHANGED_EGRESS_ID, SupplyChangedPrinter())

            config.forEach { (key, value) ->
                withGlobalConfiguration(key, value)
            }

            start()
        }
    }
}
