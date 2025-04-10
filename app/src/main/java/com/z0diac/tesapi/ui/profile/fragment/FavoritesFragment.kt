package com.z0diac.tesapi.ui.profile.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.z0diac.tesapi.data.model.Movie1
import com.z0diac.tesapi.databinding.FragmentFavoritesBinding
import com.z0diac.tesapi.ui.adapters.FavoritesAdapter
import com.z0diac.tesapi.ui.dashboard.MovieDetailsActivity

class FavoritesFragment : Fragment() {

    private lateinit var binding: FragmentFavoritesBinding
    private lateinit var adapter: FavoritesAdapter
    private lateinit var detailLauncher: ActivityResultLauncher<Intent>
    private val favorites = mutableListOf<Movie1>()
    private var userId: String? = null

    companion object {
        private const val ARG_USER_ID = "user_id"

        fun newInstance(userId: String): FavoritesFragment {
            val fragment = FavoritesFragment()
            val args = Bundle()
            args.putString(ARG_USER_ID, userId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(ARG_USER_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        detailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && isAdded) {
                loadFavorites()
                parentFragmentManager.setFragmentResult("favorites_updated", Bundle())
            }
        }

        adapter = FavoritesAdapter(favorites) { movie ->
            val intent = Intent(requireContext(), MovieDetailsActivity::class.java).apply {
                putExtra("movie_id", movie.id)
                putExtra("movie_title", movie.title)
                putExtra("movie_poster", movie.posterPath)
                putExtra("movie_backdrop", movie.backdropPath)
                putExtra("movie_release_date", movie.releaseDate)
                putExtra("movie_rating", movie.rating ?: 0f)
                putExtra("movie_overview", movie.overview)
            }
            detailLauncher.launch(intent)
        }

        binding.recyclerViewFavorites.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerViewFavorites.adapter = adapter

        loadFavorites()
    }

    private fun loadFavorites() {
        val userIdToLoad = userId ?: FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        binding.tvEmptyFavorites.visibility = View.GONE

        db.collection("users").document(userIdToLoad).collection("favorites")
            .get()
            .addOnSuccessListener { result ->
                favorites.clear()
                for (document in result) {
                    try {
                        val id = listOfNotNull(
                            document.getLong("id"),
                            document.getLong("movieId")
                        ).firstOrNull()?.toInt() ?: 0

                        val title = document.getString("title") ?: ""
                        val posterPath = document.getString("posterPath") ?: document.getString("poster_path") ?: ""
                        val backdropPath = document.getString("backdropPath") ?: document.getString("backdrop_path")
                        val releaseDate = document.getString("releaseDate") ?: document.getString("release_date") ?: ""
                        val overview = document.getString("overview") ?: ""
                        val rating = document.getDouble("rating")?.toFloat() ?: document.getDouble("vote_average")?.toFloat() ?: 0f

                        if (posterPath.isNotBlank()) {
                            val movie = Movie1(
                                id = id,
                                title = title,
                                posterPath = posterPath,
                                backdropPath = backdropPath,
                                releaseDate = releaseDate,
                                rating = rating,
                                overview = overview,
                                genres = emptyList()
                            )
                            favorites.add(movie)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (!isAdded) return@addOnSuccessListener

                if (favorites.isEmpty()) {
                    binding.tvEmptyFavorites.visibility = View.VISIBLE
                    binding.recyclerViewFavorites.visibility = View.GONE
                } else {
                    binding.tvEmptyFavorites.visibility = View.GONE
                    binding.recyclerViewFavorites.visibility = View.VISIBLE
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Failed to load favorites: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.tvEmptyFavorites.visibility = View.VISIBLE
                binding.recyclerViewFavorites.visibility = View.GONE
            }
    }

}
