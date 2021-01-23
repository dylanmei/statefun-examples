package shopping

import org.apache.flink.api.java.ExecutionEnvironment
import org.apache.flink.api.java.tuple.Tuple2
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.statefun.flink.state.processor.Context
import org.apache.flink.statefun.flink.state.processor.StateBootstrapFunction
import org.apache.flink.statefun.flink.state.processor.StatefulFunctionsSavepointCreator
import org.apache.flink.statefun.sdk.annotations.Persisted
import org.apache.flink.statefun.sdk.io.Router
import org.apache.flink.statefun.sdk.state.PersistedValue
import org.apache.flink.statefun.sdk.FunctionType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object StateBootstrap {
    val log: Logger = LoggerFactory.getLogger(Module::class.java)
    val SUPPLY_FUNCTION_TYPE = FunctionType("shopping", "supply")
    val BASKET_FUNCTION_TYPE = FunctionType("shopping", "basket")

    @JvmStatic
    fun main(args: Array<String>) {
        log.info("Starting bootstrap state processor...")
        val params = ParameterTool.fromArgs(args)
        val savepointPath = params["savepointPath"] ?: "/tmp/shopping.state"
        val env = ExecutionEnvironment.getExecutionEnvironment()

        val supplyState = env.fromCollection(
            listOf(
                Tuple2.of("Shoes", 100),
                Tuple2.of("Coat", 100),
                Tuple2.of("Backpack", 100),
                Tuple2.of("Purse", 100),
                Tuple2.of("Hat", 100),
                Tuple2.of("Watch", 100),
                Tuple2.of("Pants", 100),
                Tuple2.of("Umbrella", 100),
            )
        ).name("supply-state")

        StatefulFunctionsSavepointCreator(8).apply {
            withBootstrapData(supplyState) {
                SupplyStateRouter()
            }
            withStateBootstrapFunctionProvider(SUPPLY_FUNCTION_TYPE) {
                SupplyStateBootstrapFun()
            }
            write(savepointPath)
        }

        env.execute()
        log.info("Done! Savepoint is at $savepointPath.")
    }

    class SupplyStateRouter : Router<Tuple2<String, Int>> {
        override fun route(message: Tuple2<String, Int>, downstream: Router.Downstream<Tuple2<String, Int>>) {
            downstream.forward(SUPPLY_FUNCTION_TYPE, message.f0, message)
        }
    }

    class SupplyStateBootstrapFun : StateBootstrapFunction {
        @Persisted
        private val supply = PersistedValue.of("supply", Int::class.java)

        override fun bootstrap(context: Context, bootstrapData: Any?) {
            if (bootstrapData is Tuple2<*,*>) {
                supply.set(bootstrapData.f1 as Int)
            }
        }
    }
}
