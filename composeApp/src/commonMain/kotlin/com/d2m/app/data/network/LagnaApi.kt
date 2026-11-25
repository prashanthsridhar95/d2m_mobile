package com.d2m.app.data.network

import com.d2m.app.data.model.LagnaDeduceOut
import com.d2m.app.data.model.LagnaDeduceRequest
import com.d2m.app.data.model.LagnaQuestionsOut

/** The two lagna calls the onboarding questionnaire needs (an interface so UI tests can fake the backend). */
interface LagnaService {
    suspend fun questions(): LagnaQuestionsOut
    suspend fun deduce(body: LagnaDeduceRequest): LagnaDeduceOut
}

/** Mirrors app/routers/lagna.py -- stateless onboarding helpers for an unknown time of birth. */
class LagnaApi(private val api: ApiClient) : LagnaService {
    override suspend fun questions(): LagnaQuestionsOut = api.get("/onboarding/lagna/questions")

    override suspend fun deduce(body: LagnaDeduceRequest): LagnaDeduceOut = api.post("/onboarding/lagna/deduce", body)
}
