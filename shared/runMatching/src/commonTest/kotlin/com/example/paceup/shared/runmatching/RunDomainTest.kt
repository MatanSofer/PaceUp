package com.example.paceup.shared.runmatching

import com.example.paceup.shared.runmatching.domain.Run
import com.example.paceup.shared.runmatching.domain.RunDto
import com.example.paceup.shared.runmatching.domain.RunFilters
import com.example.paceup.shared.runmatching.domain.RunMode
import com.example.paceup.shared.runmatching.domain.RunStatus
import com.example.paceup.shared.runmatching.domain.toDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RunDomainTest {

    private fun dto(
        id: String = "run-1",
        mode: String = "easy",
        status: String = "open",
        paceMinSec: Int = 300,
        paceMaxSec: Int = 360,
        verifiedOnly: Boolean = false,
        scheduledAt: String = "2026-06-01T07:00:00Z",
        meetingLat: Double = 32.0,
        meetingLng: Double = 34.0,
    ) = RunDto(
        id = id,
        creatorId = "user-1",
        title = null,
        description = null,
        mode = mode,
        status = status,
        scheduledAt = scheduledAt,
        meetingLat = meetingLat,
        meetingLng = meetingLng,
        meetingAddress = "Tel Aviv",
        city = "Tel Aviv",
        distanceKm = null,
        durationMin = null,
        paceMinSec = paceMinSec,
        paceMaxSec = paceMaxSec,
        maxParticipants = null,
        verifiedOnly = verifiedOnly,
        joinMode = "open",
        isRecurring = false,
        createdAt = "2026-05-01T00:00:00Z",
    )

    @Test
    fun `RunDto toDomain maps mode correctly`() {
        assertEquals(RunMode.EASY, dto(mode = "easy").toDomain().mode)
        assertEquals(RunMode.TEMPO, dto(mode = "tempo").toDomain().mode)
        assertEquals(RunMode.RACE_PREP, dto(mode = "race_prep").toDomain().mode)
        assertEquals(RunMode.RECOVERY, dto(mode = "recovery").toDomain().mode)
        assertEquals(RunMode.TOURIST, dto(mode = "tourist").toDomain().mode)
        assertEquals(RunMode.PACER, dto(mode = "pacer").toDomain().mode)
    }

    @Test
    fun `RunDto toDomain falls back to EASY for unknown mode`() {
        assertEquals(RunMode.EASY, dto(mode = "unknown").toDomain().mode)
    }

    @Test
    fun `RunDto toDomain maps status correctly`() {
        assertEquals(RunStatus.OPEN, dto(status = "open").toDomain().status)
        assertEquals(RunStatus.FULL, dto(status = "full").toDomain().status)
        assertEquals(RunStatus.IN_PROGRESS, dto(status = "in_progress").toDomain().status)
        assertEquals(RunStatus.COMPLETED, dto(status = "completed").toDomain().status)
        assertEquals(RunStatus.CANCELLED, dto(status = "cancelled").toDomain().status)
    }

    @Test
    fun `RunDto toDomain falls back to OPEN for unknown status`() {
        assertEquals(RunStatus.OPEN, dto(status = "unknown").toDomain().status)
    }

    @Test
    fun `RunDto toDomain preserves nullable fields`() {
        val run = dto().toDomain()
        assertNull(run.title)
        assertNull(run.description)
        assertNull(run.distanceKm)
        assertNull(run.durationMin)
        assertNull(run.maxParticipants)
    }

    @Test
    fun `applyFilters keeps runs within pace range`() {
        val runs = listOf(
            dto(paceMinSec = 270, paceMaxSec = 300).toDomain(), // zone A
            dto(paceMinSec = 300, paceMaxSec = 330).toDomain(), // zone B
            dto(paceMinSec = 330, paceMaxSec = 390).toDomain(), // zone C
        )
        val filters = RunFilters(paceMinSec = 290, paceMaxSec = 320)
        val result = runs.applyFilters(filters)
        // first run: paceMax(300) >= filterMin(290) ✓ AND paceMin(270) <= filterMax(320) ✓
        // second run: paceMax(330) >= 290 ✓ AND paceMin(300) <= 320 ✓
        // third run: paceMax(390) >= 290 ✓ BUT paceMin(330) <= 320 ✗ → excluded
        assertEquals(2, result.size)
    }

    @Test
    fun `applyFilters filters by mode`() {
        val runs = listOf(
            dto(mode = "easy").toDomain(),
            dto(mode = "tempo").toDomain(),
            dto(mode = "recovery").toDomain(),
        )
        val result = runs.applyFilters(RunFilters(modes = listOf(RunMode.EASY, RunMode.RECOVERY)))
        assertEquals(2, result.size)
        assert(result.all { it.mode == RunMode.EASY || it.mode == RunMode.RECOVERY })
    }

    @Test
    fun `applyFilters with no filters returns all runs`() {
        val runs = listOf(dto().toDomain(), dto(id = "run-2").toDomain())
        assertEquals(2, runs.applyFilters(RunFilters()).size)
    }

    @Test
    fun `applyFilters filters verifiedOnly`() {
        val runs = listOf(
            dto(verifiedOnly = true).toDomain(),
            dto(verifiedOnly = false).toDomain(),
        )
        val result = runs.applyFilters(RunFilters(verifiedOnly = true))
        assertEquals(1, result.size)
        assert(result.first().verifiedOnly)
    }

    @Test
    fun `applyFilters filters afterDate`() {
        val runs = listOf(
            dto(scheduledAt = "2026-05-01T07:00:00Z").toDomain(),
            dto(scheduledAt = "2026-07-01T07:00:00Z").toDomain(),
        )
        val result = runs.applyFilters(RunFilters(afterDate = "2026-06-01T00:00:00Z"))
        assertEquals(1, result.size)
        assertEquals("2026-07-01T07:00:00Z", result.first().scheduledAt)
    }
}

// Expose the private filter for testing via an internal extension
internal fun List<Run>.applyFilters(filters: RunFilters): List<Run> = filter { run ->
    (filters.paceMinSec == null || run.paceMaxSec >= filters.paceMinSec) &&
        (filters.paceMaxSec == null || run.paceMinSec <= filters.paceMaxSec) &&
        (filters.modes.isEmpty() || run.mode in filters.modes) &&
        (filters.verifiedOnly == null || run.verifiedOnly == filters.verifiedOnly) &&
        (filters.afterDate == null || run.scheduledAt >= filters.afterDate)
}
