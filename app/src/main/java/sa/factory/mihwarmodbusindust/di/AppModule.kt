package sa.factory.mihwarmodbusindust.di

import sa.factory.mihwarmodbusindust.data.repository.MihwarModbusRepositoryImpl
import sa.factory.mihwarmodbusindust.domain.repository.MihwarModbusRepository
import sa.factory.mihwarmodbusindust.domain.usecase.*
import sa.factory.mihwarmodbusindust.hardware.HardwareSensorDriver
import sa.factory.mihwarmodbusindust.ui.viewmodel.MihwarModbusViewModel

class DiContainer {
    val sensorDriver = HardwareSensorDriver()
    val repository: MihwarModbusRepository = MihwarModbusRepositoryImpl(sensorDriver)

    val getCatalogUseCase = GetCatalogUseCase(repository)
    val manageCartUseCase = ManageCartUseCase(repository)
    val processCheckoutUseCase = ProcessCheckoutUseCase(repository)
    val observeHardwareTelemetryUseCase = ObserveHardwareTelemetryUseCase(repository)

    fun provideViewModel(): MihwarModbusViewModel = MihwarModbusViewModel(
        getCatalogUseCase,
        manageCartUseCase,
        processCheckoutUseCase,
        observeHardwareTelemetryUseCase
    )
}
