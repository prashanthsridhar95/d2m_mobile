package com.d2m.app.ui.screens.shared.wedding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.d2m.app.data.model.*
import com.d2m.app.data.network.friendlyError
import com.d2m.app.domain.repository.WeddingRepository
import com.d2m.app.ui.strings.WeddingStrings
import com.d2m.app.ui.strings.WeddingStringsEn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Screen state + actions for the wedding checklist. Changes are optimistic where it matters (ticking a task). */
class WeddingController(
    val repo: WeddingRepository,
    val primaryId: String,
    private val scope: CoroutineScope,
) {
    var data by mutableStateOf<WeddingOut?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
        private set
    val pm = PanchangamStore({ start, end -> repo.panchangam(primaryId, start, end) }, scope)
    /** Current language; the screen refreshes it on every composition so error text follows a language switch. */
    var s: WeddingStrings = WeddingStringsEn

    fun load() {
        scope.launch {
            loading = true
            runCatching { repo.get(primaryId) }
                .onSuccess { data = it; error = null; ensurePanchangam(it) }
                .onFailure { error = friendlyError(it, s.loadError) }
            loading = false
        }
    }

    /** Loads panchangam flags for every month a task or key date lands in (a handful of small cached requests). */
    private fun ensurePanchangam(d: WeddingOut) {
        pm.ensure(listOf(today().toString(), d.plan.engagementDate, d.plan.marriageDate, d.plan.honeymoonDate) + d.tasks.map { it.dueDate })
    }

    private fun setTasks(f: (List<WeddingTaskOut>) -> List<WeddingTaskOut>) {
        val d = data ?: return
        val tasks = f(d.tasks)
        data = d.copy(tasks = tasks, progress = recount(d.plan, tasks))
    }

    private fun recount(plan: WeddingPlanOut, tasks: List<WeddingTaskOut>): WeddingProgressOut {
        val t = today()
        return WeddingProgressOut(
            total = tasks.size,
            done = tasks.count { it.status == "done" },
            myOpen = tasks.count { plan.myRole in it.participants && it.status != "done" },
            overdue = tasks.count { it.status != "done" && it.dueDate != null && it.dueDate.toDate() < t },
        )
    }

    private fun replace(task: WeddingTaskOut) = setTasks { ts -> ts.map { if (it.id == task.id) task else it } }

    private fun act(block: suspend () -> Unit) {
        scope.launch {
            busy = true
            try { block(); error = null } catch (e: Exception) { error = friendlyError(e, s.actionError); reload() } finally { busy = false }
        }
    }

    private suspend fun reload() { runCatching { repo.get(primaryId) }.onSuccess { data = it } }

    fun toggle(task: WeddingTaskOut) {
        val me = data?.plan?.myRole ?: return
        val on = task.status != "done"
        replace(task.copy(
            status = if (on) "done" else "todo", iConfirmed = on, overdue = false,
            confirmedBy = if (on) (task.confirmedBy + me).distinct() else task.confirmedBy - me,
        ))
        act { replace(repo.setConfirmed(primaryId, task.id, on)) }
    }

    fun setDue(task: WeddingTaskOut, due: String?) {
        if (due != null) pm.ensure(listOf(due))
        act { replace(repo.setDue(primaryId, task.id, due)) }
    }

    fun setTitle(task: WeddingTaskOut, title: String) {
        val t = title.trim()
        if (t.isEmpty() || t == task.title) return
        act { replace(repo.setTitle(primaryId, task.id, t)) }
    }

    fun setVisibility(task: WeddingTaskOut, visibility: String) {
        if (visibility == task.visibility) return
        act { replace(repo.setVisibility(primaryId, task.id, visibility)) }
    }

    fun delete(task: WeddingTaskOut, done: () -> Unit) {
        act { repo.deleteTask(primaryId, task.id); setTasks { ts -> ts.filter { it.id != task.id } }; done() }
    }

    fun create(body: WeddingTaskCreateIn, created: (String) -> Unit) {
        act { val t = repo.createTask(primaryId, body); reload(); data?.let { ensurePanchangam(it) }; created(t.id) }
    }

    fun saveDates(engagement: String?, marriage: String?, honeymoon: String?, tradition: String?, ok: () -> Unit) {
        act {
            var out = repo.setDates(primaryId, WeddingDatesIn(engagement, marriage, honeymoon))
            if ((out.plan.tradition ?: "") != (tradition ?: "")) out = repo.setTradition(primaryId, tradition?.ifEmpty { null })
            data = out
            ensurePanchangam(out)
            ok()
        }
    }

    /** Called by the task sheet whenever its comments/attachments change, so list badges stay in step. */
    fun updateCounts(taskId: String, comments: Int, attachments: Int) = setTasks { ts ->
        ts.map { if (it.id == taskId && (it.commentCount != comments || it.attachmentCount != attachments)) it.copy(commentCount = comments, attachmentCount = attachments) else it }
    }
}
