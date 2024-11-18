package com.example.reelapp

import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract.CommonDataKinds.Website.URL
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.reelapp.databinding.FragmentReelBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.bumptech.glide.Glide
import com.example.reelapp.R
import android.provider.ContactsContract.CommonDataKinds.Website.URL


import com.google.firebase.database.ValueEventListener
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class ReelFragment : Fragment() {
    private var _binding: FragmentReelBinding? = null
    private val binding get() = _binding!!
    private var player: ExoPlayer? = null
    private var videoItem: VideoItem? = null
    private var isLiked = false
    private var isMuted = false  // Track mute state
    private var isDoubleTap = false  // Flag to prevent conflict


    private lateinit var database: DatabaseReference

    companion object {
        private const val ARG_VIDEO_ITEM = "video_item"

        fun newInstance(videoItem: VideoItem): ReelFragment {
            val fragment = ReelFragment()
            val args = Bundle()
            args.putParcelable(ARG_VIDEO_ITEM, videoItem)
            fragment.arguments = args
            return fragment
        }
    }

    // Get instance of firebase real time database
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            videoItem = it.getParcelable(ARG_VIDEO_ITEM)
        }
        database = FirebaseDatabase.getInstance("https://reel-view-app-default-rtdb.firebaseio.com/").reference.child("videos")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPlayer()
        setupLikeButton()
        setupDownloadButton()
        updateLikeCountDisplay()
        fetchProfileData()
// Initialize GestureDetector for detecting double taps
        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                // Action to perform on double tap (Like/Unlike)
                isLiked = !isLiked
                val drawableResId = if (isLiked) {
                    requireContext().resources.getIdentifier("ic_favorite", "drawable", requireContext().packageName)
                } else {
                    requireContext().resources.getIdentifier("ic_favorite_border", "drawable", requireContext().packageName)
                }
                binding.likeButton.setImageResource(drawableResId)
                binding.liked.setImageResource(drawableResId)

                // Reset after 2 seconds
                binding.liked.postDelayed({
                    binding.liked.setImageResource(0)
                }, 1000)

                updateLikeCount(isLiked)
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                // Action to perform on single tap (Mute/Unmute)
                isMuted = !isMuted
                player?.volume = if (isMuted) 0f else 1f  // Mute or unmute
                val muteIconResId = if (isMuted) {
                    requireContext().resources.getIdentifier("ic_volume_off", "drawable", requireContext().packageName)
                } else {
                    requireContext().resources.getIdentifier("ic_volume_on", "drawable", requireContext().packageName)
                }

                binding.mute.setImageResource(muteIconResId)

                // Reset after 2 seconds
                binding.mute.postDelayed({
                    binding.mute.setImageResource(0)
                }, 2000)

                return true
            }
        })

        binding.root.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }



    }

    private fun setupDownloadButton() {
        binding.download.setOnClickListener {
            videoItem?.url?.let { videoUrl ->
                thread {
                    try {
                        val url = URL(videoUrl)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.connect()

                        val inputStream = connection.inputStream
                        val file = File("/storage/emulated/0/Download/downloaded_video.mp4") ;                        val outputStream = FileOutputStream(file)

                        val buffer = ByteArray(1024)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }

                        outputStream.close()
                        inputStream.close()
                        connection.disconnect()

                        requireActivity().runOnUiThread {
                            binding.download.setImageResource(R.drawable.download_sucess)
                        }
                    } catch (e: Exception) {
                        Log.e("Download", "Error downloading video: ${e.message}")
                    }
                }
            }
        }
    }

    private fun fetchProfileData() {
        videoItem?.url?.let { url ->
            database.orderByChild("url").equalTo(url)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (data in snapshot.children) {
                                val username = data.child("username").value as? String
                                val description = data.child("description").value as? String
                                val profilePicUrl = data.child("profilePicUrl").value as? String

                                // Set description
                                val descriptionTextView: TextView? = view?.findViewById(R.id.descp)
                                descriptionTextView?.text = description ?: "No description available"

                                // Set username
                                val usernameTextView: TextView? = view?.findViewById(R.id.user)
                                usernameTextView?.text = username ?: "No username available"

                                // Set profile picture using Glide (handling nullable ImageView)
                                val profilePicImageView: ImageView = view?.findViewById(R.id.pp) ?: return
                                Glide.with(requireContext())
                                    .load(profilePicUrl) // Load the profile picture from the URL
                                    .circleCrop() // Crop it into a circle
                                    .into(profilePicImageView)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Firebase", "Error fetching profile data: ${error.message}")
                    }
                })
        }
    }


    // Setup  the Exoplayer instance
    private fun setupPlayer() {
        player = ExoPlayer.Builder(requireContext()).build()
        binding.playerView.player = player

        videoItem?.url?.let { url ->
            val mediaItem = MediaItem.fromUri(url)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true
        }
    }

    private fun setupLikeButton() {
        binding.likeButton.setOnClickListener {
            isLiked = !isLiked

            // Use getIdentifier to dynamically fetch the drawable resource ID
            val drawableResId = if (isLiked) {
                requireContext().resources.getIdentifier("ic_favorite", "drawable", requireContext().packageName)
            } else {
                requireContext().resources.getIdentifier("ic_favorite_border", "drawable", requireContext().packageName)
            }

            // Set the drawable resource ID to the like button's image
            binding.likeButton.setImageResource(drawableResId)
// Set the mute icon immediately
            binding.liked.setImageResource(drawableResId)

            // After 2 seconds, set the image to none (or reset it)
            binding.liked.postDelayed({
                binding.liked.setImageResource(0) // Clears the image (sets to default)
            }, 1000) // 2 seconds delay

            updateLikeCount(isLiked)
        }
    }

    private fun updateLikeCount(isLiked: Boolean) {
        videoItem?.let { item ->
            val newLikeCount = if (isLiked) item.likeCount + 1 else maxOf(0, item.likeCount - 1)
            videoItem = item.copy(likeCount = newLikeCount)
            updateLikeCountDisplay()
            updateLikeCountInDatabase(newLikeCount)
        }
    }

    private fun updateLikeCountInDatabase(newLikeCount: Int) {
        videoItem?.url?.let { url ->
            database.child("videos").orderByChild("url").equalTo(url)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val videoRef = snapshot.children.first().ref
                            videoRef.child("likeCount").setValue(newLikeCount)
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Firebase", "Error updating like count: ${error.message}")
                    }
                })
        }
    }

    private fun updateLikeCountDisplay() {
        binding.likeCountText.text = videoItem?.likeCount?.toString() ?: "0"
    }

    fun pauseVideo() {
        player?.pause()
    }

    fun resumeVideo() {
        player?.play()
    }

    override fun onPause() {
        super.onPause()
        pauseVideo()
    }

    override fun onResume() {
        super.onResume()
        resumeVideo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
        player = null
        _binding = null
    }
}