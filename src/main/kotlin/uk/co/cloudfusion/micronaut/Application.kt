package uk.co.cloudfusion.micronaut

import io.micronaut.context.annotation.Bean
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.runtime.Micronaut.run
import io.micronaut.scheduling.async.AsyncInterceptor
import io.micronaut.serde.annotation.Serdeable
import java.time.Duration
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

fun main(args: Array<String>) {
    run(*args)
}

@Controller
class AiChiefController(
    val fridgeReactiveClient: FridgeReactiveClient,
    val fridgeCoroutineClient: FridgeCoroutineClient,
    val hobReactiveClient: HobReactiveClient,
    val childrenClient: ChildrenClient,
    private val async: AsyncInterceptor
) {

    @Get("/api/v1/breakfast3")
    suspend fun getBreakfast3(): List<FoodItems> = coroutineScope {

        val sausages = async { fridgeCoroutineClient.getSassages() }
        val bacon = async { fridgeCoroutineClient.getBacon() }
        val eggs = async { fridgeCoroutineClient.getEgg() }

        val friedItems = hobReactiveClient.fry(awaitAll(sausages, bacon, eggs)).asFlow()
        childrenClient.call()
        friedItems.toList()
    }

    @Get("/api/v1/breakfast2")
    suspend fun getBreakfast2(): List<FoodItems> = coroutineScope {
        val sausages = async { fridgeReactiveClient.getSassages().awaitSingle() }
        val bacon = async { fridgeReactiveClient.getBacon().awaitSingle() }
        val eggs = async { fridgeReactiveClient.getEgg().awaitSingle() }

        val friedItems = hobReactiveClient.fry(awaitAll(sausages, bacon, eggs)).asFlow()
        childrenClient.call()
        friedItems.toList()
    }

    @Get("/api/v1/breakfast")
    fun getBreakfast(): Flux<FoodItems> {
        return Flux.zip(
            fridgeReactiveClient.getSassages(),
            fridgeReactiveClient.getBacon(),
            fridgeReactiveClient.getEgg()
        )
            .flatMap { Flux.zip(hobReactiveClient.fry(it.t1, it.t2, it.t3), childrenClient.call()).map { z -> z.t1 } }
            .doOnNext { println("Cooked $it") }
            .doOnError { println("Burnt $it") }
    }
}

@Bean
class FridgeReactiveClient {

    private fun delayAndLog(FoodItems: FoodItems): Mono<FoodItems> {
        return Mono.delay(Duration.ofSeconds(1))
            .doOnNext { println("Got $FoodItems") }
            .map { FoodItems }
    }

    fun getSassages() = delayAndLog(FoodItems.Sausages("Cumberland"))

    fun getBacon() = delayAndLog(FoodItems.Bacon(1))

    fun getEgg() = delayAndLog(FoodItems.Egg(1))
}

@Bean
class FridgeCoroutineClient {

    private suspend fun delayAndLog(FoodItems: FoodItems): FoodItems = coroutineScope {
        delay(1000)
        println("Got $FoodItems")
        FoodItems
    }

    suspend fun getSassages() = delayAndLog(FoodItems.Sausages("Cumberland"))

    suspend fun getBacon() = delayAndLog(FoodItems.Bacon(1))

    suspend fun getEgg() = delayAndLog(FoodItems.Egg(1))
}

@Bean
class ChildrenClient {


    fun call(): Flux<Void> {
        return Flux.empty()
    }
}

@Bean
class HobReactiveClient {


    fun fry(vararg foodItems: FoodItems): Flux<FoodItems> {
        return Flux.fromArray(foodItems)
    }

    fun fry(foodItems: List<FoodItems>): Flux<FoodItems> {
        return Flux.fromIterable(foodItems)
    }
}


@Serdeable
sealed class FoodItems() {
    @Serdeable
    data class Sausages(val type: String) : FoodItems()

    @Serdeable
    data class Bacon(val thinknees: Int) : FoodItems()

    @Serdeable
    data class Egg(val size: Int) : FoodItems()
}
