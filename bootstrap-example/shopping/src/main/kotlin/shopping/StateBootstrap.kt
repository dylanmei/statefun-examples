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
    val log: Logger = LoggerFactory.getLogger(Module::class.java)
    val SUPPLY_FUNCTION_TYPE = FunctionType("shopping", "supply")
    //val BASKET_FUNCTION_TYPE = FunctionType("shopping", "basket")

    @JvmStatic
    fun main(args: Array<String>) {
        log.info("Starting bootstrap state processor...")
        val params = ParameterTool.fromArgs(args)
        val savepointPath = params["savepointPath"] ?: "/tmp/shopping.state"
        val env = ExecutionEnvironment.getExecutionEnvironment()

        val supplyState = env.fromCollection(
            listOf(
                Tuple2.of("Shoes", makeSupply(100)),
                Tuple2.of("Coat", makeSupply(100)),
                Tuple2.of("Backpack", makeSupply(100)),
                Tuple2.of("Purse", makeSupply(100)),
                Tuple2.of("Hat", makeSupply(100)),
                Tuple2.of("Watch", makeSupply(100)),
                Tuple2.of("Pants", makeSupply(100)),
                Tuple2.of("Umbrella", makeSupply(100)),
            )
        ).name("supply-source")

        //val basketState = env.fromCollection(
        //    listOf(
        //        Tuple2.of("Dylan", makeEmptyBasket())
        //    )
        //).name("basket-source")

        StatefulFunctionsSavepointCreator(8).apply {
            withBootstrapData(supplyState) {
                SupplyStateRouter()
            }
            withStateBootstrapFunctionProvider(SUPPLY_FUNCTION_TYPE) {
                SupplyStateBootstrapFun()
            }

            //withBootstrapData(basketState) {
            //    BasketStateRouter()
            //}
            //withStateBootstrapFunctionProvider(BASKET_FUNCTION_TYPE) {
            //    BasketStateBootstrapFun()
            //}

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

    //class BasketStateRouter : Router<Tuple2<String, Basket>> {
    //    override fun route(message: Tuple2<String, Basket>, downstream: Router.Downstream<Tuple2<String, Basket>>) {
    //        downstream.forward(BASKET_FUNCTION_TYPE, message.f0, message)
    //    }
    //}

    class SupplyStateBootstrapFun : StateBootstrapFunction {
        @Persisted
        private val supply = PersistedValue.of("supply", Supply::class.java)

        override fun bootstrap(context: Context, bootstrapData: Any?) {
            if (bootstrapData is Tuple2<*,*>) {
                supply.set(bootstrapData.f1 as Supply)
            }
        }
    }

    //class BasketStateBootstrapFun : StateBootstrapFunction {
    //    @Persisted
    //    private val basket = PersistedValue.of("basket", Basket::class.java)

    //    override fun bootstrap(context: Context, bootstrapData: Any?) {
    //        if (bootstrapData is Tuple2<*,*>) {
    //            basket.set(bootstrapData.f1 as Basket)
    //        }
    //    }
    //}

    fun makeSupply(quantity: Int) = Supply.newBuilder()
        .setQuantity(quantity)
        .build()

    //fun makeEmptyBasket() = Basket.newBuilder()
    //    .addAllItems(emptyList())
    //    .build()
}
