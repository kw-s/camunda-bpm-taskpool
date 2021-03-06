package io.holunda.camunda.taskpool.example.process.view.simple

import io.holunda.camunda.taskpool.view.simple.EnableTaskPoolSimpleView
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!mongo")
@EnableTaskPoolSimpleView
open class SimpleViewConfiguration
