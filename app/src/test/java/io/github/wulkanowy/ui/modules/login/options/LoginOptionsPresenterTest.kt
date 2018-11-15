package io.github.wulkanowy.ui.modules.login.options

import io.github.wulkanowy.TestSchedulersProvider
import io.github.wulkanowy.data.ErrorHandler
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.repositories.SessionRepository
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class LoginOptionsPresenterTest {

    @Mock
    lateinit var errorHandler: ErrorHandler

    @Mock
    lateinit var loginOptionsView: LoginOptionsView

    @Mock
    lateinit var repository: SessionRepository

    private lateinit var presenter: LoginOptionsPresenter

    private val testStudent by lazy { Student(email = "test", password = "test123", endpoint = "https://fakelog.cf", loginType = "AUTO") }

    private val testException by lazy { RuntimeException("Problem") }

    @Before
    fun initPresenter() {
        MockitoAnnotations.initMocks(this)
        clearInvocations(repository, loginOptionsView)
        presenter = LoginOptionsPresenter(errorHandler, repository, TestSchedulersProvider())
        presenter.onAttachView(loginOptionsView)
    }

    @Test
    fun initViewTest() {
        verify(loginOptionsView).initView()
    }

    @Test
    fun refreshDataTest() {
        doReturn(Single.just(listOf(testStudent))).`when`(repository).cachedStudents
        presenter.onParentViewLoadData()
        verify(loginOptionsView).showActionBar(true)
        verify(loginOptionsView).updateData(listOf(LoginOptionsItem(testStudent)))
    }

    @Test
    fun refreshDataErrorTest() {
        doReturn(Single.error<List<Student>>(testException)).`when`(repository).cachedStudents
        presenter.onParentViewLoadData()
        verify(loginOptionsView).showActionBar(true)
        verify(errorHandler).proceed(testException)
    }

    @Test
    fun onSelectedStudentTest() {
        doReturn(Completable.complete()).`when`(repository).saveStudent(testStudent)
        presenter.onSelectItem(LoginOptionsItem(testStudent))
        verify(loginOptionsView).showContent(false)
        verify(loginOptionsView).showProgress(true)
        verify(loginOptionsView).openMainView()
    }

    @Test
    fun onSelectedStudentErrorTest() {
        doReturn(Completable.error(testException)).`when`(repository).saveStudent(testStudent)
        presenter.onSelectItem(LoginOptionsItem(testStudent))
        verify(loginOptionsView).showContent(false)
        verify(loginOptionsView).showProgress(true)
        verify(errorHandler).proceed(testException)
    }
}