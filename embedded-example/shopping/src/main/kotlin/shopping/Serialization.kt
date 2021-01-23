package shopping

import shopping.protocols.generated.*
import com.google.protobuf.InvalidProtocolBufferException
import org.apache.flink.statefun.sdk.kafka.KafkaEgressSerializer
import org.apache.flink.statefun.sdk.kafka.KafkaIngressDeserializer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord

class RestockDeserializer : KafkaIngressDeserializer<Supply.Restock> {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    override fun deserialize(input: ConsumerRecord<ByteArray, ByteArray>): Supply.Restock {
        return try {
            Supply.Restock.parseFrom(input.value())
        } catch (ex: InvalidProtocolBufferException) {
            throw RuntimeException(ex)
        }
    }
}

class AddToBasketDeserializer : KafkaIngressDeserializer<Basket.Add> {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    override fun deserialize(input: ConsumerRecord<ByteArray, ByteArray>): Basket.Add {
        return try {
            Basket.Add.parseFrom(input.value())
        } catch (ex: InvalidProtocolBufferException) {
            throw RuntimeException(ex)
        }
    }
}

class SupplyChangedSerializer : KafkaEgressSerializer<Supply.Changed> {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    override fun serialize(event: Supply.Changed): ProducerRecord<ByteArray, ByteArray> {
        val key = event.id.toByteArray()
        val value = event.toByteArray()
        return ProducerRecord("supply-changed", key, value)
    }
}