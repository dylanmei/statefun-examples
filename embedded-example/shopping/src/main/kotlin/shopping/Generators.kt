package shopping

import shopping.protocols.generated.Basket
import shopping.protocols.generated.Supply
import org.apache.flink.statefun.flink.harness.io.SerializableSupplier

class RestockGenerator(intervalMs: Long)
    : Generator<Supply.Restock>(intervalMs)
{
    override fun generate() = Supply.Restock.newBuilder()
        .setId(sourceProducts.random())
        .setQuantity(arrayOf(1, 1, 1, 1, 2, 2, 3).random())
        .build()
}

class AddToBasketGenerator(intervalMs: Long)
    : Generator<Basket.Add>(intervalMs)
{
    override fun generate() = Basket.Add.newBuilder()
        .setId(sourceUsers.random())
        .setProductId(sourceProducts.random())
        .setQuantity(arrayOf(1, 1, 1, 1, 1, 1, 2).random())
        .build()
}

abstract class Generator<T>(val intervalMs: Long) : SerializableSupplier<T> {
    private var alreadyRunning = false

    abstract fun generate(): T

    override fun get(): T {
        if (alreadyRunning) {
            Thread.sleep(intervalMs)
        }
        return generate().also {
            alreadyRunning = true
        }
    }

    companion object {
        val sourceUsers = arrayOf("Dylan")
        val sourceProducts = arrayOf("Shoes", "Coat", "Backpack", "Purse", "Hat", "Watch", "Pants")
    }
}
