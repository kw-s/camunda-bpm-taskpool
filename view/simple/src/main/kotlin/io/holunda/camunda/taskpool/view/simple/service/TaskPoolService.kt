package io.holunda.camunda.taskpool.view.simple.service

import io.holunda.camunda.taskpool.api.business.DataEntryCreatedEvent
import io.holunda.camunda.taskpool.api.business.DataEntryUpdatedEvent
import io.holunda.camunda.taskpool.api.business.dataIdentity
import io.holunda.camunda.taskpool.api.task.*
import io.holunda.camunda.taskpool.view.DataEntry
import io.holunda.camunda.taskpool.view.Task
import io.holunda.camunda.taskpool.view.TaskWithDataEntries
import io.holunda.camunda.taskpool.view.query.*
import io.holunda.camunda.taskpool.view.simple.filter.createPredicates
import io.holunda.camunda.taskpool.view.simple.filter.filterByPredicates
import io.holunda.camunda.taskpool.view.simple.filter.toCriteria
import io.holunda.camunda.taskpool.view.simple.sort.comparator
import io.holunda.camunda.taskpool.view.task
import mu.KLogging
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.axonframework.queryhandling.QueryHandler
import org.axonframework.queryhandling.QueryUpdateEmitter
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
@ProcessingGroup(SimpleServiceViewProcessingGroup.PROCESSING_GROUP)
open class TaskPoolService(
  private val queryUpdateEmitter: QueryUpdateEmitter
) : TaskApi {

  companion object : KLogging()

  private val tasks = ConcurrentHashMap<String, Task>()
  private val dataEntries = ConcurrentHashMap<String, DataEntry>()

  /**
   * Retrieves a list of all user tasks for current user.
   */
  @QueryHandler
  override fun query(query: TasksForUserQuery): List<Task> = tasks.values.filter { query.applyFilter(it) }

  /**
   * Retrieves a list of all data entries of given entry type (and optional id).
   */
  @QueryHandler
  override fun query(query: DataEntryQuery): List<DataEntry> = dataEntries.values.filter { query.applyFilter(it) }


  /**
   * Retrieves a task for given task id.
   */
  @QueryHandler
  override fun query(query: TaskForIdQuery): Task? = tasks.values.firstOrNull { query.applyFilter(it) }

  /**
   * Retrieves a task with data entries for given task id.
   */
  @QueryHandler
  override fun query(query: TaskWithDataEntriesForIdQuery): TaskWithDataEntries? {
    val task = tasks.values.firstOrNull { query.applyFilter(TaskWithDataEntries(it)) }
    return if (task != null) {
      TaskWithDataEntries(task, this.dataEntries.values.toList())
    } else {
      null
    }
  }

  /**
   * Retrieves a list of tasks with correlated data entries of given entry type (and optional id).
   */
  @QueryHandler
  override fun query(query: TasksWithDataEntriesForUserQuery): TasksWithDataEntriesResponse {

    val predicates = createPredicates(toCriteria(query.filters))

    val filtered = query(TasksForUserQuery(query.user))
      .asSequence()
      .map { task -> TaskWithDataEntries.correlate(task, this.dataEntries.values.toList()) }
      .filter { filterByPredicates(it, predicates) }
      .toList()

    val comparator = comparator(query.sort)

    val sorted = if (comparator != null) {
      filtered.sortedWith(comparator)
    } else {
      filtered
    }

    return slice(list = sorted, query = query)
  }

  /**
   * Retrieves the count of tasks grouped by source application. Supports subscription queries.
   */
  @QueryHandler
  override fun query(query: TaskCountByApplicationQuery): List<ApplicationWithTaskCount> =
    tasks.values.groupingBy { it.sourceReference.applicationName }.eachCount().map { ApplicationWithTaskCount(it.key, it.value) }

  fun slice(list: List<TaskWithDataEntries>, query: TasksWithDataEntriesForUserQuery): TasksWithDataEntriesResponse {
    val totalCount = list.size
    val offset = query.page * query.size
    return if (totalCount > offset) {
      TasksWithDataEntriesResponse(totalCount, list.slice(offset until Math.min(offset + query.size, totalCount)))
    } else {
      TasksWithDataEntriesResponse(totalCount, list)
    }
  }

  @EventHandler
  open fun on(event: TaskCreatedEngineEvent) {
    logger.debug { "Task created $event received" }
    val task = task(event)
    tasks[task.id] = task
    updateTaskForUserQuery(event.id)
    updateTaskCountByApplicationQuery(task.sourceReference.applicationName)
  }

  @EventHandler
  open fun on(event: TaskAssignedEngineEvent) {
    logger.debug { "Task assigned $event received" }
    if (tasks.containsKey(event.id)) {
      tasks[event.id] = task(event, tasks[event.id]!!)
      updateTaskForUserQuery(event.id)
      updateTaskCountByApplicationQuery(tasks[event.id]!!.sourceReference.applicationName)
    }
  }

  @Suppress("unused")
  @EventHandler
  open fun on(event: TaskCompletedEngineEvent) {
    logger.debug { "Task completed $event received" }
    val applicationName = tasks[event.id]?.sourceReference?.applicationName
    tasks.remove(event.id)
    updateTaskForUserQuery(event.id)
    applicationName?.let { updateTaskCountByApplicationQuery(it) }
  }

  @Suppress("unused")
  @EventHandler
  open fun on(event: TaskDeletedEngineEvent) {
    logger.debug { "Task deleted $event received" }
    val applicationName = tasks[event.id]?.sourceReference?.applicationName
    tasks.remove(event.id)
    updateTaskForUserQuery(event.id)
    applicationName?.let { updateTaskCountByApplicationQuery(it) }
  }

  @EventHandler
  open fun on(event: TaskAttributeUpdatedEngineEvent) {
    logger.debug { "Task attributes updated $event received" }
    if (tasks.containsKey(event.id)) {
      tasks[event.id] = task(event, tasks[event.id]!!)
      updateTaskForUserQuery(event.id)
      updateTaskCountByApplicationQuery(tasks[event.id]!!.sourceReference.applicationName)
    }
  }

  @EventHandler
  open fun on(event: TaskCandidateGroupChanged) {
    logger.debug { "Task candidate groups changed $event received" }
    if (tasks.containsKey(event.id)) {
      tasks[event.id] = task(event, tasks[event.id]!!)
      updateTaskForUserQuery(event.id)
      updateTaskCountByApplicationQuery(tasks[event.id]!!.sourceReference.applicationName)
    }
  }

  @EventHandler
  open fun on(event: TaskCandidateUserChanged) {
    logger.debug { "Task user groups changed $event received" }
    if (tasks.containsKey(event.id)) {
      tasks[event.id] = task(event, tasks[event.id]!!)
      updateTaskForUserQuery(event.id)
      updateTaskCountByApplicationQuery(tasks[event.id]!!.sourceReference.applicationName)
    }
  }

  @Suppress("unused")
  @EventHandler
  open fun on(event: DataEntryCreatedEvent) {
    logger.debug { "Business data entry created $event" }
    dataEntries[dataIdentity(entryType = event.entryType, entryId = event.entryId)] = DataEntry(
      entryType = event.entryType,
      entryId = event.entryId,
      payload = event.payload
    )
    updateDataEntryQuery(dataIdentity(entryType = event.entryType, entryId = event.entryId))
  }

  @Suppress("unused")
  @EventHandler
  open fun on(event: DataEntryUpdatedEvent) {
    logger.debug { "Business data entry updated $event" }
    dataEntries[dataIdentity(entryType = event.entryType, entryId = event.entryId)] = DataEntry(
      entryType = event.entryType,
      entryId = event.entryId,
      payload = event.payload
    )
    updateDataEntryQuery(dataIdentity(entryType = event.entryType, entryId = event.entryId))
  }


  private fun updateTaskForUserQuery(taskId: String) {
    updateMapFilterQuery(tasks, taskId, TasksForUserQuery::class.java)

    val mapTasksWithDataEntries = TaskWithDataEntries.correlate(tasks.values.toList(), dataEntries.values.toList())
      .map { it.task.id to it }
      .toMap()

    updateMapFilterQuery(mapTasksWithDataEntries, taskId, TasksWithDataEntriesForUserQuery::class.java)
  }

  private fun updateDataEntryQuery(identity: String) = updateMapFilterQuery(dataEntries, identity, DataEntryQuery::class.java)

  private fun <T : Any, Q : FilterQuery<T>> updateMapFilterQuery(map: Map<String, T>, key: String, clazz: Class<Q>) {
    if (map.contains(key)) {
      val entry = map.getValue(key)
      queryUpdateEmitter.emit(clazz, { query -> query.applyFilter(entry) }, entry)
    }
  }

  private fun updateTaskCountByApplicationQuery(applicationName: String) {
    queryUpdateEmitter.emit(TaskCountByApplicationQuery::class.java, { true }, ApplicationWithTaskCount(applicationName, tasks.values.count { it.sourceReference.applicationName == applicationName }))
  }
}

