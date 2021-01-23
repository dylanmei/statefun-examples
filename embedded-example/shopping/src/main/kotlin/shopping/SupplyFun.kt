package shopping

import shopping.protocols.generated.*

import org.apache.flink.statefun.sdk.Context
import org.apache.flink.statefun.sdk.FunctionType
import org.apache.flink.statefun.sdk.StatefulFunction
import org.apache.flink.statefun.sdk.annotations.Persisted
import org.apache.flink.statefun.sdk.state.PersistedValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class SupplyFun : StatefulFunction {
    companion object {
        val TYPE = FunctionType("example", "supply")
        val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }

    @Persisted
    private val supply = PersistedValue.of("supply", Int::class.java)

    override fun invoke(context: Context, input: Any) {
        when (input) {
            is Supply.Restock -> {
                // Get the state or create it
                val quantity = supply.getOrDefault(0) + input.quantity
                supply.set(quantity)

                // Emit a change event
                context.send(ModuleIO.SUPPLY_CHANGED_EGRESS_ID,
                    Supply.Changed.newBuilder()
                        .setId(input.id)
                        .setTotalQuantity(quantity)
                        .setDifference(input.quantity)
                        .build()
                )
            }
            is Supply.Request -> {
                // Get the state or create it
                val quantity = supply.getOrDefault(0) - input.quantity
                if (quantity < 0) {
                    // Not enough availability to fulfil the request
                    context.send(context.caller(),
                        Supply.Received.newBuilder()
                            .setId(input.id)
                            .setQuantity(0)
                            .setStatus(Supply.Availability.OUT_OF_STOCK)
                            .build()
                    )
                }
                else {
                    // Update our stock
                    supply.set(quantity)

                    context.send(context.caller(),
                        Supply.Received.newBuilder()
                            .setId(input.id)
                            .setQuantity(input.quantity)
                            .setStatus(Supply.Availability.IN_STOCK)
                            .build()
                    )

                    // Emit a change event
                    context.send(ModuleIO.SUPPLY_CHANGED_EGRESS_ID,
                        Supply.Changed.newBuilder()
                            .setId(input.id)
                            .setTotalQuantity(quantity)
                            .setDifference(-input.quantity)
                            .build()
                    )
                }
            }
        }
    }
}