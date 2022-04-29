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
import shopping.protocols.generated.*
import java.io.File

object StateBootstrap {
    val log: Logger = LoggerFactory.getLogger(StateBootstrap::class.java)
    val SUPPLY_FUNCTION_TYPE = FunctionType("shopping", "supply")

    @JvmStatic
    fun main(args: Array<String>) {
        log.info("Starting bootstrap state processor...")
        val params = ParameterTool.fromArgs(args)
        val savepointPath = params["savepointPath"] ?: "/tmp/shopping.state"
        val env = ExecutionEnvironment.getExecutionEnvironment()

        val supplyState = env.fromElements(
            Tuple2.of("Shoes", makeSupply(100)),
            Tuple2.of("Coat", makeSupply(100)),
            Tuple2.of("Backpack", makeSupply(100)),
            Tuple2.of("Purse", makeSupply(100)),
            Tuple2.of("Hat", makeSupply(100)),
            Tuple2.of("Watch", makeSupply(100)),
            Tuple2.of("Pants", makeSupply(100)),
            Tuple2.of("Umbrella", makeSupply(100)),
        )

        StatefulFunctionsSavepointCreator(64).apply {
            withFsStateBackend()

            withBootstrapData(supplyState) {
                SupplyStateRouter()
            }
            withStateBootstrapFunctionProvider(SUPPLY_FUNCTION_TYPE) {
                SupplyStateBootstrapFun()
            }

            write(savepointPath)
        }

        File(savepointPath).run {
            deleteRecursively()
        }

        env.execute()
        log.info("Done! Savepoint is at $savepointPath.")
    }

    class SupplyStateRouter : Router<Tuple2<String, Supply>> {
        override fun route(message: Tuple2<String, Supply>, downstream: Router.Downstream<Tuple2<String, Supply>>) {
            downstream.forward(SUPPLY_FUNCTION_TYPE, message.f0, message)
        }
    }

    class SupplyStateBootstrapFun : StateBootstrapFunction {
        @Persisted
        private val supplyState = PersistedValue.of("supply", Int::class.java)

        override fun bootstrap(context: Context, bootstrapData: Any?) {
            if (bootstrapData is Tuple2<*,*>) {
                log.info("Setting Supply state for ${bootstrapData.f0}")

                val supply = bootstrapData.f1 as Supply
                supplyState.set(supply.quantity)
            } else {
                log.error("Unexpected Supply state for ${context.self().id()}")
            }
        }
    }

    fun makeSupply(quantity: Int) = Supply.newBuilder()
        .setQuantity(quantity)
        .build()
}
