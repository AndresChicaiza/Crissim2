package com.example.crissim.domain.usecase

import com.example.crissim.data.repository.LiveDrillRepository
import com.example.crissim.domain.model.DrillIncident
import com.example.crissim.domain.model.LiveDrill
import com.example.crissim.domain.model.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LiveDrillUseCasesTest {

    private lateinit var fakeDrillRepository: FakeLiveDrillRepositoryForTest
    private lateinit var getGuide: GetDrillGuideUseCase
    private lateinit var startDrill: StartLiveDrillUseCase
    private lateinit var finishDrill: FinishLiveDrillUseCase

    @Before
    fun setUp() {
        fakeDrillRepository = FakeLiveDrillRepositoryForTest()
        getGuide = GetDrillGuideUseCase()
        startDrill = StartLiveDrillUseCase()
        finishDrill = FinishLiveDrillUseCase(fakeDrillRepository)
    }

    @Test
    fun getDrillGuide_returnsTeacherSteps() {
        val teacherSteps = getGuide.execute(UserRole.TEACHER)
        assertEquals(4, teacherSteps.size)
        assertTrue(teacherSteps[0].roleAction.contains("DOCENTE"))
        assertTrue(teacherSteps[1].title.contains("Conteo"))
    }

    @Test
    fun getDrillGuide_returnsBrigadistSteps() {
        val brigadistSteps = getGuide.execute(UserRole.BRIGADIST)
        assertEquals(4, brigadistSteps.size)
        assertTrue(brigadistSteps[0].roleAction.contains("BRIGADISTA"))
        assertTrue(brigadistSteps[3].title.contains("Puesto de Mando"))
    }

    @Test
    fun startLiveDrill_createsDrillWith180sTargetTime() {
        val drill = startDrill.execute(userId = 10L, userName = "Carlos Docente", role = UserRole.TEACHER)
        assertNotNull(drill.id)
        assertEquals("Carlos Docente", drill.userName)
        assertEquals(UserRole.TEACHER, drill.role)
        assertEquals(180, drill.targetTimeSeconds) // Meta ILC: 3 min (180 s)
    }

    @Test
    fun finishLiveDrill_calculatesDurationAndSavesReport() = runBlocking {
        val drill = startDrill.execute(userId = 10L, userName = "Carlos Docente", role = UserRole.TEACHER)
        val incidents = listOf(
            DrillIncident(timestamp = System.currentTimeMillis(), stepTitle = "Paso 2", note = "Bloque B congestión")
        )

        val report = finishDrill.execute(drill, incidents)

        assertTrue(report.isCompleted)
        assertEquals(1, report.incidents.size)
        assertEquals("Bloque B congestión", report.incidents[0].note)
        assertNotNull(fakeDrillRepository.savedDrill)
        assertEquals(report.id, fakeDrillRepository.savedDrill?.id)
    }
}

class FakeLiveDrillRepositoryForTest : LiveDrillRepository {
    var savedDrill: LiveDrill? = null
    private val map = mutableMapOf<String, LiveDrill>()

    override suspend fun saveDrill(drill: LiveDrill) {
        savedDrill = drill
        map[drill.id] = drill
    }

    override suspend fun getDrillById(id: String): LiveDrill? = map[id]

    override suspend fun getDrillsForUser(userId: Long): List<LiveDrill> =
        map.values.filter { it.userId == userId }
}
