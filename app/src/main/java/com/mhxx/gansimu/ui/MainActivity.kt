package com.mhxx.gansimu.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.mhxx.gansimu.GanSimuApp
import com.mhxx.gansimu.R
import com.mhxx.gansimu.databinding.ActivityMainBinding
import com.mhxx.gansimu.ui.tabs.*

/**
 * メイン画面
 * 元アプリ ui.e のタブ構成を再現
 *  - シミュレータ
 *  - 除外装備設定
 *  - 固定装備設定
 *  - 装飾品除外設定
 *  - お守り設定
 *  - マイセット
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val tabTitles = listOf(
        "シミュレータ",
        "除外装備",
        "固定装備",
        "装飾品除外",
        "お守り",
        "マイセット"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        waitForDataAndSetup()
    }

    private fun waitForDataAndSetup() {
        val progress = binding.progressBar
        progress.visibility = View.VISIBLE
        binding.viewPager.visibility = View.GONE
        binding.tabLayout.visibility = View.GONE

        Thread {
            val repo = GanSimuApp.instance.repository
            var retries = 0
            while (!repo.isLoaded && retries < 100) {
                Thread.sleep(50)
                retries++
            }
            runOnUiThread {
                progress.visibility = View.GONE
                if (!repo.isLoaded) {
                    Toast.makeText(this, "データ読み込みに失敗しました", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                setupTabs()
                binding.viewPager.visibility = View.VISIBLE
                binding.tabLayout.visibility = View.VISIBLE
                Toast.makeText(
                    this,
                    "装備 ${repo.getAllEquipment().size} / スキル ${repo.skills.size} 件読込完了",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.start()
    }

    private fun setupTabs() {
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = tabTitles.size

            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> SimulatorFragment()
                1 -> ExcludeEquipFragment()
                2 -> FixedEquipFragment()
                3 -> ExcludeDecoFragment()
                4 -> CharmFragment()
                5 -> MySetFragment()
                else -> SimulatorFragment()
            }
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }
}
