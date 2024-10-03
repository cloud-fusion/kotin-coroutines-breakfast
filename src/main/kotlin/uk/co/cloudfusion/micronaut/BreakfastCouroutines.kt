package uk.co.cloudfusion.micronaut

import io.micronaut.context.annotation.Bean
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import jdk.internal.org.jline.utils.Colors.s
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2

@Controller("/api/v1/coroutines")
class AiChiefCoroutinesController(
    val fridgeCoroutineClient: FridgeCoroutineClient,
    val hobCoroutineClient: HobCoroutineClient,
    val childrenCoroutineClient: ChildrenCoroutineClient,
) {

    @Get("/breakfaslt/seq")
    suspend fun getBreakfastSeq(): List<FoodItems> {
        val sausages = fridgeCoroutineClient.getSassages()
        val bacon = fridgeCoroutineClient.getBacon()
        val eggs = fridgeCoroutineClient.getEgg()

        val friedItems = hobCoroutineClient.fry(sausages, bacon, eggs)
        childrenCoroutineClient.call()
        return friedItems.toList()
    }

    @Get("breakfast/async")
    suspend fun getBreakfastAsync(): List<FoodItems> = coroutineScope {
        val sausages = async { fridgeCoroutineClient.getSassages() }
        val bacon = async { fridgeCoroutineClient.getBacon() }
        val eggs = async { fridgeCoroutineClient.getEgg() }

        val friedItems = hobCoroutineClient.fry(awaitAll(sausages, bacon, eggs))
        async { childrenCoroutineClient.call() }
        friedItems.toList()
    }

    @Get("breakfast/async/flux")
    fun getBreakfastFlux(): Flux<FoodItems> {
        val sausages = mono { fridgeCoroutineClient.getSassages() }
        val bacon = mono { fridgeCoroutineClient.getBacon() }
        val eggs = mono { fridgeCoroutineClient.getEgg() }

        return Flux.zip(sausages, bacon, eggs)
            .flatMap {
                Mono.zip(
                    mono { childrenCoroutineClient.call() },
                    mono { hobCoroutineClient.fry(it.t1, it.t2, it.t3) })
            }
            .flatMap { it.t2.asFlux()}
    }
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
class ChildrenCoroutineClient {

    fun call(): Flow<Void> {
        return flow { }
    }
}

@Bean
class HobCoroutineClient {


    fun fry(vararg foodItems: FoodItems): Flow<FoodItems> {
        return foodItems.asFlow()
    }

    fun fry(foodItems: List<FoodItems>): Flow<FoodItems> {
        return foodItems.asFlow()
    }
}
