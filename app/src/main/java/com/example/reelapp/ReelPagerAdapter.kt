package com.example.reelapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ReelPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val videoItems: List<VideoItem>
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = videoItems.size

    override fun createFragment(position: Int): Fragment {
        return ReelFragment.newInstance(videoItems[position])
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        return itemId < itemCount
    }
}