package com.maciel.wavereaderkmm.data

import com.maciel.wavereaderkmm.model.WaveApiQuery
import com.maciel.wavereaderkmm.model.WaveDataResponse
import com.maciel.wavereaderkmm.network.WaveApiService


interface WaveApiRepository {
    suspend fun getWaveApiData(query: WaveApiQuery): WaveDataResponse
}


class NetworkWaveApiRepository(
    private val waveApiService: WaveApiService = WaveApiService()
) : WaveApiRepository {
    override suspend fun getWaveApiData(query: WaveApiQuery): WaveDataResponse {
        return waveApiService.getWaveData(query)
    }
}