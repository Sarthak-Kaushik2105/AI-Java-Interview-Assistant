package Java.Interview.Assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class Jackson2ObjectMapperAutoConfigurationTest {
    @Test
    void jackson2AutoConfigurationProvidesTheObjectMapperRequiredByEvaluationParser() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(Jackson2AutoConfiguration.class))
                .run(context -> assertThat(context).hasSingleBean(ObjectMapper.class));
    }
}
