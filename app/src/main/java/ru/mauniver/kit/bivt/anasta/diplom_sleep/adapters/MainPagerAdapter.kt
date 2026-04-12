package ru.mauniver.kit.bivt.anasta.diplom_sleep.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.fragments.HomeFragment
import ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.fragments.DiaryFragment
import ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.fragments.AnalyticsFragment
import ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.fragments.SettingsFragment

class MainPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> DiaryFragment()
            2 -> AnalyticsFragment()
            3 -> SettingsFragment()
            else -> HomeFragment()
        }
    }
}