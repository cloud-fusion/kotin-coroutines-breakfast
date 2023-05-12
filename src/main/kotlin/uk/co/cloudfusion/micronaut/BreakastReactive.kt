package uk.co.cloudfusion.micronaut

import io.micronaut.context.annotation.Bean
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.serde.annotation.Serdeable
import java.time.Duration
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2

@Controller("/api/v1/reactive")
class AiChiefReactiveController(
    val fridgeReactiveClient: FridgeReactiveClient,
    val hobReactiveClient: HobReactiveClient,
    val childrenReactiveClient: ChildrenReactiveClient,
) {

    @Get("/breakfaslt/seq")
    fun getBreakfastSeq(): Flux<FoodItems> {
        return fridgeReactiveClient.getSassages()
            .flatMap { s-> fridgeReactiveClient.getBacon()
                .flatMap { b -> fridgeReactiveClient.getEgg().map { e -> Triple(b, e, s) } }
            }
            .doOnNext { childrenReactiveClient.call() }
            .flatMapMany { bes -> hobReactiveClient.fry(bes.first, bes.second, bes.third) }
    }
    @Get("/breakfaslt/async/couroutine-addaptor")
    suspend fun getBreakfastWithCoroutineAdaptors(): List<FoodItems> = coroutineScope {
        val sausages = async { fridgeReactiveClient.getSassages().awaitSingle() }
        val bacon = async { fridgeReactiveClient.getBacon().awaitSingle() }
        val eggs = async { fridgeReactiveClient.getEgg().awaitSingle() }

        val friedItems = hobReactiveClient.fry(awaitAll(sausages, bacon, eggs)).asFlow()
        async { childrenReactiveClient.call().awaitSingle() }
        friedItems.toList()
    }

    @Get("breakfast/async")
    fun getBreakfast(): Flux<FoodItems> {
        return Flux.zip(
            fridgeReactiveClient.getSassages(),
            fridgeReactiveClient.getBacon(),
            fridgeReactiveClient.getEgg()
        )
            .flatMap {
                Flux.zip(
                    hobReactiveClient.fry(it.t1, it.t2, it.t3),
                    childrenReactiveClient.call()
                )
                    .map { z -> z.t1 }
            }
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
class ChildrenReactiveClient {
    fun call(): Mono<Void> {
        return Mono.empty()
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
