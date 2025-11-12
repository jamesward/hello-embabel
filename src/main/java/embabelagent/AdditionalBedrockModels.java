package embabelagent;


import com.embabel.agent.config.models.bedrock.BedrockModels;
import com.embabel.agent.config.models.bedrock.BedrockOptionsConverter;
import com.embabel.agent.config.models.bedrock.EmbabelBedrockProxyChatModelBuilder;
import com.embabel.agent.spi.config.spring.AgentPlatformConfiguration;
import com.embabel.common.ai.model.Llm;
import com.embabel.common.ai.model.PerTokenPricingModel;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.model.bedrock.autoconfigure.BedrockAwsConnectionProperties;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.util.ArrayList;

@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(AgentPlatformConfiguration.class)
class AdditionalBedrockModels {

    private final AwsCredentialsProvider credentialsProvider;
    private final AwsRegionProvider regionProvider;
    private final BedrockAwsConnectionProperties connectionProperties;
    private final ObjectProvider<ObservationRegistry> observationRegistry;
    private final ObjectProvider<ChatModelObservationConvention> observationConvention;
    private final ObjectProvider<BedrockRuntimeClient> bedrockRuntimeClient;
    private final ObjectProvider<BedrockRuntimeAsyncClient> bedrockRuntimeAsyncClient;

    public AdditionalBedrockModels(AwsCredentialsProvider credentialsProvider, AwsRegionProvider regionProvider, BedrockAwsConnectionProperties bedrockAwsConnectionProperties, ObjectProvider<ObservationRegistry> observationRegistry, ObjectProvider<ChatModelObservationConvention> observationConvention, ObjectProvider<BedrockRuntimeClient> bedrockRuntimeClient, ObjectProvider<BedrockRuntimeAsyncClient> bedrockRuntimeAsyncClient) {
        this.credentialsProvider = credentialsProvider;
        this.regionProvider = regionProvider;
        this.connectionProperties = bedrockAwsConnectionProperties;
        this.observationRegistry = observationRegistry;
        this.observationConvention = observationConvention;
        this.bedrockRuntimeClient = bedrockRuntimeClient;
        this.bedrockRuntimeAsyncClient = bedrockRuntimeAsyncClient;
    }

    ChatModel chatModelOf(String model) {
        var chatModel = new EmbabelBedrockProxyChatModelBuilder()
                .credentialsProvider(credentialsProvider)
                .region(regionProvider.getRegion())
                .timeout(connectionProperties.getTimeout())
                .defaultOptions(ToolCallingChatOptions.builder().model(model).build())
                .observationRegistry(observationRegistry.getIfUnique() != null ? observationRegistry.getIfUnique() : ObservationRegistry.NOOP)
                .bedrockRuntimeClient(bedrockRuntimeClient.getIfAvailable())
                .bedrockRuntimeAsyncClient(bedrockRuntimeAsyncClient.getIfAvailable())
                .build();

        observationConvention.ifAvailable(chatModel::setObservationConvention);

        return chatModel;
    }

    @Bean("bedrockModel-us.amazon.nova-pro-v1:0")
    Llm usNovaPro() {
        var model = "us.amazon.nova-pro-v1:0";

        return new Llm(
                model,
                BedrockModels.PROVIDER,
                chatModelOf(model),
                BedrockOptionsConverter.INSTANCE,
                java.time.LocalDate.parse("2024-05-23"),
                new ArrayList<>(),
                new PerTokenPricingModel(0.8, 3.2)
        );
    }
}
