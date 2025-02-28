package io.micronaut.configuration.kafka.offsets

import io.micronaut.configuration.kafka.AbstractKafkaContainerSpec
import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.configuration.kafka.annotation.KafkaListener
import io.micronaut.configuration.kafka.annotation.Topic
import io.micronaut.context.annotation.Requires
import io.micronaut.messaging.Acknowledgement
import jakarta.inject.Singleton

import static io.micronaut.configuration.kafka.annotation.OffsetReset.EARLIEST
import static io.micronaut.configuration.kafka.annotation.OffsetStrategy.DISABLED
import static io.micronaut.configuration.kafka.config.AbstractKafkaConfiguration.EMBEDDED_TOPICS

class ManualAckSpec extends AbstractKafkaContainerSpec {

    public static final String TOPIC_SYNC = "ManualAckSpec-products-sync"

    protected Map<String, Object> getConfiguration() {
        super.configuration +
                [(EMBEDDED_TOPICS): [TOPIC_SYNC]]
    }

    void "test manual ack"() {
        given:
        ProductClient client = context.getBean(ProductClient)
        ProductListener listener = context.getBean(ProductListener)

        when:
        client.send(new Product(name: "Apple"))
        client.send(new Product(name: "Orange"))

        then:
        conditions.eventually {
            listener.products.size() == 2
            listener.products.find() { it.name == "Apple"}
        }
    }

    @Requires(property = 'spec.name', value = 'ManualAckSpec')
    @KafkaClient
    static interface ProductClient {
        @Topic(ManualAckSpec.TOPIC_SYNC)
        void send(Product product)
    }

    @Requires(property = 'spec.name', value = 'ManualAckSpec')
    @Singleton
    static class ProductListener {

        List<Product> products = []

        @KafkaListener(offsetReset = EARLIEST, offsetStrategy = DISABLED)
        @Topic(ManualAckSpec.TOPIC_SYNC)
        void receive(Product product, Acknowledgement acknowledgement) {
            products << product

            acknowledgement.ack()
        }
    }

    static class Product {
        String name
    }
}
