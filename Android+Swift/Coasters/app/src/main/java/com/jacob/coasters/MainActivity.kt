package com.jacob.coasters

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jacob.coasters.data.RollerCoasterRepository
import com.jacob.coasters.data.SwiftRollerCoasterRepository
import com.jacob.coasters.feature.detail.RollerCoasterDetailScreen
import com.jacob.coasters.feature.detail.RollerCoasterDetailViewModel
import com.jacob.coasters.feature.list.RollerCoasterListScreen
import com.jacob.coasters.feature.list.RollerCoasterListViewModel
import com.jacob.coasters.model.RollerCoasterListItem
import com.jacob.coasters.ui.CoastersTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CoastersTheme {
                CoastersNavHost()
            }
        }
    }
}

@Composable
private fun CoastersNavHost() {
    val repository = remember { SwiftRollerCoasterRepository() }
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            val listViewModel: RollerCoasterListViewModel = viewModel(
                factory = RollerCoasterListViewModelFactory(repository)
            )
            val state by listViewModel.state.collectAsState()
            RollerCoasterListScreen(
                state = state,
                onQueryChange = listViewModel::onQueryChange,
                onItemSelected = { item: RollerCoasterListItem ->
                    navController.navigate("detail/${item.coaster.slug}")
                }
            )
        }
        composable(
            route = "detail/{slug}",
            arguments = listOf(
                navArgument("slug") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val slug = backStackEntry.arguments?.getString("slug").orEmpty()
            val detailViewModel: RollerCoasterDetailViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = RollerCoasterDetailViewModelFactory(repository, slug)
        )
            val state by detailViewModel.state.collectAsState()
            RollerCoasterDetailScreen(
                state = state,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private class RollerCoasterListViewModelFactory(
    private val repository: RollerCoasterRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RollerCoasterListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RollerCoasterListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class $modelClass")
    }
}

private class RollerCoasterDetailViewModelFactory(
    private val repository: RollerCoasterRepository,
    private val slug: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RollerCoasterDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RollerCoasterDetailViewModel(repository, slug) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class $modelClass")
    }
}
