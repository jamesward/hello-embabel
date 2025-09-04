package embabelagent

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.autonomy.Autonomy
import com.embabel.agent.api.common.createObject
import com.embabel.agent.config.annotation.AgentPlatform
import com.embabel.agent.config.models.BedrockModels
import com.embabel.agent.domain.io.UserInput
import com.embabel.common.ai.model.*
import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


@SpringBootApplication
@AgentPlatform(BedrockModels.BEDROCK_PROFILE)
open class Application {

    @Primary
    @Bean
    @DependsOn("bedrockModels")
    open fun modelProvider(
        llms: List<Llm>,
        embeddingServices: List<EmbeddingService>,
        properties: ConfigurableModelProviderProperties,
    ): ModelProvider = ConfigurableModelProvider(
        llms = llms,
        embeddingServices = embeddingServices,
        properties = properties,
    )

}

@JsonClassDescription("Jokes")
@JsonDeserialize
data class Jokes(val content: List<String>)

@JsonClassDescription("Rating")
@JsonDeserialize
data class Rating(val percentage: Double)

@JsonClassDescription("Joke and Rating")
data class JokeAndRating(val content: String, val rating: Rating)

@JsonClassDescription("Jokes and Ratings")
@JsonDeserialize
data class JokesAndRatings(val content: Set<JokeAndRating>)

@Agent(description = "Generates a funny joke")
class JokesAgent {

    @Action
    fun generateJokes(userInput: UserInput, context: OperationContext): Jokes =
        context.ai()
            .withDefaultLlm()
            .createObject<Jokes>("Generate 5 of: ${userInput.content}")

    // todo: rate with a different model than the one that generated the jokes
    @Action
    fun rateJokes(jokes: Jokes, context: OperationContext): JokesAndRatings =
        JokesAndRatings(
            jokes.content.map { joke ->
                val rating = context.ai()
                        .withDefaultLlm()
                        .createObject<Rating>("Rate this joke: $joke")
                JokeAndRating(joke, rating)
            }.toSet()
        )

    @AchievesGoal(description = "Best Joke")
    @Action
    fun bestJoke(jokesAndRatings: JokesAndRatings): JokeAndRating = run {
        println(jokesAndRatings.content)
        jokesAndRatings.content.maxBy { it.rating.percentage }
    }
}

@RestController
class ConversationalController(val autonomy: Autonomy) {

    @GetMapping("/")
    fun joke(): JokeAndRating =
        autonomy.chooseAndRunAgent("a great joke about programming").output as JokeAndRating

}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
