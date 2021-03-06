package io.holunda.camunda.taskpool.view.mongo.filter

import io.holunda.camunda.taskpool.view.mongo.service.Criterion.DataEntryCriterion
import io.holunda.camunda.taskpool.view.mongo.service.Criterion.TaskCriterion
import io.holunda.camunda.taskpool.view.mongo.service.EQUALS
import io.holunda.camunda.taskpool.view.mongo.service.isTaskAttribute
import io.holunda.camunda.taskpool.view.mongo.service.toCriteria
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class FilterTest {

  @get: Rule
  val expected = ExpectedException.none()

  private val filtersList = listOf("task.name${EQUALS}myName", "task.assignee${EQUALS}kermit", "dataAttr1${EQUALS}value", "dataAttr2${EQUALS}another")

  @Test
  fun `should classify properties`() {
    assertThat(isTaskAttribute("task.id")).isTrue()
    assertThat(isTaskAttribute("task.name")).isTrue()
    assertThat(isTaskAttribute("task.assignee")).isTrue()

    assertThat(isTaskAttribute("task.")).isFalse()
    assertThat(isTaskAttribute("assignee")).isFalse()
    assertThat(isTaskAttribute("someOther")).isFalse()
    assertThat(isTaskAttribute("described")).isFalse()
  }

  @Test
  fun `should create criteria`() {

    val criteria = toCriteria(filtersList)

    assertThat(criteria).isNotNull
    assertThat(criteria.size).isEqualTo(4)
    assertThat(criteria).containsExactlyElementsOf(listOf(TaskCriterion("name", "myName"), TaskCriterion("assignee", "kermit"),
      DataEntryCriterion("dataAttr1", "value"), DataEntryCriterion("dataAttr2", "another")))
  }

  @Test
  fun `should fail to create criteria`() {
    expected.expect(IllegalArgumentException::class.java)
    toCriteria(listOf("$EQUALS$EQUALS"))
  }

  @Test
  fun `should fail to create criteria 2`() {
    expected.expect(IllegalArgumentException::class.java)
    toCriteria(listOf("${EQUALS}some$EQUALS"))
  }

  @Test
  fun `should ignore wrong format`() {
    assertThat(toCriteria(listOf("noEQUALS"))).isEmpty()
  }
}

