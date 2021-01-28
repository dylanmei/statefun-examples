package shopping

import shopping.protocols.generated.Basket
import shopping.protocols.generated.Supply
import org.apache.flink.statefun.flink.harness.io.SerializableSupplier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class RestockGenerator
    : SerializableSupplier<Supply.Restock>
{
    val supplies = sourceProducts.map {
        Supply.Restock.newBuilder()
            .setId(it)
            .setQuantity(5)
            .build()
    }.toMutableList()

    fun done() = supplies.isEmpty()

    override fun get(): Supply.Restock {
        if (done()) {
            log.debug("Nothing more to generate. Sleeping for 10m...")
            Thread.sleep(600000)
        }
        return supplies.removeAt(0)
    }

    companion object {
        val sourceProducts = arrayOf("Shoes", "Coat", "Backpack", "Purse", "Hat", "Watch", "Pants", "Umbrella")
        val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}

class AddToBasketGenerator(val initialWaitMs: Long, val intervalMs: Long)
    : SerializableSupplier<Basket.Add>
{
    private var alreadyRunning = false

    override fun get(): Basket.Add {
        if (alreadyRunning) {
            Thread.sleep(intervalMs)
        } else {
            Thread.sleep(initialWaitMs)
        }

        return Basket.Add.newBuilder()
            .setId(sourceUsers.random())
            .setProductId(RestockGenerator.sourceProducts.random())
            .setQuantity(arrayOf(1, 1, 1, 1, 1, 2, 2, 3).random())
            .build()
            .also {
                alreadyRunning = true
            }
    }

    companion object {
        val sourceUsers = arrayOf("Dylan")
    }
}