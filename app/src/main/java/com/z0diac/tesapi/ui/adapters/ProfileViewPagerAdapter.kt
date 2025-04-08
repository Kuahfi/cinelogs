package com.z0diac.tesapi.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.z0diac.tesapi.profile.fragment.ReviewsFragment
import com.z0diac.tesapi.ui.profile.fragment.FavoritesFragment
import com.z0diac.tesapi.ui.profile.fragment.WatchlistFragment

class ProfileViewPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val userId: String
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ReviewsFragment.newInstance(userId)
            1 -> WatchlistFragment.newInstance(userId)
            2 -> FavoritesFragment.newInstance(userId)
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}