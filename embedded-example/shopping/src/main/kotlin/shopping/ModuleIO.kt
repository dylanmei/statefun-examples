package shopping

import shopping.protocols.generated.*
import org.apache.flink.statefun.sdk.io.EgressIdentifier
import org.apache.flink.statefun.sdk.io.EgressSpec
import org.apache.flink.statefun.sdk.io.IngressIdentifier
import org.apache.flink.statefun.sdk.io.IngressSpec
import org.apache.flink.statefun.sdk.kafka.KafkaEgressBuilder

import org.apache.flink.statefun.sdk.kafka.KafkaIngressBuilder
import org.apache.kafka.clients.consumer.ConsumerConfig

class ModuleIO(val config: Map<String, String>) {
    companion object {
        const val FUNCTION_NAMESPACE = "shopping"

        val RESTOCK_INGRESS_ID = IngressIdentifier(
            Supply.Restock::class.java, FUNCTION_NAMESPACE, "restock"
        )

        val ADD_TO_BASKET_INGRESS_ID = IngressIdentifier(
            Basket.Add::class.java, FUNCTION_NAMESPACE, "add-to-basket"
        )

        val SUPPLY_CHANGED_EGRESS_ID = EgressIdentifier(
            FUNCTION_NAMESPACE, "supply-changed", Supply.Changed::class.java
        )

        val BASKET_SNAPSHOTS_EGRESS_ID = EgressIdentifier(
            FUNCTION_NAMESPACE, "basket-snapshots", Basket.Snapshot::class.java
        )
    }

    val restockIngressSpec: IngressSpec<Supply.Restock>
        get() = KafkaIngressBuilder.forIdentifier(RESTOCK_INGRESS_ID)
            .withKafkaAddress(config["kafka.bootstrap.servers"])
            .withTopic("restock")
            .withProperty(ConsumerConfig.GROUP_ID_CONFIG, FUNCTION_NAMESPACE)
            .withDeserializer(RestockDeserializer::class.java)
            .build()

    val addToBasketIngressSpec: IngressSpec<Basket.Add>
        get() = KafkaIngressBuilder.forIdentifier(ADD_TO_BASKET_INGRESS_ID)
            .withKafkaAddress(config["kafka.bootstrap.servers"])
            .withTopic("add-to-basket")
            .withProperty(ConsumerConfig.GROUP_ID_CONFIG, FUNCTION_NAMESPACE)
            .withDeserializer(AddToBasketDeserializer::class.java)
            .build()

    val supplyChangedEgressSpec: EgressSpec<Supply.Changed>
        get() = KafkaEgressBuilder.forIdentifier(SUPPLY_CHANGED_EGRESS_ID)
            .withKafkaAddress(config["kafka.bootstrap.servers"])
            .withSerializer(SupplyChangedSerializer::class.java)
            .build()

    val basketSnapshotsEgressSpec: EgressSpec<Basket.Snapshot>
        get() = KafkaEgressBuilder.forIdentifier(BASKET_SNAPSHOTS_EGRESS_ID)
            .withKafkaAddress(config["kafka.bootstrap.servers"])
            .withSerializer(BasketSnapshotSerializer::class.java)
            .build()
}
