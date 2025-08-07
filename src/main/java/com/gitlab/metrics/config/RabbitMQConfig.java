package com.gitlab.metrics.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange names
    public static final String GITLAB_EVENTS_EXCHANGE = "gitlab.events.exchange";
    
    // Queue names
    public static final String COMMIT_ANALYSIS_QUEUE = "commit.analysis.queue";
    public static final String QUALITY_ANALYSIS_QUEUE = "quality.analysis.queue";
    public static final String ISSUE_ANALYSIS_QUEUE = "issue.analysis.queue";
    public static final String MERGE_REQUEST_ANALYSIS_QUEUE = "merge.request.analysis.queue";
    public static final String BUG_TRACKING_ANALYSIS_QUEUE = "bug.tracking.analysis.queue";
    public static final String EFFICIENCY_ANALYSIS_QUEUE = "efficiency.analysis.queue";
    
    // Routing keys
    public static final String COMMIT_ROUTING_KEY = "gitlab.event.push";
    public static final String QUALITY_ROUTING_KEY = "gitlab.event.quality";
    public static final String ISSUE_ROUTING_KEY = "gitlab.event.issue";
    public static final String MERGE_REQUEST_ROUTING_KEY = "gitlab.event.merge_request";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(5);
        factory.setMaxConcurrentConsumers(10);
        return factory;
    }

    // Exchange
    @Bean
    public TopicExchange gitlabEventsExchange() {
        return new TopicExchange(GITLAB_EVENTS_EXCHANGE, true, false);
    }

    // Queues
    @Bean
    public Queue commitAnalysisQueue() {
        return QueueBuilder.durable(COMMIT_ANALYSIS_QUEUE)
                .withArgument("x-dead-letter-exchange", GITLAB_EVENTS_EXCHANGE + ".dlx")
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .build();
    }

    @Bean
    public Queue qualityAnalysisQueue() {
        return QueueBuilder.durable(QUALITY_ANALYSIS_QUEUE)
                .withArgument("x-dead-letter-exchange", GITLAB_EVENTS_EXCHANGE + ".dlx")
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .build();
    }

    @Bean
    public Queue issueAnalysisQueue() {
        return QueueBuilder.durable(ISSUE_ANALYSIS_QUEUE)
                .withArgument("x-dead-letter-exchange", GITLAB_EVENTS_EXCHANGE + ".dlx")
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .build();
    }

    @Bean
    public Queue mergeRequestAnalysisQueue() {
        return QueueBuilder.durable(MERGE_REQUEST_ANALYSIS_QUEUE)
                .withArgument("x-dead-letter-exchange", GITLAB_EVENTS_EXCHANGE + ".dlx")
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .build();
    }

    @Bean
    public Queue bugTrackingAnalysisQueue() {
        return QueueBuilder.durable(BUG_TRACKING_ANALYSIS_QUEUE)
                .withArgument("x-dead-letter-exchange", GITLAB_EVENTS_EXCHANGE + ".dlx")
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .build();
    }

    @Bean
    public Queue efficiencyAnalysisQueue() {
        return QueueBuilder.durable(EFFICIENCY_ANALYSIS_QUEUE)
                .withArgument("x-dead-letter-exchange", GITLAB_EVENTS_EXCHANGE + ".dlx")
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .build();
    }

    // Bindings
    @Bean
    public Binding commitAnalysisBinding() {
        return BindingBuilder.bind(commitAnalysisQueue())
                .to(gitlabEventsExchange())
                .with(COMMIT_ROUTING_KEY);
    }

    @Bean
    public Binding qualityAnalysisBinding() {
        return BindingBuilder.bind(qualityAnalysisQueue())
                .to(gitlabEventsExchange())
                .with(QUALITY_ROUTING_KEY);
    }

    @Bean
    public Binding issueAnalysisBinding() {
        return BindingBuilder.bind(issueAnalysisQueue())
                .to(gitlabEventsExchange())
                .with(ISSUE_ROUTING_KEY);
    }

    @Bean
    public Binding mergeRequestAnalysisBinding() {
        return BindingBuilder.bind(mergeRequestAnalysisQueue())
                .to(gitlabEventsExchange())
                .with(MERGE_REQUEST_ROUTING_KEY);
    }

    // Dead Letter Queue setup
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(GITLAB_EVENTS_EXCHANGE + ".dlx", true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("dead.letter.queue").build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("dead.letter");
    }
}