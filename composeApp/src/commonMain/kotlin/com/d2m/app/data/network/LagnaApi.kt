package com.d2m.app.data.network

import com.d2m.app.data.model.LagnaDeduceOut
import com.d2m.app.data.model.LagnaDeduceRequest
import com.d2m.app.data.model.LagnaQuestionsOut

/** Mirrors app/routers/lagna.py -- stateless onboarding helpers for an unknown time of birth. */
class LagnaApi(private val api: ApiClient) {
    suspend fun questions(): LagnaQuestionsOut = api.get("/onboarding/lagna/questions")

    suspend fun deduce(body: LagnaDeduceRequest): LagnaDeduceOut = api.post("/onboarding/lagna/deduce", body)
}
