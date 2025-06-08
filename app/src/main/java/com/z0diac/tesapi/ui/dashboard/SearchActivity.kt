package com.z0diac.tesapi.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.z0diac.tesapi.R
import com.z0diac.tesapi.data.api.RetrofitInstance
import com.z0diac.tesapi.databinding.ActivitySearchBinding
import com.z0diac.tesapi.ui.adapters.SearchAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var searchAdapter: SearchAdapter
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearchInput()
    }

    private fun setupSearchInput() {
        binding.etSearch.apply {
            // Request focus and show the keyboard automatically when the activity opens
            requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)

            // Add a TextWatcher to listen for changes in the EditText
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // Not needed
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Every time the text changes, cancel the previous search job
                    searchJob?.cancel()

                    // Start a new coroutine job to perform the search
                    searchJob = lifecycleScope.launch {
                        // Wait for 500 milliseconds after the user stops typing
                        delay(500L)

                        val query = s.toString().trim()
                        if (query.isNotEmpty()) {
                            performSearch(query)
                        } else {
                            // If the search bar is empty, clear the results
                            searchAdapter.updateData(emptyList())
                            binding.tvNoResults.visibility = View.GONE
                        }
                    }
                }

                override fun afterTextChanged(s: Editable?) {
                    // Not needed
                }
            })
        }
    }

    private fun setupRecyclerView() {
        searchAdapter = SearchAdapter(emptyList())
        binding.rvSearchResults.apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(this@SearchActivity)
        }
    }

    private fun performSearch(query: String) {
        binding.loadingSpinner.visibility = View.VISIBLE
        binding.tvNoResults.visibility = View.GONE
        binding.rvSearchResults.visibility = View.GONE

        val apiKey = getString(R.string.tmdb_api_key)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.api.searchMovies(apiKey, query).execute()
                withContext(Dispatchers.Main) {
                    binding.loadingSpinner.visibility = View.GONE
                    if (response.isSuccessful) {
                        val movies = response.body()?.results ?: emptyList()
                        if (movies.isNotEmpty()) {
                            binding.rvSearchResults.visibility = View.VISIBLE
                            searchAdapter.updateData(movies)
                        } else {
                            // If API returns no movies, show "No results found"
                            binding.tvNoResults.visibility = View.VISIBLE
                            searchAdapter.updateData(emptyList()) // Clear old results
                        }
                    } else {
                        Toast.makeText(this@SearchActivity, "Search failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                        binding.tvNoResults.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                // This catch block might be entered if the coroutine job is cancelled.
                // We check if the scope is still active to avoid showing an error on cancellation.
                if (lifecycleScope.isActive) {
                    withContext(Dispatchers.Main) {
                        binding.loadingSpinner.visibility = View.GONE
                        binding.tvNoResults.visibility = View.VISIBLE
                        Toast.makeText(this@SearchActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
