package com.example.proyectofinal

import com.example.proyectofinal.data.EcoConnectRepository
import com.example.proyectofinal.data.ReporteEntity
import com.example.proyectofinal.viewmodel.EcoConnectViewModelAvanzado
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EcoConnectViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: EcoConnectRepository
    private lateinit var viewModel: EcoConnectViewModelAvanzado

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        
        // Mocking repository flows
        every { repository.todosLosReportes } returns flowOf(emptyList())
        every { repository.notificaciones } returns flowOf(emptyList())
        
        viewModel = EcoConnectViewModelAvanzado(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial EcoPuntos value`() {
        assertEquals(320, viewModel.puntosAcumulados)
    }

    @Test
    fun `test adding points when voting`() {
        viewModel.aplicarVotoComunitario("report_id")
        assertEquals(325, viewModel.puntosAcumulados)
    }
}
