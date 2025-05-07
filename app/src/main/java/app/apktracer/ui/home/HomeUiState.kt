package app.apktracer.ui.home

data class HomeUiState(
    val logs: List<String> = emptyList(),
    val isLoading: Boolean = false
)