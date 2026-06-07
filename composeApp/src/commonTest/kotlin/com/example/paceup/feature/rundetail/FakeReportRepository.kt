package com.example.paceup.feature.rundetail

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.result.EmptyResult
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.ReportParams
import com.example.paceup.shared.runmatching.domain.ReportRepository

/** Test double for [ReportRepository]. Always succeeds by default. */
class FakeReportRepository : ReportRepository {
    var result: EmptyResult<AppError> = Result.Success(Unit)
    val submissions = mutableListOf<ReportParams>()

    override suspend fun submitReport(params: ReportParams): EmptyResult<AppError> {
        submissions += params
        return result
    }
}
