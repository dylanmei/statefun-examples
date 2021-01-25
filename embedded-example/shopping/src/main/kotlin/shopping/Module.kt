package shopping

import org.apache.flink.statefun.sdk.spi.StatefulFunctionModule
import com.google.auto.service.AutoService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@AutoService(StatefulFunctionModule::class)
class Module : StatefulFunctionModule {
    companion object {
        val log: Logger = LoggerFactory.getLogger(Module::class.java)
    }

    override fun configure(globalConfiguration: Map<String, String>, binder: StatefulFunctionModule.Binder) {
        log.debug("Configuring our module")

        val mio = ModuleIO(globalConfiguration)

        // Bind ingress to bring input into the system
        binder.bindIngress(mio.restockIngressSpec)
        binder.bindIngress(mio.addToBasketIngressSpec)

        // Route messages from ingress to functions
        binder.bindIngressRouter(ModuleIO.RESTOCK_INGRESS_ID, RestockRouter())
        binder.bindIngressRouter(ModuleIO.ADD_TO_BASKET_INGRESS_ID, AddToBasketRouter())

        // Bind egress to emit events or objects
        binder.bindEgress(mio.supplyChangedEgressSpec)
        binder.bindEgress(mio.basketSnapshotsEgressSpec)

        // Bind a function provider to a function type
        binder.bindFunctionProvider(SupplyFun.TYPE) {
            SupplyFun()
        }
        binder.bindFunctionProvider(BasketFun.TYPE) {
            BasketFun()
        }
    }
}
