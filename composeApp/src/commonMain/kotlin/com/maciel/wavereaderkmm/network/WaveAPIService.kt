package com.maciel.wavereaderkmm.network

import com.maciel.wavereaderkmm.model.WaveApiQuery
import com.maciel.wavereaderkmm.model.WaveDataResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json


class WaveApiService {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    suspend fun getWaveData(query: WaveApiQuery): WaveDataResponse {
        val variableList = query.variables.joinToString(",") { it.paramName }
        return client.get("https://marine-api.open-meteo.com/v1/marine") {
            parameter("latitude", query.latitude)
            parameter("longitude", query.longitude)
            parameter("hourly", variableList)
            parameter("current", variableList)
            parameter("forecast_days", query.forecastDays)
            parameter("length_unit", query.lengthUnit)
            parameter("timezone", "auto")
        }.body()
    }
}