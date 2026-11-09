package com.example.ui.form

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class FormLaporanAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 4 // Kita siapkan 4 Tab

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FormTab1Fragment()
            1 -> FormTab2Fragment()
            2 -> FormTab3Fragment()
            3 -> FormTab4Fragment()
            else -> FormTab1Fragment() // Sementara fallback ke Tab 1
        }
    }
}
