package sa.factory.mihwarmodbusindust.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sa.factory.mihwarmodbusindust.domain.usecase.*
import sa.factory.mihwarmodbusindust.model.MihwarModbusItem
import sa.factory.mihwarmodbusindust.model.MihwarModbusOrder
import sa.factory.mihwarmodbusindust.model.MihwarModbusTelemetry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MihwarModbusUiState(
    val catalog: List<MihwarModbusItem> = emptyList(),
    val filteredCatalog: List<MihwarModbusItem> = emptyList(),
    val selectedCategory: String = "الكل",
    val cart: List<MihwarModbusItem> = emptyList(),
    val telemetry: MihwarModbusTelemetry? = null,
    val lastOrder: MihwarModbusOrder? = null,
    val isCheckingOut: Boolean = false,
    val activeTab: String = "home"
)

class MihwarModbusViewModel(
    private val getCatalogUseCase: GetCatalogUseCase,
    private val manageCartUseCase: ManageCartUseCase,
    private val processCheckoutUseCase: ProcessCheckoutUseCase,
    private val observeHardwareTelemetryUseCase: ObserveHardwareTelemetryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MihwarModbusUiState())
    val uiState: StateFlow<MihwarModbusUiState> = _uiState.asStateFlow()

    init {
        // Observe Catalog
        getCatalogUseCase().onEach { list ->
            _uiState.update { it.copy(catalog = list, filteredCatalog = list) }
        }.launchIn(viewModelScope)

        // Observe Cart
        manageCartUseCase.cart.onEach { cartItems ->
            _uiState.update { it.copy(cart = cartItems) }
        }.launchIn(viewModelScope)

        // Observe Telemetry
        observeHardwareTelemetryUseCase().onEach { telem ->
            _uiState.update { it.copy(telemetry = telem) }
        }.launchIn(viewModelScope)
    }

    fun selectCategory(category: String) {
        _uiState.update { state ->
            val filtered = if (category == "الكل") state.catalog else state.catalog.filter { it.category == category }
            state.copy(selectedCategory = category, filteredCatalog = filtered)
        }
    }

    fun switchTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun addToCart(item: MihwarModbusItem) = manageCartUseCase.add(item)
    fun removeFromCart(item: MihwarModbusItem) = manageCartUseCase.remove(item)

    fun checkout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingOut = true) }
            val order = processCheckoutUseCase()
            _uiState.update { it.copy(isCheckingOut = false, lastOrder = order, activeTab = "checkout") }
        }
    }
}
