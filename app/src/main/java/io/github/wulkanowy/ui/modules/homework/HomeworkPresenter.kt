package io.github.wulkanowy.ui.modules.homework

import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import io.github.wulkanowy.data.ErrorHandler
import io.github.wulkanowy.data.repositories.HomeworkRepository
import io.github.wulkanowy.data.repositories.SessionRepository
import io.github.wulkanowy.ui.base.BasePresenter
import io.github.wulkanowy.utils.SchedulersProvider
import io.github.wulkanowy.utils.isHolidays
import io.github.wulkanowy.utils.logEvent
import io.github.wulkanowy.utils.nextOrSameSchoolDay
import io.github.wulkanowy.utils.nextSchoolDay
import io.github.wulkanowy.utils.previousSchoolDay
import io.github.wulkanowy.utils.toFormattedString
import org.threeten.bp.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class HomeworkPresenter @Inject constructor(
    private val errorHandler: ErrorHandler,
    private val schedulers: SchedulersProvider,
    private val homeworkRepository: HomeworkRepository,
    private val sessionRepository: SessionRepository
) : BasePresenter<HomeworkView>(errorHandler) {

    lateinit var currentDate: LocalDate
        private set

    fun onAttachView(view: HomeworkView, date: Long?) {
        super.onAttachView(view)
        view.initView()
        loadData(LocalDate.ofEpochDay(date ?: LocalDate.now().nextOrSameSchoolDay.toEpochDay()))
        reloadView()
    }

    fun onPreviousDay() {
        loadData(currentDate.previousSchoolDay)
        reloadView()
        logEvent("Homework day changed", mapOf("button" to "prev", "date" to currentDate.toFormattedString()))
    }

    fun onNextDay() {
        loadData(currentDate.nextSchoolDay)
        reloadView()
        logEvent("Homework day changed", mapOf("button" to "next", "date" to currentDate.toFormattedString()))
    }

    fun onSwipeRefresh() {
        loadData(currentDate, true)
    }

    fun onHomeworkItemSelected(item: AbstractFlexibleItem<*>?) {
        if (item is HomeworkItem) view?.showTimetableDialog(item.homework)
    }

    private fun loadData(date: LocalDate, forceRefresh: Boolean = false) {
        currentDate = date
        disposable.apply {
            clear()
            add(sessionRepository.getSemesters()
                .delay(200, TimeUnit.MILLISECONDS)
                .map { it.single { semester -> semester.current } }
                .flatMap { homeworkRepository.getHomework(it, currentDate, forceRefresh) }
                .map { items -> items.map { HomeworkItem(it) } }
                .subscribeOn(schedulers.backgroundThread)
                .observeOn(schedulers.mainThread)
                .doFinally {
                    view?.run {
                        hideRefresh()
                        showProgress(false)
                    }
                }
                .subscribe({
                    view?.apply {
                        updateData(it)
                        showEmpty(it.isEmpty())
                        showContent(it.isNotEmpty())
                    }
                    logEvent("Homework load", mapOf("items" to it.size, "forceRefresh" to forceRefresh, "date" to currentDate.toFormattedString()))
                }) {
                    view?.run { showEmpty(isViewEmpty()) }
                    errorHandler.proceed(it)
                })
        }
    }

    private fun reloadView() {
        view?.apply {
            showProgress(true)
            showContent(false)
            showEmpty(false)
            clearData()
            showNextButton(!currentDate.plusDays(1).isHolidays)
            showPreButton(!currentDate.minusDays(1).isHolidays)
            updateNavigationDay(currentDate.toFormattedString("EEEE \n dd.MM.YYYY").capitalize())
        }
    }
}