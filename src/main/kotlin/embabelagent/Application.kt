package embabelagent

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.autonomy.AgentInvocation
import com.embabel.agent.api.common.createObject
import com.embabel.agent.config.annotation.EnableAgents
import com.embabel.agent.core.AgentPlatform
import com.embabel.agent.domain.io.UserInput
import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


@SpringBootApplication
@EnableAgents
open class Application

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
        context.promptRunner().createObject("Generate 5 of: ${userInput.content}")

    // todo: rate with a different model than the one that generated the jokes
    @Action
    fun rateJokes(jokes: Jokes, context: OperationContext): JokesAndRatings =
        JokesAndRatings(
            context.parallelMap(jokes.content, 8) { joke ->
                JokeAndRating(joke, context.promptRunner().createObject("Rate this joke: $joke"))
            }.toSet()
        )

    @Action
    @AchievesGoal(description = "Best Joke")
    fun bestJoke(jokesAndRatings: JokesAndRatings): JokeAndRating = run {
        println(jokesAndRatings.content)
        jokesAndRatings.content.maxBy { it.rating.percentage }
    }
}

@RestController
class ConversationalController(val agentPlatform: AgentPlatform) {

    @GetMapping("/")
    fun joke(): JokeAndRating =
        AgentInvocation.create<JokeAndRating>(agentPlatform).invoke(UserInput("a great joke about programming"))

}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
