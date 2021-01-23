package shopping

import shopping.protocols.generated.Basket
import shopping.protocols.generated.Supply

import org.apache.flink.statefun.sdk.io.Router
import org.apache.flink.statefun.sdk.io.Router.Downstream

class RestockRouter : Router<Supply.Restock> {
    override fun route(message: Supply.Restock, downstream: Downstream<Supply.Restock>) {
        downstream.forward(SupplyFun.TYPE, message.id, message)
    }
}

class AddToBasketRouter : Router<Basket.Add> {
    override fun route(message: Basket.Add, downstream: Downstream<Basket.Add>) {
        downstream.forward(BasketFun.TYPE, message.id, message)
    }
}