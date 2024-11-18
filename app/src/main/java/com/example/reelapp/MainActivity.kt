package com.example.reelapp

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import com.example.reelapp.databinding.ActivityMainBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : FragmentActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var reelPagerAdapter: ReelPagerAdapter
    private lateinit var database: DatabaseReference
    private val videoUrls = mutableListOf<VideoItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkGooglePlayServices()
        database = FirebaseDatabase.getInstance("https://reel-view-app-default-rtdb.firebaseio.com/").reference.child("videos")
        fetchVideosFromFirebase()
    }

    override fun onResume() {
        super.onResume()
        getCurrentReelFragment()?.resumeVideo()
    }

    override fun onPause() {
        super.onPause()
        getCurrentReelFragment()?.pauseVideo()
    }

    private fun getCurrentReelFragment(): ReelFragment? {
        val currentItem = binding.viewPager.currentItem
        return supportFragmentManager.findFragmentByTag("f$currentItem") as? ReelFragment
    }

    private fun checkGooglePlayServices() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)

        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, 9000)?.show()
            } else {
                Log.e("PlayServices", "This device is not supported")
                finish()
            }
        } else {
            Log.d("PlayServices", "Google Play Services is available")
        }
    }

    private fun fetchVideosFromFirebase() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("Firebase", "Data fetch started")
                videoUrls.clear()

                if (snapshot.exists()) {
                    Log.d("Firebase", "Snapshot exists with ${snapshot.childrenCount} children")
                    for (videoSnapshot in snapshot.children) {
                        val url = videoSnapshot.child("url").getValue(String::class.java)
                        val likeCount = videoSnapshot.child("likeCount").getValue(Int::class.java) ?: 0
                        val profilePicUrl = videoSnapshot.child("profilePicUrl").getValue(String::class.java)
                        val description = videoSnapshot.child("description").getValue(String::class.java)
                        val username = videoSnapshot.child("username").getValue(String::class.java)

                        if (url != null) {
                            // Ensure that the URL is passed as the first parameter (with the likeCount as the second)
                            if (url != null) {
                                videoUrls.add(
                                    VideoItem(
                                        url = url,
                                        likeCount = likeCount,
                                        profilePicUrl = profilePicUrl,
                                        description = description,
                                        username = username
                                    )
                                )
                                Log.d(
                                    "Firebase",
                                    "Added URL: $url, like count: $likeCount, profilePicUrl: $profilePicUrl, description: $description, username: $username"
                                )
                            }

                        }
                    }
                    setupViewPager()
                    Log.d("Firebase", "Total URLs fetched: ${videoUrls.size}")
                } else {
                    Log.d("Firebase", "Snapshot does not exist or is empty")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching data: ${error.message}")
            }
        })
    }

    private fun setupViewPager() {
        if (videoUrls.isNotEmpty()) {
            reelPagerAdapter = ReelPagerAdapter(this, videoUrls)
            binding.viewPager.adapter = reelPagerAdapter
            Log.d("ViewPager", "ViewPager setup complete with ${videoUrls.size} items")
        } else {
            Log.d("ViewPager", "No items to display in ViewPager")
        }
    }
}