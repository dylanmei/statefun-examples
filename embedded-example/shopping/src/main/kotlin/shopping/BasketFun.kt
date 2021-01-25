package shopping

import shopping.protocols.generated.*

import org.apache.flink.statefun.sdk.Context
import org.apache.flink.statefun.sdk.FunctionType
import org.apache.flink.statefun.sdk.StatefulFunction
import org.apache.flink.statefun.sdk.annotations.Persisted
import org.apache.flink.statefun.sdk.state.PersistedTable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class BasketFun : StatefulFunction {
    companion object {
        val TYPE = FunctionType(ModuleIO.FUNCTION_NAMESPACE, "basket")
        val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }

    @Persisted
    private val basket = PersistedTable.of("basket", String::class.java, Int::class.java)

    override fun invoke(context: Context, input: Any) {
        when (input) {
            is Basket.Add -> {
                context.send(SupplyFun.TYPE, input.productId, Supply.Request.newBuilder()
                    .setId(input.productId)
                    .setQuantity(input.quantity)
                    .build()
                )
            }
            is Supply.Received -> {
                if (input.status == Availability.OUT_OF_STOCK) {
                    log.warn("OUT OF STOCK! Couldn't add ${input.id} to ${context.self().id()}'s basket")
                } else {
                    // Update our state
                    val quantity = basket.get(input.id) ?: 0
                    basket.set(context.caller().id(), quantity + input.quantity)

                    // Emit a new snapshot of the basket
                    context.send(ModuleIO.BASKET_SNAPSHOTS_EGRESS_ID, Basket.Snapshot.newBuilder()
                        .setId(context.self().id())
                        .addAllItems(
                            basket.entries().map { (key, value) ->
                                Basket.Snapshot.Item.newBuilder()
                                    .setId(key)
                                    .setQuantity(value)
                                    .build()
                            }
                        )
                        .build()
                    )
                }
            }
        }
    }
}