package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.adapter.MultiSelectCategoryAdapter
import com.example.myapplication.data.CafeRepository
import com.example.myapplication.data.ReviewRepository
import com.example.myapplication.data.UserSessionManager
import com.example.myapplication.model.Cafe
import com.example.myapplication.databinding.FragmentCheckInBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.ArrayList

class CheckIn : Fragment() {

    private var cafe: Cafe? = null
    private var currentPhotoFile: File? = null
    private var currentPhotoUri: Uri? = null
    private var currentPhotoPath: String? = null

    private var previousPhotoFile: File? = null
    private var previousPhotoUri: Uri? = null
    private var previousPhotoPath: String? = null

    private var tagAdapter: MultiSelectCategoryAdapter? = null
    private var tagLoadJob: Job? = null
    private var submitJob: Job? = null
    private var restoredTagIds: List<String> = emptyList()

    private var _binding: FragmentCheckInBinding? = null
    private val binding: FragmentCheckInBinding
        get() = _binding ?: throw IllegalStateException("Binding accessed after view destroyed")

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            previousPhotoFile?.let { if (it.exists()) it.delete() }
            previousPhotoFile = null
            previousPhotoUri = null
            previousPhotoPath = null
            currentPhotoUri?.let {
                showPhotoPreview(it)
            } ?: run {
                Toast.makeText(requireContext(), R.string.review_capture_failed, Toast.LENGTH_SHORT).show()
                clearPhoto(deleteFile = true)
            }
        } else {
            currentPhotoFile?.let { if (it.exists()) it.delete() }
            currentPhotoFile = previousPhotoFile
            currentPhotoUri = previousPhotoUri
            currentPhotoPath = previousPhotoPath
            previousPhotoFile = null
            previousPhotoUri = null
            previousPhotoPath = null
            if (currentPhotoUri != null) {
                currentPhotoUri?.let { showPhotoPreview(it) }
            } else {
                clearPhoto(deleteFile = false)
            }
            Toast.makeText(requireContext(), R.string.review_capture_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), R.string.review_camera_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cafe = arguments?.getSerializable(ARG_CAFE) as? Cafe
        if (savedInstanceState != null) {
            currentPhotoPath = savedInstanceState.getString(STATE_PHOTO_PATH)
            restoredTagIds = savedInstanceState.getStringArrayList(STATE_SELECTED_TAGS)?.toList().orEmpty()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cafeName.text = cafe?.name ?: getString(R.string.review_unknown_cafe)

        binding.cameraBlock.setOnClickListener { handleCameraClick() }
        binding.cameraBlock.contentDescription = getString(R.string.camera_tagline)

        setupCategoriesRecycler()
        restorePhotoIfNeeded()

        binding.postBtn.setOnClickListener { submitReview() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentPhotoPath?.let { outState.putString(STATE_PHOTO_PATH, it) }
        val selectedIds = tagAdapter?.getSelectedTagIds()
        if (!selectedIds.isNullOrEmpty()) {
            outState.putStringArrayList(STATE_SELECTED_TAGS, ArrayList(selectedIds))
        }
    }

    override fun onDestroyView() {
        tagLoadJob?.cancel()
        submitJob?.cancel()
        tagAdapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun setupCategoriesRecycler() {
        val binding = _binding ?: return
        val recycler = binding.categoriesRecycle
        recycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val adapter = MultiSelectCategoryAdapter(
            itemLayout = R.layout.item_category_button_small,
            maxSelection = MAX_TAG_SELECTION,
            onSelectionChanged = { /* no-op */ },
            onSelectionLimitReached = { showTagLimitWarning() }
        )
        tagAdapter = adapter
        recycler.adapter = adapter

        tagLoadJob?.cancel()
        tagLoadJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tags = CafeRepository.getAllTags()
                adapter.submitList(tags)
                if (restoredTagIds.isNotEmpty()) {
                    adapter.setSelectedTagIds(restoredTagIds)
                    restoredTagIds = emptyList()
                }
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to load review tags", error)
                Toast.makeText(requireContext(), R.string.review_tags_fetch_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleCameraClick() {
        val context = context ?: return
        val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val context = context ?: return
        val file = try {
            createImageFile()
        } catch (error: IOException) {
            Log.e(TAG, "Unable to create image file", error)
            Toast.makeText(context, R.string.review_capture_failed, Toast.LENGTH_SHORT).show()
            return
        }

        val authority = fileProviderAuthority()
        val uri = FileProvider.getUriForFile(context, authority, file)

        previousPhotoFile = currentPhotoFile
        previousPhotoUri = currentPhotoUri
        previousPhotoPath = currentPhotoPath

        currentPhotoFile = file
        currentPhotoUri = uri
        currentPhotoPath = file.absolutePath

        takePictureLauncher.launch(uri)
    }

    private fun showPhotoPreview(uri: Uri) {
        val binding = _binding ?: return
        binding.cameraPreview.isVisible = true
        binding.cameraPreview.setImageURI(null)
        binding.cameraPreview.setImageURI(uri)
        binding.cameraPromptContainer.isVisible = false
        binding.cameraTaglineText.text = getString(R.string.review_retake_hint)
        binding.cameraBlock.contentDescription = getString(R.string.review_retake_hint)
    }

    private fun clearPhoto(deleteFile: Boolean = true) {
        if (deleteFile) {
            currentPhotoFile?.let { if (it.exists()) it.delete() }
        }
        currentPhotoFile = null
        currentPhotoUri = null
        currentPhotoPath = null
        previousPhotoFile = null
        previousPhotoUri = null
        previousPhotoPath = null
        val binding = _binding ?: return
        binding.cameraPreview.setImageDrawable(null)
        binding.cameraPreview.isVisible = false
        binding.cameraPromptContainer.isVisible = true
        binding.cameraTaglineText.text = getString(R.string.camera_tagline)
        binding.cameraBlock.contentDescription = getString(R.string.camera_tagline)
    }

    private fun restorePhotoIfNeeded() {
        val path = currentPhotoPath ?: return
        val file = File(path)
        if (!file.exists()) {
            currentPhotoPath = null
            return
        }
        currentPhotoFile = file
        val authority = fileProviderAuthority()
        currentPhotoUri = FileProvider.getUriForFile(requireContext(), authority, file)
        currentPhotoUri?.let { showPhotoPreview(it) }
    }

    private fun submitReview() {
        val cafe = cafe
        if (cafe == null) {
            Toast.makeText(requireContext(), R.string.review_select_cafe_first, Toast.LENGTH_SHORT).show()
            return
        }

        val userId = UserSessionManager.getUserId(requireContext())
        if (userId.isNullOrBlank()) {
            Toast.makeText(requireContext(), R.string.review_login_required, Toast.LENGTH_SHORT).show()
            return
        }

        val binding = _binding ?: return
        val ratingValue = binding.ratingBar.rating
        if (ratingValue <= 0f) {
            Toast.makeText(requireContext(), R.string.review_rating_required, Toast.LENGTH_SHORT).show()
            return
        }

        val commentText = binding.reviewInput.text?.toString()?.trim().orEmpty()
        val selectedTagIds = tagAdapter?.getSelectedTagIds().orEmpty()
        val photoFile = currentPhotoFile

        setLoading(true)
        submitJob?.cancel()
        submitJob = viewLifecycleOwner.lifecycleScope.launch {
            val imageUrl = if (photoFile != null) {
                try {
                    ReviewRepository.uploadReviewImage(photoFile)
                } catch (error: Throwable) {
                    Log.e(TAG, "Failed to upload review photo", error)
                    Toast.makeText(requireContext(), R.string.review_upload_error, Toast.LENGTH_SHORT).show()
                    setLoading(false)
                    return@launch
                }
            } else {
                null
            }

            try {
                val reviewId = ReviewRepository.createReview(
                    userId = userId,
                    cafeId = cafe.cafe_id,
                    comment = commentText.takeIf { it.isNotBlank() },
                    rating = ratingValue.toDouble(),
                    imageUrl = imageUrl
                )
                if (selectedTagIds.isNotEmpty()) {
                    ReviewRepository.attachTags(reviewId, selectedTagIds)
                }
                Toast.makeText(requireContext(), R.string.review_submit_success, Toast.LENGTH_SHORT).show()
                clearForm()
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to submit review", error)
                Toast.makeText(requireContext(), R.string.review_submit_error, Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun clearForm() {
        val binding = _binding ?: return
        binding.reviewInput.text?.clear()
        binding.ratingBar.rating = 0f
        tagAdapter?.clearSelection()
        clearPhoto(deleteFile = true)
    }

    private fun showTagLimitWarning() {
        Toast.makeText(requireContext(), R.string.review_category_limit_warning, Toast.LENGTH_SHORT).show()
    }

    private fun setLoading(loading: Boolean) {
        val binding = _binding ?: return
        binding.postBtn.isEnabled = !loading
        binding.postProgress.isVisible = loading
    }

    private fun fileProviderAuthority(): String = "${requireContext().packageName}.fileprovider"

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: requireContext().filesDir
        return File.createTempFile("review_${System.currentTimeMillis()}_", ".jpg", storageDir)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraBlock = view.findViewById(R.id.camera_block)
        cameraPromptContainer = view.findViewById(R.id.camera_prompt_container)
        cameraPreview = view.findViewById(R.id.camera_preview)
        cameraTagline = view.findViewById(R.id.camera_tagline_text)
        reviewInput = view.findViewById(R.id.review_input)
        ratingBar = view.findViewById(R.id.rating_bar)
        postButton = view.findViewById(R.id.post_btn)
        progressBar = view.findViewById(R.id.post_progress)

        val cafeNameView = view.findViewById<TextView>(R.id.cafe_name)
        cafeNameView.text = cafe?.name ?: getString(R.string.review_unknown_cafe)

        cameraBlock?.setOnClickListener { handleCameraClick() }
        cameraBlock?.contentDescription = getString(R.string.camera_tagline)

        setupCategoriesRecycler(view)
        restorePhotoIfNeeded()

        postButton?.setOnClickListener { submitReview() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentPhotoPath?.let { outState.putString(STATE_PHOTO_PATH, it) }
        val selectedIds = tagAdapter?.getSelectedTagIds()
        if (!selectedIds.isNullOrEmpty()) {
            outState.putStringArrayList(STATE_SELECTED_TAGS, ArrayList(selectedIds))
        }
    }

    override fun onDestroyView() {
        tagLoadJob?.cancel()
        submitJob?.cancel()
        cameraBlock = null
        cameraPromptContainer = null
        cameraPreview = null
        cameraTagline = null
        reviewInput = null
        ratingBar = null
        postButton = null
        progressBar = null
        tagAdapter = null
        super.onDestroyView()
    }

    private fun setupCategoriesRecycler(root: View) {
        val recycler = root.findViewById<RecyclerView>(R.id.categories_recycle)
        recycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val adapter = MultiSelectCategoryAdapter(
            itemLayout = R.layout.item_category_button_small,
            maxSelection = MAX_TAG_SELECTION,
            onSelectionChanged = { /* no-op */ },
            onSelectionLimitReached = { showTagLimitWarning() }
        )
        tagAdapter = adapter
        recycler.adapter = adapter

        tagLoadJob?.cancel()
        tagLoadJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tags = CafeRepository.getAllTags()
                adapter.submitList(tags)
                if (restoredTagIds.isNotEmpty()) {
                    adapter.setSelectedTagIds(restoredTagIds)
                    restoredTagIds = emptyList()
                }
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to load review tags", error)
                Toast.makeText(requireContext(), R.string.review_tags_fetch_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleCameraClick() {
        val context = context ?: return
        val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val context = context ?: return
        val file = try {
            createImageFile()
        } catch (error: IOException) {
            Log.e(TAG, "Unable to create image file", error)
            Toast.makeText(context, R.string.review_capture_failed, Toast.LENGTH_SHORT).show()
            return
        }

        val authority = fileProviderAuthority()
        val uri = FileProvider.getUriForFile(context, authority, file)

        previousPhotoFile = currentPhotoFile
        previousPhotoUri = currentPhotoUri
        previousPhotoPath = currentPhotoPath

        currentPhotoFile = file
        currentPhotoUri = uri
        currentPhotoPath = file.absolutePath

        takePictureLauncher.launch(uri)
    }

    private fun showPhotoPreview(uri: Uri) {
        cameraPreview?.isVisible = true
        cameraPreview?.setImageURI(null)
        cameraPreview?.setImageURI(uri)
        cameraPromptContainer?.isVisible = false
        cameraTagline?.text = getString(R.string.review_retake_hint)
        cameraBlock?.contentDescription = getString(R.string.review_retake_hint)
    }

    private fun clearPhoto(deleteFile: Boolean = true) {
        if (deleteFile) {
            currentPhotoFile?.let { if (it.exists()) it.delete() }
        }
        currentPhotoFile = null
        currentPhotoUri = null
        currentPhotoPath = null
        previousPhotoFile = null
        previousPhotoUri = null
        previousPhotoPath = null
        cameraPreview?.setImageDrawable(null)
        cameraPreview?.isVisible = false
        cameraPromptContainer?.isVisible = true
        cameraTagline?.text = getString(R.string.camera_tagline)
        cameraBlock?.contentDescription = getString(R.string.camera_tagline)
    }

    private fun restorePhotoIfNeeded() {
        val path = currentPhotoPath ?: return
        val file = File(path)
        if (!file.exists()) {
            currentPhotoPath = null
            return
        }
        currentPhotoFile = file
        val authority = fileProviderAuthority()
        currentPhotoUri = FileProvider.getUriForFile(requireContext(), authority, file)
        currentPhotoUri?.let { showPhotoPreview(it) }
    }

    private fun submitReview() {
        val cafe = cafe
        if (cafe == null) {
            Toast.makeText(requireContext(), R.string.review_select_cafe_first, Toast.LENGTH_SHORT).show()
            return
        }

        val userId = UserSessionManager.getUserId(requireContext())
        if (userId.isNullOrBlank()) {
            Toast.makeText(requireContext(), R.string.review_login_required, Toast.LENGTH_SHORT).show()
            return
        }

        val ratingValue = ratingBar?.rating ?: 0f
        if (ratingValue <= 0f) {
            Toast.makeText(requireContext(), R.string.review_rating_required, Toast.LENGTH_SHORT).show()
            return
        }

        val commentText = reviewInput?.text?.toString()?.trim().orEmpty()
        val selectedTagIds = tagAdapter?.getSelectedTagIds().orEmpty()
        val photoFile = currentPhotoFile

        setLoading(true)
        submitJob?.cancel()
        submitJob = viewLifecycleOwner.lifecycleScope.launch {
            val imageUrl = if (photoFile != null) {
                try {
                    ReviewRepository.uploadReviewImage(photoFile)
                } catch (error: Throwable) {
                    Log.e(TAG, "Failed to upload review photo", error)
                    Toast.makeText(requireContext(), R.string.review_upload_error, Toast.LENGTH_SHORT).show()
                    setLoading(false)
                    return@launch
                }
            } else {
                null
            }

            try {
                val reviewId = ReviewRepository.createReview(
                    userId = userId,
                    cafeId = cafe.cafe_id,
                    comment = commentText.takeIf { it.isNotBlank() },
                    rating = ratingValue.toDouble(),
                    imageUrl = imageUrl
                )
                if (selectedTagIds.isNotEmpty()) {
                    ReviewRepository.attachTags(reviewId, selectedTagIds)
                }
                Toast.makeText(requireContext(), R.string.review_submit_success, Toast.LENGTH_SHORT).show()
                clearForm()
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to submit review", error)
                Toast.makeText(requireContext(), R.string.review_submit_error, Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun clearForm() {
        reviewInput?.text?.clear()
        ratingBar?.rating = 0f
        tagAdapter?.clearSelection()
        clearPhoto(deleteFile = true)
    }

    private fun showTagLimitWarning() {
        Toast.makeText(requireContext(), R.string.review_category_limit_warning, Toast.LENGTH_SHORT).show()
    }

    private fun setLoading(loading: Boolean) {
        postButton?.isEnabled = !loading
        progressBar?.isVisible = loading
    }

    private fun fileProviderAuthority(): String = "${requireContext().packageName}.fileprovider"

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: requireContext().filesDir
        return File.createTempFile("review_${System.currentTimeMillis()}_", ".jpg", storageDir)
    }

    companion object {
        private const val ARG_CAFE = "arg_cafe"
        private const val STATE_PHOTO_PATH = "state_photo_path"
        private const val STATE_SELECTED_TAGS = "state_selected_tags"
        private const val MAX_TAG_SELECTION = 2
        private const val TAG = "CheckIn"

        fun newInstance(cafe: Cafe): CheckIn {
            return CheckIn().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CAFE, cafe)
                }
            }
        }
    }
}
